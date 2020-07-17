package com.blueskycharts.app.map.assetmanagement

import android.util.JsonReader
import android.util.JsonWriter
import com.blueskycharts.app.assests.PersistentFileContents
import java.io.StringWriter

data class Manifest(val mapGroups: MutableMap<Int, MapList> = mutableMapOf()) : PersistentFileContents {
    override val jsonString: String
        get() {
            val stringWriter = StringWriter()
            val jsonWriter = JsonWriter(stringWriter)
            write(jsonWriter)
            return stringWriter.toString()
        }

    private fun write(jsonWriter: JsonWriter) {
        jsonWriter.beginObject()
        jsonWriter.name("mapGroups")
        jsonWriter.beginObject()
        for ( group in mapGroups ) {
            jsonWriter.name(group.key.toString())
            group.value.write(jsonWriter)
        }

        jsonWriter.endObject()
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