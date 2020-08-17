package com.blueskycharts.app

import androidx.appcompat.app.AppCompatActivity

open class BlueSkyChartsActivity(private val redirectOnPurchaseMade: Boolean, private val redirectOnNotPurchased: Boolean) : AppCompatActivity() {
    override fun onResume() {
        super.onResume()

        val application = application as BlueSkyChartsApplication
        application.redirectOnNotPurchased = redirectOnNotPurchased
        application.redirectOnPurchaseMade = redirectOnPurchaseMade
    }
}