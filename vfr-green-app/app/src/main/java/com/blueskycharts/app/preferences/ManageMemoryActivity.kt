package com.blueskycharts.app.preferences

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.blueskycharts.app.R
import com.blueskycharts.app.map.assetmanagement.TilePersistenceManager

class ManageMemoryActivity : MeteredWifiWarningActivity(R.id.wifiNote) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_memory)

        val clearCacheButton = findViewById<Button>(R.id.clearCacheButton)
        clearCacheButton?.setOnClickListener {
            Preferences.instance.setPreference(Preferences.propertyNameRequestClearCache, true)
            TilePersistenceManager.getInstance(null).start()
        }
    }
}