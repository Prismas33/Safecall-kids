package com.safecallkids.app.monetization

import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.safecallkids.app.CallBlockingService

class TrialExpiryWorker(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {
    override fun doWork(): Result {
        val accessManager = PremiumAccessManager(applicationContext)
        val reminder = TrialReminder.fromWorkInput(inputData.getString(TrialExpiryScheduler.REMINDER_INPUT_KEY))
        val nowMillis = System.currentTimeMillis()
        if (accessManager.getSnapshot(nowMillis = nowMillis).state == MonetizationState.PREMIUM ||
            accessManager.wasTrialReminderHandled(reminder)
        ) return Result.success()
        if (nowMillis < reminder.dueAtMillis(accessManager.trialStartedAtMillis(nowMillis))) {
            return Result.retry()
        }
        if (!accessManager.shouldSendTrialReminder(reminder)) return Result.success()

        if (TrialExpiryNotifier.show(applicationContext, reminder)) {
            accessManager.markTrialReminderHandled(reminder)
            applicationContext.sendBroadcast(
                Intent(CallBlockingService.ACTION_UPDATE_NOTIFICATION).setPackage(applicationContext.packageName)
            )
        }
        return Result.success()
    }
}
