package pl.gpssos.app

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private val model: GpsViewModel by viewModels()
    private val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    private val preferences by lazy { getSharedPreferences("permission_setup", MODE_PRIVATE) }
    private var foreground = false
    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (foreground) refreshLocation()
    }
    private val providerChanges = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (foreground) refreshLocation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by model.state.collectAsStateWithLifecycle()
            LaunchedEffect(state.readingFix != null) {
                if (state.readingFix != null) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            GpsSosTheme {
                GpsScreen(
                    state = state,
                    onPermission = ::askForLocation,
                    onLocationSettings = { openSettings(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
                    onRefresh = ::refreshLocation,
                    onShare = ::share,
                    onCopy = ::copy,
                    onRead = model::beginReading,
                    onBack = model::finishReading,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        foreground = true
        ContextCompat.registerReceiver(this, providerChanges, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION), ContextCompat.RECEIVER_NOT_EXPORTED)
        refreshLocation()
    }

    override fun onStop() {
        foreground = false
        unregisterReceiver(providerChanges)
        model.stop()
        super.onStop()
    }

    private fun granted(permission: String) = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun refreshLocation() {
        val precise = granted(Manifest.permission.ACCESS_FINE_LOCATION)
        model.start(precise || granted(Manifest.permission.ACCESS_COARSE_LOCATION), precise)
    }

    private fun askForLocation() {
        val previouslyAsked = preferences.getBoolean("asked", false)
        if (previouslyAsked && permissions.none(::shouldShowRequestPermissionRationale)) {
            openSettings(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri()))
        } else {
            preferences.edit().putBoolean("asked", true).apply()
            requestPermission.launch(permissions)
        }
    }

    private fun openSettings(intent: Intent) {
        try { startActivity(intent) } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "Otwórz ustawienia lokalizacji telefonu.", Toast.LENGTH_LONG).show()
        }
    }

    private fun share(fix: GeoFix) {
        val intent = createShareIntent(fix, SystemClock.elapsedRealtimeNanos())
        try {
            startActivity(Intent.createChooser(intent, "Przekaż lokalizację"))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "Brak aplikacji do udostępniania. Możesz skopiować współrzędne.", Toast.LENGTH_LONG).show()
        }
    }

    private fun copy(fix: GeoFix) {
        // Preserve time and uncertainty in copied text as well as shared messages.
        getSystemService(ClipboardManager::class.java).setPrimaryClip(
            ClipData.newPlainText("Lokalizacja GPS S.O.S", LocationText.message(fix, SystemClock.elapsedRealtimeNanos()))
        )
        Toast.makeText(this, "Skopiowano współrzędne i dane pomiaru", Toast.LENGTH_SHORT).show()
    }
}

internal fun createShareIntent(fix: GeoFix, nowNanos: Long) = Intent(Intent.ACTION_SEND).apply {
    type = "text/plain"
    putExtra(Intent.EXTRA_TITLE, "Moja lokalizacja — GPS S.O.S")
    putExtra(Intent.EXTRA_TEXT, LocationText.message(fix, nowNanos))
}
