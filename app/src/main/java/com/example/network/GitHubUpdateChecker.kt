package com.example.network

import com.example.BuildConfig
import com.example.model.UpdateCheckState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object GitHubUpdateChecker {

    fun normalizeRepo(rawInput: String): String {
        val trimmed = rawInput.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("github.com/")
            .removeSuffix(".git")
            .trim('/')

        val parts = trimmed.split("/").filter { it.isNotBlank() }
        return if (parts.size >= 2) "${parts[0]}/${parts[1]}" else ""
    }

    suspend fun checkLatestRelease(rawRepo: String): UpdateCheckState = withContext(Dispatchers.IO) {
        val repo = normalizeRepo(rawRepo)
        if (repo.isEmpty()) {
            return@withContext UpdateCheckState.RepoNotConfigured
        }

        val currentVersion = BuildConfig.VERSION_NAME

        val releaseResult = fetchJson("https://api.github.com/repos/$repo/releases/latest")
        if (releaseResult.statusCode == 200 && releaseResult.body != null) {
            return@withContext runCatching {
                val json = JSONObject(releaseResult.body)
                val tag = json.optString("tag_name").ifBlank { json.optString("name") }
                val htmlUrl = json.optString("html_url")
                    .ifBlank { "https://github.com/$repo/releases" }
                val notes = json.optString("body").trim()
                val assets = json.optJSONArray("assets")
                var apkUrl: String? = null
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        val name = asset.optString("name")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url").takeIf { it.isNotBlank() }
                            break
                        }
                    }
                }

                if (tag.isNotBlank() && isRemoteVersionNewer(currentVersion, tag)) {
                    UpdateCheckState.UpdateAvailable(
                        currentVersion = currentVersion,
                        latestVersion = tag,
                        releaseNotes = notes,
                        releaseUrl = htmlUrl,
                        apkUrl = apkUrl
                    )
                } else {
                    UpdateCheckState.UpToDate(currentVersion)
                }
            }.getOrElse {
                UpdateCheckState.Error("Ошибка разбора ответа GitHub")
            }
        }

        if (releaseResult.statusCode == 404) {
            // Repository exists without Releases, or repo is private/not found — try tags fallback
            val tagsResult = fetchJson("https://api.github.com/repos/$repo/tags?per_page=1")
            if (tagsResult.statusCode == 200 && tagsResult.body != null) {
                return@withContext runCatching {
                    val arr = JSONArray(tagsResult.body)
                    if (arr.length() == 0) {
                        UpdateCheckState.UpToDate(currentVersion)
                    } else {
                        val tag = arr.optJSONObject(0)?.optString("name").orEmpty()
                        if (tag.isNotBlank() && isRemoteVersionNewer(currentVersion, tag)) {
                            UpdateCheckState.UpdateAvailable(
                                currentVersion = currentVersion,
                                latestVersion = tag,
                                releaseNotes = "Найден новый тег $tag. Проверьте релизы или GitHub Actions.",
                                releaseUrl = "https://github.com/$repo/releases",
                                apkUrl = null
                            )
                        } else {
                            UpdateCheckState.UpToDate(currentVersion)
                        }
                    }
                }.getOrElse {
                    UpdateCheckState.UpToDate(currentVersion)
                }
            }
            return@withContext UpdateCheckState.Error("Репозиторий или релиз не найден (404)")
        }

        val errMsg = when (releaseResult.statusCode) {
            403 -> "Превышен лимит запросов GitHub API (повторите позже)"
            -1 -> "Нет соединения с GitHub"
            else -> "Ошибка GitHub API (${releaseResult.statusCode})"
        }
        UpdateCheckState.Error(errMsg)
    }

    internal fun isRemoteVersionNewer(currentRaw: String, remoteRaw: String): Boolean {
        val currentParts = parseVersionParts(currentRaw)
        val remoteParts = parseVersionParts(remoteRaw)
        if (currentParts.isEmpty() || remoteParts.isEmpty()) {
            return remoteRaw.trim().removePrefix("v").removePrefix("V") !=
                currentRaw.trim().removePrefix("v").removePrefix("V")
        }
        val maxLen = maxOf(currentParts.size, remoteParts.size)
        for (i in 0 until maxLen) {
            val c = currentParts.getOrElse(i) { 0 }
            val r = remoteParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    private fun parseVersionParts(raw: String): List<Int> {
        val cleaned = raw.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore("-")
            .substringBefore(" ")
        return cleaned.split(".")
            .mapNotNull { segment ->
                segment.filter { it.isDigit() }.toIntOrNull()
            }
    }

    private data class HttpResponse(val statusCode: Int, val body: String?)

    private fun fetchJson(urlString: String): HttpResponse {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7000
                readTimeout = 7000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "HavalM6Launcher/${BuildConfig.VERSION_NAME}")
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }
            HttpResponse(code, text)
        } catch (_: Exception) {
            HttpResponse(-1, null)
        } finally {
            conn?.disconnect()
        }
    }
}
