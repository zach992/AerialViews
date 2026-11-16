package com.neilturner.aerialviews.utils

import android.content.Context
import android.os.Bundle
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.crashlytics
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.Executors

object FirebaseHelper {
    private const val LOGGING_END_DATE = "2026-11-01"

    // One thread so initializeApp always finishes before any Firebase call, and none of that
    // work sits on the main thread where it would delay the dream binding.
    private val executor =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "firebase-init").apply { isDaemon = true }
        }

    @Volatile
    private var appContext: Context? = null

    fun start(context: Context) {
        appContext = context.applicationContext
        executor.execute { initialize() }
    }

    private fun initialize() {
        val context = appContext ?: return
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
    }

    private fun submit(block: () -> Unit) {
        executor.execute {
            try {
                initialize()
                block()
            } catch (ex: Exception) {
                Timber.w(ex, "Firebase call failed")
            }
        }
    }

    private fun isWithinLoggingPeriod(): Boolean {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val endDate = simpleDateFormat.parse(LOGGING_END_DATE)
        val currentDate = Calendar.getInstance().time
        return currentDate.before(endDate)
    }

    fun analyticsScreenView(
        screenName: String,
        screenClass: Any,
    ) {
        val screenClassName = screenClass::class.java.simpleName
        submit {
            val parameters =
                Bundle().apply {
                    putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                    putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClassName)
                }
            Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, parameters)
        }
    }

    fun analyticsEvent(
        eventName: String,
        parameters: Bundle,
        alwaysLog: Boolean = false,
    ) {
        if (isWithinLoggingPeriod() || alwaysLog) {
            val copy = Bundle(parameters)
            submit {
                Firebase.analytics.logEvent(eventName, copy)
            }
        }
    }

    fun crashlyticsException(
        ex: Throwable?,
        alwaysLog: Boolean = false,
    ) {
        if (isWithinLoggingPeriod() || alwaysLog) {
            ex?.let { error ->
                submit {
                    Firebase.crashlytics.recordException(error)
                }
            }
        }
    }

    fun crashlyticsLogMessage(
        error: String,
        alwaysLog: Boolean = false,
    ) {
        if (isWithinLoggingPeriod() || alwaysLog) {
            submit {
                Firebase.crashlytics.log(error)
            }
        }
    }

    fun <T> crashlyticsLogKeys(
        key: String,
        value: T,
        alwaysLog: Boolean = false,
    ) {
        if (isWithinLoggingPeriod() || alwaysLog) {
            submit {
                when (value) {
                    is String -> Firebase.crashlytics.setCustomKey(key, value)
                    is Int -> Firebase.crashlytics.setCustomKey(key, value)
                    is Long -> Firebase.crashlytics.setCustomKey(key, value)
                    is Float -> Firebase.crashlytics.setCustomKey(key, value)
                    is Double -> Firebase.crashlytics.setCustomKey(key, value)
                    is Boolean -> Firebase.crashlytics.setCustomKey(key, value)
                    else -> Firebase.crashlytics.setCustomKey(key, value.toString())
                }
            }
        }
    }
}
