package com.blueskycharts.app.mapselection

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ExpandableListView
import com.blueskycharts.app.R
import com.blueskycharts.app.map.MapViewActivity
import com.blueskycharts.app.preferences.Preferences
import com.blueskycharts.app.subscription.SubscriptionActivity
import com.blueskycharts.app.subscription.SubscriptionChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MapSelectionActivity : SubscriptionChecker(false, true) {
    private var adapter: MapSelectionAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_selection)

        val buttonLayout = findViewById<ExpandableListView>(R.id.select_map_layout)
        buttonLayout.setOnChildClickListener { _, _, groupPosition, childPosition, _ ->
            val groupData = adapter?.getGroup(groupPosition)
            val childData = adapter?.getChild(groupPosition, childPosition)
            if (groupData is MapSelectionAdapter.Group && childData is MapSelectionAdapter.Child) {
                if (groupData.displayAll) {
                    Preferences.instance.setPreference(Preferences.propertyNameDisplayedSubMapId, "")
                    Preferences.instance.setPreference(Preferences.propertyNameDisplayedMapGroupId, groupData.id)
                } else {
                    Preferences.instance.setPreference(Preferences.propertyNameDisplayedSubMapId, childData.id)
                    Preferences.instance.setPreference(Preferences.propertyNameDisplayedMapGroupId, groupData.id)
                }
                val intent = Intent(this, MapViewActivity::class.java)
                startActivity(intent)
            }
            true
        }

        adapter = MapSelectionAdapter(object: MapSelectionAdapter.Listener {
            override fun loadComplete(adapter: MapSelectionAdapter) {
                GlobalScope.launch(Dispatchers.Main) {
                    buttonLayout.setAdapter(adapter)
                }
            }
        }, applicationContext)
    }
}
