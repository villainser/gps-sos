package pl.gpssos.app

import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class LocationTextTest {
    private val fix = GeoFix(52.22968, 21.01223, 8.2f, 0, 100_000_000_000)

    @Test fun `shared decimal always uses dot regardless of phone locale`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.FRANCE)
            assertEquals("52.22968, 21.01223", LocationText.pair(fix))
            assertEquals("52,22968° N", LocationText.coordinate(fix.latitude, true))
        } finally { Locale.setDefault(original) }
    }

    @Test fun `southern and western positions preserve signs in map link`() {
        val south = fix.copy(latitude = -33.8688, longitude = -70.6693)
        assertEquals("33,86880° S", LocationText.coordinate(south.latitude, true))
        assertEquals("70,66930° W", LocationText.coordinate(south.longitude, false))
        assertTrue(LocationText.mapUrl(south).endsWith("query=-33.86880%2C-70.66930"))
    }

    @Test fun `dictation preserves leading zeros after decimal point`() {
        assertEquals("dwa jeden, przecinek,\nzero jeden dwa dwa trzy.\nWschód.", LocationText.spoken(21.01223, false))
    }

    @Test fun `rounding carries into integer part`() {
        assertEquals("90,00000° N", LocationText.coordinate(89.9999999, true))
        assertTrue(LocationText.spoken(89.9999999, true).startsWith("dziewięć zero,"))
    }

    @Test fun `negative zero does not create inconsistent hemispheres`() {
        assertEquals("0.00000", LocationText.decimal(-0.0000001))
        assertEquals("0,00000° N", LocationText.coordinate(-0.0000001, true))
    }

    @Test fun `stale threshold and age use monotonic time`() {
        assertFalse(fix.isStale(159_999_999_999))
        assertTrue(fix.isStale(160_000_000_000))
        assertEquals("Pomiar sprzed 1 min", LocationText.age(fix, 160_000_000_000))
        assertEquals(0L, fix.ageSeconds(99_000_000_000))
    }

    @Test fun `stale share prominently identifies an old location`() {
        val message = LocationText.message(fix, 200_000_000_000, ZoneId.of("UTC"))
        assertTrue(message.startsWith("Ostatnia znana lokalizacja"))
        assertTrue(message.contains("Szacowana dokładność: około 9 m"))
        assertTrue(message.contains("01.01.1970, 00:00:00 (Z)"))
    }

    @Test fun `approximate and mock positions stay labeled when shared`() {
        val message = LocationText.message(fix.copy(approximate = true, mock = true), 100_000_000_000)
        assertTrue(message.startsWith("LOKALIZACJA TESTOWA"))
        assertTrue(message.contains("Lokalizacja przybliżona."))
    }

    @Test fun `missing accuracy is not presented as zero error`() {
        assertEquals("nieznana", LocationText.accuracy(fix.copy(accuracyMeters = null)))
    }

    @Test fun `older cached provider cannot replace newer position`() {
        val old = fix.copy(latitude = 10.0, elapsedNanos = 90_000_000_000, accuracyMeters = 1f)
        assertEquals(fix, LocationText.newer(fix, old))
    }

    @Test fun `new measurement replaces old even when less accurate`() {
        val next = fix.copy(latitude = 10.0, elapsedNanos = 110_000_000_000, accuracyMeters = 80f)
        assertEquals(next, LocationText.newer(fix, next))
    }

    @Test fun `same timestamp prefers measured accuracy over unknown`() {
        assertEquals(fix, LocationText.newer(fix.copy(accuracyMeters = null), fix))
    }

    @Test(expected = IllegalArgumentException::class) fun `invalid coordinates cannot enter share message`() {
        fix.copy(latitude = Double.NaN)
    }

    @Test(expected = IllegalArgumentException::class) fun `out of range longitude rejected`() {
        fix.copy(longitude = 180.1)
    }
}
