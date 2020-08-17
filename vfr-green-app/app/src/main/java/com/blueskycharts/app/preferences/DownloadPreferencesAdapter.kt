package com.blueskycharts.app.preferences

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import com.blueskycharts.app.map.assetmanagement.MapPersistenceStatistics
import com.blueskycharts.app.map.assetmanagement.TilePersistenceManager
import com.blueskycharts.app.map.assetmanagement.TilePersistenceManagerFactory
import com.blueskycharts.app.map.configuration.Inventory
import com.blueskycharts.app.utility.ScreenUnits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

class DownloadPreferencesAdapter(context: Context) : BaseExpandableListAdapter(), MapPersistenceStatistics.Listener {
    private val itemDictionary = mutableListOf<Group>()
    private val groupLeftPadding: Int
    private val mapViewLibrary = mutableMapOf<Int, ViewRecord>()
    private val downloadLabelLookup = mutableMapOf<String, String>()
    private var nextId = AtomicInteger(1)

    class Group(val name: String) {
        val children = mutableListOf<Child>()
    }

    class Child(val mapId: String, val groupId: Int, val name: String)

    class ViewRecord(val layoutView: LinearLayout, val nameView: TextView, val downloadView: TextView, var groupId: Int, var mapId: String)

    init {
        val arr = IntArray(1)
        arr[0] = android.R.attr.expandableListPreferredItemPaddingLeft
        val attributeValue = context.obtainStyledAttributes(arr)
        groupLeftPadding = attributeValue.getDimensionPixelSize(0, 0)

        GlobalScope.launch {
            for (group in Inventory.instance.mapGroups) {
                val newGroup = Group(group.humanName)
                val config = group.getConfiguration()
                if (config != null) {
                    for (map in config.mapList) {
                        newGroup.children.add(Child(map, group.id, map))
                    }
                }
                if (newGroup.children.size > 0) {
                    newGroup.children.sortBy {
                        it.name
                    }
                    itemDictionary.add(newGroup)
                }
            }

            GlobalScope.launch(Dispatchers.Main) {
                this@DownloadPreferencesAdapter.notifyDataSetChanged()
            }
        }
    }

    fun connectListeners() {
        GlobalScope.launch {
            for (group in itemDictionary) {
                for (child in group.children) {
                    TilePersistenceManagerFactory.instance.getMapStatistics(child.groupId, child.mapId).addListener(this@DownloadPreferencesAdapter)
                }
            }
        }
    }

    fun detachListeners() {
        GlobalScope.launch {
            for (group in itemDictionary) {
                for (child in group.children) {
                    TilePersistenceManagerFactory.instance.getMapStatistics(child.groupId, child.mapId).removeListener(this@DownloadPreferencesAdapter)
                }
            }
        }
    }

    fun statisticsUpdated(groupId: Int, mapId: String) {
        GlobalScope.launch {
            val size = TilePersistenceManagerFactory.instance.getMapStatistics(groupId, mapId).downloadedSizeBytes
            statisticsUpdated(groupId, mapId, size)
        }
    }

    override fun statisticsUpdated(groupId: Int, mapId: String, downloadedSizeBytes: Long) {
        if (Preferences.instance.getBooleanValue(Preferences.propertyTemplateMapProactiveDownload(groupId, mapId), Preferences.defaultValueMapProactiveDownload)) {
            downloadLabelLookup[getKeyForId(groupId, mapId)] = "${downloadedSizeBytes / 1000000} Mb"
        } else {
            downloadLabelLookup[getKeyForId(groupId, mapId)] = ""
        }

        GlobalScope.launch(Dispatchers.Main) {
            notifyDataSetChanged()
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
        val mapInfo = this.itemDictionary[groupIndex].children[childIndex]
        val nameLabel: TextView
        val downloadLabel: TextView
        val layout: LinearLayout

        val viewLibraryEntry = mapViewLibrary[convertView?.id]
        if (viewLibraryEntry != null) {
            nameLabel = viewLibraryEntry.nameView
            downloadLabel = viewLibraryEntry.downloadView
            layout = viewLibraryEntry.layoutView

            viewLibraryEntry.groupId = mapInfo.groupId
            viewLibraryEntry.mapId = mapInfo.mapId
        } else {
            val padding = ScreenUnits.convertDipToPixels(10f, parent.context).toInt()
            nameLabel = TextView(parent.context)
            nameLabel.setPadding(padding, padding, padding, padding)
            nameLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20F)

            downloadLabel = TextView(parent.context)
            downloadLabel.setPadding(padding, padding, padding, padding)
            downloadLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20F)

            val spacerLayoutParams = LinearLayout.LayoutParams(0, 0, 1f)
            val spacer = Space(parent.context)
            spacer.layoutParams = spacerLayoutParams

            val layoutLayoutParameters = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layout = LinearLayout(parent.context)
            layout.id = nextId.getAndIncrement()
            layout.orientation = LinearLayout.HORIZONTAL
            layout.layoutParams = layoutLayoutParameters
            layout.addView(nameLabel)
            layout.addView(spacer)
            layout.addView(downloadLabel)

            val viewRecord = ViewRecord(layout, nameLabel, downloadLabel, mapInfo.groupId, mapInfo.mapId)
            mapViewLibrary[layout.id] = viewRecord
        }

        nameLabel.text = mapInfo.name
        downloadLabel.text = downloadLabelLookup[getKeyForId(mapInfo.groupId, mapInfo.mapId)] ?: ""

        return layout
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

    private fun getKeyForId(groupId: Int, mapId: String): String {
        return "$groupId|$mapId"
    }
}