package com.blueskycharts.app.map.assetmanagement

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger

class MapPersistenceStatistics(val groupId: Int, val mapName: String, totalFiles: Int, downloadedFiles: Int, downloadedSizeBytes: Int) {
    var totalFiles = LoudInt(this, totalFiles)
        private set
    var downloadedFiles = LoudInt(this, downloadedFiles)
        private set
    var downloadedSizeBytes = LoudInt(this, downloadedSizeBytes)
        private set

    private val listeners = mutableListOf<Listener>()
    private var broadcastNeeded = false
    private val broadcastMutex = Mutex()

    /**
     * Adds a listener and guarantees an updated broadcast of statistics
     */
    fun addListener(newListener: Listener) {
        GlobalScope.launch {    //ok1
            broadcastMutex.withLock {
                listeners.add(newListener)
                newListener.statisticsUpdated(totalFiles.value, downloadedFiles.value, downloadedSizeBytes.value)
            }
        }
    }

    suspend fun setStatistics(totalFiles: Int, downloadedFiles: Int, downloadedSizeBytes: Int) {
        this.totalFiles = LoudInt(this, totalFiles)
        this.downloadedFiles = LoudInt(this, downloadedFiles)
        this.downloadedSizeBytes = LoudInt(this, downloadedSizeBytes)
        fieldUpdated()
    }

    private suspend fun fieldUpdated() {
        broadcastNeeded = true
        broadcastMutex.withLock {
            if (!broadcastNeeded) {
                return
            }
            for (listener in listeners) {
                listener.statisticsUpdated(totalFiles.value, downloadedFiles.value, downloadedSizeBytes.value)
            }
            broadcastNeeded = false
        }
    }

    interface Listener {
        fun statisticsUpdated(totalFiles: Int, downloadedFiles: Int, downloadedSizeBytes: Int)
    }

    class LoudInt(private val listener: MapPersistenceStatistics, initialValue: Int) {
        private val _value = AtomicInteger(initialValue)
        val value: Int
            get() {
                return _value.get()
            }

        suspend fun increment() {
            _value.incrementAndGet()
            listener.fieldUpdated()
        }
    }
}