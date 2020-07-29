package com.blueskycharts.app.preferences

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ToggleButton
import com.blueskycharts.app.R
import com.blueskycharts.app.map.assetmanagement.MapPersistenceStatistics
import com.blueskycharts.app.map.assetmanagement.TilePersistenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * A simple [Fragment] subclass.
 * Use the [PreferenceToggleFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PreferenceToggleFragment(private val mapGroup: Int, private val mapName: String) : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_preference_toggle, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup the label saying what map this is
        val label = view.findViewById<TextView>(R.id.mapNameLabel)
        label?.text = mapName

        // Set up the proactive download checkbox
        val proactiveDownload = Preferences.instance.getBooleanValue(
            Preferences.propertyTemplateMapProactiveDownload(mapGroup, mapName),
            Preferences.defaultValueMapProactiveDownload
        )
        val toggleButton = view.findViewById<ToggleButton>(R.id.proactivePersistToggleButton)
        toggleButton.isChecked = proactiveDownload
        toggleButton.setOnCheckedChangeListener { _, isChecked ->
            Preferences.instance.setPreference(Preferences.propertyTemplateMapProactiveDownload(mapGroup, mapName), isChecked)
        }

        // Set up the download progress indicator
        // Set up the downloaded file size indicator
        val tilePersistenceManager = TilePersistenceManager.getInstance(this.context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
        val downloadSizeLabel = view.findViewById<TextView>(R.id.size_on_disk_label)
        GlobalScope.launch {
            val statistics = tilePersistenceManager.getMapStatistics(mapGroup, mapName)
            statistics.addListener(object : MapPersistenceStatistics.Listener {
                private var updateNeeded = false
                private var sizeText: String = ""

                override fun statisticsUpdated(downloadedSizeBytes: Long) {
                    sizeText = "${downloadedSizeBytes / 1000000} M"
                    if (sizeText != downloadSizeLabel.text) {
                        updateNeeded = true
                        GlobalScope.launch(context = Dispatchers.Main) {
                            if (updateNeeded) {
                                updateNeeded = false
                                downloadSizeLabel.text = sizeText
                            }
                        }
                    }
                }
            })
        }
    }
}