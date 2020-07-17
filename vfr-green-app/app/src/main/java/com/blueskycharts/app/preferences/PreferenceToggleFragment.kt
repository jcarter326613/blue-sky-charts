package com.blueskycharts.app.preferences

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
        val downloadIndicatorLabel = view.findViewById<TextView>(R.id.download_complete_label)
        val downloadSizeLabel = view.findViewById<TextView>(R.id.size_on_disk_label)
        TilePersistenceManager.instance.getMapStatistics(mapGroup, mapName) {
            it.addListener( object: MapPersistenceStatistics.Listener {
                private var updateNeeded = false
                private var percentageText: String = ""
                private var sizeText: String = ""

                override fun statisticsUpdated(totalFiles: Int, downloadedFiles: Int, downloadedSizeBytes: Int){
                    val percent = if (totalFiles == 0) {
                        100
                    } else {
                        (downloadedFiles * 100) / totalFiles
                    }
                    percentageText = "Downloading $percent% complete"
                    sizeText = "${downloadedSizeBytes / 1000000} M"
                    if ( percentageText != downloadIndicatorLabel.text || sizeText != downloadSizeLabel.text ) {
                        updateNeeded = true
                        GlobalScope.launch(context = Dispatchers.Main) {
                            if ( updateNeeded ) {
                                updateNeeded = false
                                downloadIndicatorLabel.text = percentageText
                                downloadSizeLabel.text = sizeText
                            }
                        }
                    }
                }
            })
        }
    }
}