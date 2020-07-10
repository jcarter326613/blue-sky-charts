package com.blueskycharts.app.preferences

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ToggleButton
import com.blueskycharts.app.R

/**
 * A simple [Fragment] subclass.
 * Use the [PreferenceToggleFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PreferenceToggleFragment(private val mapName: String) : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_preference_toggle, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val label = view.findViewById<TextView>(R.id.mapNameLabel)
        label?.text = mapName

        val proactiveDownload = Preferences.instance.getBooleanValue(
            Preferences.propertyTemplateMapProactiveDownload(mapName),
            Preferences.defaultTemplateMapProactiveDownload
        )
        val toggleButton = view.findViewById<ToggleButton>(R.id.proactivePersistToggleButton)
        toggleButton.isChecked = proactiveDownload
        toggleButton.setOnCheckedChangeListener { _, isChecked ->
            Preferences.instance.setPreference(Preferences.propertyTemplateMapProactiveDownload(mapName), isChecked)
        }
    }
}