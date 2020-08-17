package com.blueskycharts.app.preferences

import android.content.Intent
import android.os.Bundle
import android.widget.ExpandableListView
import com.blueskycharts.app.R
import com.blueskycharts.app.map.MapViewActivity
import com.blueskycharts.app.map.assetmanagement.TilePersistenceManager
import com.blueskycharts.app.mapselection.MapSelectionAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class DownloadPreferencesActivity : MeteredWifiWarningActivity(R.id.wifiNote), Preferences.Listener {
    private var adapter: DownloadPreferencesAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download_preferences)

        val expandableListView = findViewById<ExpandableListView>(R.id.set_preferences_layout)
        adapter = DownloadPreferencesAdapter(expandableListView.context)
        expandableListView?.setOnChildClickListener { _, _, groupPosition, childPosition, _ ->
            val childData = adapter?.getChild(groupPosition, childPosition)
            var retVal = false
            if (childData is DownloadPreferencesAdapter.Child) {
                val propertyTemplateValue = Preferences.propertyTemplateMapProactiveDownload(childData.groupId, childData.mapId)
                val proactiveDownload = Preferences.instance.getBooleanValue(propertyTemplateValue, Preferences.defaultValueMapProactiveDownload)
                Preferences.instance.setPreference(propertyTemplateValue, !proactiveDownload)
                retVal = true
            }
            retVal
        }

        expandableListView?.setAdapter(adapter)
        Preferences.instance.addListener(this)
    }

    override fun preferenceChanged(preferenceName: String) {
        if (preferenceName.startsWith(Preferences.propertyTemplatePrefixProactiveDownload)) {
            val groupAndMap = Preferences.extractMapGroupAndNameFromProactiveDownloadKey(preferenceName)
            val groupId = groupAndMap.first
            val map = groupAndMap.second
            adapter?.updateView(groupId, map)
        }
    }
}