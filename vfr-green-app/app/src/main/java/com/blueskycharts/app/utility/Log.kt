package com.blueskycharts.app.utility

import android.content.Context
import android.os.Bundle
import com.blueskycharts.app.map.view.OverlayTypes
import com.blueskycharts.app.preferences.Preferences
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
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
            this.crashalytics = crashalytics
            crashalyticsConsentGiven = Preferences.instance.getBooleanValue(Preferences.propertyNameAllowFirebaseCrashalytics, Preferences.defaultValueAllowFirebaseCrashalytics)
            crashalytics.setCrashlyticsCollectionEnabled(crashalyticsConsentGiven)

            val firebaseAnalytics = this.firebaseAnalytics ?: FirebaseAnalytics.getInstance(context)
            this.firebaseAnalytics = firebaseAnalytics
            firebaseAnalyticsConsentGiven = Preferences.instance.getBooleanValue(Preferences.propertyNameAllowFirebaseLogging, Preferences.defaultValueAllowFirebaseLogging)
            firebaseAnalytics.setAnalyticsCollectionEnabled(firebaseAnalyticsConsentGiven)
        }

        fun error(tag: String?, msg: String) {
            if (shouldLog) {
                android.util.Log.e(tag, msg)
            }
            if (crashalyticsConsentGiven) {
                crashalytics?.log(msg)
            }
        }

        fun mapSelection(mapName: String) {
            if (firebaseAnalyticsConsentGiven) {
                firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT) {
                    param(FirebaseAnalytics.Param.CONTENT_TYPE, "map")
                    param(FirebaseAnalytics.Param.ITEM_ID, "map.$mapName")
                }
            }
        }

        fun overlaySelection(type: OverlayTypes) {
            if (firebaseAnalyticsConsentGiven) {
                firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT) {
                    param(FirebaseAnalytics.Param.CONTENT_TYPE, "overlay")
                    param(FirebaseAnalytics.Param.ITEM_ID, "overlay.${type}")
                }
            }
        }

        fun changePreference(preferenceName: String) {
            if (preferenceName.startsWith("map.") && preferenceName.endsWith(".position")) {
                return
            }
            if (firebaseAnalyticsConsentGiven) {
                val value = Preferences.instance.getStringValue(preferenceName, "")
                firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT) {
                    param(FirebaseAnalytics.Param.CONTENT_TYPE, "preference_change")
                    param(FirebaseAnalytics.Param.ITEM_ID, "preference_change.$preferenceName=$value")
                }
            }
        }
    }
}