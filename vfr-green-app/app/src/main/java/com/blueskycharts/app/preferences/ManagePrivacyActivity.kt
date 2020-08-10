package com.blueskycharts.app.preferences

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.blueskycharts.app.R

class ManagePrivacyActivity : MeteredWifiWarningActivity(R.id.wifiNote) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_privacy)
    }
}