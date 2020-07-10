package com.blueskycharts.app.preferences

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Contacts
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.ToggleButton
import androidx.constraintlayout.widget.ConstraintLayout
import com.blueskycharts.app.R
import com.blueskycharts.app.map.configuration.ConfigurationRetriever
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
        val retriever = ConfigurationRetriever()
        retriever.retrieveConfiguration {
            if ( it == null ) {
                //TODO: post a message about how the preferences could not be loaded
                return@retrieveConfiguration
            }

            // Switch back to the main thread
            GlobalScope.launch(context = Dispatchers.Main) {
                // Add the toggles
                val fragmentTransaction = supportFragmentManager.beginTransaction()
                for (mapName in it.mapList) {
                    val toggleFragment = PreferenceToggleFragment(mapName)
                    fragmentTransaction.add(R.id.setPreferencesLayout, toggleFragment)
                }
                fragmentTransaction.commit()
            }
        }
    }
}