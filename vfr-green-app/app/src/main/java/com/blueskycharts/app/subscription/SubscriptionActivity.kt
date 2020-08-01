package com.blueskycharts.app.subscription

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.DialogInterface.OnShowListener
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.*
import com.blueskycharts.app.R
import com.blueskycharts.app.assests.AssetProvider
import com.blueskycharts.app.assests.RemoteAssetDescription
import com.blueskycharts.app.assests.StorageLocation
import com.blueskycharts.app.assests.Volatility
import com.blueskycharts.app.map.MapViewActivity
import com.blueskycharts.app.preferences.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.URL

class SubscriptionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)

        val goToMapsButton = findViewById<Button>(R.id.go_to_map_button)
        goToMapsButton.setOnClickListener {
            startActivity(Intent(this, MapViewActivity::class.java))
        }

        // Setup billing api stuff
        val purchaseUpdateListener =
            PurchasesUpdatedListener { billingResult, purchases ->
                // To be implemented in a later section.
            }

        var billingClient = BillingClient.newBuilder(this)
            .setListener(purchaseUpdateListener)
            .enablePendingPurchases()
            .build()

        val isSubscriptionPurchased = billingClient.queryPurchases("basic.annual").responseCode == Purchase.PurchaseState.PURCHASED

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode ==  BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    if (billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS).responseCode != BillingClient.BillingResponseCode.OK) {
                        // Notify that they need to update their google play store application because subscriptions are not supported on their install
                    } else {
                        // Check if the user already has a subscription
                    }
                }
            }
            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        })
    }

}