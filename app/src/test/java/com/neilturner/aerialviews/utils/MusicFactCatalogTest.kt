package com.neilturner.aerialviews.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File
import java.time.LocalDate
import java.util.Locale

@DisplayName("Music fact catalog")
internal class MusicFactCatalogTest {
    @Test
    @DisplayName("Should cover every calendar day, including February 29")
    fun coversEveryDay() {
        val facts = loadBundledFacts()
        val expected = MusicFactCatalog.allDateKeys()

        assertEquals(366, expected.size)
        assertTrue("02-29" in expected)
        assertEquals(expected, facts.keys)
    }

    @Test
    @DisplayName("Should keep each fact to two short lines")
    fun linesFitTheOverlay() {
        loadBundledFacts().forEach { (key, fact) ->
            assertTrue(fact.lines.isNotEmpty(), "$key is empty")
            assertTrue(fact.lines.size <= 2, "$key has ${fact.lines.size} lines")
            fact.lines.forEach { line ->
                assertTrue(line.isNotBlank(), "$key has a blank line")
                assertTrue(!line.contains('\n'), "$key embeds a line break")
                assertTrue(line.length <= MAX_LINE_LENGTH, "$key line is ${line.length} characters: $line")
            }
        }
    }

    @Test
    @DisplayName("Should resolve a date to its fact and follow the calendar")
    fun resolvesByDate() {
        val facts = loadBundledFacts()
        val releaseDay = LocalDate.of(1980, 11, 3)
        val text = MusicFactCatalog.overlayTextFor(facts, releaseDay)

        assertTrue(text.contains("9 to 5"), text)
        assertTrue(text.contains("RCA"), text)
        assertTrue(text.contains('\n'))

        val kindOfBlue = MusicFactCatalog.overlayTextFor(facts, LocalDate.of(1959, 8, 17))
        assertTrue(kindOfBlue.contains("Kind of Blue"), kindOfBlue)

        val leapDay = MusicFactCatalog.factFor(facts, LocalDate.of(1964, 2, 29))
        assertTrue(leapDay != null && leapDay.lines.isNotEmpty())

        val nextDay = MusicFactCatalog.dateKey(releaseDay.plusDays(1))
        assertEquals("11-04", nextDay)
        assertTrue(MusicFactCatalog.overlayTextFor(facts, releaseDay.plusDays(1)) != text)
    }

    @Test
    @DisplayName("Should look up facts when the default locale uses non-Latin digits")
    fun lookupUsesAsciiKeysRegardlessOfLocale() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"))
            assertEquals("11-16", MusicFactCatalog.dateKey(11, 16))
            val text = MusicFactCatalog.overlayTextFor(loadBundledFacts(), LocalDate.of(1980, 11, 3))
            assertTrue(text.contains("9 to 5"), text)
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    @DisplayName("Should ignore a date that is not in the calendar")
    fun missingDateIsBlank() {
        val facts = mapOf("01-01" to MusicFact(listOf("Only New Year's Day.")))
        assertNull(MusicFactCatalog.factFor(facts, LocalDate.of(2024, 6, 15)))
        assertEquals("", MusicFactCatalog.overlayTextFor(facts, LocalDate.of(2024, 6, 15)))
    }

    private fun loadBundledFacts(): Map<String, MusicFact> {
        val file =
            listOf(
                File("src/main/assets/music_facts.json"),
                File("app/src/main/assets/music_facts.json"),
            ).first { it.isFile }
        return MusicFactCatalog.parse(file.readText())
    }

    private companion object {
        const val MAX_LINE_LENGTH = 88
    }
}
