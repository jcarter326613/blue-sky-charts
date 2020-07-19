package com.blueskycharts.app.assests

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A thread safe persistent file filled with whatever data structure you want.  Only ever edit the file when inside a makeChange call.
 */
class PersistentFile<T: PersistentFileContents>(private val contents: T, private val fileDescription: LocalAssetDescription) {
    private var fileVersion = 0
    private var writtenVersion = 0
    private val editMutex: Mutex = Mutex()
    private val serializeMutex: Mutex = Mutex()

    /**
     * Allows the caller to make a change to the wrapped object.  The callback must return true if a
     * change was made and false otherwise.
     */
    suspend fun access(callback: (suspend (it: T) -> Boolean) ) {
        val changeMade: Boolean
        editMutex.withLock {
            changeMade = callback(contents)
        }
        if ( changeMade ) {
            fileVersion++
            serializeFile()
        }
    }

    /**
     * Writes out the file in a seperate thread.  This can be called multiple times from many threads and will only
     * run once if those requests pile up faster than than the file can be written to disk
     */
    private fun serializeFile() {
        GlobalScope.launch(Dispatchers.IO) {    //ok1
            serializeMutex.withLock {
                if (fileVersion > writtenVersion) {
                    var versionToWrite: Int
                    val manifestAsset = Asset(fileDescription)
                    editMutex.withLock {
                        versionToWrite = fileVersion
                        manifestAsset.bytes = contents.getJsonString().toByteArray()
                    }
                    DiskCacheFactory.instance.writeAsset(manifestAsset)
                    writtenVersion = versionToWrite
                }
            }
        }
    }
}