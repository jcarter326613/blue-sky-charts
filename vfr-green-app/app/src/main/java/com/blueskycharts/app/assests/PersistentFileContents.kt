package com.blueskycharts.app.assests

interface PersistentFileContents {
    suspend fun getJsonString(): String
}