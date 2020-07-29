package com.blueskycharts.app.map

import android.R.attr.name
import android.R.id
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blueskycharts.app.R
import com.blueskycharts.app.map.assetmanagement.TilePersistenceManager
import com.google.firebase.analytics.FirebaseAnalytics


class MapViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_view)

        TilePersistenceManager.instance
    }
}