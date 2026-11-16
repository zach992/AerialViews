package com.neilturner.aerialviews.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

@DisplayName("Music city catalog")
internal class MusicCityCatalogTest {
    private val catalog =
        MusicCityCatalog(
            footage =
                mapOf(
                    "london" to FootageSet("London", listOf("comp_L010_C006", "video_inspire_uk_london_")),
                    "city-night" to FootageSet("City at night", listOf("comp_DB_D002_C003")),
                ),
            dates =
                mapOf(
                    "08-08" to
                        FactPlace(
                            place = "London",
                            tieIn = "where Abbey Road was recorded",
                            direct = "london",
                            footage = listOf("london", "city-night"),
                        ),
                    "07-13" to
                        FactPlace(
                            place = "Philadelphia",
                            footage = listOf("city-night"),
                        ),
                ),
        )

    private val londonClip = "comp_L010_C006_PS_v01_SDR_PS_20180925_SDR_2K_HEVC.mov"
    private val dubaiNight = "comp_DB_D002_C003_PSNK_v04_SDR_PS_FINAL_20180709_SDR_2K_HEVC.mov"
    private val other = "comp_GMT026_363A_103NC_E1027_KOREA_JAPAN_NIGHT_v18_SDR.mov"

    @Test
    @DisplayName("Should caption direct footage with its own place and the tie-in")
    fun directMatchGetsTieIn() {
        val selection = catalog.select("08-08", listOf(other, londonClip, dubaiNight)) { it }
        assertNotNull(selection)
        assertEquals("london", selection!!.footageKey)
        assertTrue(selection.isDirect)
        assertEquals("London, where Abbey Road was recorded", selection.caption)
        assertEquals(listOf(londonClip), selection.clips)
    }

    @Test
    @DisplayName("Should fall back without a tie-in when the city has no available clip")
    fun fallbackHasNoCaption() {
        val selection = catalog.select("08-08", listOf(other, dubaiNight)) { it }
        assertNotNull(selection)
        assertEquals("city-night", selection!!.footageKey)
        assertFalse(selection.isDirect)
        assertNull(selection.caption)
    }

    @Test
    @DisplayName("Should do nothing for a date without a place or without footage")
    fun noPlaceNoSelection() {
        assertNull(catalog.select("01-01", listOf(londonClip)) { it })
        assertNull(catalog.select("08-08", listOf(other)) { it })
    }

    @Test
    @DisplayName("Should open each day with a city clip, then interleave and rotate")
    fun spotlightRotation() {
        val second = "video_inspire_uk_london_hampstead-heath_003.1080-sdr.mov"
        val spotlight = CitySpotlight(catalog, listOf(londonClip, second)) { it }
        val picks = (0 until 11).map { spotlight.next("08-08")?.first }
        // Two clips: every 5th item, starting with the first.
        assertEquals(londonClip, picks[0])
        assertEquals(second, picks[5])
        assertEquals(londonClip, picks[10])
        assertEquals(3, picks.count { it != null })

        // A new day restarts with a city clip straight away.
        val nextDay = spotlight.next("07-13")
        assertNull(nextDay)
        assertEquals(londonClip, spotlight.next("08-08")?.first)
    }

    @Test
    @DisplayName("Should keep the bundled catalog consistent with the bundled facts")
    fun bundledCatalogIsConsistent() {
        val bundled = MusicCityCatalog.parse(asset("music_fact_places.json").readText())
        val facts = MusicFactCatalog.parse(asset("music_facts.json").readText())

        assertTrue(bundled.footage.containsKey(MusicCityCatalog.CITY_NIGHT))
        bundled.dates.forEach { (key, place) ->
            assertTrue(key in facts.keys, "$key is not a fact date")
            assertTrue(place.place.isNotBlank(), "$key has a blank place")
            assertTrue(place.footage.isNotEmpty(), "$key has no footage chain")
            place.footage.forEach { set ->
                assertTrue(bundled.footage.containsKey(set), "$key refers to unknown footage $set")
            }
            if (place.direct != null) {
                assertEquals(place.direct, place.footage.first(), "$key direct footage must be tried first")
            } else {
                assertNull(place.tieIn, "$key has a tie-in but no footage of that place")
            }
            place.tieIn?.let { assertTrue(it.length <= 48, "$key tie-in is too long: $it") }
        }
        bundled.footage.forEach { (key, set) ->
            assertTrue(set.label.isNotBlank(), "$key has no label")
            assertTrue(set.tokens.isNotEmpty(), "$key has no clips")
        }
    }

    private fun asset(name: String): File =
        listOf(
            File("src/main/assets/$name"),
            File("app/src/main/assets/$name"),
        ).first { it.isFile }
}
