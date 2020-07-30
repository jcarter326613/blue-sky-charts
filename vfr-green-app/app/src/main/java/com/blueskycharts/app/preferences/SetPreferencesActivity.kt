package com.blueskycharts.app.preferences

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import android.widget.ToggleButton
import com.blueskycharts.app.R
import com.blueskycharts.app.assests.DiskCacheFactory
import com.blueskycharts.app.map.assetmanagement.TilePersistenceManager
import com.blueskycharts.app.map.configuration.Inventory
import com.blueskycharts.app.map.configuration.MapConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SetPreferencesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_preferences)

        val clearCacheButton = findViewById<Button>(R.id.clearCacheButton)
        clearCacheButton?.setOnClickListener {
            Preferences.instance.setPreference(Preferences.propertyNameRequestClearCache, true)
            TilePersistenceManager.getInstance(null).start()
        }

        displayPreferences()
    }

    private fun displayPreferences() {
        GlobalScope.launch {    //ok1
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
}