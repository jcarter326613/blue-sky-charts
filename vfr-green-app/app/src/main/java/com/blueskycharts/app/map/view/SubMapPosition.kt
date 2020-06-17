package com.blueskycharts.app.map.view

import com.blueskycharts.app.coordinates.BoxWebMercator

data class SubMapPosition (
    val subMapView: SubMapView,
    var location: BoxWebMercator
)