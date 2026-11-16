package com.neilturner.aerialviews.ui.overlays

import android.content.Context
import com.neilturner.aerialviews.utils.MusicFact
import com.neilturner.aerialviews.utils.MusicFactCatalog
import timber.log.Timber

/**
 * Reads the bundled facts once. A bad or missing asset leaves the overlay blank
 * instead of taking down the screensaver.
 */
object MusicFactLoader {
    private var cached: Map<String, MusicFact>? = null

    fun load(context: Context): Map<String, MusicFact> {
        cached?.let { return it }
        return try {
            val raw =
                context.assets.open(MusicFactCatalog.ASSET_FILE).bufferedReader().use { reader ->
                    reader.readText()
                }
            MusicFactCatalog.parse(raw).also { parsed ->
                if (parsed.isNotEmpty()) {
                    cached = parsed
                }
            }
        } catch (ex: Exception) {
            Timber.e(ex, "Unable to load this-day-in-music facts")
            emptyMap()
        }
    }
}
