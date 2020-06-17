package com.blueskycharts.app.map.models

import com.blueskycharts.app.coordinates.BoxGeo
import com.blueskycharts.app.coordinates.PointGeo

data class BoxGeoModel( val topLeft: PointGeo?,
                        val topRight: PointGeo?,
                        val bottomLeft: PointGeo?,
                        val bottomRight: PointGeo? ) {

    public fun createBoxGeo(): BoxGeo {
        return BoxGeo(topLeft ?: PointGeo(), bottomRight ?: PointGeo(), topRight, bottomLeft);
    }
};
