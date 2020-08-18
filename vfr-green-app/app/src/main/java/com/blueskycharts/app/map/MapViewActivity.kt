package com.blueskycharts.app.map

import android.os.Bundle
import com.blueskycharts.app.BlueSkyChartsActivity
import com.blueskycharts.app.R
import com.blueskycharts.app.map.view.NavigableMap2d
import com.blueskycharts.app.preferences.Preferences

class MapViewActivity : BlueSkyChartsActivity(false, true), Preferences.Listener {
    private var map: NavigableMap2d? = null
    private var menu: MainMenuFragment? = null
    private var mapFragment: NavigableMapFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_view)
        map = findViewById<NavigableMap2d>(R.id.navigableMap2d)
        menu = supportFragmentManager.findFragmentById(R.id.mainMenu) as MainMenuFragment
        mapFragment = supportFragmentManager.findFragmentById(R.id.navigableMap) as NavigableMapFragment
    }

    override fun onResume() {
        super.onResume()
        Preferences.instance.addListener(this)
        mapFragment?.setExtraLocationUpdateListener(menu)
    }

    override fun onPause() {
        super.onPause()
        Preferences.instance.removeListener(this)
        mapFragment?.setExtraLocationUpdateListener(null)
    }

    override fun preferenceChanged(preferenceName: String) {
        if (preferenceName == Preferences.propertyNameMapTrackLocation) {
            val track = Preferences.instance.getBooleanValue(Preferences.propertyNameMapTrackLocation, Preferences.defaultValueMapTrackLocation)
            map?.trackCurrentLocation = track
        }
    }
}