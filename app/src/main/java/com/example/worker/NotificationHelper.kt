package com.example.worker

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

object NotificationHelper {
    const val CHANNEL_ID = "MONZ_BREAK_TIME_ALERTS"
    private const val CHANNEL_NAME = "Break Time Notifications"
    private const val ALARM_ID_WARNING = 1001
    private const val ALARM_ID_COMPLETED = 1002

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Alerts regarding break times and deadlines."
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleBreakNotifications(context: Context, username: String, startTime: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        
        // 1. Warning alarm (13 minutes, i.e., 2 minutes before the 15-minute break ends)
        val warningTime = startTime + (13 * 60 * 1000)
        val warningIntent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("title", "Break ending soon!")
            putExtra("message", "Hey $username, you have 2 minutes remaining before your break ends.")
            putExtra("notification_id", ALARM_ID_WARNING)
        }
        val warningPendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_ID_WARNING,
            warningIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Completed alarm (15 minutes exactly)
        val completedTime = startTime + (15 * 60 * 1000)
        val completedIntent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("title", "Break completed!")
            putExtra("message", "Hey $username, your 15-minute break has completed. Please return to work immediately.")
            putExtra("notification_id", ALARM_ID_COMPLETED)
        }
        val completedPendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_ID_COMPLETED,
            completedIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, warningTime, warningPendingIntent)
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, completedTime, completedPendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, warningTime, warningPendingIntent)
                alarmManager.set(AlarmManager.RTC_WAKEUP, completedTime, completedPendingIntent)
            }
            Log.d("NotificationHelper", "Successfully scheduled break notifications for $username starting at $startTime.")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error scheduling exact alarms, falling back to standard scheduling", e)
            alarmManager.set(AlarmManager.RTC_WAKEUP, warningTime, warningPendingIntent)
            alarmManager.set(AlarmManager.RTC_WAKEUP, completedTime, completedPendingIntent)
        }
    }

    fun cancelNotifications(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        
        val warningIntent = Intent(context, NotificationReceiver::class.java)
        val warningPendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_ID_WARNING,
            warningIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val completedIntent = Intent(context, NotificationReceiver::class.java)
        val completedPendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_ID_COMPLETED,
            completedIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(warningPendingIntent)
        alarmManager.cancel(completedPendingIntent)
        Log.d("NotificationHelper", "Cancelled scheduled break notifications.")
    }
}
