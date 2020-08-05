package com.blueskycharts.app.subscription

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.DialogInterface.OnShowListener
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.opengl.Visibility
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BulletSpan
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.*
import com.blueskycharts.app.R
import com.blueskycharts.app.assests.AssetProvider
import com.blueskycharts.app.assests.RemoteAssetDescription
import com.blueskycharts.app.assests.StorageLocation
import com.blueskycharts.app.assests.Volatility
import com.blueskycharts.app.map.MapViewActivity
import com.blueskycharts.app.mapselection.MapSelectionActivity
import com.blueskycharts.app.preferences.Preferences
import com.blueskycharts.app.preferences.SetPreferencesActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.URL

class SubscriptionActivity : SubscriptionChecker(true, false)  {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)

        val addSubscriptionButton = findViewById<Button>(R.id.add_subscription_button)
        addSubscriptionButton.setOnClickListener {
            sendCustomerToOrderFlow()
        }

        val newCustomer = intent.getBooleanExtra(NewCustomerParameter, false)
        if (newCustomer) {
            setupFreeTrialText()
        } else {
            setupReturningCustomerText()
        }
    }

    private fun setupReturningCustomerText() {
        setupText(R.string.subscription_ad_returning_customer, R.string.subscription_ad_returning_customer_cta)
    }

    private fun setupFreeTrialText() {
        setupText(R.string.subscription_ad_free_trial, R.string.subscription_ad_free_trial_cta)
    }

    private fun setupText(marketingTextId: Int, ctaId: Int) {
        val freeTrialText = findViewById<TextView>(R.id.marketingText)
        val freeTrialString = getString(marketingTextId)
        val freeTrialSpanText = SpannableString(freeTrialString.replace("|", "\n"))
        val gapWidth = convertDipToPixels(5f)

        var currentIndex = freeTrialString.indexOf("|")
        var nextIndex = freeTrialString.indexOf("|", currentIndex + 1)

        while (currentIndex >= 0) {
            if (nextIndex > currentIndex) {
                freeTrialSpanText.setSpan(BulletSpan(gapWidth), currentIndex+1, nextIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else {
                freeTrialSpanText.setSpan(BulletSpan(gapWidth), currentIndex+1, freeTrialString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            currentIndex = nextIndex
            nextIndex = freeTrialString.indexOf("|", currentIndex + 1)
        }

        freeTrialText.text = freeTrialSpanText
        freeTrialText.visibility = View.VISIBLE

        // Setup the cta
        val addSubscriptionButton = findViewById<Button>(R.id.add_subscription_button)
        addSubscriptionButton.text = getString(ctaId)
    }

    private fun convertDipToPixels(dp: Float): Int {
        return (dp * applicationContext.resources.displayMetrics.density + 0.5f).toInt()
    }

    companion object {
        const val NewCustomerParameter = "newCustomer"
    }
}