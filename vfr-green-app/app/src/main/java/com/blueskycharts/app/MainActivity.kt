package com.blueskycharts.app

import android.os.Bundle
import android.os.PersistableBundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.blueskycharts.app.assests.DiskCacheFactory
import com.blueskycharts.app.map.assetmanagement.TilePersistenceManager
import com.blueskycharts.app.map.configuration.ConfigurationRetriever

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