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
    private val versionApiUrlTemplate = "https://api.blueskycharts.com/version-authorization/appVersion"
    private val versionComplianceUrl: URL
        get() {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val applicationVersion = packageInfo.versionName
            val privacyVersion = Preferences.instance.getStringValue(Preferences.propertyNameAcceptedPrivacyVersion, Preferences.defaultValueAcceptedPrivacyVersion)
            return URL(versionApiUrlTemplate + "?appVersion=${applicationVersion}&privacyVersion=${privacyVersion}")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)

        lookupVersionCompliance()

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

        val isSubscriptionPurchased = billingClient.queryPurchases("the sku").responseCode == Purchase.PurchaseState.PURCHASED

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

    private fun lookupVersionCompliance() {
        val assetProvider = AssetProvider()
        assetProvider.retrieveAsset(RemoteAssetDescription(
            versionComplianceUrl,
            Volatility.DayCache,
            StorageLocation.Internal,
            true)) {
            if (!it.errorLoading) {
                val jsonReader = it.asJsonReader()
                if ( jsonReader != null ) {
                    var appUpdateNeeded = false
                    var policyUpdateNeeded = false
                    jsonReader.beginObject()
                    while (jsonReader.hasNext()) {
                        when(jsonReader.nextName()) {
                            "appUpdateNeeded" -> {
                                appUpdateNeeded = jsonReader.nextBoolean()
                            }
                            "policyUpdateNeeded" -> {
                                policyUpdateNeeded = jsonReader.nextBoolean()
                            }
                            else -> {
                                jsonReader.skipValue()
                            }
                        }
                    }
                    jsonReader.endObject()

                    if ( policyUpdateNeeded ) {
                        showPrivacyDialog(appUpdateNeeded)
                    } else if ( appUpdateNeeded ) {
                        showAppUpdateDialog()
                    }
                }
            }
        }
    }

    private fun showPrivacyDialog(appUpdateNeeded: Boolean) {
        GlobalScope.launch(Dispatchers.Main) {
            val alertDialog = AlertDialog.Builder(this@SubscriptionActivity)
                .setMessage("Please accept changes to the privacy policy.")
                .setPositiveButton("I accept") { dialogInterface: DialogInterface, i: Int ->
                    //TODO: Set preferences record for what date we accepted the policy on
                    if ( appUpdateNeeded ) {
                        showAppUpdateDialog()
                    }
                }
                .setNeutralButton("Read Policy") { dialogInterface: DialogInterface, i: Int ->
                    val uri: Uri = Uri.parse("https://blueskycharts.com/privacy.pdf")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    startActivity(intent)
                }
                .setCancelable(false)
                .create()

            alertDialog.setOnShowListener(OnShowListener {
                val neutralButton =
                    (alertDialog as AlertDialog).getButton(AlertDialog.BUTTON_NEUTRAL)
                neutralButton.setOnClickListener {
                    val uri: Uri = Uri.parse("https://blueskycharts.com/privacy.pdf")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    startActivity(intent)
                }
            })

            alertDialog.show()
        }
    }

    private fun showAppUpdateDialog() {
        GlobalScope.launch(Dispatchers.Main) {
            val alertDialog = AlertDialog.Builder(this@SubscriptionActivity)
                .setMessage("The application must be updated from the Google Play Store.  Some features may not work until an update is completed.")
                .setPositiveButton("Ok") { dialogInterface: DialogInterface, i: Int ->
                }
                .create()
            alertDialog.show()
        }
    }
}