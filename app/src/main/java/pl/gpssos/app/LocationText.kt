package pl.gpssos.app

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil

/** One measured position. No coordinate history is persisted by the application. */
data class GeoFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val timeMillis: Long,
    val elapsedNanos: Long,
    val approximate: Boolean = false,
    val mock: Boolean = false,
) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
        require(accuracyMeters == null || accuracyMeters.isFinite() && accuracyMeters >= 0)
        require(elapsedNanos >= 0)
    }
    fun ageSeconds(nowNanos: Long): Long = ((nowNanos - elapsedNanos).coerceAtLeast(0) / 1_000_000_000)
    fun isStale(nowNanos: Long) = ageSeconds(nowNanos) >= 60
}

object LocationText {
    private val polish = Locale.forLanguageTag("pl-PL")
    private val digits = listOf("zero", "jeden", "dwa", "trzy", "cztery", "pięć", "sześć", "siedem", "osiem", "dziewięć")

    fun decimal(value: Double): String = String.format(Locale.US, "%.5f", if (abs(value) < 0.000005) 0.0 else value)
    fun hemisphere(value: Double, latitude: Boolean): String = if (latitude) {
        if (value < 0 && abs(value) >= 0.000005) "S" else "N"
    } else {
        if (value < 0 && abs(value) >= 0.000005) "W" else "E"
    }
    fun direction(value: Double, latitude: Boolean): String = when (hemisphere(value, latitude)) {
        "N" -> "północ"
        "S" -> "południe"
        "W" -> "zachód"
        else -> "wschód"
    }
    fun coordinate(value: Double, latitude: Boolean): String =
        "${decimal(abs(value)).replace('.', ',')}° ${hemisphere(value, latitude)}"

    fun spoken(value: Double, latitude: Boolean): String {
        val parts = decimal(abs(value)).split('.')
        fun say(part: String) = part.map { digits[it.digitToInt()] }.joinToString(" ")
        return "${say(parts[0])}, przecinek,\n${say(parts[1])}.\n${direction(value, latitude).replaceFirstChar { it.titlecase(polish) }}."
    }

    fun accuracy(fix: GeoFix): String = fix.accuracyMeters?.let { "około ${ceil(it.toDouble()).toLong().coerceAtLeast(1)} m" }
        ?: "nieznana"
    fun age(fix: GeoFix, nowNanos: Long): String {
        val seconds = fix.ageSeconds(nowNanos)
        return when {
            seconds < 1 -> "Pomiar przed chwilą"
            seconds < 60 -> "Pomiar sprzed $seconds s"
            seconds < 3600 -> "Pomiar sprzed ${seconds / 60} min"
            seconds < 86400 -> "Pomiar sprzed ${seconds / 3600} godz."
            else -> "Pomiar sprzed ${seconds / 86400} dni"
        }
    }
    fun timestamp(fix: GeoFix, zone: ZoneId = ZoneId.systemDefault()): String =
        DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm:ss (XXX)", polish).withZone(zone).format(Instant.ofEpochMilli(fix.timeMillis))

    fun pair(fix: GeoFix) = "${decimal(fix.latitude)}, ${decimal(fix.longitude)}"
    fun mapUrl(fix: GeoFix) = "https://www.google.com/maps/search/?api=1&query=${decimal(fix.latitude)}%2C${decimal(fix.longitude)}"

    fun message(fix: GeoFix, nowNanos: Long, zone: ZoneId = ZoneId.systemDefault()): String = buildString {
        if (fix.mock) appendLine("LOKALIZACJA TESTOWA")
        if (fix.isStale(nowNanos)) appendLine("Ostatnia znana lokalizacja — może być nieaktualna.")
        if (fix.approximate) appendLine("Lokalizacja przybliżona.")
        appendLine("Moja lokalizacja:")
        appendLine(pair(fix))
        appendLine("Szerokość: ${coordinate(fix.latitude, true)}")
        appendLine("Długość: ${coordinate(fix.longitude, false)}")
        appendLine()
        appendLine("Mapa: ${mapUrl(fix)}")
        appendLine()
        appendLine("Szacowana dokładność: ${accuracy(fix)}")
        append("Pomiar: ${timestamp(fix, zone)}")
    }

    fun newer(current: GeoFix?, candidate: GeoFix): GeoFix = when {
        current == null -> candidate
        candidate.elapsedNanos > current.elapsedNanos -> candidate
        candidate.elapsedNanos == current.elapsedNanos &&
            (candidate.accuracyMeters ?: Float.MAX_VALUE) < (current.accuracyMeters ?: Float.MAX_VALUE) -> candidate
        else -> current
    }
}
