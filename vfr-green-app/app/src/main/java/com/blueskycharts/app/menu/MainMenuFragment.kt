package com.blueskycharts.app.menu

import android.content.Intent
import android.os.Bundle
import android.util.Log
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

            setupMainMenuHandlers()
            setupWeatherHandlers()
        } else {
            Log.e(null, "Loaded view without activity.  Can not connect view model.")
        }
    }

    private fun setupMainMenuHandlers() {
        val weatherMenu = view?.findViewById<View>(R.id.weather_grid_layout)
        val weatherButton = view?.findViewById<Button>(R.id.weather_button)
        weatherButton?.setOnClickListener {
            showOrToggleMenu(weatherMenu)
        }

        val preferencesButton = view?.findViewById<Button>(R.id.preferences)
        preferencesButton?.setOnClickListener {
            startActivity(Intent(context, SetPreferencesActivity::class.java))
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