package com.safecallkids.app.monetization

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object TrialExpiryScheduler {
    const val REMINDER_INPUT_KEY = "trial_reminder"

    fun schedule(context: Context) {
        val appContext = context.applicationContext
        val accessManager = PremiumAccessManager(appContext)
        val workManager = WorkManager.getInstance(appContext)

        if (accessManager.getSnapshot().state == MonetizationState.PREMIUM) {
            TrialReminder.entries.forEach { workManager.cancelUniqueWork(it.uniqueWorkName) }
            return
        }

        val nowMillis = System.currentTimeMillis()
        val trialStartedAt = accessManager.trialStartedAtMillis(nowMillis)
        val latestDueReminder = TrialReminder.entries.lastOrNull {
            nowMillis >= it.dueAtMillis(trialStartedAt)
        }

        TrialReminder.entries.forEach { reminder ->
            if (accessManager.wasTrialReminderHandled(reminder)) return@forEach

            if (latestDueReminder != null && reminder.ordinal < latestDueReminder.ordinal) {
                accessManager.markTrialReminderHandled(reminder)
                workManager.cancelUniqueWork(reminder.uniqueWorkName)
                return@forEach
            }

            val delayMillis = (reminder.dueAtMillis(trialStartedAt) - nowMillis).coerceAtLeast(0L)
            val input = Data.Builder().putString(REMINDER_INPUT_KEY, reminder.name).build()
            val request = OneTimeWorkRequest.Builder(TrialExpiryWorker::class.java)
                .setInputData(input)
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .build()
            workManager.enqueueUniqueWork(reminder.uniqueWorkName, ExistingWorkPolicy.KEEP, request)
        }
    }
}
