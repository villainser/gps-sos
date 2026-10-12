package pl.gpssos.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightPalette = lightColorScheme(
    primary = Color(0xFFAB292D), onPrimary = Color.White,
    background = Color(0xFFFCFCFA), onBackground = Color(0xFF1B2427),
    surface = Color(0xFFFCFCFA), onSurface = Color(0xFF1B2427),
    surfaceVariant = Color(0xFFEDF2EF), onSurfaceVariant = Color(0xFF526065),
    outline = Color(0xFF74817D), outlineVariant = Color(0xFFD8DEDC),
    errorContainer = Color(0xFFFFEBC4), onErrorContainer = Color(0xFF5A3900),
)
private val DarkPalette = darkColorScheme(
    primary = Color(0xFFFFB3AC), onPrimary = Color(0xFF5E1217),
    background = Color(0xFF151719), onBackground = Color(0xFFF0F3F3),
    surface = Color(0xFF151719), onSurface = Color(0xFFF0F3F3),
    surfaceVariant = Color(0xFF232E2C), onSurfaceVariant = Color(0xFFAFBABC),
    outline = Color(0xFF899895), outlineVariant = Color(0xFF394346),
    errorContainer = Color(0xFF483510), onErrorContainer = Color(0xFFFFDF9F),
)

@Composable
fun GpsSosTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkPalette else LightPalette, content = content)
}

