package com.blueskycharts.app.preferences

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import com.blueskycharts.app.R
import com.blueskycharts.app.map.MapViewActivity
import com.blueskycharts.app.subscription.SubscriptionChecker
import com.blueskycharts.app.utility.Log

class ManagePrivacyActivity : SubscriptionChecker(false, true) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_privacy)

        val firebaseConsentCheckmark = findViewById<CheckBox>(R.id.firebaseConsent)
        val ctaButton = findViewById<Button>(R.id.cta)
        ctaButton?.setOnClickListener {
            val firebaseConsent = firebaseConsentCheckmark?.isChecked ?: false

            Preferences.instance.setPreference(Preferences.propertyNameAllowFirebaseLogging, firebaseConsent)
            Preferences.instance.setPreference(Preferences.propertyNameAllowFirebaseCrashalytics, firebaseConsent)

            it.context?.let { it2 -> Log.refreshFirebaseConsent(it2) }

            val intent = Intent(this, MapViewActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()

        val firebaseConsentCheckmark = findViewById<CheckBox>(R.id.firebaseConsent)
        val firebaseConsent = Preferences.instance.getBooleanValue(Preferences.propertyNameAllowFirebaseLogging, Preferences.defaultValueAllowFirebaseLogging)
        val crashalyticsConsent = Preferences.instance.getBooleanValue(Preferences.propertyNameAllowFirebaseCrashalytics, Preferences.defaultValueAllowFirebaseCrashalytics)
        firebaseConsentCheckmark.isChecked = firebaseConsent && crashalyticsConsent
    }
}