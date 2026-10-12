package pl.gpssos.app

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GpsScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val fix = GeoFix(52.22968, 21.01223, 8f, 0, 100_000_000_000)

    @Test fun shareUsesDisplayedFix() {
        var received: GeoFix? = null
        compose.setContent {
            GpsSosTheme { GpsScreen(GpsUiState(true, true, true, fix, nowNanos = 101_000_000_000), {}, {}, {}, { received = it }, {}, {}, {}) }
        }
        compose.onNodeWithText("Przekaż lokalizację").assertIsDisplayed().performClick()
        assertEquals(fix, received)
        screenshot("ekran-glowny")
    }

    @Test fun firstFixIsRequiredBeforeSharing() {
        compose.setContent {
            GpsSosTheme { GpsScreen(GpsUiState(hasPermission = true, precisePermission = true), {}, {}, {}, {}, {}, {}, {}) }
        }
        compose.onNodeWithText("Przekaż lokalizację").assertIsNotEnabled()
        compose.onNodeWithText("Ustalam lokalizację").assertIsDisplayed()
    }

    @Test fun permissionGateHasOneClearAction() {
        var requested = false
        compose.setContent { GpsSosTheme { GpsScreen(GpsUiState(), { requested = true }, {}, {}, {}, {}, {}, {}) } }
        compose.onNodeWithText("Zezwól na lokalizację").performClick()
        assertTrue(requested)
    }

    @Test fun readingSnapshotDoesNotChangeWithNewLiveFix() {
        val state = mutableStateOf(GpsUiState(true, true, true, fix, fix, 101_000_000_000))
        compose.setContent { GpsSosTheme { GpsScreen(state.value, {}, {}, {}, {}, {}, {}, {}) } }
        compose.runOnIdle { state.value = state.value.copy(fix = fix.copy(latitude = 50.0), nowNanos = 200_000_000_000) }
        compose.onNodeWithText("52,22968° N").assertExists()
        compose.onNodeWithText("Ten pomiar może być nieaktualny. Sprawdź czas pomiaru przed przekazaniem.").assertExists()
        screenshot("dyktowanie")
    }

    @Test fun largeTextKeepsMainActionsAvailable() {
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 2f)) {
                GpsSosTheme { GpsScreen(GpsUiState(true, true, true, fix, nowNanos = 101_000_000_000), {}, {}, {}, {}, {}, {}, {}) }
            }
        }
        compose.onNodeWithText("Przekaż lokalizację").assertIsDisplayed()
        compose.onNodeWithText("Kopiuj współrzędne").assertIsDisplayed()
        screenshot("duza-czcionka")
    }

    private fun screenshot(name: String) {
        val directory = System.getProperty("gps.screenshotDir") ?: return
        val file = File(directory, "$name.png")
        file.parentFile?.mkdirs()
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    @Test fun shareIntentContainsLocationAndTimeButDoesNotSendSmsDirectly() {
        val intent = createShareIntent(fix, 101_000_000_000)
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("text/plain", intent.type)
        assertTrue(intent.getStringExtra(Intent.EXTRA_TEXT)!!.contains("52.22968, 21.01223"))
        assertTrue(intent.getStringExtra(Intent.EXTRA_TEXT)!!.contains("Pomiar:"))
        assertNull(intent.component)
    }
}
