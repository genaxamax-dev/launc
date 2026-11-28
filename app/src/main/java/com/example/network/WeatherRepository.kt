package com.example.network

import com.example.BuildConfig
import com.example.model.HeadlightMode
import com.example.model.Season
import com.example.model.WeatherCondition
import com.example.model.WeatherSourceMode
import com.example.model.WeatherState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

object WeatherRepository {

    private data class GeoPoint(
        val lat: Double,
        val lon: Double,
        val cityName: String
    )

    private var cachedCityQuery: String? = null
    private var cachedGeoPoint: GeoPoint? = null

    fun currentSeason(calendar: Calendar = Calendar.getInstance()): Season {
        return when (calendar.get(Calendar.MONTH)) {
            Calendar.DECEMBER, Calendar.JANUARY, Calendar.FEBRUARY -> Season.WINTER
            Calendar.MARCH, Calendar.APRIL, Calendar.MAY -> Season.SPRING
            Calendar.JUNE, Calendar.JULY, Calendar.AUGUST -> Season.SUMMER
            else -> Season.AUTUMN
        }
    }

    fun computeHeadlightsOn(
        mode: HeadlightMode,
        calendar: Calendar = Calendar.getInstance()
    ): Boolean = when (mode) {
        HeadlightMode.ALWAYS_ON -> true
        HeadlightMode.ALWAYS_OFF -> false
        HeadlightMode.AUTO -> {
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val minutesOfDay = hour * 60 + minute
            when (currentSeason(calendar)) {
                Season.WINTER -> minutesOfDay >= 17 * 60 || minutesOfDay < 8 * 60
                Season.SUMMER -> minutesOfDay >= 21 * 60 || minutesOfDay < 4 * 60 + 30
                Season.SPRING, Season.AUTUMN -> minutesOfDay >= 18 * 60 || minutesOfDay < 6 * 60 + 30
            }
        }
    }

    fun seasonalWeather(calendar: Calendar = Calendar.getInstance()): WeatherState {
        val month = calendar.get(Calendar.MONTH)
        val isNight = computeHeadlightsOn(HeadlightMode.AUTO, calendar)
        val season = currentSeason(calendar)

        val (dayTemp, nightTemp, condition, label) = when (month) {
            Calendar.DECEMBER, Calendar.JANUARY -> Quad(-6, -11, WeatherCondition.SNOW, "Снег")
            Calendar.FEBRUARY -> Quad(-5, -10, WeatherCondition.SNOW, "Снег и мороз")
            Calendar.MARCH -> Quad(2, -3, WeatherCondition.PARTLY_CLOUDY, "Облачно с прояснениями")
            Calendar.APRIL -> Quad(10, 3, WeatherCondition.PARTLY_CLOUDY, "Прохладно")
            Calendar.MAY -> Quad(18, 10, WeatherCondition.CLEAR, "Ясно")
            Calendar.JUNE -> Quad(22, 14, WeatherCondition.CLEAR, "Ясно")
            Calendar.JULY -> Quad(25, 17, WeatherCondition.CLEAR, "Тепло и ясно")
            Calendar.AUGUST -> Quad(23, 15, WeatherCondition.PARTLY_CLOUDY, "Переменная облачность")
            Calendar.SEPTEMBER -> Quad(15, 8, WeatherCondition.PARTLY_CLOUDY, "Свежо")
            Calendar.OCTOBER -> Quad(7, 2, WeatherCondition.RAIN, "Дождливо")
            Calendar.NOVEMBER -> Quad(0, -4, WeatherCondition.CLOUDY, "Пасмурно")
            else -> Quad(2, -2, WeatherCondition.CLOUDY, "Пасмурно")
        }

        val temp = if (isNight) nightTemp else dayTemp
        return WeatherState(
            temperatureC = temp,
            feelsLikeC = temp - 2,
            condition = condition,
            conditionLabel = label,
            cityName = season.label,
            sourceLabel = "По времени года",
            season = season,
            isNight = isNight,
            windSpeedMs = null,
            isLoading = false
        )
    }

