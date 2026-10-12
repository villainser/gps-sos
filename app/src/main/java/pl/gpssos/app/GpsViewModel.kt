package pl.gpssos.app

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GpsUiState(
    val hasPermission: Boolean = false,
    val precisePermission: Boolean = false,
    val locationEnabled: Boolean = true,
    val fix: GeoFix? = null,
    val readingFix: GeoFix? = null,
    val nowNanos: Long = SystemClock.elapsedRealtimeNanos(),
    val waitingLong: Boolean = false,
    val issue: String? = null,
)

class GpsViewModel(application: Application) : AndroidViewModel(application) {
    private val source = DeviceLocationSource(application)
    private val mutableState = MutableStateFlow(GpsUiState())
    val state = mutableState.asStateFlow()
    private var ticker: Job? = null
    private var started = false

    fun start(hasPermission: Boolean, precise: Boolean) {
        source.stop()
        ticker?.cancel()
        started = true
        val downgrade = state.value.precisePermission && !precise
        mutableState.update {
            it.copy(
                hasPermission = hasPermission,
                precisePermission = precise,
                locationEnabled = source.isEnabled(),
                fix = if (!hasPermission || downgrade) null else it.fix,
                readingFix = if (!hasPermission || downgrade) null else it.readingFix,
                waitingLong = false,
                nowNanos = SystemClock.elapsedRealtimeNanos(),
                issue = null,
            )
        }
        val startNanos = SystemClock.elapsedRealtimeNanos()
        ticker = viewModelScope.launch {
            while (started) {
                val now = SystemClock.elapsedRealtimeNanos()
                mutableState.update { it.copy(nowNanos = now, waitingLong = it.fix == null && now - startNanos > 15_000_000_000) }
                delay(1000)
            }
        }
        if (hasPermission && state.value.locationEnabled) {
            source.start(precise,
                onFix = { fix ->
                    mutableState.update {
                        it.copy(fix = LocationText.newer(it.fix, fix), nowNanos = SystemClock.elapsedRealtimeNanos(), issue = null)
                    }
                },
                onIssue = { issue -> mutableState.update { it.copy(issue = issue) } },
            )
        }
    }

    fun stop() {
        started = false
        source.stop()
        ticker?.cancel()
        ticker = null
    }

    fun beginReading() = mutableState.update { it.copy(readingFix = it.fix) }
    fun finishReading() = mutableState.update { it.copy(readingFix = null) }

    override fun onCleared() {
        stop()
        super.onCleared()
    }
}
