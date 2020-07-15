package com.blueskycharts.app.preferences

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.blueskycharts.app.R
import com.blueskycharts.app.map.configuration.Inventory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SetPreferencesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_preferences)
        displayPreferences()
    }

    private fun displayPreferences() {
        for ( group in Inventory.instance.mapGroups ) {
            group.getConfiguration {
                if ( it == null ) {
                    //TODO: post a message about how the preferences could not be loaded
                    return@getConfiguration
                }

                // Switch back to the main thread
                GlobalScope.launch(context = Dispatchers.Main) {
                    // Add the toggles
                    val fragmentTransaction = supportFragmentManager.beginTransaction()
                    for (mapName in it.mapList) {
                        val toggleFragment = PreferenceToggleFragment(mapName)
                        fragmentTransaction.add(R.id.set_preferences_layout, toggleFragment)
                    }
                    fragmentTransaction.commit()
                }
            }
        }
    }
}