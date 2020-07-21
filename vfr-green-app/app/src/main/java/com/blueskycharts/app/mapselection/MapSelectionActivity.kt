package com.blueskycharts.app.mapselection

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.blueskycharts.app.R
import com.blueskycharts.app.map.MapViewActivity
import com.blueskycharts.app.map.configuration.Inventory
import com.blueskycharts.app.preferences.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MapSelectionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_selection)

        // Make sure we're allowed to be here
        //startActivity(Intent(this, MapViewActivity::class.java))
        displayMaps()
    }

    private fun displayMaps() {
        GlobalScope.launch {    //ok1
            // Get the group configurations
            val buttonGroups: Array<GroupDetails?> = Array(Inventory.instance.mapGroups.size) {null}
            for ( group in Inventory.instance.mapGroups ) {
                buttonGroups[group.id - 1] = getGroupConfig(group)
            }

            // Switch back to the main thread
            GlobalScope.launch(context = Dispatchers.Main) {
                val buttonLayout = findViewById<LinearLayout>(R.id.select_map_layout)
                for ( group in buttonGroups ) {
                    if ( group == null ) {
                        continue
                    }
                    if ( group.displayGroupOnly ) {
                        val mapButton = Button(buttonLayout.context)
                        mapButton.text = group.text
                        mapButton.setOnClickListener {
                            Preferences.instance.setPreference(Preferences.propertyNameDisplayedSubMapId, "")
                            Preferences.instance.setPreference(Preferences.propertyNameDisplayedMapGroupId, group.id)
                        }
                        buttonLayout.addView(mapButton)
                    } else if (group.buttons != null) {
                        val groupLabel = TextView(buttonLayout.context)
                        groupLabel.text = group.text
                        buttonLayout.addView(groupLabel)
                        for (button in group.buttons) {
                            val mapButton = Button(buttonLayout.context)
                            mapButton.text = button.text
                            mapButton.setOnClickListener {
                                Preferences.instance.setPreference(Preferences.propertyNameDisplayedSubMapId, button.subMapId)
                                Preferences.instance.setPreference(Preferences.propertyNameDisplayedMapGroupId, group.id)
                            }
                            buttonLayout.addView(mapButton)
                        }
                    }
                }
            }
        }
    }

    private suspend fun getGroupConfig(group: Inventory.Group): GroupDetails? {
        val config = group.getConfiguration()

        // Check if the config failed to download
        if (config == null) {
            //TODO: post a message about how the preferences could not be loaded
            return null
        }

        // Pull out the needed information to display a button
        val displayGroupOnly = config.displayAll
        val groupName = group.humanName
        return if (displayGroupOnly) {
            GroupDetails(displayGroupOnly, groupName, group.id, null)
        } else {
            val buttonList = mutableListOf<ButtonDetails>()
            for (map in config.mapList) {
                buttonList.add(ButtonDetails(map, map))
            }
            GroupDetails(displayGroupOnly, groupName, group.id, buttonList)
        }
    }

    private class GroupDetails(val displayGroupOnly: Boolean, val text: String, val id: Int, val buttons: List<ButtonDetails>?)
    private class ButtonDetails(val text: String, val subMapId: String )
}