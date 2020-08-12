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
import androidx.core.view.get
import com.blueskycharts.app.map.assetmanagement.TilePersistenceManager
import com.blueskycharts.app.map.configuration.Inventory
import com.blueskycharts.app.utility.ScreenUnits
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

class DownloadPreferencesAdapter(private var listener: DownloadPreferencesAdapter.Listener?, context: Context) : BaseExpandableListAdapter(), Preferences.Listener {
    private val itemDictionary = mutableListOf<Group>()
    private val groupLeftPadding: Int
    private val mapViewLibrary = mutableMapOf<Int, ViewRecord>()
    private val mapIdLibrary = mutableMapOf<String, ViewRecord>()
    private var nextId = AtomicInteger(1)

    interface Listener {
        fun loadComplete(adapter: DownloadPreferencesAdapter)
    }

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
            listener?.loadComplete(this@DownloadPreferencesAdapter)
            listener = null
        }

        Preferences.instance.addListener(this)
    }

    override fun preferenceChanged(preferenceName: String) {
        if (preferenceName.startsWith(Preferences.propertyTemplatePrefixProactiveDownload)) {
            val groupAndMap = Preferences.extractMapGroupAndNameFromProactiveDownloadKey(preferenceName)
            val groupId = groupAndMap.first
            val map = groupAndMap.second
            val viewEntry = this.
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

            mapIdLibrary.remove(getKeyForId(viewLibraryEntry.groupId, viewLibraryEntry.mapId))

            viewLibraryEntry.groupId = mapInfo.groupId
            viewLibraryEntry.mapId = mapInfo.mapId

            mapIdLibrary[getKeyForId(mapInfo.groupId, mapInfo.mapId)] = viewLibraryEntry
        } else {
            val padding = ScreenUnits.convertDipToPixels(10f, parent.context).toInt()
            nameLabel = TextView(parent.context)
            nameLabel.setPadding(padding, padding, padding, padding)
            nameLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20F)

            downloadLabel = TextView(parent.context)
            downloadLabel.setPadding(padding, padding, padding, padding)
            downloadLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20F)

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
            mapIdLibrary[getKeyForId(mapInfo.groupId, mapInfo.mapId)] = viewRecord
        }

        nameLabel.text = mapInfo.name
        downloadLabel.text = getDownloadLabelText(mapInfo.groupId, mapInfo.mapId)

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

    private fun getDownloadLabelText(groupId: Int, mapId: String): String {
        if (Preferences.instance.getBooleanValue(Preferences.propertyTemplateMapProactiveDownload(groupId, mapId), Preferences.defaultValueMapProactiveDownload)) {
            val persistenceManager = TilePersistenceManager.getInstance(null)
            //val statistics = persistenceManager.getMapStatistics(groupId, mapId)
            //statistics.downloadedSizeBytes
            return "Downloading ..."
        } else {
            return ""
        }
    }

    private fun getKeyForId(groupId: Int, mapId: String): String {
        return "$groupId|$mapId"
    }
}