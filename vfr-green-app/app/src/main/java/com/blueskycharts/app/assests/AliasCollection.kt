package com.blueskycharts.app.assests

import android.util.JsonReader
import android.util.JsonWriter
import java.io.StringWriter

class AliasCollection: PersistentFileContents {
    override val jsonString: String
        get() {
            val stringWriter = StringWriter()
            val jsonWriter = JsonWriter(stringWriter)
            write(jsonWriter)
            return stringWriter.toString()
        }

    private fun write(jsonWriter: JsonWriter) {
        jsonWriter.beginObject()
        for ( f in actualFiles ) {
            jsonWriter.name(f.key)
            jsonWriter.beginArray()
            for ( f2 in f.value ) {
                jsonWriter.value(f2)
            }
            jsonWriter.endArray()
        }
        jsonWriter.endObject()
    }

    val actualFiles: MutableMap<String,MutableSet<String>> = mutableMapOf()
    val aliasFiles: MutableMap<String,String> = mutableMapOf()

    companion object {
        fun readFromJsonReader(reader: JsonReader): AliasCollection {
            val retVal = AliasCollection()

            reader.beginObject()
            while ( reader.hasNext() ) {
                val realFileName = reader.nextName()
                reader.beginArray()
                while ( reader.hasNext() ) {
                    val aliasFileName = reader.nextString()
                    var actualFilesList = retVal.actualFiles[realFileName]
                    if ( actualFilesList == null ) {
                        actualFilesList = mutableSetOf()
                        retVal.actualFiles[realFileName] = actualFilesList
                    }
                    actualFilesList.add(aliasFileName)
                    retVal.aliasFiles[aliasFileName] = realFileName
                }
                reader.endArray()
            }
            reader.endObject()

            return retVal
        }
    }
}