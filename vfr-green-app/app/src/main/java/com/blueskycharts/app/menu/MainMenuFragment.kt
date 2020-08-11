package com.blueskycharts.app.menu

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.blueskycharts.app.R
import com.blueskycharts.app.preferences.DownloadPreferencesActivity
import com.blueskycharts.app.map.models.OverlayViewModel
import com.blueskycharts.app.map.view.OverlayTypes
import com.blueskycharts.app.mapselection.MapSelectionActivity
import com.blueskycharts.app.preferences.ManageMemoryActivity
import com.blueskycharts.app.preferences.ManagePrivacyActivity
import com.blueskycharts.app.preferences.Preferences
import com.blueskycharts.app.subscription.ManageSubscriptionActivity
import com.blueskycharts.app.utility.Log
import com.blueskycharts.app.utility.ScreenUnits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainMenuFragment() : Fragment() {
    private var overlayModel: OverlayViewModel? = null
    private var displayedMenu: View? = null
    private var currentlyTracking: Boolean = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        //overlayModel = ViewModelProvider(this).get(OverlayViewModel::class.java)
        return inflater.inflate(R.layout.fragment_main_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = this.activity
        if ( activity != null ) {
            overlayModel = ViewModelProvider(activity).get(OverlayViewModel::class.java)

            val mainMenu = view.findViewById<View>(R.id.main_menu_grid_layout)
            val openMenuButton = view.findViewById<ImageButton>(R.id.open_menu_button)
            openMenuButton?.setOnClickListener {
                showOrToggleMenu(mainMenu)
            }

            val zoomToSelfButton = view.findViewById<ImageButton>(R.id.zoomToSelfButton)
            zoomToSelfButton?.setOnClickListener {
                Preferences.instance.setPreference(Preferences.propertyNameMapTrackLocation, true)
            }

            setupMainMenuHandlers()
            setupWeatherHandlers()
            setupPreferencesHandlers()
        } else {
            Log.error(null, "Loaded view without activity.  Can not connect view model.")
        }
    }

    override fun onResume() {
        super.onResume()
        Preferences.instance.addListener(object: Preferences.Listener{
            override fun preferenceChanged(preferenceName: String) {
                if (preferenceName == Preferences.propertyNameMapTrackLocation) {
                    GlobalScope.launch(Dispatchers.Main) {
                        val zoomToSelfButton =
                            view?.findViewById<ImageButton>(R.id.zoomToSelfButton)
                        val newTracking = Preferences.instance.getBooleanValue(
                            Preferences.propertyNameMapTrackLocation,
                            Preferences.defaultValueMapTrackLocation
                        )
                        if (currentlyTracking != newTracking) {
                            if (newTracking) {
                                //zoomToSelfButton?.setImageResource(R.drawable.ic_my_location_set)
                                zoomToSelfButton?.visibility = View.INVISIBLE
                            } else {
                                //zoomToSelfButton?.setImageResource(R.drawable.ic_my_location_not_set)
                                zoomToSelfButton?.visibility = View.VISIBLE
                            }
                            currentlyTracking = newTracking
                        }
                    }
                }
            }
        })
    }

    private fun setupMainMenuHandlers() {
        val weatherMenu = view?.findViewById<View>(R.id.weather_grid_layout)
        val weatherButton = view?.findViewById<ImageButton>(R.id.weather_button)
        weatherButton?.setOnClickListener {
            showOrToggleMenu(weatherMenu)
        }

        val mapSelectionButton = view?.findViewById<ImageButton>(R.id.map_selection_button)
        mapSelectionButton?.setOnClickListener {
            startActivity(Intent(context, MapSelectionActivity::class.java))
        }

        val preferencesMenu = view?.findViewById<View>(R.id.settings_selector)
        val preferencesButton = view?.findViewById<ImageButton>(R.id.preferences)
        preferencesButton?.setOnClickListener {
            showOrToggleMenu(preferencesMenu)
        }

        val subscriptionButton = view?.findViewById<ImageButton>(R.id.subscription)
        subscriptionButton?.setOnClickListener {
            val intent = Intent(context, ManageSubscriptionActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupPreferencesHandlers() {
        val downloadsButton = view?.findViewById<ImageButton>(R.id.settings_downloads)
        downloadsButton?.setOnClickListener {
            val intent = Intent(context, DownloadPreferencesActivity::class.java)
            startActivity(intent)
        }

        val memoryButton = view?.findViewById<ImageButton>(R.id.settings_memory)
        memoryButton?.setOnClickListener {
            val intent = Intent(context, ManageMemoryActivity::class.java)
            startActivity(intent)
        }

        val privacyButton = view?.findViewById<ImageButton>(R.id.settings_privacy)
        privacyButton?.setOnClickListener {
            val intent = Intent(context, ManagePrivacyActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupWeatherHandlers() {
        val weatherButtonContainer = view?.findViewById<LinearLayout>(R.id.weather_button_container) ?: return

        for (type in OverlayTypes.values()) {
            val newButton = Button(context)
            newButton.text = getNameForOverlayType(type)

            val layout = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            val marginSize = context?.let { ScreenUnits.convertDipToPixels(10f, it) }?.toInt() ?: 0
            val paddingSize = context?.let { ScreenUnits.convertDipToPixels(30f, it) }?.toInt() ?: 0
            layout.setMargins(marginSize, marginSize, marginSize, marginSize)

            newButton.setPadding(0, paddingSize, 2, paddingSize)
            newButton.setOnClickListener {
                showOrToggleMenu()
                overlayModel?.setOverlayType(type)
            }

            weatherButtonContainer.addView(newButton)
        }
    }

    private fun getNameForOverlayType(type: OverlayTypes): String {
        return when (type) {
            OverlayTypes.None -> {
                "None"
            }
            OverlayTypes.Category -> {
                "Flight Category"
            }
            OverlayTypes.Ceiling -> {
                "Ceiling"
            }
            OverlayTypes.CloudCover -> {
                "Cloud Cover"
            }
            OverlayTypes.DewPointSpread -> {
                "Dew Point Spread Celsius"
            }
            OverlayTypes.SurfaceWind -> {
                "Surface Wind"
            }
            OverlayTypes.Temperature -> {
                "Temperature Celcius"
            }
            OverlayTypes.Visibility -> {
                "Visibility"
            }
        }
    }

    private fun showOrToggleMenu(menu: View? = null) {
        val displayedMenu = this.displayedMenu
        if ( displayedMenu != null && displayedMenu != menu ) displayedMenu.visibility = View.GONE
        if ( menu != null ) {
            if ( menu.visibility == View.VISIBLE ) {
                menu.visibility = View.INVISIBLE
               this.displayedMenu = null
            } else {
                menu.visibility = View.VISIBLE
                this.displayedMenu = menu
            }
        }
    }
}