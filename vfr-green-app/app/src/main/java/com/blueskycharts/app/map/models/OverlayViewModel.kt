package com.blueskycharts.app.map.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.blueskycharts.app.map.view.OverlayTypes

class OverlayViewModel : ViewModel() {
    private var overlayType: MutableLiveData<OverlayTypes> = MutableLiveData()

    init {
        overlayType.value =  OverlayTypes.None
    }

    fun getOverlayType(): LiveData<OverlayTypes> {
        return overlayType
    }

    fun setOverlayType(type: OverlayTypes) {
        overlayType.value = type
    }
}