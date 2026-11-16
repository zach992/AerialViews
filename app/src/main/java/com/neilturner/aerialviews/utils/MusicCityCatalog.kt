package com.neilturner.aerialviews.utils

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A set of shipped aerial clips that really show one place. [tokens] are matched against the
 * clip's file name, so a clip only joins a set when its own manifest entry is that place.
 * [label] names the place in the footage and is what a tie-in caption starts with.
 */
@Serializable
data class FootageSet(
    val label: String,
    val tokens: List<String>,
)

/**
 * Where the fact for one date happened, and which footage sets to try for it, best first.
 * [direct] is the footage set that actually shows [place] (null when no shipped clip does);
 * only that set may carry the [tieIn] caption.
 */
@Serializable
data class FactPlace(
    val place: String,
    val region: String? = null,
    val tieIn: String? = null,
    val direct: String? = null,
    val footage: List<String> = emptyList(),
)

@Serializable
data class MusicCityCatalog(
    val footage: Map<String, FootageSet> = emptyMap(),
    val dates: Map<String, FactPlace> = emptyMap(),
) {
    fun isInSet(
        setKey: String,
        fileName: String,
    ): Boolean {
        val set = footage[setKey] ?: return false
        val name = fileName.lowercase()
        return set.tokens.any { name.contains(it.lowercase()) }
    }

    /** Every token in the catalog, used to keep only clips that can ever be picked. */
    fun matchesAnySet(fileName: String): Boolean = footage.keys.any { isInSet(it, fileName) }

    /**
     * Picks the clips to prefer on [dateKey]. Walks the date's footage chain and returns the first
     * set that has at least one available clip. The tie-in caption is only attached when that set
     * is the direct match, so footage is never captioned as a place it isn't.
     */
    fun <T> select(
        dateKey: String,
        candidates: List<T>,
        fileNameOf: (T) -> String,
    ): CitySelection<T>? {
        val place = dates[dateKey] ?: return null
        for (key in place.footage) {
            val set = footage[key] ?: continue
            val clips = candidates.filter { isInSet(key, fileNameOf(it)) }
            if (clips.isEmpty()) continue
            val isDirect = key == place.direct
            val caption =
                if (isDirect && !place.tieIn.isNullOrBlank()) {
                    "${set.label}, ${place.tieIn}"
                } else {
                    null
                }
            return CitySelection(
                dateKey = dateKey,
                footageKey = key,
                isDirect = isDirect,
                caption = caption,
                clips = clips,
            )
        }
        return null
    }

    companion object {
        const val ASSET_FILE = "music_fact_places.json"
        const val CITY_NIGHT = "city-night"

        private val json = Json { ignoreUnknownKeys = true }

        fun parse(raw: String): MusicCityCatalog = json.decodeFromString(serializer(), raw)

        /**
         * How often a city clip is slotted in. The first item of a day is always one; after that
         * a larger pool comes round more often, a single clip less often so it does not loop.
         */
        fun interval(poolSize: Int): Int =
            when {
                poolSize <= 0 -> Int.MAX_VALUE
                poolSize == 1 -> 8
                poolSize == 2 -> 5
                else -> 3
            }
    }
}

data class CitySelection<T>(
    val dateKey: String,
    val footageKey: String,
    val isDirect: Boolean,
    val caption: String?,
    val clips: List<T>,
)

/**
 * Slots the date's city clips into playback. The first item of each day is a city clip, then one
 * every [MusicCityCatalog.interval] items, rotating through the pool. The day is re-checked on
 * every call, so the city follows the fact across midnight.
 */
class CitySpotlight<T>(
    private val catalog: MusicCityCatalog,
    private val candidates: List<T>,
    private val fileNameOf: (T) -> String,
) {
    private var dateKey: String? = null
    private var selection: CitySelection<T>? = null
    private var counter = 0
    private var rotation = 0

    /** A clip to play instead of the next playlist item, or null to play the playlist item. */
    fun next(dateKey: String): Pair<T, CitySelection<T>>? {
        if (dateKey != this.dateKey) {
            this.dateKey = dateKey
            selection = catalog.select(dateKey, candidates, fileNameOf)
            counter = 0
            rotation = 0
        }
        val current = selection ?: return null
        val due = counter % MusicCityCatalog.interval(current.clips.size) == 0
        counter++
        if (!due) return null
        val clip = current.clips[rotation % current.clips.size]
        rotation++
        return clip to current
    }
}
