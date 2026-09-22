package com.blueskycharts.app.map.resources

import android.content.Context
import android.graphics.Canvas
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import com.blueskycharts.app.R
import com.blueskycharts.app.coordinates.Box2d
import com.blueskycharts.app.coordinates.Point2d
import com.blueskycharts.app.map.configuration.Inventory
import com.blueskycharts.app.map.view.Map

class ShadowProvider(context: Context): TileProviderInterface {
    private val image = ResourcesCompat.getDrawable(context.resources, R.drawable.world_shadow, null)

    override fun retrieveTile(mapName: String, mapVersion: String, zoomLevel: Int, location: Point2d, tileDimensions: Point2d,
                     receiver: TileReceiver, data: Any?, canvas: Canvas ) {
        val box = Box2d(0.0, 0.0, 1.0, 1.0)
        receiver.receiveTile(box, image?.toBitmap(), data, true, canvas)
    }
}