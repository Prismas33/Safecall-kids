package com.safecallkids.app.monetization

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.safecallkids.app.MainActivity
import com.safecallkids.app.R

object TrialExpiryNotifier {
    private const val CHANNEL_ID = "trial_expiry"

    fun dismissAll(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        TrialReminder.entries.forEach { manager.cancel(it.notificationId) }
    }

    fun show(context: Context, reminder: TrialReminder): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false

        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val platformManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.trial_expired_notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            platformManager.createNotificationChannel(channel)
            if (platformManager.getNotificationChannel(CHANNEL_ID)?.importance ==
                NotificationManager.IMPORTANCE_NONE
            ) return false
        }

        val openAppIntent = PendingIntent.getActivity(
            context,
            reminder.notificationId,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = when (reminder) {
            TrialReminder.EXPIRED -> R.string.trial_expired_dialog_title
            TrialReminder.AFTER_THREE_DAYS -> R.string.trial_reminder_three_days_title
            TrialReminder.AFTER_SEVEN_DAYS -> R.string.trial_reminder_seven_days_title
        }
        val message = when (reminder) {
            TrialReminder.EXPIRED -> R.string.trial_expired_notification_message
            TrialReminder.AFTER_THREE_DAYS -> R.string.trial_reminder_three_days_message
            TrialReminder.AFTER_SEVEN_DAYS -> R.string.trial_reminder_seven_days_message
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(title))
            .setContentText(context.getString(message))
            .setContentIntent(openAppIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        return try {
            notificationManager.notify(reminder.notificationId, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
