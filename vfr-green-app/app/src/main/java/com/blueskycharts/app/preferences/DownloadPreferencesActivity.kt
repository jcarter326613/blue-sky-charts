package com.blueskycharts.app.preferences

import android.os.Bundle
import android.widget.ExpandableListView
import com.blueskycharts.app.R
import com.blueskycharts.app.mapselection.MapSelectionAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class DownloadPreferencesActivity : MeteredWifiWarningActivity(R.id.wifiNote) {
    private var adapter: DownloadPreferencesAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download_preferences)

        val buttonLayout = findViewById<ExpandableListView>(R.id.set_preferences_layout)
        adapter = DownloadPreferencesAdapter(object: DownloadPreferencesAdapter.Listener {
            override fun loadComplete(adapter: DownloadPreferencesAdapter) {
                GlobalScope.launch(Dispatchers.Main) {
                    buttonLayout.setAdapter(adapter)
                }
            }
        }, applicationContext)
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