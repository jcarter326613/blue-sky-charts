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

class SubscriptionActivity : SubscriptionChecker(true, false)  {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)

        val goToMapsButton = findViewById<Button>(R.id.go_to_map_button)
        goToMapsButton.setOnClickListener {
            startActivity(Intent(this, MapViewActivity::class.java))
        }

        val addSubscriptionButton = findViewById<Button>(R.id.add_subscription_button)
        addSubscriptionButton.setOnClickListener {
            sendCustomerToOrderFlow()
        }
    }
}