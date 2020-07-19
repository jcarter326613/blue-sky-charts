package com.blueskycharts.app.map.assetmanagement

import android.util.JsonReader
import android.util.JsonWriter
import com.blueskycharts.app.assests.PersistentFileContents
import com.blueskycharts.app.preferences.Preferences
import com.blueskycharts.app.utility.HashQueue
import com.blueskycharts.app.utility.JsonSerializable
import java.io.StringWriter

data class Manifest(val mapGroups: MutableMap<Int, MapList> = mutableMapOf()) : PersistentFileContents {
    private var touchOrder = HashQueue<FileDescription>()

    override suspend fun getJsonString(): String {
        val stringWriter = StringWriter()
        val jsonWriter = JsonWriter(stringWriter)
        write(jsonWriter)
        return stringWriter.toString()
    }

    suspend fun touchFile(groupId: Int, mapName: String, z: Int, x: Int, y: Int) {
        val newDescription = FileDescription(groupId, mapName, z, x, y)
        touchOrder.touch(newDescription)
    }

    suspend fun popOldestUnPersistedFile(): FileDescription? {
        val iterator = touchOrder.reverseIterator()
        try {
            while (iterator.hasNext()) {
                val item = iterator.next()
                if (!Preferences.instance.getBooleanValue(
                        Preferences.propertyTemplateMapProactiveDownload(
                            item.groupId,
                            item.mapName
                        ), Preferences.defaultValueMapProactiveDownload
                    )
                ) {
                    touchOrder.removeItem(iterator)
                    val mapLocal = mapGroups[item.groupId]?.mapList?.get(item.mapName)
                    item.versions = mapLocal?.versionList?.keys
                    val allVersions = mapLocal?.versionList
                    if ( allVersions != null ) {
                        val versionsToDelete = mutableListOf<String>()
                        for (version in allVersions) {
                            val zoomMap = version.value.zoomMap[item.z]
                            val xMap = zoomMap?.get(item.x)
                            xMap?.remove(item.y)
                            if (xMap != null && xMap.isEmpty()) {
                                zoomMap.remove(item.x)
                            }
                            if (zoomMap != null && zoomMap.isEmpty()) {
                                version.value.zoomMap.remove(item.z)
                            }
                            if (version.value.zoomMap.isEmpty()) {
                                versionsToDelete.add(version.key)
                            }
                        }
                        for (version in versionsToDelete) {
                            allVersions.remove(version)
                        }
                    }
                    return item
                }
            }
        } finally {
            touchOrder.freeIterator()
        }
        return null
    }

    data class FileDescription(val groupId: Int, val mapName: String, val z: Int, val x: Int, val y: Int) : JsonSerializable {
        var versions: Set<String>? = null

        override suspend fun write(jsonWriter: JsonWriter) {
            jsonWriter.beginObject()
            jsonWriter.name("groupId")
            jsonWriter.value(groupId)
            jsonWriter.name("mapName")
            jsonWriter.value(mapName)
            jsonWriter.name("z")
            jsonWriter.value(z)
            jsonWriter.name("x")
            jsonWriter.value(x)
            jsonWriter.name("y")
            jsonWriter.value(y)
            jsonWriter.endObject()
        }
    }

    private suspend fun write(jsonWriter: JsonWriter) {
        jsonWriter.beginObject()
        jsonWriter.name("mapGroups")
        jsonWriter.beginObject()
        for ( group in mapGroups ) {
            jsonWriter.name(group.key.toString())
            group.value.write(jsonWriter)
        }

        jsonWriter.endObject()

        jsonWriter.name("touchOrder")
        try {
            touchOrder.write(jsonWriter)
        } catch (e: Throwable) {
            val t0 = 0
            val t1 = 1
        }
        jsonWriter.endObject()
    }

    companion object {
        fun readFromJsonReader(reader: JsonReader): Manifest {
            val retVal = Manifest()
            reader.beginObject()
            while ( reader.hasNext() ) {
                when ( reader.nextName() ) {
                    "mapGroups" -> {
                        reader.beginObject()
                        while ( reader.hasNext() ) {
                            val name = reader.nextName()
                            val value = MapList.readFromJsonReader(reader)
                            retVal.mapGroups[name.toInt()] = value
                        }
                        reader.endObject()
                    }
                    "touchOrder" -> {
                        retVal.touchOrder = HashQueue.read(reader) {
                            var groupId: Int? = null
                            var mapName: String? = null
                            var z: Int? = null
                            var x: Int? = null
                            var y: Int? = null

                            it.beginObject()
                            while (it.hasNext()) {
                                when (it.nextName()) {
                                    "groupId" -> {
                                        groupId = reader.nextInt()
                                    }
                                    "mapName" -> {
                                        mapName = reader.nextString()
                                    }
                                    "z" -> {
                                        z = reader.nextInt()
                                    }
                                    "x" -> {
                                        x = reader.nextInt()
                                    }
                                    "y" -> {
                                        y = reader.nextInt()
                                    }
                                }
                            }
                            it.endObject()

                            return@read if ( groupId != null && mapName != null && z != null && x != null && y != null ) {
                                FileDescription(groupId, mapName, z, x, y)
                            } else {
                                null
                            }
                        }
                    }
                    else -> {
                        reader.skipValue()
                    }
                }
            }
            reader.endObject()
            return retVal
        }
    }

