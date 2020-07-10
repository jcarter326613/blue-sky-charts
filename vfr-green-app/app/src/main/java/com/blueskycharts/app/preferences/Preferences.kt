package com.blueskycharts.app.preferences

import com.blueskycharts.app.assests.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class Preferences private constructor() {
    // Private variables
    private val preferences: MutableMap<String, String> = mutableMapOf()
    private val preferencesAssetDescription = LocalAssetDescription("global-preferences", Volatility.Indefinite)
    private var version = 0
    private var writtenVersion = 0
    private val persistMutex = Mutex()

    init {
        val preferencesAsset = Asset(preferencesAssetDescription)
        if ( DiskCacheFactory.instance.retrieveAssetBytesNoAliasCheck(preferencesAsset) ) {
            val array = preferencesAsset.asStringArray()
            if ( array != null ) {
                for (line in array) {
                    val keyAndValue = line.split("=")
                    if (keyAndValue.count() == 2) {
                        val key = keyAndValue[0]
                        val value = keyAndValue[1]
                        preferences[key] = value
                    }
                }
            }
        }
    }

    fun setPreference(key: String, value: String) {
        this.preferences[key] = value
        persist()
    }

    fun setPreference(key: Array<String>, value: String) {
        if ( !validateKeyArray(key) ) {
            throw Error("Invalid key for set preferences ${key.contentToString()}")
        }

        setPreference(getKeyString(key), value)
    }

    fun setPreference(key: String, value: Int) = setPreference(key, value.toString())
    fun setPreference(key: Array<String>, value: Int) = setPreference(key, value.toString())
    fun setPreference(key: String, value: Boolean) = setPreference(key, value.toString())
    fun setPreference(key: Array<String>, value: Boolean) = setPreference(key, value.toString())

    fun getStringValue(key: String, defaultValue: String): String = this.preferences[key] ?: defaultValue
    fun getStringValue(key: Array<String>, defaultValue: String): String = getStringValue(getKeyString(key), defaultValue)
    fun getBooleanValue(key: String, defaultValue: Boolean): Boolean = this.preferences[key]?.toBoolean() ?: defaultValue
    fun getBooleanValue(key: Array<String>, defaultValue: Boolean): Boolean = getBooleanValue(getKeyString(key), defaultValue)
    fun getIntValue(key: String, defaultValue: Int): Int = this.preferences[key]?.toInt() ?: defaultValue
    fun getIntValue(key: Array<String>, defaultValue: Int): Int = getIntValue(getKeyString(key), defaultValue)

    private fun validateKeyArray(tokens: Array<String>): Boolean {
        for ( token in tokens ) {
            if ( !validateKeyToken(token) ) {
                return false
            }
        }
        return true
    }

    private fun getKeyString(tokens: Array<String>): String = tokens.joinToString(".")

    private fun validateKeyToken(token: String): Boolean {
        for ( c in token ) {
            if (!((c in 'a'..'z') ||
                  (c in 'A'..'Z') ||
                  (c in '0'..'9')) ) {
                return false
            }
        }
        return true
    }

    private fun persist() {
        version++
        GlobalScope.launch {
            persistMutex.withLock {
                if ( version > writtenVersion ) {
                    // Generate the file contents
                    val asset = Asset(preferencesAssetDescription)
                    var fileContents = ""
                    var firstPreference = true
                    for ( preference in preferences ) {
                        if (!firstPreference) {
                            fileContents += "\n"
                        }
                        firstPreference = false
                        val entry = "${preference.key}=${preference.value}"
                        fileContents += entry
                    }
                    asset.bytes = fileContents.toByteArray()

                    // Write the contents to the file
                    DiskCacheFactory.instance.writeAsset(asset)

                    // Update the written version
                    writtenVersion = version
                }
            }
        }
    }

    companion object {
        val instance: Preferences = Preferences()

        // Property constants
        fun propertyTemplateMapProactiveDownload(mapName: String): String = "map.$mapName.proactiveDownload"

        // Default values
        const val defaultTemplateMapProactiveDownload = false
    }
}