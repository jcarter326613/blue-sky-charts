package com.blueskycharts.app.map.view

import com.blueskycharts.app.coordinates.Box2d
import com.blueskycharts.app.coordinates.RectangularArea

data class SubMapPosition (
    val subMapView: SubMapView,
    var location: Box2d
)