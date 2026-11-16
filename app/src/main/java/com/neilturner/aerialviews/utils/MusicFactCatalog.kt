package com.neilturner.aerialviews.utils

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate

/**
 * One bundled "this day in music" note: at most two short overlay lines.
 */
data class MusicFact(
    val lines: List<String>,
) {
    fun overlayText(): String = lines.joinToString(separator = "\n")
}

/**
 * Offline calendar of music-business facts, keyed by "MM-dd".
 * Parsing stays free of Android so the bundled file can be checked on the JVM.
 */
object MusicFactCatalog {
    const val ASSET_FILE = "music_facts.json"

    private val json = Json { ignoreUnknownKeys = true }

    fun dateKey(
        month: Int,
        day: Int,
    ): String = "%02d-%02d".format(month, day)

    fun dateKey(date: LocalDate): String = dateKey(date.monthValue, date.dayOfMonth)

    fun parse(raw: String): Map<String, MusicFact> {
        val root =
            json.parseToJsonElement(raw) as? JsonObject
                ?: error("Music facts must be a JSON object")
        return root.mapValues { (key, value) ->
            val array =
                value as? JsonArray
                    ?: error("Fact $key must be an array of lines")
            MusicFact(array.map { it.jsonPrimitive.content })
        }
    }

    fun factFor(
        facts: Map<String, MusicFact>,
        date: LocalDate,
    ): MusicFact? = facts[dateKey(date)]

    fun overlayTextFor(
        facts: Map<String, MusicFact>,
        date: LocalDate,
    ): String = factFor(facts, date)?.overlayText().orEmpty()

    /** Every calendar day, including February 29. */
    fun allDateKeys(): Set<String> {
        val keys = mutableSetOf<String>()
        for (month in 1..12) {
            val days =
                when (month) {
                    2 -> 29
                    4, 6, 9, 11 -> 30
                    else -> 31
                }
            for (day in 1..days) {
                keys += dateKey(month, day)
            }
        }
        return keys
    }
}
