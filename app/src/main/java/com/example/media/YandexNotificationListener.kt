package com.example.media

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class YandexNotificationListener : NotificationListenerService() {

    companion object {
        private const val YANDEX_MUSIC_PKG = "ru.yandex.music"

        private val _connected = MutableStateFlow(false)
        val connected: StateFlow<Boolean> = _connected.asStateFlow()

        @Volatile
        var instance: YandexNotificationListener? = null
            private set

        fun component(context: Context): ComponentName =
            ComponentName(context, YandexNotificationListener::class.java)
    }

    override fun onListenerConnected() {
        instance = this
        _connected.value = true
    }

    override fun onListenerDisconnected() {
        instance = null
        _connected.value = false
    }

    override fun onDestroy() {
        instance = null
        _connected.value = false
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null || sbn.packageName != YANDEX_MUSIC_PKG) return
        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        MediaIntegrationManager.onYandexNotification(title, text)
    }
}
