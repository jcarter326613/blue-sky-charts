package com.blueskycharts.app.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import androidx.fragment.app.Fragment
import com.blueskycharts.app.R

class MainMenu : Fragment() {
    private var gridLayout: GridLayout? = null;

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_main_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.gridLayout = view.findViewById(R.id.main_menu_grid_layout)

        val openMenuButton = Button(context)
        openMenuButton.setOnClickListener {
            openMenuButton.visibility = View.INVISIBLE
        }
    }
}