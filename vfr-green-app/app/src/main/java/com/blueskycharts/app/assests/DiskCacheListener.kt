package com.blueskycharts.app.assests

interface DiskCacheListener {
    suspend fun totalSizeChanged(totalSize: Long)
}