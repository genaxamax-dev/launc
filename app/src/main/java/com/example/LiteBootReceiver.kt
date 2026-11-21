package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.AlarmManager
import android.app.PendingIntent
import android.os.SystemClock
import android.os.Handler
import android.os.Looper
import android.util.Log

/** Opens Yandex Music Lite after boot, leaving the launcher as the HOME app. */
class LiteBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ADAYO_BOOT_COMPLETED -> {
                Log.i(TAG, "Adayo source restore completed; scheduling Lite after it")
                scheduleLaunch(context, AFTER_ADAYO_DELAY_MS)
            }
            ACTION_LAUNCH_LITE -> launchLite(context)
        }
    }

    private fun scheduleLaunch(context: Context, delayMs: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val launchPendingIntent = PendingIntent.getBroadcast(
            context,
            LAUNCH_REQUEST_CODE,
            Intent(context, LiteBootReceiver::class.java).setAction(ACTION_LAUNCH_LITE),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.setExact(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + delayMs,
            launchPendingIntent
        )
    }

    private fun launchLite(context: Context) {
        try {
            val launchIntent = context.packageManager
                .getLaunchIntentForPackage(LITE_PACKAGE)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

            if (launchIntent == null) {
                Log.i(TAG, "Yandex Music Lite is not installed; keeping launcher in HOME")
            } else {
                context.startActivity(launchIntent)
                Log.i(TAG, "Started Yandex Music Lite after source restoration")
            }
        } catch (error: Exception) {
            Log.e(TAG, "Could not start Yandex Music Lite after boot", error)
        }
    }

    private companion object {
        const val TAG = "LiteBootReceiver"
        const val LITE_PACKAGE = "YandexMusicLite.KorenskoyR"
        const val ADAYO_BOOT_COMPLETED = "android.intent.action.ADAYO_BOOT_COMPLETED"
        const val ACTION_LAUNCH_LITE = "com.example.action.LAUNCH_LITE_AFTER_BOOT"
        const val LAUNCH_REQUEST_CODE = 7301
        const val AFTER_ADAYO_DELAY_MS = 2_000L
    }
}
