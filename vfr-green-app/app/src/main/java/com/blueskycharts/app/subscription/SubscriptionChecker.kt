package com.blueskycharts.app.subscription

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.*
import com.blueskycharts.app.BlueSkyChartsApplication
import com.blueskycharts.app.R
import com.blueskycharts.app.map.MapViewActivity
import com.blueskycharts.app.preferences.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.util.concurrent.atomic.AtomicBoolean

open class SubscriptionChecker(): Application(), BillingClientStateListener, PurchasesUpdatedListener {
    private val skipCheck = false // com.blueskycharts.app.BuildConfig.DEBUG
    var activeActivityContext: Context? = null
    private var billingClient: BillingClient? = null
    private var subscriptionVerified = AtomicBoolean(false)
    var redirectOnNotPurchased: Boolean = false
        set(value) {
            field = value
            if (value && subscriptionStatus == SubscriptionStatus.NotActive) {
                val intent = Intent(this, SubscriptionActivity::class.java)
                intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
                intent.addFlags(FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
            }
        }
    var redirectOnPurchaseMade: Boolean = false
    var subscriptionStatus: SubscriptionStatus = SubscriptionStatus.Unknown

    override fun onCreate() {
        super.onCreate()
        setupBillingClient()
        verifySubscription()
    }

    private fun setupBillingClient() {
        if (billingClient != null) {
            return
        }
        billingClient = BillingClient.newBuilder(this)
            .setListener(this)
            .enablePendingPurchases()
            .build()
        billingClient?.startConnection(this)
    }

    fun sendCustomerToOrderFlow(parentActivity: Activity) {
        GlobalScope.launch {
            var billingClient = this@SubscriptionChecker.billingClient
            while (billingClient == null) {
                yield()
                billingClient = this@SubscriptionChecker.billingClient
            }

            //Billing client is not null
            var purchaseSku: SkuDetails? = null
            val params = SkuDetailsParams.newBuilder()
            params.setSkusList(listOf(skuBasicAnnual)).setType(BillingClient.SkuType.SUBS)
            val details = billingClient.querySkuDetails(params.build())
            val skuDetailsList = details.skuDetailsList
            if (skuDetailsList != null) {
                for (skuDetail in skuDetailsList) {
                    if (skuDetail.sku == skuBasicAnnual) {
                        purchaseSku = skuDetail
                    }
                }
            }

            if (purchaseSku != null) {
                val flowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(purchaseSku)
                    .build()
                val responseCode = billingClient.launchBillingFlow(parentActivity, flowParams).responseCode
                if (responseCode != BillingClient.BillingResponseCode.OK) {
                    purchaseSku = null
                }
            }

            if (purchaseSku == null) {
                GlobalScope.launch(Dispatchers.Main) {
                    val alertDialog = AlertDialog.Builder(this@SubscriptionChecker.activeActivityContext, R.style.AlertTheme)
                        .setMessage("There was a problem sending you to the purchase flow.  Please ensure you are logged into the Google Play Store and restart this app.")
                        .setPositiveButton("Ok") { _: DialogInterface, _: Int ->
                        }
                        .create()
                    alertDialog.show()
                }
            }
        }
    }

    private fun verifySubscription() {
        val billingClient = this.billingClient?: return
        if (!billingClient.isReady) {
            return
        }
        if (subscriptionVerified.getAndSet(true)) {
            return
        }

        var isSubscriptionPurchased = skipCheck
        if (billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS).responseCode != BillingClient.BillingResponseCode.OK) {
            // Notify that they need to update their google play store application because subscriptions are not supported on their install
            if (!redirectOnNotPurchased) {
                val alertDialog = AlertDialog.Builder(this@SubscriptionChecker.activeActivityContext, R.style.AlertTheme)
                    .setMessage("Your version of Google Play Store does not support subscriptions.  Please update before proceeding.")
                    .setPositiveButton("Ok") { _: DialogInterface, _: Int ->
                    }
                    .create()
                alertDialog.show()
            }
        } else {
            // Check if the user already has a subscription
            val queryResults = billingClient.queryPurchases(BillingClient.SkuType.SUBS)
            if (queryResults.responseCode == BillingClient.BillingResponseCode.OK) {
                val purchaseList = queryResults.purchasesList
                if (purchaseList != null) {
                    for (purchase in purchaseList) {
                        if (purchase.sku == skuBasicAnnual && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                            if (!purchase.isAcknowledged) {
                                GlobalScope.launch {
                                    val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                                        .setPurchaseToken(purchase.purchaseToken)
                                        .build()
                                    billingClient.acknowledgePurchase(acknowledgeParams)
                                }
                            }
                            isSubscriptionPurchased = true
                        }
                    }
                }
            }
        }

        if (!isSubscriptionPurchased) {
            subscriptionStatus = SubscriptionStatus.NotActive
            if (redirectOnNotPurchased) {
                val intent = Intent(this, SubscriptionActivity::class.java)
                intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
                intent.addFlags(FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
            }
        } else {
            Preferences.instance.setPreference(Preferences.propertyNameMapShift, mapShift)
            subscriptionStatus = SubscriptionStatus.Active
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                var acknowledge = !purchase.isAcknowledged
                when (purchase.purchaseState) {
                    Purchase.PurchaseState.PURCHASED -> {
                        //TODO: Look at doing this in the future
                        //https://developer.android.com/google/play/billing/security#verify

                        subscriptionStatus = SubscriptionStatus.Active
                        Preferences.instance.setPreference(Preferences.propertyNameMapShift, mapShift)
                        val alertDialog = AlertDialog.Builder(this@SubscriptionChecker.activeActivityContext, R.style.AlertTheme)
                            .setMessage("Your subscription is now active")
                            .setPositiveButton("Ok") { _: DialogInterface, _: Int ->
                                (this@SubscriptionChecker.applicationContext as BlueSkyChartsApplication).alreadyAskedLocationPermission = false
                            }
                            .create()
                        alertDialog.show()
                        alertDialog.setOnDismissListener {
                            if (redirectOnPurchaseMade) {
                                val intent = Intent(this, MapViewActivity::class.java)
                                intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
                                intent.addFlags(FLAG_ACTIVITY_CLEAR_TASK)
                                startActivity(intent)
                            }
                        }
                    }
                    Purchase.PurchaseState.PENDING -> {
                        val alertDialog = AlertDialog.Builder(this@SubscriptionChecker.activeActivityContext, R.style.AlertTheme)
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
            verifySubscription()
        } else {
            if (Preferences.instance.getIntValue(Preferences.propertyNameMapShift, Preferences.defaultValueMapShift) != mapShift) {
                subscriptionStatus = SubscriptionStatus.NotActive
                if (redirectOnNotPurchased) {
                    val intent = Intent(this, SubscriptionActivity::class.java)
                    intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
                    intent.addFlags(FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                }
            }
        }
    }

    override fun onBillingServiceDisconnected() {
        GlobalScope.launch(Dispatchers.IO) {
            billingClient?.startConnection(this@SubscriptionChecker)
        }
    }

    companion object {
        const val skuBasicAnnual = "basic.annual"
        private const val mapShift = 20
    }
}