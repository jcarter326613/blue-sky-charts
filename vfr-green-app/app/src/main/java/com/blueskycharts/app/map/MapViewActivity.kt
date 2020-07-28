package com.blueskycharts.app.map

import android.R.attr.name
import android.R.id
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blueskycharts.app.R
import com.blueskycharts.app.map.assetmanagement.TilePersistenceManager
import com.google.firebase.analytics.FirebaseAnalytics


class MapViewActivity : AppCompatActivity() {
    private var mFirebaseAnalytics: FirebaseAnalytics? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_view)

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "testId")
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "testName")
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "image")
        mFirebaseAnalytics!!.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)

        TilePersistenceManager.instance
    }
}