    data class MapList(val mapList: MutableMap<String, MapVersionList> = mutableMapOf()) {
        fun write(jsonWriter: JsonWriter) {
            jsonWriter.beginObject()
            jsonWriter.name("mapList")
            jsonWriter.beginObject()
            for ( list in mapList ) {
                jsonWriter.name(list.key)
                list.value.write(jsonWriter)
            }
            jsonWriter.endObject()
            jsonWriter.endObject()
        }

        companion object {
            fun readFromJsonReader(reader: JsonReader): MapList {
                val retVal = MapList()
                reader.beginObject()
                while ( reader.hasNext() ) {
                    when ( reader.nextName() ) {
                        "mapList" -> {
                            reader.beginObject()
                            while ( reader.hasNext() ) {
                                val name = reader.nextName()
                                val value = MapVersionList.readFromJsonReader(reader)
                                retVal.mapList[name] = value
                            }
                            reader.endObject()
                        }
                        else -> {
                            reader.skipValue()
                        }
                    }
                }
                reader.endObject()
                return retVal
            }
        }

        data class MapVersionList(val versionList: MutableMap<String, MapVersion> = mutableMapOf()) {
            fun write(jsonWriter: JsonWriter) {
                jsonWriter.beginObject()
                jsonWriter.name("versionList")
                jsonWriter.beginObject()
                for ( version in versionList ) {
                    jsonWriter.name(version.key)
                    version.value.write(jsonWriter)
                }
                jsonWriter.endObject()
                jsonWriter.endObject()
            }

            companion object {
                fun readFromJsonReader(reader: JsonReader): MapVersionList {
                    val retVal = MapVersionList()
                    reader.beginObject()
                    while ( reader.hasNext() ) {
                        when ( reader.nextName() ) {
                            "versionList" -> {
                                reader.beginObject()
                                while ( reader.hasNext() ) {
                                    val name = reader.nextName()
                                    val value = MapVersion.readFromJsonReader(reader)
                                    retVal.versionList[name] = value
                                }
                                reader.endObject()
                            }
                            else -> {
                                reader.skipValue()
                            }
                        }
                    }
                    reader.endObject()
                    return retVal
                }
            }

            data class MapVersion(
                val zoomMap: MutableMap<Int,MutableMap<Int, MutableMap<Int, Int>>> = mutableMapOf() // z, x, y, fileSize
            ) {
                fun write(jsonWriter: JsonWriter) {
                    jsonWriter.beginObject()
                    jsonWriter.name("zoom")
                    jsonWriter.beginObject()
                    for (z in zoomMap) {
                        jsonWriter.name(z.key.toString())
                        jsonWriter.beginObject()
                        jsonWriter.name("x")
                        jsonWriter.beginObject()
                        for ( x in z.value ) {
                            jsonWriter.name(x.key.toString())
                            jsonWriter.beginObject()
                            for (y in x.value) {
                                jsonWriter.name(y.key.toString())
                                jsonWriter.value(y.value)
                            }
                            jsonWriter.endObject()
                        }
                        jsonWriter.endObject()
                        jsonWriter.endObject()
                    }
                    jsonWriter.endObject()
                    jsonWriter.endObject()
                }

                companion object {
                    fun readFromJsonReader(reader: JsonReader): MapVersion {
                        val retVal = MapVersion()
                        reader.beginObject()
                        while ( reader.hasNext() ) {
                            when ( reader.nextName() ) {
                                "zoom" -> {
                                    reader.beginObject()
                                    while ( reader.hasNext() ) {
                                        val zoom = reader.nextName().toInt()
                                        reader.beginObject()
                                        while ( reader.hasNext() ) {
                                            when ( reader.nextName() ) {
                                                "x" -> {
                                                    val xMap = mutableMapOf<Int, MutableMap<Int,Int>>()
                                                    reader.beginObject()
                                                    while ( reader.hasNext() ) {
                                                        val xString = reader.nextName()
                                                        val ySet = mutableMapOf<Int, Int>()
                                                        reader.beginObject()
                                                        while ( reader.hasNext() ) {
                                                            val yString = reader.nextName()
                                                            ySet[yString.toInt()] = reader.nextInt()
                                                        }
                                                        xMap[xString.toInt()] = ySet
                                                        reader.endObject()
                                                    }
                                                    reader.endObject()
                                                    retVal.zoomMap[zoom] = xMap
                                                }
                                                else -> {
                                                    reader.skipValue()
                                                }
                                            }
                                        }
                                        reader.endObject()
                                    }
                                    reader.endObject()
                                }
                                else -> {
                                    reader.skipValue()
                                }
                            }
                        }
                        reader.endObject()
                        return retVal
                    }
                }
            }
        }
    }
}