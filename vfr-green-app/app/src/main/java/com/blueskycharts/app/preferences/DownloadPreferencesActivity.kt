package com.blueskycharts.app.preferences

import android.content.Intent
import android.os.Bundle
import android.widget.ExpandableListView
import com.blueskycharts.app.R
import com.blueskycharts.app.map.MapViewActivity
import com.blueskycharts.app.map.assetmanagement.TilePersistenceManager
import com.blueskycharts.app.mapselection.MapSelectionAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class DownloadPreferencesActivity : MeteredWifiWarningActivity(R.id.wifiNote), Preferences.Listener {
    private var adapter: DownloadPreferencesAdapter? = null
    private var expandableListView: ExpandableListView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download_preferences)

        expandableListView = findViewById<ExpandableListView>(R.id.set_preferences_layout)
        expandableListView?.setOnChildClickListener { _, _, groupPosition, childPosition, _ ->
            val childData = adapter?.getChild(groupPosition, childPosition)
            var retVal = false
            if (childData is DownloadPreferencesAdapter.Child) {
                val propertyTemplateValue = Preferences.propertyTemplateMapProactiveDownload(childData.groupId, childData.mapId)
                val proactiveDownload = Preferences.instance.getBooleanValue(propertyTemplateValue, Preferences.defaultValueMapProactiveDownload)
                Preferences.instance.setPreference(propertyTemplateValue, !proactiveDownload)
                retVal = true
            }
            retVal
        }

        adapter = DownloadPreferencesAdapter(object: DownloadPreferencesAdapter.Listener {
            override fun loadComplete(adapter: DownloadPreferencesAdapter) {
                GlobalScope.launch(Dispatchers.Main) {
                    expandableListView?.setAdapter(adapter)
                }
            }
        }, applicationContext)

        Preferences.instance.addListener(this)
    }

    override fun preferenceChanged(preferenceName: String) {
        if (preferenceName.startsWith(Preferences.propertyTemplatePrefixProactiveDownload)) {
            val groupAndMap = Preferences.extractMapGroupAndNameFromProactiveDownloadKey(preferenceName)
            val groupId = groupAndMap.first
            val map = groupAndMap.second
            adapter?.updateView(groupId, map)
        }
    }

    /*
    private fun displayPreferences() {
        GlobalScope.launch {
            val configList = mutableListOf<MapConfiguration>()
            for (group in Inventory.instance.mapGroups) {
                val config = group.getConfiguration()
                    ?: continue  //TODO: post a message about how the preferences could not be loaded
                configList.add(config)
            }

            // Switch back to the main thread
            GlobalScope.launch(context = Dispatchers.Main) {
                // Add the toggles
                val fragmentTransaction = supportFragmentManager.beginTransaction()
                for (config in configList) {
                    for (mapName in config.mapList) {
                        val toggleFragment = PreferenceToggleFragment(config.groupId, mapName)
                        fragmentTransaction.add(R.id.set_preferences_layout, toggleFragment)
                    }
                }
                fragmentTransaction.commit()
            }
        }
    }
     */
}