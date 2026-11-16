package com.neilturner.aerialviews.utils

import android.content.Context
import android.os.Bundle

object FirebaseHelper {
    fun start(context: Context) {
        // No op. This flavor does not ship Firebase.
    }

    fun logException(ex: Throwable) {
        // No op
    }

    fun analyticsScreenView(
        screenName: String,
        screenClass: Any,
    ) {
        // No op
    }

    fun analyticsEvent(
        eventName: String,
        parameters: Bundle,
        alwaysLog: Boolean = false,
    ) {
        // No op
    }

    fun crashlyticsException(
        ex: Throwable?,
        alwaysLog: Boolean = false,
    ) {
        // No op
    }

    fun crashlyticsLogMessage(
        error: String,
        alwaysLog: Boolean = false,
    ) {
        // No op
    }

    fun <T> crashlyticsLogKeys(
        key: String,
        value: T,
        alwaysLog: Boolean = false,
    ) {
        // No op
    }
}
