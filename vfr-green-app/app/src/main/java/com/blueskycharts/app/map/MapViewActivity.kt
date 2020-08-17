package com.blueskycharts.app.map

import android.os.Bundle
import com.blueskycharts.app.BlueSkyChartsActivity
import com.blueskycharts.app.R
import com.blueskycharts.app.map.view.NavigableMap2d
import com.blueskycharts.app.preferences.Preferences

class MapViewActivity : BlueSkyChartsActivity(false, true), Preferences.Listener {
    private var map: NavigableMap2d? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_view)
        map = findViewById<NavigableMap2d>(R.id.navigableMap2d)
    }

    override fun onResume() {
        super.onResume()
        Preferences.instance.addListener(this)
    }

    override fun onPause() {
        super.onPause()
        Preferences.instance.removeListener(this)
    }

    override fun preferenceChanged(preferenceName: String) {
        if (preferenceName == Preferences.propertyNameMapTrackLocation) {
            val track = Preferences.instance.getBooleanValue(Preferences.propertyNameMapTrackLocation, Preferences.defaultValueMapTrackLocation)
            map?.trackCurrentLocation = track
        }
    }
}