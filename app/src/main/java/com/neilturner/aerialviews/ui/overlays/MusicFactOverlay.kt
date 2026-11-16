package com.neilturner.aerialviews.ui.overlays

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.widget.TextViewCompat
import com.neilturner.aerialviews.R
import com.neilturner.aerialviews.utils.MusicFact
import com.neilturner.aerialviews.utils.MusicFactCatalog
import java.time.LocalDate

/**
 * Two-line music-business note for today's date. Re-checks the date while the
 * screensaver is up so the fact changes after midnight.
 */
class MusicFactOverlay : AppCompatTextView {
    private var facts: Map<String, MusicFact> = emptyMap()
    private var shownKey: String? = null
    private var refresh: Runnable? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        TextViewCompat.setTextAppearance(this, R.style.OverlayText)
        maxLines = 2
        setLineSpacing(0f, 1.1f)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (facts.isEmpty()) {
            facts = MusicFactLoader.load(context)
        }
        showFor(LocalDate.now())
        scheduleRefresh()
    }

    override fun onDetachedFromWindow() {
        refresh?.let { removeCallbacks(it) }
        refresh = null
        super.onDetachedFromWindow()
    }

    private fun scheduleRefresh() {
        refresh?.let { removeCallbacks(it) }
        val task =
            Runnable {
                showFor(LocalDate.now())
                scheduleRefresh()
            }
        refresh = task
        postDelayed(task, REFRESH_INTERVAL_MS)
    }

    private fun showFor(date: LocalDate) {
        val key = MusicFactCatalog.dateKey(date)
        if (key == shownKey) return
        shownKey = key
        val fact = MusicFactCatalog.overlayTextFor(facts, date)
        text = fact
        visibility = if (fact.isBlank()) View.GONE else View.VISIBLE
    }

    private companion object {
        const val REFRESH_INTERVAL_MS = 60_000L
    }
}
