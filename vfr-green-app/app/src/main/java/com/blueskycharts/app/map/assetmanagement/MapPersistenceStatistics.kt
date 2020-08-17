package com.blueskycharts.app.map.assetmanagement

import com.blueskycharts.app.preferences.Preferences
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class MapPersistenceStatistics(val groupId: Int, val mapName: String, downloadedSizeBytes: Long) {
    var downloadedSizeBytes: Long = downloadedSizeBytes
        private set

    private val listeners = mutableListOf<Listener>()
    private var broadcastNeeded = false
    private val broadcastMutex = Mutex()
    private val pendingRemoveCount = AtomicInteger(0)

    /**
     * Adds a listener and guarantees an updated broadcast of statistics
     */
    fun addListener(newListener: Listener) {
        GlobalScope.launch {
            broadcastMutex.withLock {
                listeners.add(newListener)
                newListener.statisticsUpdated(groupId, mapName, downloadedSizeBytes)
            }
        }
    }

    fun removeListener(oldListener: Listener) {
        pendingRemoveCount.incrementAndGet()
        GlobalScope.launch {
            broadcastMutex.withLock {
                listeners.remove(oldListener)
                pendingRemoveCount.decrementAndGet()
            }
        }
    }

    suspend fun setStatistics(downloadedSizeBytes: Long) {
        this.downloadedSizeBytes = downloadedSizeBytes
        fieldUpdated()
    }

    private suspend fun fieldUpdated() {
        broadcastNeeded = true
        broadcastMutex.withLock {
            if (!broadcastNeeded) {
                return
            }
            for (listener in listeners) {
                listener.statisticsUpdated(groupId, mapName, downloadedSizeBytes)
            }
            broadcastNeeded = false
        }
    }

    interface Listener {
        fun statisticsUpdated(groupId: Int, mapId: String, downloadedSizeBytes: Long)
    }
}