package com.blueskycharts.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blueskycharts.app.map.assetmanagement.TilePersistenceManager

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Create the view
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Create singleton monitors/managers
        if (savedInstanceState == null) {
            TilePersistenceManager.initializeOrRefresh()
        }
    }
}