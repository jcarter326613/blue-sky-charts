package com.blueskycharts.app.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.blueskycharts.app.R
import com.blueskycharts.app.map.models.OverlayViewModel
import com.blueskycharts.app.map.view.NavigableMap2d
import com.blueskycharts.app.utility.Log

class NavigableMapFragment: Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_navigable_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = this.activity
        if ( activity != null ) {
            val overlayModel = ViewModelProvider(activity).get(OverlayViewModel::class.java)
            val mapView = view.findViewById<NavigableMap2d>(R.id.navigableMap2d)

            overlayModel.getOverlayType().observe(viewLifecycleOwner, Observer {
                mapView.setOverlayType(it)
            })
        } else {
            Log.e(null, "Loaded view without activity.  Can not connect view model.")
        }
    }
}