package com.blueskycharts.app.preferences

import com.blueskycharts.app.assests.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class Preferences private constructor() {
    // Private variables
    private val preferences: MutableMap<String, String> = mutableMapOf()
    private val preferencesAssetDescription = LocalAssetDescription("global-preferences", Volatility.Indefinite, StorageLocation.Internal)
    private var version = 0
    private var writtenVersion = 0
    private val persistMutex = Mutex()
    private val listeners = mutableListOf<Listener>()
    private val listenerMutex = Mutex()

    init {
        val preferencesAsset = Asset(preferencesAssetDescription)
        if ( DiskCacheFactory.instance.retrieveAssetBytes(preferencesAsset) ) {
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

    /**
     * Adds a listener to receive notification of property updates. Guarantees a call to the new listener with every property after added.
     */
    fun addListener(newListener: Listener) {
        GlobalScope.launch {    //ok1
            listenerMutex.withLock {
                this@Preferences.listeners.add(newListener)
            }
            persistMutex.withLock {
                for (preference in this@Preferences.preferences) {
                    newListener.preferenceChanged(preference.key)
                }
            }
        }
    }

    fun setPreference(key: String, value: String) {
        GlobalScope.launch {    //ok1
            persistMutex.withLock {
                this@Preferences.preferences[key] = value
                persist()
            }
            listenerMutex.withLock {
                for (listener in this@Preferences.listeners) {
                    listener.preferenceChanged(key)
                }
            }
        }
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
        GlobalScope.launch {    //ok1
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

    interface Listener {
        fun preferenceChanged(preferenceName: String)
    }

    companion object {
        val instance: Preferences = Preferences()

        // Property constants
        fun propertyTemplateMapProactiveDownload(mapGroup: Int, mapName: String): String = "map.$mapGroup.$mapName.proactiveDownload"
        val propertyNameDisplayedMapGroupId: String = "map.active.group"
        val propertyNameDisplayedSubMapId: String = "map.active.submap"
        val propertyNameMaxUnPersistedTileDiskSpace: String = "map.cache.size"
        val propertyNameStoreMapsExternally: String = "map.storage.external"

        // Default values
        const val defaultValueMapProactiveDownload = false
        const val defaultValueDisplayedMapGroupId = 1
        const val defaultValueDisplayedSubMapId: String = ""
        const val defaultValueMaxUnPersistedTileDiskSpace = 50 * 1000 * 1000
        const val defaultValueStoreMapsExternally = false
    }
}