@Composable
fun GpsScreen(
    state: GpsUiState,
    onPermission: () -> Unit,
    onLocationSettings: () -> Unit,
    onRefresh: () -> Unit,
    onShare: (GeoFix) -> Unit,
    onCopy: (GeoFix) -> Unit,
    onRead: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(enabled = state.readingFix != null, onBack = onBack)
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 520.dp).fillMaxSize().padding(horizontal = 20.dp)) {
                Row(Modifier.padding(top = 12.dp, bottom = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("GPS ", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("S.O.S", color = MaterialTheme.colorScheme.primary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                val frozen = state.readingFix
                if (frozen != null) {
                    TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text("‹  Wróć do lokalizacji") }
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                        Title("Podaj przez telefon")
                        Text("Współrzędne zatrzymane do odczytania", style = MaterialTheme.typography.bodyMedium)
                        FixStatus(frozen, state.nowNanos)
                        Text("Pomiar: ${LocationText.timestamp(frozen)}", style = MaterialTheme.typography.bodySmall)
                        SpokenCoordinate("1. Szerokość geograficzna", frozen.latitude, true)
                        SpokenCoordinate("2. Długość geograficzna", frozen.longitude, false)
                    }
                } else {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        when {
                            !state.hasPermission -> {
                                Title("Twoje współrzędne, od razu")
                                Text("Zezwól na lokalizację podczas używania aplikacji. Wybierz dokładną lokalizację, aby uzyskać możliwie precyzyjny pomiar.")
                                Spacer(Modifier.height(16.dp))
                                Text("Pozycję możesz przekazać wiadomością lub odczytać przez telefon.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            state.fix != null -> {
                                val fix = state.fix
                                Title(when {
                                    !state.locationEnabled || fix.isStale(state.nowNanos) -> "Ostatnia znana lokalizacja"
                                    fix.approximate -> "Lokalizacja przybliżona"
                                    else -> "Twoja lokalizacja"
                                })
                                FixStatus(fix, state.nowNanos)
                                if (!state.locationEnabled) {
                                    Notice("Lokalizacja telefonu jest wyłączona. Pomiar nie jest odświeżany.")
                                    TextButton(onClick = onLocationSettings) { Text("Włącz lokalizację") }
                                }
                                if (!state.precisePermission) {
                                    TextButton(onClick = onPermission) { Text("Zwiększ dokładność") }
                                }
                                Spacer(Modifier.height(22.dp))
                                Coordinate("Szerokość geograficzna", fix.latitude, true)
                                HorizontalDivider(Modifier.padding(vertical = 20.dp))
                                Coordinate("Długość geograficzna", fix.longitude, false)
                                Spacer(Modifier.height(12.dp))
                                TextButton(onClick = onRefresh) { Text("Odśwież pomiar") }
                            }
                            !state.locationEnabled -> {
                                Title("Włącz lokalizację")
                                Text("Lokalizacja telefonu jest wyłączona. Włącz ją, aby uzyskać współrzędne.")
                            }
                            else -> {
                                Title("Ustalam lokalizację")
                                CircularProgressIndicator(Modifier.padding(vertical = 16.dp).size(30.dp), strokeWidth = 3.dp)
                                Text(if (state.waitingLong) "Wciąż czekam na pomiar. Jeśli możesz, podejdź do okna lub wyjdź na otwartą przestrzeń." else "Oczekiwanie na pierwszy pomiar…")
                                if (!state.precisePermission) {
                                    Spacer(Modifier.height(12.dp))
                                    Text("Telefon udostępnia tylko przybliżoną pozycję.")
                                    TextButton(onClick = onPermission) { Text("Zwiększ dokładność") }
                                }
                            }
                        }
                        state.issue?.let { Notice(it) }
                    }
                    Column(Modifier.padding(top = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        when {
                            !state.hasPermission -> PrimaryAction("Zezwól na lokalizację", onPermission)
                            state.fix != null -> {
                                val fix = state.fix
                                PrimaryAction("Przekaż lokalizację", { onShare(fix) })
                                OutlinedButton(onClick = onRead, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), shape = RoundedCornerShape(16.dp)) {
                                    Text("Podaj przez telefon", fontSize = 16.sp, textAlign = TextAlign.Center)
                                }
                                TextButton(onClick = { onCopy(fix) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Kopiuj współrzędne") }
                            }
                            !state.locationEnabled -> PrimaryAction("Włącz lokalizację", onLocationSettings)
                            else -> {
                                PrimaryAction("Przekaż lokalizację", {}, enabled = false)
                                Text("Czekam na współrzędne", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
                                if (state.waitingLong) TextButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) { Text("Spróbuj ponownie") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Title(text: String) {
    Text(text, modifier = Modifier.padding(bottom = 12.dp).semantics { heading() }, fontSize = 27.sp, lineHeight = 33.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun PrimaryAction(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp), shape = RoundedCornerShape(16.dp)) {
        Text(label, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun FixStatus(fix: GeoFix, nowNanos: Long) {
    Text(LocationText.age(fix, nowNanos), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    Text("Szacowana dokładność: ${LocationText.accuracy(fix)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    if (fix.mock) Notice("Lokalizacja testowa — pochodzi z symulacji GPS.")
    if (fix.isStale(nowNanos)) Notice("Ten pomiar może być nieaktualny. Sprawdź czas pomiaru przed przekazaniem.")
    else if (fix.approximate || (fix.accuracyMeters ?: 0f) > 100) Notice("Pozycja orientacyjna — zwróć uwagę na dokładność pomiaru.")
}

@Composable
private fun Notice(text: String) {
    Surface(Modifier.fillMaxWidth().padding(top = 12.dp), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer) {
        Text(text, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Coordinate(label: String, value: Double, latitude: Boolean) {
    Column {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        SelectionContainer {
            Text(
                LocationText.coordinate(value, latitude),
                Modifier.fillMaxWidth().padding(top = 4.dp).semantics { contentDescription = "$label: ${LocationText.spoken(value, latitude)}" },
                fontSize = 38.sp, lineHeight = 46.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 20.sp, maxFontSize = 38.sp),
            )
        }
        Text(LocationText.direction(value, latitude).replaceFirstChar(Char::uppercase), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SpokenCoordinate(label: String, value: Double, latitude: Boolean) {
    Column(Modifier.padding(top = 24.dp)) {
        Text(label, modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
        Text(LocationText.coordinate(value, latitude), Modifier.padding(vertical = 8.dp), fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
            Text(LocationText.spoken(value, latitude), Modifier.fillMaxWidth().padding(16.dp), fontSize = 22.sp, lineHeight = 33.sp)
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Preview(showBackground = true, widthDp = 360, heightDp = 800, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun PreviewGps() {
    val sample = GeoFix(52.22968, 21.01223, 8f, 1791808328000L, 100_000_000_000)
    GpsSosTheme {
        GpsScreen(GpsUiState(true, true, true, sample, nowNanos = 103_000_000_000), {}, {}, {}, {}, {}, {}, {})
    }
}