    suspend fun fetchWeather(
        cityQuery: String,
        yandexApiKey: String,
        mode: WeatherSourceMode,
        wifiConnected: Boolean
    ): WeatherState = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        val fallback = seasonalWeather(calendar)

        if (mode == WeatherSourceMode.SEASONAL_ONLY || !wifiConnected) {
            return@withContext fallback
        }

        val geo = resolveLocation(cityQuery)

        if (yandexApiKey.isNotBlank()) {
            val yandex = fetchFromYandexWeather(geo, yandexApiKey, fallback)
            if (yandex != null) return@withContext yandex
        }

        val openMeteo = fetchFromOpenMeteo(geo, fallback)
        return@withContext openMeteo ?: fallback
    }

    private fun resolveLocation(rawCity: String): GeoPoint {
        val cleanCity = rawCity.trim()
        val cached = cachedGeoPoint
        if (cached != null && cachedCityQuery == cleanCity) {
            return cached
        }

        if (cleanCity.isNotEmpty()) {
            val encoded = runCatching { URLEncoder.encode(cleanCity, "UTF-8") }.getOrDefault(cleanCity)
            val jsonStr = httpGet("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&language=ru&format=json")
            if (jsonStr != null) {
                val parsed = runCatching {
                    val results = JSONObject(jsonStr).optJSONArray("results")
                    if (results != null && results.length() > 0) {
                        val first = results.getJSONObject(0)
                        GeoPoint(
                            lat = first.getDouble("latitude"),
                            lon = first.getDouble("longitude"),
                            cityName = first.optString("name").ifBlank { cleanCity }
                        )
                    } else null
                }.getOrNull()
                if (parsed != null) {
                    cachedCityQuery = cleanCity
                    cachedGeoPoint = parsed
                    return parsed
                }
            }
        } else {
            val ipJson = httpGet("https://ipwho.is/?lang=ru")
            if (ipJson != null) {
                val parsed = runCatching {
                    val obj = JSONObject(ipJson)
                    if (obj.optBoolean("success", true) && obj.has("latitude") && obj.has("longitude")) {
                        GeoPoint(
                            lat = obj.getDouble("latitude"),
                            lon = obj.getDouble("longitude"),
                            cityName = obj.optString("city").ifBlank { "По сети" }
                        )
                    } else null
                }.getOrNull()
                if (parsed != null) {
                    cachedCityQuery = cleanCity
                    cachedGeoPoint = parsed
                    return parsed
                }
            }
        }

        return GeoPoint(55.7558, 37.6173, if (cleanCity.isNotEmpty()) cleanCity else "Москва")
    }

    private fun fetchFromYandexWeather(
        geo: GeoPoint,
        apiKey: String,
        fallback: WeatherState
    ): WeatherState? {
        val url = String.format(
            Locale.US,
            "https://api.weather.yandex.ru/v2/forecast?lat=%.4f&lon=%.4f&lang=ru_RU&limit=1",
            geo.lat,
            geo.lon
        )
        val body = httpGet(
            urlString = url,
            headers = mapOf(
                "X-Yandex-Weather-Key" to apiKey,
                "X-Yandex-API-Key" to apiKey
            )
        ) ?: return null

        return runCatching {
            val root = JSONObject(body)
            val fact = root.getJSONObject("fact")
            val temp = fact.getInt("temp")
            val feelsLike = if (fact.has("feels_like")) fact.getInt("feels_like") else null
            val windSpeed = if (fact.has("wind_speed")) fact.getDouble("wind_speed").roundToInt() else null
            val rawCond = fact.optString("condition", "cloudy")
            val daytime = fact.optString("daytime", "d")
            val (cond, label) = mapYandexCondition(rawCond)

            WeatherState(
                temperatureC = temp,
                feelsLikeC = feelsLike,
                condition = cond,
                conditionLabel = label,
                cityName = geo.cityName,
                sourceLabel = "Яндекс.Погода",
                season = fallback.season,
                isNight = daytime == "n",
                windSpeedMs = windSpeed,
                latitude = geo.lat,
                longitude = geo.lon,
                isLoading = false
            )
        }.getOrNull()
    }

    private fun fetchFromOpenMeteo(
        geo: GeoPoint,
        fallback: WeatherState
    ): WeatherState? {
        val url = String.format(
            Locale.US,
            "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,apparent_temperature,weather_code,wind_speed_10m,is_day&wind_speed_unit=ms&timezone=auto",
            geo.lat,
            geo.lon
        )
        val body = httpGet(url) ?: return null

        return runCatching {
            val current = JSONObject(body).getJSONObject("current")
            val temp = current.getDouble("temperature_2m").roundToInt()
            val feelsLike = current.optDouble("apparent_temperature", Double.NaN)
                .takeIf { !it.isNaN() }?.roundToInt()
            val wind = current.optDouble("wind_speed_10m", Double.NaN)
                .takeIf { !it.isNaN() }?.roundToInt()
            val code = current.optInt("weather_code", 3)
            val isDay = current.optInt("is_day", 1) == 1
            val (cond, label) = mapWmoCode(code)

            WeatherState(
                temperatureC = temp,
                feelsLikeC = feelsLike,
                condition = cond,
                conditionLabel = label,
                cityName = geo.cityName,
                sourceLabel = "Онлайн-погода",
                season = fallback.season,
                isNight = !isDay,
                windSpeedMs = wind,
                latitude = geo.lat,
                longitude = geo.lon,
                isLoading = false
            )
        }.getOrNull()
    }

    private fun mapYandexCondition(raw: String): Pair<WeatherCondition, String> = when (raw) {
        "clear" -> WeatherCondition.CLEAR to "Ясно"
        "partly-cloudy" -> WeatherCondition.PARTLY_CLOUDY to "Малооблачно"
        "cloudy" -> WeatherCondition.CLOUDY to "Облачно"
        "overcast" -> WeatherCondition.CLOUDY to "Пасмурно"
        "light-rain", "drizzle" -> WeatherCondition.RAIN to "Небольшой дождь"
        "rain", "moderate-rain" -> WeatherCondition.RAIN to "Дождь"
        "heavy-rain", "continuous-heavy-rain", "showers" -> WeatherCondition.RAIN to "Сильный дождь"
        "wet-snow", "light-snow" -> WeatherCondition.SNOW to "Небольшой снег"
        "snow", "snow-showers", "hail" -> WeatherCondition.SNOW to "Снег"
        "thunderstorm", "thunderstorm-with-rain", "thunderstorm-with-hail" ->
            WeatherCondition.THUNDERSTORM to "Гроза"
        else -> WeatherCondition.CLOUDY to "Облачно"
    }

    private fun mapWmoCode(code: Int): Pair<WeatherCondition, String> = when (code) {
        0 -> WeatherCondition.CLEAR to "Ясно"
        1, 2 -> WeatherCondition.PARTLY_CLOUDY to "Малооблачно"
        3 -> WeatherCondition.CLOUDY to "Пасмурно"
        45, 48 -> WeatherCondition.FOG to "Туман"
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> WeatherCondition.RAIN to "Дождь"
        71, 73, 75, 77, 85, 86 -> WeatherCondition.SNOW to "Снег"
        95, 96, 99 -> WeatherCondition.THUNDERSTORM to "Гроза"
        else -> WeatherCondition.CLOUDY to "Облачно"
    }

    private fun httpGet(
        urlString: String,
        headers: Map<String, String> = emptyMap()
    ): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", "HavalM6Launcher/${BuildConfig.VERSION_NAME}")
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
            }
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
