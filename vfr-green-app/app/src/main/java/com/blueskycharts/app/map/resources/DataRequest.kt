package com.blueskycharts.app.map.resources

import android.graphics.Canvas
import android.util.Log
import com.blueskycharts.app.assests.AssetProvider
import com.blueskycharts.app.coordinates.BoxGeo
import com.blueskycharts.app.coordinates.CoordinateConversion
import com.blueskycharts.app.coordinates.PointGeo
import com.blueskycharts.app.map.models.WeatherConditionResponse
import com.blueskycharts.app.map.view.OverlayTypes
import com.blueskycharts.app.assests.RemoteAssetDescription
import com.blueskycharts.app.assests.Volatility
import java.net.URL
import kotlin.math.ceil

class DataRequest(private val provider: DataProvider, private var receiver: DataReceiver?, private val area: BoxGeo,
                  private val resolution: PointGeo, private val type: OverlayTypes, private var receiverData: Any?) :
    CachedProviderRequest(provider, 1) {

    private var data: WeatherConditionResponse? = null
    private var timeReceived: Long = 0
    private var oldestDataAgeAtRetrievalSeconds: Int? = null
    private val urlBase = "https://api.blueskycharts.com/condition-v2/getConditions"
    private val maxAgeMilliseconds = 5 * 60 * 1000

    val expired: Boolean        //TODO: Make sure expired is respected by the cache
        get() {
            return if (this.loaded) {
                val now = System.currentTimeMillis()
                now - this.timeReceived > maxAgeMilliseconds;
            } else {
                false;
            }
        }

    fun setReceiver(receiver: DataReceiver?, data: Any?) {
        this.receiver = receiver
        this.receiverData = data
    }

    override fun sendRequest() {
        val tl = this.area.topLeft
        val br = this.area.bottomRight
        val url = "$urlBase?startLongitude=${tl.longitude}&endLongitude=${br.longitude}&startLatitude=${br.latitude}&endLatitude=${tl.latitude}&bufferLongitude=${this.resolution.longitude}&bufferLatitude=${this.resolution.latitude}&information=${this.getInformationForType(this.type)}"

        val provider = AssetProvider()

        provider.retrieveAsset(RemoteAssetDescription(URL(url), Volatility.NeverCache, true)) {
            try {
                if ( !it.errorLoading ) {
                    val conditionResponse = it.asJsonObject<WeatherConditionResponse>()
                    this@DataRequest.data = conditionResponse
                    this@DataRequest.timeReceived = System.currentTimeMillis()
                    if (conditionResponse != null) {
                        this@DataRequest.oldestDataAgeAtRetrievalSeconds = conditionResponse.oldestDataAgeSeconds
                        this@DataRequest.completeRequest(true);
                    } else {
                        this@DataRequest.completeRequest(false);
                    }
                } else {
                    this@DataRequest.completeRequest(false);
                }
            } catch (e: Throwable) {
                this@DataRequest.completeRequest(false);
            }
        }
    }

    override fun broadcastData(immediate: Boolean, canvas: Canvas?) {
        val oldestDataAgeAtRetrievalSeconds = this.oldestDataAgeAtRetrievalSeconds
        val data = this.data
        val dataConditions = data?.conditions
        val receiver = this.receiver
        if ( data == null || oldestDataAgeAtRetrievalSeconds == null || dataConditions == null || receiver == null ) return

        val secondsSinceRequest = ceil((System.currentTimeMillis() - this.timeReceived) / 1000.0).toInt()
        val dataAgeSeconds =  oldestDataAgeAtRetrievalSeconds + secondsSinceRequest
        for ( condition in dataConditions ) {
            val longitude = condition.longitude
            val latitude = condition.latitude
            if (longitude == null || latitude == null) continue

            val geoLocation = PointGeo(longitude.toDouble(), latitude.toDouble())
            val mercatorLocation = CoordinateConversion.convertPointGeoToPointWebMercator(geoLocation)

            receiver.receiveData(mercatorLocation, condition, dataAgeSeconds, immediate, canvas, this.receiverData)
        }
    }

    private fun getInformationForType(type: OverlayTypes): String {
        return when( type ) {
            OverlayTypes.Category -> "flightCategory"
            OverlayTypes.Ceiling -> "ceiling"
            OverlayTypes.CloudCover -> "cloudCover"
            OverlayTypes.DewPointSpread -> "dewpointSpreadCelcius"
            OverlayTypes.Temperature -> "temperatureCelcius"
            OverlayTypes.Visibility -> "visibility"
            OverlayTypes.SurfaceWind -> "wind"
            else -> {
                Log.e(null, "Invalid overlay type")
                ""
            }
        }
    }
}