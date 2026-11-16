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
 * Music-business note for today's date. A short fact stays on one line; a long
 * one uses a second line. Those lines may wrap on a narrow screen or at a large
 * text size, and the whole note stays visible. Uses [R.style.OverlayText] and
 * the shared font helper, with no background of its own.
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
        // Do not cap the display line count. A fact is one or two logical lines, and
        // those lines wrap on a narrow screen or at a large text size. At the default
        // 15sp on a 1080p TV they still fit without wrapping.
        ellipsize = null
        setLineSpacing(0f, 1.1f)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        MusicFactLoader.whenReady(context) { loaded ->
            post {
                if (!isAttachedToWindow) return@post
                facts = loaded
                shownKey = null
                showFor(LocalDate.now())
            }
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
        if (facts.isEmpty()) {
            text = ""
            visibility = View.GONE
            return
        }
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
