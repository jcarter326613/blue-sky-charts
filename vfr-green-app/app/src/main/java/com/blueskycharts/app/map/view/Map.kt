package com.blueskycharts.app.map.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.blueskycharts.app.utility.ScreenUnits
import java.util.*
import kotlin.concurrent.timerTask

abstract class Map(context: Context, attributes: AttributeSet): View(context, attributes) {
    private val periodicRefreshTimer = Timer(false)

    init {
        val task: TimerTask = timerTask {
            requestRedraw()
        }
        periodicRefreshTimer.scheduleAtFixedRate(task, 3 * 1000, 3 * 1000)
    }

    fun requestRedraw() {
        this.postInvalidate()
    }

    fun convertDipToPixels(dp: Float): Float {
        return ScreenUnits.convertDipToPixels(dp, context)
    }
}
