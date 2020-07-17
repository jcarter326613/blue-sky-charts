package com.blueskycharts.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blueskycharts.app.map.assetmanagement.TilePersistenceManager

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        TilePersistenceManager.instance
    }
}