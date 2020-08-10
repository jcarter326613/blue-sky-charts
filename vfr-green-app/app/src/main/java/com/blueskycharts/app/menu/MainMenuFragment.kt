package com.blueskycharts.app.menu

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.blueskycharts.app.R
import com.blueskycharts.app.preferences.SetPreferencesActivity
import com.blueskycharts.app.map.models.OverlayViewModel
import com.blueskycharts.app.map.view.OverlayTypes
import com.blueskycharts.app.mapselection.MapSelectionActivity
import com.blueskycharts.app.preferences.Preferences
import com.blueskycharts.app.subscription.ManageSubscriptionActivity
import com.blueskycharts.app.subscription.SubscriptionActivity
import com.blueskycharts.app.subscription.SubscriptionChecker
import com.blueskycharts.app.utility.Log

class MainMenuFragment() : Fragment() {
    private var overlayModel: OverlayViewModel? = null
    private var displayedMenu: View? = null

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
                if (displayedMenu != null) showOrToggleMenu(displayedMenu)
                else showOrToggleMenu(mainMenu)
            }

            val zoomToSelfButton = view.findViewById<ImageButton>(R.id.zoomToSelfButton)
            zoomToSelfButton?.setOnClickListener {
                Preferences.instance.setPreference(Preferences.propertyNameMapTrackLocation, true)
            }

            setupMainMenuHandlers()
            setupWeatherHandlers()
        } else {
            Log.error(null, "Loaded view without activity.  Can not connect view model.")
        }
    }

    override fun onResume() {
        super.onResume()
        Preferences.instance.addListener(object: Preferences.Listener{
            override fun preferenceChanged(preferenceName: String) {
                if (preferenceName == Preferences.propertyNameMapTrackLocation) {
                    val zoomToSelfButton = view?.findViewById<ImageButton>(R.id.zoomToSelfButton)
                    if (Preferences.instance.getBooleanValue(Preferences.propertyNameMapTrackLocation, Preferences.defaultValueMapTrackLocation)) {
                        zoomToSelfButton?.setImageResource(R.drawable.ic_my_location_set)
                    } else {
                        zoomToSelfButton?.setImageResource(R.drawable.ic_my_location_not_set)
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

        val preferencesButton = view?.findViewById<Button>(R.id.preferences)
        preferencesButton?.setOnClickListener {
            startActivity(Intent(context, SetPreferencesActivity::class.java))
        }

        val subscriptionButton = view?.findViewById<Button>(R.id.subscription)
        subscriptionButton?.setOnClickListener {
            val intent = Intent(context, ManageSubscriptionActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupWeatherHandlers() {
        var button: Button? = view?.findViewById(R.id.none)
        button?.setOnClickListener {
            showOrToggleMenu()
            overlayModel?.setOverlayType(OverlayTypes.None)
        }

        button = view?.findViewById(R.id.ceiling)
        button?.setOnClickListener {
            showOrToggleMenu()
            overlayModel?.setOverlayType(OverlayTypes.Ceiling)
        }

        button = view?.findViewById(R.id.visibility)
        button?.setOnClickListener {
            showOrToggleMenu()
            overlayModel?.setOverlayType(OverlayTypes.Visibility)
        }

        button = view?.findViewById(R.id.cloud_cover)
        button?.setOnClickListener {
            showOrToggleMenu()
            overlayModel?.setOverlayType(OverlayTypes.CloudCover)
        }

        button = view?.findViewById(R.id.wind)
        button?.setOnClickListener {
            showOrToggleMenu()
            overlayModel?.setOverlayType(OverlayTypes.SurfaceWind)
        }

        button = view?.findViewById(R.id.temperature)
        button?.setOnClickListener {
            showOrToggleMenu()
            overlayModel?.setOverlayType(OverlayTypes.Temperature)
        }

        button = view?.findViewById(R.id.dewpoint)
        button?.setOnClickListener {
            showOrToggleMenu()
            overlayModel?.setOverlayType(OverlayTypes.DewPointSpread)
        }

        button = view?.findViewById(R.id.category)
        button?.setOnClickListener {
            showOrToggleMenu()
            overlayModel?.setOverlayType(OverlayTypes.Category)
        }
    }

    private fun showOrToggleMenu(menu: View? = null) {
        val displayedMenu = this.displayedMenu
        if ( displayedMenu != null && displayedMenu != menu ) displayedMenu.visibility = View.INVISIBLE
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