package com.blueskycharts.app.mapselection

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.blueskycharts.app.R
import com.blueskycharts.app.map.configuration.Inventory
import com.blueskycharts.app.preferences.PreferenceToggleFragment
import com.blueskycharts.app.preferences.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.util.concurrent.atomic.AtomicInteger

class MapSelectionActivity : AppCompatActivity() {
    private var numGroups = AtomicInteger(0)
    private var buttonGroups: Array<GroupDetails?> = arrayOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_selection)
        displayMaps()
    }

    private fun displayMaps() {
        buttonGroups = Array(Inventory.instance.mapGroups.size) {null}
        numGroups.set(Inventory.instance.mapGroups.size)
        for ( group in Inventory.instance.mapGroups ) {
            getGroupConfig(group)
        }

        //Wait for all the configurations to download
        GlobalScope.launch {
            while (numGroups.get() > 0) {
                yield()
            }

            // Switch back to the main thread
            GlobalScope.launch(context = Dispatchers.Main) {
                var buttonLayout = findViewById<LinearLayout>(R.id.select_map_layout)
                for ( group in this@MapSelectionActivity.buttonGroups ) {
                    if ( group == null ) {
                        continue
                    }
                    if ( group.displayGroupOnly ) {
                        val mapButton = Button(buttonLayout.context)
                        mapButton.text = group.text
                        mapButton.setOnClickListener {
                            Preferences.instance.setPreference(Preferences.propertyNameDisplayedMapGroupId, group.id)
                            Preferences.instance.setPreference(Preferences.propertyNameDisplayedSubMapId, "")
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
                                Preferences.instance.setPreference(Preferences.propertyNameDisplayedMapGroupId, group.id)
                                Preferences.instance.setPreference(Preferences.propertyNameDisplayedSubMapId, button.subMapId)
                            }
                            buttonLayout.addView(mapButton)
                        }
                    }
                }
            }
        }
    }

    private fun getGroupConfig(group: Inventory.Group) {
        group.getConfiguration {
            // Check if the config failed to download
            if ( it == null ) {
                //TODO: post a message about how the preferences could not be loaded
                numGroups.getAndDecrement()
                return@getConfiguration
            }

            // Pull out the needed information to display a button
            val displayGroupOnly = it.displayAll
            val groupName = group.humanName
            val newGroup = if (displayGroupOnly) {
                GroupDetails(displayGroupOnly, groupName, group.id, null)
            } else {
                val buttonList = mutableListOf<ButtonDetails>()
                for (map in it.mapList) {
                    buttonList.add(ButtonDetails(map, map))
                }
                GroupDetails(displayGroupOnly, groupName, group.id, buttonList)
            }
            buttonGroups[group.id - 1] = newGroup
            numGroups.getAndDecrement()
        }
    }

    private class GroupDetails(val displayGroupOnly: Boolean, val text: String, val id: Int, val buttons: List<ButtonDetails>?)
    private class ButtonDetails(val text: String, val subMapId: String )
}