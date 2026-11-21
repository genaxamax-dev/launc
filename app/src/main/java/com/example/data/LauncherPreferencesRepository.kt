package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.LauncherSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "haval_m6_launcher_prefs"
)

class LauncherPreferencesRepository(private val context: Context) {

    private object Keys {
        val WALLPAPER_DIM = floatPreferencesKey("wallpaper_dim")
        val ENFORCE_LEFT_INSET = booleanPreferencesKey("enforce_haval_left_inset")
        val ENFORCE_TOP_INSET = booleanPreferencesKey("enforce_haval_top_inset")
        val SHOW_SECONDS = booleanPreferencesKey("show_seconds_in_clock")
        val LIGHTWEIGHT_BG = booleanPreferencesKey("lightweight_background")
        val REDUCE_ANIMATIONS = booleanPreferencesKey("reduce_animations")
        val GITHUB_REPO = stringPreferencesKey("github_repo")
        val AUTO_CHECK_UPDATES = booleanPreferencesKey("auto_check_updates")
        val LAST_UPDATE_CHECK_MS = longPreferencesKey("last_update_check_ms")
        val PINNED_PACKAGES = stringPreferencesKey("pinned_packages_csv")
    }

    private val defaultPinned = LauncherSettings().pinnedPackages

    val settingsFlow: Flow<LauncherSettings> = context.launcherDataStore.data.map { prefs ->
        val pinnedRaw = prefs[Keys.PINNED_PACKAGES]
        val pinnedList = if (pinnedRaw == null) {
            defaultPinned
        } else if (pinnedRaw.isBlank()) {
            emptyList()
        } else {
            pinnedRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }

        LauncherSettings(
            wallpaperDim = prefs[Keys.WALLPAPER_DIM] ?: 0.44f,
            enforceHavalLeftInset = prefs[Keys.ENFORCE_LEFT_INSET] ?: true,
            enforceHavalTopInset = prefs[Keys.ENFORCE_TOP_INSET] ?: true,
            showSecondsInClock = prefs[Keys.SHOW_SECONDS] ?: false,
            lightweightBackground = prefs[Keys.LIGHTWEIGHT_BG] ?: false,
            reduceAnimations = prefs[Keys.REDUCE_ANIMATIONS] ?: true,
            githubRepo = prefs[Keys.GITHUB_REPO] ?: "",
            autoCheckUpdates = prefs[Keys.AUTO_CHECK_UPDATES] ?: true,
            lastUpdateCheckMs = prefs[Keys.LAST_UPDATE_CHECK_MS] ?: 0L,
            pinnedPackages = pinnedList
        )
    }

    suspend fun setWallpaperDim(dim: Float) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.WALLPAPER_DIM] = dim.coerceIn(0.15f, 0.85f)
        }
    }

    suspend fun setEnforceHavalLeftInset(enabled: Boolean) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.ENFORCE_LEFT_INSET] = enabled
        }
    }

    suspend fun setEnforceHavalTopInset(enabled: Boolean) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.ENFORCE_TOP_INSET] = enabled
        }
    }

    suspend fun setShowSecondsInClock(enabled: Boolean) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.SHOW_SECONDS] = enabled
        }
    }

    suspend fun setLightweightBackground(enabled: Boolean) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.LIGHTWEIGHT_BG] = enabled
        }
    }

    suspend fun setReduceAnimations(enabled: Boolean) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.REDUCE_ANIMATIONS] = enabled
        }
    }

    suspend fun setGithubRepo(repo: String) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.GITHUB_REPO] = repo.trim()
        }
    }

    suspend fun setAutoCheckUpdates(enabled: Boolean) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.AUTO_CHECK_UPDATES] = enabled
        }
    }

    suspend fun setLastUpdateCheckMs(timestampMs: Long) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.LAST_UPDATE_CHECK_MS] = timestampMs
        }
    }

    suspend fun togglePinnedPackage(packageName: String) {
        context.launcherDataStore.edit { prefs ->
            val currentRaw = prefs[Keys.PINNED_PACKAGES]
            val current = if (currentRaw == null) {
                defaultPinned.toMutableList()
            } else if (currentRaw.isBlank()) {
                mutableListOf()
            } else {
                currentRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
            }

            if (current.contains(packageName)) {
                current.remove(packageName)
            } else {
                if (current.size >= 8) {
                    current.removeAt(current.lastIndex)
                }
                current.add(packageName)
            }
            prefs[Keys.PINNED_PACKAGES] = current.joinToString(",")
        }
    }
}
