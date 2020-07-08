package com.blueskycharts.app.map.configuration

import com.blueskycharts.app.map.models.MapMetaDataModelCollection
import com.blueskycharts.app.map.models.SubMapModel
import com.blueskycharts.app.map.view.MapTileView
import com.blueskycharts.app.map.view.SubMapPosition
import java.time.LocalDateTime
import java.util.*

/**
 * Represents the configuration for an entire map
 * Allows convenience queries into a MapMetaDataModelCollection
 */
class MapConfiguration(val data: MapMetaDataModelCollection) {
    private var _mapList: List<String>? = null
    val mapList: List<String>
        get() {
            var retVal = _mapList
            if ( retVal == null ) {
                retVal = mutableListOf()
                for (key in data.maps.keys) {
                    retVal.add(key)
                }
                _mapList = retVal
            }
            return retVal
        }

    fun getCurrentVersion(mapName: String): SubMapModel? {
        //TODO: fix the fact that this doens't respect effectiveDate
        val mapVersionCollection = data.maps[mapName] ?: return null
        var latestActiveVersion: Date? = null
        var latestActiveVersionMap: SubMapModel? = null
        val now = Date()
        for (versionEntry in mapVersionCollection.versions) {
            val versionDate = convertVersionToDateTime(versionEntry.key)
            if ( versionDate != null && ( versionDate < now && (latestActiveVersion == null || versionDate > latestActiveVersion)) ) {
                latestActiveVersion = versionDate
                latestActiveVersionMap = versionEntry.value
            }
        }
        return latestActiveVersionMap
    }

    fun getVersion(mapName: String, version: String): SubMapModel? {
        val mapVersionCollection = data.maps[mapName] ?: return null
        for (versionEntry in mapVersionCollection.versions) {
            if ( versionEntry.key == version ) {
                return versionEntry.value
            }
        }
        return null
    }

    /**
     * Sorted from earliest to latest
     */
    fun getFutureSortedVersionList(mapName: String, includeCurrent: Boolean = false): List<String> {
        val currentVersion = getCurrentVersion(mapName)
        if (currentVersion?.version == null) {
            return listOf()
        }

        val mapVersionCollection = data.maps[mapName] ?: return listOf()
        val now = convertVersionToDateTime(currentVersion.version)
        val retVal = mutableListOf<String>()
        for (versionEntry in mapVersionCollection.versions) {
            val versionDate = convertVersionToDateTime(versionEntry.key)
            if ( versionDate != null && versionDate > now ) {
                retVal.add(versionEntry.key)
            }
        }
        if ( includeCurrent ) {
            retVal.add(currentVersion.version)
        }
        return retVal.sorted()
    }

    /**
     * Sorted from latest to earliest
     */
    fun getPastSortedVersionList(mapName: String, includeCurrent: Boolean = false): List<String> {
        val currentVersion = getCurrentVersion(mapName)
        if (currentVersion?.version == null) {
            return listOf()
        }

        val mapVersionCollection = data.maps[mapName] ?: return listOf()
        val now = convertVersionToDateTime(currentVersion.version)
        val retVal = mutableListOf<String>()
        for (versionEntry in mapVersionCollection.versions) {
            val versionDate = convertVersionToDateTime(versionEntry.key)
            if ( versionDate != null && versionDate < now ) {
                retVal.add(versionEntry.key)
            }
        }
        if ( includeCurrent ) {
            retVal.add(currentVersion.version)
        }
        return retVal.sortedDescending()
    }

    companion object {
        fun convertVersionToDateTime(version: String): Date? {
            if (version.length < 19 ) {
                return null
            }
            @Suppress("DEPRECATION")
            return Date(version.substring(0, 4).toInt() - 1900, version.substring(5, 7).toInt() - 1, version.substring(8, 10).toInt(),
                version.substring(11, 13).toInt(), version.substring(14, 16).toInt(), version.substring(17, 19).toInt())
        }
    }
}