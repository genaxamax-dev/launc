package com.example.data

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.model.AppCategory
import com.example.model.CarActionKind
import com.example.model.HavalCarAction
import com.example.model.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {

    private val pm: PackageManager = context.packageManager

    fun getHavalCarActions(): List<HavalCarAction> = listOf(
        HavalCarAction(
            id = "climate",
            title = "Климат (AC)",
            subtitle = "Кондиционер и печка",
            kind = CarActionKind.CLIMATE,
            primaryPackage = "com.adayo.ac",
            primaryActivity = "com.adayo.ac.AcMainActivity"
        ),
        HavalCarAction(
            id = "radio",
            title = "Радио FM/AM",
            subtitle = "Штатный тюнер Adayo",
            kind = CarActionKind.RADIO,
            primaryPackage = "com.adayo.ora.radio",
            primaryActivity = "com.adayo.ui.main.Radio_MainActivity",
            fallbackComponents = listOf(
                "com.android.car.radio" to "com.android.car.radio.CarRadioActivity"
            )
        ),
        HavalCarAction(
            id = "avm360",
            title = "Камера 360°",
            subtitle = "Круговой обзор AVM",
            kind = CarActionKind.AVM_360,
            primaryPackage = "com.adayo.avm",
            primaryActivity = "com.adayo.avm.AvmMainActivity"
        ),
        HavalCarAction(
            id = "dvr",
            title = "Регистратор",
            subtitle = "Штатный DVR",
            kind = CarActionKind.DVR,
            primaryPackage = "com.adayo.dvr",
            primaryActivity = "com.adayo.dvr.DvrMainActivity"
        ),
        HavalCarAction(
            id = "bluetooth",
            title = "Bluetooth",
            subtitle = "Устройства и аудио",
            kind = CarActionKind.BLUETOOTH,
            primaryPackage = "com.adayo.bluetooth",
            primaryActivity = "com.adayo.bluetooth.ui.BT_MainActivity",
            fallbackIntentAction = Settings.ACTION_BLUETOOTH_SETTINGS
        ),
        HavalCarAction(
            id = "car_settings",
            title = "Настройки авто",
            subtitle = "Бортовые параметры",
            kind = CarActionKind.CAR_SETTINGS,
            primaryPackage = "com.adayo.adayocarsettings",
            primaryActivity = "com.adayo.adayocarsettings.CarSettingsActivity",
            fallbackComponents = listOf(
                "com.android.car.settings" to "com.android.car.settings.common.CarSettingActivity",
                "com.adayo.settings" to "com.adayo.settings.Settings_MainActivity"
            ),
            fallbackIntentAction = Settings.ACTION_SETTINGS
        )
    )

    fun getClimateAction(): HavalCarAction = HavalCarAction(
        id = "climate",
        title = "Климат (AC)",
        subtitle = "Кондиционер и печка",
        kind = CarActionKind.CLIMATE,
        primaryPackage = "com.adayo.ac",
        primaryActivity = "com.adayo.ac.AcMainActivity"
    )

    fun getRadioAction(): HavalCarAction = HavalCarAction(
        id = "radio",
        title = "Радио FM/AM",
        subtitle = "Штатный тюнер Adayo",
        kind = CarActionKind.RADIO,
        primaryPackage = "com.adayo.ora.radio",
        primaryActivity = "com.adayo.ui.main.Radio_MainActivity",
        fallbackComponents = listOf(
            "com.android.car.radio" to "com.android.car.radio.CarRadioActivity"
        )
    )

    fun getUsbMediaAction(): HavalCarAction = HavalCarAction(
        id = "usb_media",
        title = "Медиа USB/BT",
        subtitle = "Штатный плеер Adayo",
        kind = CarActionKind.USB_MEDIA,
        primaryPackage = "com.adayo.media",
        primaryActivity = "com.adayo.ui.main.Media_MainActivity",
        fallbackIntentAction = Intent.ACTION_MAIN
    )

    fun getPhoneAction(): HavalCarAction = HavalCarAction(
        id = "phone",
        title = "Телефон",
        subtitle = "Звонки и контакты",
        kind = CarActionKind.PHONE,
        primaryPackage = "com.android.car.dialer",
        primaryActivity = "com.android.car.dialer.TelecomActivity",
        fallbackIntentAction = Intent.ACTION_DIAL
    )

    suspend fun loadInstalledApps(pinnedPackages: Set<String>): List<InstalledApp> =
        withContext(Dispatchers.IO) {
            val queryIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(
                    queryIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(queryIntent, 0)
            }

            val ownPackage = context.packageName
            resolveInfos
                .asSequence()
                .filter { it.activityInfo != null && it.activityInfo.packageName != ownPackage }
                .distinctBy { it.activityInfo.packageName to it.activityInfo.name }
                .map { ri ->
                    val pkg = ri.activityInfo.packageName
                    val cls = ri.activityInfo.name
                    val label = runCatching { ri.loadLabel(pm).toString() }
                        .getOrDefault(pkg)
                        .trim()
                        .ifEmpty { pkg }
                    val icon = runCatching { ri.loadIcon(pm) }.getOrNull()
                    InstalledApp(
                        packageName = pkg,
                        activityName = cls,
                        label = label,
                        icon = icon,
                        category = categorizeApp(pkg, label),
                        isPinned = pinnedPackages.contains(pkg)
                    )
                }
                .sortedWith(
                    compareByDescending<InstalledApp> { it.packageName == "ru.yandex.music" }
                        .thenByDescending { it.isPinned }
                        .thenBy { it.label.lowercase() }
                )
                .toList()
        }

    fun observePackageChanges(): Flow<Unit> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                trySend(Unit)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        awaitClose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    fun launchApp(packageName: String, activityName: String? = null): Result<Unit> {
        return runCatching {
            val intent = if (!activityName.isNullOrBlank()) {
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    component = ComponentName(packageName, activityName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
            } else {
                pm.getLaunchIntentForPackage(packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                } ?: error("Приложение не найдено")
            }
            context.startActivity(intent)
        }.recoverCatching {
            val fallback = pm.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            } ?: throw it
            context.startActivity(fallback)
        }
    }

    fun launchAndroidAuto(): Result<Unit> = runCatching {
        // Открываем видимое окно Open Headunit. Раньше здесь был фоновый
        // ACTION_CONNECT -> AutomationActivity: он сразу закрывался, если
        // телефон не был в сети, и казалось, что кнопка не работает.
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(
                ComponentName(
                    "com.andrerinas.headunitrevived",
                    "com.andrerinas.openheadunit.main.MainActivity"
                )
            )
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        context.startActivity(intent)
    }

    fun launchCarAction(action: HavalCarAction): Result<Unit> {
        val candidates = buildList {
            add(action.primaryPackage to action.primaryActivity)
            addAll(action.fallbackComponents)
        }
        for ((pkg, cls) in candidates) {
            val explicit = Intent().apply {
                component = ComponentName(pkg, cls)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (explicit.resolveActivity(pm) != null) {
                val res = runCatching { context.startActivity(explicit) }
                if (res.isSuccess) return res
            }
            val pkgLaunch = pm.getLaunchIntentForPackage(pkg)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (pkgLaunch != null) {
                val res = runCatching { context.startActivity(pkgLaunch) }
                if (res.isSuccess) return res
            }
        }

        val fallbackAction = action.fallbackIntentAction
        if (!fallbackAction.isNullOrBlank()) {
            val intent = if (fallbackAction == Intent.ACTION_MAIN) {
                Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            } else {
                Intent(fallbackAction).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(pm) != null) {
                return runCatching { context.startActivity(intent) }
            }
        }

        return Result.failure(IllegalStateException("Компонент «${action.title}» недоступен на этом устройстве"))
    }

    fun openWifiSettings(): Result<Unit> {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (intent.resolveActivity(pm) != null) {
            runCatching { context.startActivity(intent) }
        } else {
            runCatching {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }

    fun openNotificationListenerSettings(): Result<Unit> {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (intent.resolveActivity(pm) != null) {
            runCatching { context.startActivity(intent) }
        } else {
            runCatching {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }

    fun openUrl(url: String): Result<Unit> = runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openAppInfo(packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openHomeLauncherSettings(): Result<Unit> {
        val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (intent.resolveActivity(pm) != null) {
            runCatching { context.startActivity(intent) }
        } else {
            runCatching {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }

    private fun categorizeApp(packageName: String, label: String): AppCategory {
        val pkg = packageName.lowercase()
        val lbl = label.lowercase()
        return when {
            pkg.startsWith("com.adayo") ||
                pkg.startsWith("com.android.car") ||
                pkg.contains("kitchensink") ||
                lbl.contains("климат") ||
                lbl.contains("камера") ||
                lbl.contains("авто") -> AppCategory.CAR

            pkg.contains("music") ||
                pkg.contains("radio") ||
                pkg.contains("media") ||
                pkg.contains("video") ||
                pkg.contains("player") ||
                pkg.contains("spotify") ||
                pkg.contains("kinopoisk") ||
                pkg.contains("youtube") ||
                pkg.contains("gallery") -> AppCategory.MEDIA

            else -> AppCategory.SYSTEM
        }
    }
}
