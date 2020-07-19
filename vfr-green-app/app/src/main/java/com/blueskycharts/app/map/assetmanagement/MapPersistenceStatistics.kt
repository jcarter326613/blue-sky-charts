package com.blueskycharts.app.map.assetmanagement

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class MapPersistenceStatistics(val groupId: Int, val mapName: String, totalFiles: Long, downloadedFiles: Long, downloadedSizeBytes: Long) {
    var totalFiles = LoudLong(this, totalFiles)
        private set
    var downloadedFiles = LoudLong(this, downloadedFiles)
        private set
    var downloadedSizeBytes = LoudLong(this, downloadedSizeBytes)
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

    suspend fun setStatistics(totalFiles: Long, downloadedFiles: Long, downloadedSizeBytes: Long) {
        if ( totalFiles < downloadedFiles ) {
            this.downloadedFiles = LoudLong(this, totalFiles)
        } else {
            this.downloadedFiles = LoudLong(this, downloadedFiles)
        }
        this.totalFiles = LoudLong(this, totalFiles)
        this.downloadedSizeBytes = LoudLong(this, downloadedSizeBytes)
        fieldUpdated()
    }

    private suspend fun fieldUpdated() {
        broadcastNeeded = true
        broadcastMutex.withLock {
            if (!broadcastNeeded) {
                return
            }
            for (listener in listeners) {
                var downloadedFiles = this.downloadedFiles.value
                if (downloadedFiles > totalFiles.value) {
                    downloadedFiles = totalFiles.value
                }
                listener.statisticsUpdated(totalFiles.value, downloadedFiles, downloadedSizeBytes.value)
            }
            broadcastNeeded = false
        }
    }

    interface Listener {
        fun statisticsUpdated(totalFiles: Long, downloadedFiles: Long, downloadedSizeBytes: Long)
    }

    class LoudLong(private val listener: MapPersistenceStatistics, initialValue: Long) {
        private val _value = AtomicLong(initialValue)
        val value: Long
            get() {
                return _value.get()
            }

        suspend fun increment() {
            _value.incrementAndGet()
            listener.fieldUpdated()
        }

        suspend fun decrement() {
            _value.decrementAndGet()
            if (_value.get() < 0) {
                _value.set(0)
            }
            listener.fieldUpdated()
        }

        suspend fun add(toAdd: Long) {
            _value.addAndGet(toAdd)
            if (_value.get() < 0) {
                _value.set(0)
            }
            listener.fieldUpdated()
        }
    }
}