package com.blueskycharts.app.map.view

import android.content.Context
import android.util.AttributeSet
import android.view.View

abstract class Map(context: Context, attributes: AttributeSet): View(context, attributes) {
    fun requestRedraw() {
        this.postInvalidate()
    }

    fun convertDipToPixels(dp: Float): Float {
        return dp * context.resources.displayMetrics.density + 0.5f
    }
}
