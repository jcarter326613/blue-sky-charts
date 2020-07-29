package com.blueskycharts.app.utility

import android.content.Context
import com.blueskycharts.app.preferences.Preferences
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.internal.common.CrashlyticsCore




class Log {
    companion object {
        private val shouldLog: Boolean = com.blueskycharts.app.BuildConfig.DEBUG
        private var crashalytics: FirebaseCrashlytics? = null
        private var crashalyticsConsentGiven: Boolean = Preferences.defaultValueAllowFirebaseCrashalytics
        private var firebaseAnalytics: FirebaseAnalytics? = null
        private var firebaseAnalyticsConsentGiven: Boolean = Preferences.defaultValueAllowFirebaseLogging

        fun refreshFirebaseConsent(context: Context) {
            val crashalytics = crashalytics ?: FirebaseCrashlytics.getInstance()
            crashalyticsConsentGiven = Preferences.instance.getBooleanValue(Preferences.propertyNameAllowFirebaseCrashalytics, Preferences.defaultValueAllowFirebaseCrashalytics)
            crashalytics.setCrashlyticsCollectionEnabled(crashalyticsConsentGiven)

            val firebaseAnalytics = this.firebaseAnalytics ?: FirebaseAnalytics.getInstance(context)
            firebaseAnalyticsConsentGiven = Preferences.instance.getBooleanValue(Preferences.propertyNameAllowFirebaseLogging, Preferences.defaultValueAllowFirebaseLogging)
            firebaseAnalytics.setAnalyticsCollectionEnabled(firebaseAnalyticsConsentGiven)
        }

        fun error(tag: String?, msg: String) {
            if (shouldLog) {
                android.util.Log.e(tag, msg)
            }
            if (firebaseAnalyticsConsentGiven) {
                firebaseAnalytics?.logEvent(msg, null)
            }
            if (crashalyticsConsentGiven) {
                crashalytics?.log(msg)
            }
        }
    }
}