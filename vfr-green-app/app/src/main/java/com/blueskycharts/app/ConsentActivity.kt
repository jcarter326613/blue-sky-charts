package com.blueskycharts.app

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.blueskycharts.app.assests.*
import com.blueskycharts.app.map.MapViewActivity
import com.blueskycharts.app.preferences.Preferences
import com.blueskycharts.app.subscription.SubscriptionChecker
import com.blueskycharts.app.utility.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.net.URL

class ConsentActivity : SubscriptionChecker(false, false) {
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
        setContentView(R.layout.activity_consent)
        lookupVersionCompliance()
    }

    private fun lookupVersionCompliance() {
        GlobalScope.launch {
            while ( !DiskCacheFactory.ready ) {
                yield()
            }

            var collectionComplete = false
            var appUpdateNeeded = false
            var policyUpdateNeeded = !ConsentFragment.policyAccepted

            val assetProvider = AssetProvider()
            assetProvider.retrieveAsset(
                RemoteAssetDescription(
                    versionComplianceUrl,
                    Volatility.DayCache,
                    StorageLocation.Internal,
                    true
                )
            ) {
                if (!it.errorLoading) {
                    val jsonReader = it.asJsonReader()
                    if (jsonReader != null) {
                        jsonReader.beginObject()
                        while (jsonReader.hasNext()) {
                            when (jsonReader.nextName()) {
                                "appUpdateNeeded" -> {
                                    appUpdateNeeded = jsonReader.nextBoolean()
                                }
                                else -> {
                                    jsonReader.skipValue()
                                }
                            }
                        }
                        jsonReader.endObject()
                    }
                }
                collectionComplete = true
            }

            while (!collectionComplete) {
                yield()
            }

            if (appUpdateNeeded) {
                showAppUpdateDialog(policyUpdateNeeded)
            } else {
                showPrivacyOrContinue(policyUpdateNeeded)
            }
        }
    }

    private fun showPrivacyOrContinue(policyUpdateNeeded: Boolean) {
        if (policyUpdateNeeded) {
            showPrivacyDialog()
        } else {
            Log.refreshFirebaseConsent(baseContext)
            //if (subscriptionStatus == SubscriptionStatus.NotActive) {
            //    startActivity(Intent(this, SubscriptionActivity::class.java))
            //} else {
                startActivity(Intent(this, MapViewActivity::class.java))
            //}
        }
    }

    private fun showPrivacyDialog() {
        GlobalScope.launch(Dispatchers.Main) {
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            val consentFragment = ConsentFragment()
            fragmentTransaction.add(R.id.mainLayout, consentFragment)
            fragmentTransaction.commit()
        }
    }

    private fun showAppUpdateDialog(policyUpdateNeeded: Boolean) {
        GlobalScope.launch(Dispatchers.Main) {
            var updatePressed = false
            val alertDialog = AlertDialog.Builder(this@ConsentActivity)
                .setMessage("The application must be updated from the Google Play Store.  Some features may not work until an update is completed.")
                .setPositiveButton("Update Now") { _: DialogInterface, _: Int ->
                    updatePressed = true
                }
                .setNegativeButton("Later") { _: DialogInterface, _: Int ->
                }
                .create()
            alertDialog.show()
            alertDialog.setOnDismissListener {
                showPrivacyOrContinue(policyUpdateNeeded)
                if (updatePressed) {
                    val uri: Uri = Uri.parse("https://play.google.com/store/apps/details?id=com.blueskycharts.app")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    startActivity(intent)
                }
            }
        }
    }
}