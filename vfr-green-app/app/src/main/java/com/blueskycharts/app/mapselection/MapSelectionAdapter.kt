package com.blueskycharts.app.mapselection

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.blueskycharts.app.R
import com.blueskycharts.app.map.configuration.Inventory
import com.blueskycharts.app.preferences.Preferences
import com.blueskycharts.app.utility.ScreenUnits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MapSelectionAdapter(context: Context) : BaseExpandableListAdapter() {
    private val itemDictionary = mutableListOf<Group>()
    private val groupLeftPadding: Int

    class Group(val name: String) {
        val children = mutableListOf<Child>()
    }

    class Child(val mapId: String, val groupId: Int, val name: String)

    init {
        val arr = IntArray(1)
        arr[0] = android.R.attr.expandableListPreferredItemPaddingLeft
        val attributeValue = context.obtainStyledAttributes(arr)
        groupLeftPadding = attributeValue.getDimensionPixelSize(0, 0)

        GlobalScope.launch {
            val downloadedGroup = Group("Downloaded")

            for (group in Inventory.instance.mapGroups) {
                val newGroup = Group(group.humanName)
                if (group.displayAll) {
                    newGroup.children.add(Child("", group.id, group.humanName))
                    val config = group.getConfiguration()
                    if (config != null) {
                        for (map in config.mapList) {
                            if (Preferences.instance.getBooleanValue(Preferences.propertyTemplateMapProactiveDownload(group.id, map),
                                    Preferences.defaultValueMapProactiveDownload)) {
                                downloadedGroup.children.add(Child(map, group.id, "${group.humanName}"))
                                break
                            }
                        }
                    }
                } else {
                    val config = group.getConfiguration()
                    if (config != null) {
                        for (map in config.mapList) {
                            newGroup.children.add(Child(map, group.id, config.getHumanName(map)))
                            if (Preferences.instance.getBooleanValue(Preferences.propertyTemplateMapProactiveDownload(group.id, map),
                                    Preferences.defaultValueMapProactiveDownload)) {
                                downloadedGroup.children.add(Child(map, group.id, "${group.humanName} - $map"))
                            }
                        }
                    }
                }
                if (newGroup.children.size > 0) {
                    newGroup.children.sortBy {
                        it.name
                    }
                    itemDictionary.add(newGroup)
                }
            }
            if (downloadedGroup.children.size > 0) {
                downloadedGroup.children.sortBy {
                    it.name
                }
                itemDictionary.add(0, downloadedGroup)
            }

            GlobalScope.launch(Dispatchers.Main) {
                this@MapSelectionAdapter.notifyDataSetChanged()
            }
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
            val padding = ScreenUnits.convertDipToPixels(10f, parent.context).toInt()
            val newView = TextView(parent.context)
            newView.text = this.itemDictionary[groupIndex].name
            newView.setPadding(groupLeftPadding, padding, padding, padding)
            newView.setTypeface(newView.typeface, Typeface.BOLD)
            newView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20F)
            newView
        }
    }

    override fun getChildView(groupIndex: Int, childIndex: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup): View {
        return if (convertView is TextView) {
            convertView.text = this.itemDictionary[groupIndex].children[childIndex].name
            convertView
        } else {
            val padding = ScreenUnits.convertDipToPixels(10f, parent.context).toInt()
            val newView = TextView(parent.context)
            newView.text = this.itemDictionary[groupIndex].children[childIndex].name
            newView.setPadding(padding, padding, padding, padding)
            newView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20F)
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
        return childIndex.toLong()
    }
}