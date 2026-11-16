package com.neilturner.aerialviews.ui.overlays

import android.content.Context
import com.neilturner.aerialviews.utils.MusicFact
import com.neilturner.aerialviews.utils.MusicFactCatalog
import timber.log.Timber
import kotlin.concurrent.thread

/**
 * Reads the bundled facts off the main thread. The dream has to finish binding within
 * five seconds of process start, so this parse must not run while that is happening.
 */
object MusicFactLoader {
    private val lock = Any()
    private var cached: Map<String, MusicFact>? = null
    private var loading = false
    private val waiters = mutableListOf<(Map<String, MusicFact>) -> Unit>()

    fun preload(context: Context) {
        val appContext = context.applicationContext
        synchronized(lock) {
            if (cached != null || loading) return
            loading = true
        }
        thread(name = "music-facts", isDaemon = true) {
            val parsed = read(appContext)
            val pending =
                synchronized(lock) {
                    cached = parsed
                    loading = false
                    waiters.toList().also { waiters.clear() }
                }
            pending.forEach { it(parsed) }
        }
    }

    fun whenReady(
        context: Context,
        onLoaded: (Map<String, MusicFact>) -> Unit,
    ) {
        synchronized(lock) {
            val existing = cached
            if (existing != null) {
                onLoaded(existing)
                return
            }
            waiters += onLoaded
        }
        preload(context)
    }

    private fun read(context: Context): Map<String, MusicFact> =
        try {
            val raw =
                context.assets.open(MusicFactCatalog.ASSET_FILE).bufferedReader().use { reader ->
                    reader.readText()
                }
            MusicFactCatalog.parse(raw)
        } catch (ex: Exception) {
            Timber.e(ex, "Unable to load this-day-in-music facts")
            emptyMap()
        }
}
