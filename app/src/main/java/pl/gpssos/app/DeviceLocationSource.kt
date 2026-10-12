package pl.gpssos.app

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.core.location.LocationManagerCompat

/** Foreground-only Android location adapter; does not depend on Google Play services. */
class DeviceLocationSource(context: Context) {
    private val manager = context.getSystemService(LocationManager::class.java)
    private var listener: LocationListener? = null
    private var generation = 0

    fun isEnabled(): Boolean = manager?.let(LocationManagerCompat::isLocationEnabled) ?: false

    @SuppressLint("MissingPermission") // Permissions are checked by the Activity before each start.
    fun start(precise: Boolean, onFix: (GeoFix) -> Unit, onIssue: (String) -> Unit) {
        stop()
        val requestGeneration = generation
        val locationManager = manager ?: run {
            onIssue("Ten telefon nie udostępnia usług lokalizacji.")
            return
        }
        fun deliver(location: Location) {
            if (requestGeneration != generation) return
            val fix = location.toFix(precise) ?: return
            onFix(fix)
        }
        val callback = object : LocationListener {
            override fun onLocationChanged(location: Location) = deliver(location)
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
            @Deprecated("Required for Android 8 compatibility")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }
        listener = callback
        val names = buildList {
            if (precise) add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            add(LocationManager.PASSIVE_PROVIDER)
        }.filter { it in locationManager.allProviders }

        var activeProviders = 0
        for (provider in names) {
            try {
                locationManager.getLastKnownLocation(provider)?.let(::deliver)
                if (locationManager.isProviderEnabled(provider)) {
                    locationManager.requestLocationUpdates(provider, 1000L, 0f, callback, Looper.getMainLooper())
                    if (provider != LocationManager.PASSIVE_PROVIDER) activeProviders++
                }
            } catch (_: SecurityException) {
                onIssue("Sprawdź uprawnienia lokalizacji w ustawieniach aplikacji.")
            } catch (_: IllegalArgumentException) {
                // Some devices advertise a provider that cannot currently serve requests.
            }
        }
        if (activeProviders == 0) {
            onIssue(if (!precise) "Włącz dokładną lokalizację, aby korzystać z GPS." else "Oczekiwanie na dostępne źródło lokalizacji.")
        }
    }

    fun stop() {
        generation++
        listener?.let { callback ->
            try { manager?.removeUpdates(callback) } catch (_: SecurityException) { /* Permission revoked. */ }
        }
        listener = null
    }
}

@Suppress("DEPRECATION")
internal fun Location.toFix(precise: Boolean): GeoFix? {
    if (!latitude.isFinite() || latitude !in -90.0..90.0 ||
        !longitude.isFinite() || longitude !in -180.0..180.0 || elapsedRealtimeNanos <= 0
    ) return null
    return GeoFix(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = if (hasAccuracy() && accuracy.isFinite() && accuracy >= 0) accuracy else null,
        timeMillis = time,
        elapsedNanos = elapsedRealtimeNanos,
        approximate = !precise,
        mock = if (Build.VERSION.SDK_INT >= 31) isMock else isFromMockProvider,
    )
}
