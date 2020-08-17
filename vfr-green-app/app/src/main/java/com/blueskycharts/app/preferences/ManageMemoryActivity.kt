package com.blueskycharts.app.preferences

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.blueskycharts.app.R
import com.blueskycharts.app.map.assetmanagement.MapPersistenceStatistics
import com.blueskycharts.app.map.assetmanagement.TilePersistenceManager
import com.blueskycharts.app.map.assetmanagement.TilePersistenceManagerFactory
import com.blueskycharts.app.map.configuration.Inventory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class ManageMemoryActivity : MeteredWifiWarningActivity(R.id.wifiNote) {
    private var downloadedMapList = mutableMapOf<String, Long>()
    private var cachedMapList = mutableMapOf<String, Long>()
    private var downloadedAmount: Long = 0
    private var cachedAmount: Long = 0
    private val amountMutex = Mutex()
    private val refreshRequested = AtomicBoolean(false)

    private var downloadAmountView: TextView? = null
    private var cachedAmountView: TextView? = null
    private val downloadListener: MapPersistenceStatistics.Listener
    private val cachedListener: MapPersistenceStatistics.Listener

    init {
        downloadListener = object: MapPersistenceStatistics.Listener {
            override fun statisticsUpdated(groupId: Int, mapId: String, downloadedSizeBytes: Long) {
                GlobalScope.launch {
                    amountMutex.withLock {
                        val key = getKeyForId(groupId, mapId)
                        val oldAmount = downloadedMapList[key] ?: 0
                        val difference = downloadedSizeBytes - oldAmount
                        downloadedAmount += difference
                        downloadedMapList[key] = downloadedSizeBytes
                        refreshContent()
                    }
                }
            }
        }

        cachedListener = object: MapPersistenceStatistics.Listener {
            override fun statisticsUpdated(groupId: Int, mapId: String, downloadedSizeBytes: Long) {
                GlobalScope.launch {
                    amountMutex.withLock {
                        val key = getKeyForId(groupId, mapId)
                        val oldAmount = cachedMapList[key] ?: 0
                        val difference = downloadedSizeBytes - oldAmount
                        cachedAmount += difference
                        cachedMapList[key] = downloadedSizeBytes
                        refreshContent()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_memory)

        val clearCacheButton = findViewById<Button>(R.id.clearCacheButton)
        clearCacheButton?.setOnClickListener {
            AlertDialog.Builder(this@ManageMemoryActivity)
                .setMessage("This will delete all map data that hasn't been marked for offline access.  Are you sure you want to do this?")
                .setPositiveButton("Yes") { _: DialogInterface, _: Int ->
                    Preferences.instance.setPreference(Preferences.propertyNameRequestClearCache, true)
                    TilePersistenceManagerFactory.instance.start()
                }
                .setNegativeButton("No") { _: DialogInterface, _: Int ->
                }
                .create()
                .show()
        }

        downloadAmountView = findViewById<TextView>(R.id.download_amount)
        cachedAmountView = findViewById<TextView>(R.id.cache_amount)
    }

    override fun onResume() {
        super.onResume()

        GlobalScope.launch {
            for (group in Inventory.instance.mapGroups) {
                val config = group.getConfiguration()
                if (config != null) {
                    for (map in config.mapList) {
                        val mapStatistics = TilePersistenceManagerFactory.instance.getMapStatistics(group.id, map)
                        if (Preferences.instance.getBooleanValue(Preferences.propertyTemplateMapProactiveDownload(group.id, map), Preferences.defaultValueMapProactiveDownload)) {
                            mapStatistics.addListener(downloadListener)
                        } else {
                            mapStatistics.addListener(cachedListener)
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()

        GlobalScope.launch {
            for (group in Inventory.instance.mapGroups) {
                val config = group.getConfiguration()
                if (config != null) {
                    for (map in config.mapList) {
                        val mapStatistics = TilePersistenceManagerFactory.instance.getMapStatistics(group.id, map)
                        if (Preferences.instance.getBooleanValue(Preferences.propertyTemplateMapProactiveDownload(group.id, map), Preferences.defaultValueMapProactiveDownload)) {
                            mapStatistics.removeListener(downloadListener)
                        } else {
                            mapStatistics.removeListener(cachedListener)
                        }
                    }
                }
            }
        }
    }
    private fun refreshContent() {
        if (!refreshRequested.getAndSet(true)) {
            GlobalScope.launch(Dispatchers.Main) {
                refreshRequested.set(false)
                downloadAmountView?.text = getDisplayMb(downloadedAmount)
                cachedAmountView?.text = getDisplayMb(cachedAmount)
            }
        }
    }

    private fun getKeyForId(groupId: Int, mapId: String): String {
        return "$groupId|$mapId"
    }

    private fun getDisplayMb(amount: Long): String = "${amount / 1000000} M"
}