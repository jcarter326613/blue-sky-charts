package com.blueskycharts.app.map.resources

import android.content.Context
import android.graphics.Canvas
import com.blueskycharts.app.coordinates.BoxGeo
import com.blueskycharts.app.coordinates.PointGeo
import com.blueskycharts.app.map.view.OverlayTypes
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.Exception
import java.lang.Math.pow
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.pow

class DataProvider(val context: Context) : CachedProvider(700) {
    private val areaBucketMultiplier = 2
    private val usedBuckets: HashMap<OverlayTypes, HashMap<String, LinkedList<BoxGeo>>> = HashMap()  //<overlay_type, <resolution as string, box geos>>

    init {
        for ( type in OverlayTypes.values() ) this.usedBuckets[type] = HashMap()
    }

    fun retrieveTile(area: BoxGeo, resolution: PointGeo, type: OverlayTypes, receiver: DataReceiver, canvas: Canvas) {
        val resolutionBucket = this.createResolutionBucket(resolution)
        val areaBucket = this.createAreaBucket(area, resolutionBucket, type)
        val key = this.getCacheKey(areaBucket, resolutionBucket, type)
        val tileRequest = this.getCachedItem(key)

        if ( tileRequest != null && tileRequest.loaded ) {
            val tileRequestScoped = tileRequest as DataRequest
            tileRequestScoped.receiver = receiver
            tileRequestScoped.broadcastData(true, canvas)
        } else {
            incrementAwaitingQueueAddition()
            GlobalScope.launch {
                try {
                    val newTileRequest = this@DataProvider.getExistingRequest(key)
                    if (newTileRequest != null && !newTileRequest.inError) {
                        val tileRequestScoped = newTileRequest as DataRequest
                        tileRequestScoped.receiver = receiver
                        if (tileRequestScoped.loaded) {
                            tileRequestScoped.broadcastData(false, canvas)
                        }
                    } else {
                        val newRequest = DataRequest(
                            this@DataProvider,
                            receiver,
                            areaBucket,
                            resolutionBucket,
                            type
                        )
                        this@DataProvider.addRequestToQueue(key, newRequest);
                    }
                } finally {
                    decrementAwaitingQueueAddition()
                }
            }
        }
    }

    private fun createResolutionBucket(resolution: PointGeo): PointGeo {
        var x = log2(resolution.longitude)
        x = ceil(x / 2) * 2
        var y = log2(resolution.latitude)
        y = ceil(y / 2) * 2

        return PointGeo(2.0.pow(x), 2.0.pow(y))
    }

    private fun createAreaBucket(area: BoxGeo, resolution: PointGeo, type: OverlayTypes): BoxGeo {
        // Check if the area exists in a used bucket
        val resolutionKey = this.getResolutionKey(resolution);
        val importantBucket = this.usedBuckets[type] ?: throw Exception("Invalid bucket requested in DataProvider")
        var bucketOfKeys = importantBucket[resolutionKey]
        if (bucketOfKeys == null) {
            bucketOfKeys = LinkedList()
            importantBucket[resolutionKey] = bucketOfKeys
        }

        val areaTl = area.topLeft
        val areaBr = area.bottomRight
        for ( obj in bucketOfKeys ) {
            val tl = obj.topLeft
            val br = obj.bottomRight
            if ( tl.longitude <= areaTl.longitude && tl.latitude >= areaTl.latitude &&
                br.longitude >= areaBr.longitude && br.latitude <= areaBr.latitude ) {
                return obj
            }
        }

        // We need to create a new bucket
        val areaDimensions = area.getDimensions()
        val tlLongitude = areaTl.longitude - (areaDimensions.longitude * areaBucketMultiplier)
        val tlLatitude = areaTl.latitude + (areaDimensions.latitude * areaBucketMultiplier)
        val brLongitude = areaBr.longitude + (areaDimensions.longitude * areaBucketMultiplier)
        val brLatitude = areaBr.latitude - (areaDimensions.latitude * areaBucketMultiplier)
        val bucket = BoxGeo(PointGeo(this.forceValidLongitude(tlLongitude), this.forceValidLatitude(tlLatitude)),
            PointGeo(this.forceValidLongitude(brLongitude), this.forceValidLatitude(brLatitude)));
        bucketOfKeys.push(bucket);
        return bucket;
    }

    private fun forceValidLongitude(longitude: Double): Double {
        if ( longitude < -180 ) {
            return -180.0
        }
        if ( longitude > 180 ) {
            return 180.0
        }
        return longitude
    }

    private fun forceValidLatitude(latitude: Double): Double {
        if ( latitude < -90 ) {
            return -90.0
        }
        if ( latitude > 90 ) {
            return 90.0
        }
        return latitude
    }

    private fun getResolutionKey(resolution: PointGeo): String {
        return "${resolution.longitude}|${resolution.latitude}"
    }

    private fun getCacheKey(area: BoxGeo, resolution: PointGeo, type: OverlayTypes): String {
        val tl = area.topLeft
        val br = area.bottomRight
        return "${type}|${tl.longitude}|${tl.latitude}|${br.longitude}|${br.latitude}|${resolution.longitude}|${resolution.latitude}"
    }
}