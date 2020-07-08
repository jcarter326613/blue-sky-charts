package com.blueskycharts.app.map.assetmanagement

import android.util.JsonReader
import android.util.JsonWriter
import java.io.StringWriter

data class Manifest(val mapGroups: MutableMap<String, MapList> = mutableMapOf()) {
    val jsonString: String
        get() {
            val stringWriter = StringWriter()
            val jsonWriter = JsonWriter(stringWriter)
            write(jsonWriter)
            return stringWriter.toString()
        }

    fun write(jsonWriter: JsonWriter) {
        jsonWriter.beginObject()
        jsonWriter.name("mapGroups")
        jsonWriter.beginObject()
        for ( group in mapGroups ) {
            jsonWriter.name(group.key)
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
                            retVal.mapGroups[name] = value
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

            data class MapVersion(val xMap: MutableMap<Int, MutableSet<Int>> = mutableMapOf()) {
                fun write(jsonWriter: JsonWriter) {
                    jsonWriter.beginObject()
                    jsonWriter.name("x")
                    jsonWriter.beginObject()
                    for ( x in xMap ) {
                        jsonWriter.name(x.key.toString())
                        jsonWriter.beginArray()
                        for (y in x.value) {
                            jsonWriter.value(y)
                        }
                        jsonWriter.endArray()
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
                                "x" -> {
                                    reader.beginObject()
                                    while ( reader.hasNext() ) {
                                        val xString = reader.nextName()
                                        val ySet = mutableSetOf<Int>()
                                        reader.beginArray()
                                        while ( reader.hasNext() ) {
                                            ySet.add(reader.nextInt())
                                        }
                                        retVal.xMap[xString.toInt()] = ySet
                                        reader.endArray()
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