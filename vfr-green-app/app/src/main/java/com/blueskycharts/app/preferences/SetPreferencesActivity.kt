package com.blueskycharts.app.preferences

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.ToggleButton
import com.blueskycharts.app.R
import com.blueskycharts.app.map.configuration.Inventory
import com.blueskycharts.app.map.configuration.MapConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SetPreferencesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_preferences)

        // Connect the internal/external toggle
        /*  This feature is too difficult for now.  Lets just assume external if it's writable
        val internalExternalToggleButton =
            findViewById<ToggleButton>(R.id.internalExternalToggleButton)
        val externalSet = Preferences.instance.getBooleanValue(
            Preferences.propertyNameStoreMapsExternally,
            Preferences.defaultValueStoreMapsExternally
        )
        internalExternalToggleButton.isChecked = externalSet
        internalExternalToggleButton.setOnCheckedChangeListener { _, isChecked ->
            Preferences.instance.setPreference(
                Preferences.propertyNameStoreMapsExternally,
                isChecked
            )
        }
         */

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