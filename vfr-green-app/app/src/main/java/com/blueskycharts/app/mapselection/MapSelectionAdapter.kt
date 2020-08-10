package com.blueskycharts.app.mapselection

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.blueskycharts.app.R
import com.blueskycharts.app.map.configuration.Inventory
import com.blueskycharts.app.preferences.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MapSelectionAdapter(private var listener: Listener?, context: Context) : BaseExpandableListAdapter() {
    private val itemDictionary = mutableListOf<Group>()
    private val groupLeftPadding: Int

    interface Listener {
        fun loadComplete(adapter: MapSelectionAdapter)
    }

    class Group(val id: Int, val name: String, val displayAll: Boolean) {
        val children = mutableListOf<Child>()
    }

    class Child(val id: String, val name: String)

    init {
        val arr = IntArray(1)
        arr[0] = android.R.attr.expandableListPreferredItemPaddingLeft
        val attributeValue = context.obtainStyledAttributes(arr)
        groupLeftPadding = attributeValue.getDimensionPixelSize(0, 0)

        GlobalScope.launch {
            for (group in Inventory.instance.mapGroups) {
                val newGroup = Group(group.id, group.humanName, group.displayAll)
                if (group.displayAll) {
                    newGroup.children.add(Child("", group.humanName))
                } else {
                    val config = group.getConfiguration()
                    if (config != null) {
                        for (map in config.mapList) {
                            newGroup.children.add(Child(map, map))
                        }
                    }
                }
                itemDictionary.add(newGroup)
            }
            listener?.loadComplete(this@MapSelectionAdapter)
            listener = null
        }
    }

    override fun getGroup(index: Int): Any {
        return this.itemDictionary[index]
    }

    override fun isChildSelectable(groupIndex: Int, childIndex: Int): Boolean {
        return true
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun getGroupView(groupIndex: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup): View {
        return if (convertView is TextView) {
            convertView.text = this.itemDictionary[groupIndex].name
            convertView
        } else {
            val newView = TextView(parent.context)
            newView.text = this.itemDictionary[groupIndex].name
            newView.setPadding(groupLeftPadding, 0, 0, 0)
            newView
        }
    }

    override fun getChildView(groupIndex: Int, childIndex: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup): View {
        return if (convertView is TextView) {
            convertView.text = this.itemDictionary[groupIndex].children[childIndex].name
            convertView
        } else {
            val newView = TextView(parent.context)
            newView.text = this.itemDictionary[groupIndex].children[childIndex].name
            newView
        }
    }

    override fun getGroupCount(): Int {
        return this.itemDictionary.size
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return this.itemDictionary[groupPosition].children.size
    }

    override fun getChild(groupIndex: Int, childIndex: Int): Any {
        return this.itemDictionary[groupIndex].children[childIndex]
    }

    override fun getGroupId(groupIndex: Int): Long {
        return groupIndex.toLong()
    }

    override fun getChildId(groupIndex: Int, childIndex: Int): Long {
        return (this.itemDictionary.size + childIndex).toLong()
    }

    /*
    private fun displayMaps() {
        GlobalScope.launch {    //ok1
            // Get the group configurations
            val buttonGroups: Array<GroupDetails?> = Array(Inventory.instance.mapGroups.size) {null}
            for ( group in Inventory.instance.mapGroups ) {
                buttonGroups[group.id - 1] = getGroupConfig(group)
            }

            // Switch back to the main thread
            GlobalScope.launch(context = Dispatchers.Main) {
                val buttonLayout = findViewById<ExpandableListView>(R.id.select_map_layout)
                buttonLayout.setAdapter(MapSelectionAdapter())
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

     */
}