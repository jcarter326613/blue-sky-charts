package com.blueskycharts.app

import androidx.appcompat.app.AppCompatActivity

open class BlueSkyChartsActivity(private val redirectOnPurchaseMade: Boolean, private val redirectOnNotPurchased: Boolean) : AppCompatActivity() {
    override fun onStart() {
        super.onStart()

        val application = application as BlueSkyChartsApplication
        application.redirectOnNotPurchased = redirectOnNotPurchased
        application.redirectOnPurchaseMade = redirectOnPurchaseMade
        application.activeActivityContext = this
    }

    override fun onStop() {
        val application = application as BlueSkyChartsApplication
        if (application.activeActivityContext == this) {
            application.activeActivityContext = null
        }
        super.onStop()
    }
}