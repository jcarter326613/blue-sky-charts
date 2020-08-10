package com.blueskycharts.app.utility

import android.content.Context

object ScreenUnits {
    fun convertDipToPixels(dp: Float, context: Context): Float {
        return dp * context.resources.displayMetrics.density + 0.5f
    }
}