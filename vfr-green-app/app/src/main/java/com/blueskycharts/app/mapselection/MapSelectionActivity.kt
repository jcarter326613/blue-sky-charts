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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_selection)

        val buttonLayout = findViewById<ExpandableListView>(R.id.select_map_layout)
        val adapter = MapSelectionAdapter(buttonLayout.context)
        buttonLayout.setOnChildClickListener { _, _, groupPosition, childPosition, _ ->
            val childData = adapter.getChild(groupPosition, childPosition)
            var retVal = false
            if (childData is MapSelectionAdapter.Child) {
                Preferences.instance.setPreference(Preferences.propertyNameDisplayedSubMapId, childData.mapId)
                Preferences.instance.setPreference(Preferences.propertyNameDisplayedMapGroupId, childData.groupId)
                startActivity(Intent(this, MapViewActivity::class.java))
                retVal = true
            }
            retVal
        }

        buttonLayout.setAdapter(adapter)
    }
}
