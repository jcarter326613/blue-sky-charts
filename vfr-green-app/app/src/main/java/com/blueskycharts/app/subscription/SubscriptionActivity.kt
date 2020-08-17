package com.blueskycharts.app.subscription

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BulletSpan
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.blueskycharts.app.R

class SubscriptionActivity : SubscriptionChecker(true, false)  {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)

        val addSubscriptionButton = findViewById<Button>(R.id.add_subscription_button)
        addSubscriptionButton.setOnClickListener {
            sendCustomerToOrderFlow()
        }

        setupFreeTrialText()
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
        return (dp * baseContext.resources.displayMetrics.density + 0.5f).toInt()
    }
}