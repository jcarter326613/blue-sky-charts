package com.blueskycharts.app.subscription

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.*
import com.blueskycharts.app.map.MapViewActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SubscriptionChecker(private val redirectOnPurchaseMade): AppCompatActivity(), BillingClientStateListener, PurchasesUpdatedListener {
    private var billingClient: BillingClient? = null

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        billingClient = BillingClient.newBuilder(this)
            .setListener(this)
            .enablePendingPurchases()
            .build()
        billingClient?.startConnection(this)
    }

    override fun onResume() {
        super.onResume()
        do query purchases
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                var acknowledge = !purchase.isAcknowledged
                when (purchase.purchaseState) {
                    Purchase.PurchaseState.PURCHASED -> {
                        //TODO: Look at doing this in the future
                        //https://developer.android.com/google/play/billing/security#verify

                        val alertDialog = AlertDialog.Builder(this)
                            .setMessage("Your subscription is now active")
                            .setPositiveButton("Ok") { _: DialogInterface, _: Int ->
                            }
                            .create()
                        alertDialog.show()
                        alertDialog.setOnDismissListener {
                            if (redirectOnPurchaseMade) {
                                startActivity(Intent(this, MapViewActivity::class.java))
                            }
                        }
                    }
                    Purchase.PurchaseState.PENDING -> {
                        val alertDialog = AlertDialog.Builder(this)
                            .setMessage("Your payment is pending.  Your subscription will activate when your payment is complete.")
                            .setPositiveButton("Ok") { _: DialogInterface, _: Int ->
                            }
                            .create()
                        alertDialog.show()
                    }
                    else -> {
                        acknowledge = false
                    }
                }

                if (acknowledge) {
                    GlobalScope.launch {
                        val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                        billingClient?.acknowledgePurchase(acknowledgeParams)
                    }
                }
            }
        }
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode ==  BillingClient.BillingResponseCode.OK) {
            // The BillingClient is ready. You can query purchases here.
            if (billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS).responseCode != BillingClient.BillingResponseCode.OK) {
                // Notify that they need to update their google play store application because subscriptions are not supported on their install
            } else {
                // Check if the user already has a subscription
                val queryResults = billingClient.queryPurchases(BillingClient.SkuType.SUBS)
                val isSubscriptionPurchased = queryResults.responseCode == Purchase.PurchaseState.PURCHASED

                GlobalScope.launch {
                    val params = SkuDetailsParams.newBuilder()
                    params.setSkusList(listOf("basic.annual")).setType(BillingClient.SkuType.SUBS)
                    val details = billingClient.querySkuDetails(params.build())
                    val skuDetailsList = details.skuDetailsList
                    if (skuDetailsList != null) {
                        for (skuDetail in skuDetailsList) {
                            val freeTrialPeriod = skuDetail.freeTrialPeriod
                            val queryResults2 = billingClient.queryPurchases(BillingClient.SkuType.SUBS)
                            val isSubscriptionPurchased2 = queryResults2.responseCode == Purchase.PurchaseState.PURCHASED

                            var t0 = 3
                            t0 = 1
                        }
                    }

                    var t0 = 0
                    t0 = 2
                }
            }
        }
    }

    override fun onBillingServiceDisconnected() {
        // Try to restart the connection on the next request to
        // Google Play by calling the startConnection() method.
        GlobalScope.launch(Dispatchers.IO) {
            billingClient.startConnection(this@SubscriptionChecker)
        }
    }
}