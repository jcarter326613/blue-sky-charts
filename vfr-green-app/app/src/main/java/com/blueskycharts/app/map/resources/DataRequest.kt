package com.blueskycharts.app.map.resources

import android.graphics.Canvas
import android.util.Log
import com.blueskycharts.app.coordinates.BoxGeo
import com.blueskycharts.app.coordinates.CoordinateConversion
import com.blueskycharts.app.coordinates.PointGeo
import com.blueskycharts.app.map.models.WeatherCondition
import com.blueskycharts.app.map.models.WeatherConditionResponse
import com.blueskycharts.app.map.view.OverlayTypes
import com.blueskycharts.app.remoteassests.AssetProvider
import com.blueskycharts.app.remoteassests.Volatility
import java.net.URL
import kotlin.math.ceil

class DataRequest(private val provider: DataProvider, var receiver: DataReceiver?, private val area: BoxGeo, private val resolution: PointGeo, private val type: OverlayTypes) :
    CachedProviderRequest(provider, 1) {

    private var data: WeatherConditionResponse? = null
    private var timeReceived: Int = 0
    private var oldestDataAgeAtRetrievalSeconds: Int? = null
    private val urlBase = "https://api.blueskycharts.com/condition-v2/getConditions"
    private val maxAgeMilliseconds = 5 * 60 * 1000

    val expired: Boolean
        get() {
            return if (this.loaded) {
                val now = System.currentTimeMillis()
                now - this.timeReceived > maxAgeMilliseconds;
            } else {
                false;
            }
        }

    override fun sendRequest() {
        val tl = this.area.topLeft
        val br = this.area.bottomRight
        val url = "$urlBase?startLongitude=${tl.longitude}&endLongitude=${br.longitude}&startLatitude=${br.latitude}&endLatitude=${tl.latitude}&bufferLongitude=${this.resolution.longitude}&bufferLatitude=${this.resolution.latitude}&information=${this.getInformationForType(this.type)}"

        val provider = AssetProvider(provider.context)
        provider.retrieveAsset(URL(url), Volatility.ScheduledLifetime) {
            try {
                val conditionResponse = it.asJsonObject<WeatherConditionResponse>()
                if ( conditionResponse != null ) {
                    this@DataRequest.data = conditionResponse
                    val success = conditionResponse.oldestDataAgeSeconds != null
                    this@DataRequest.completeRequest(success);
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
            val mercatorLocation = CoordinateConversion.convertToWebMercator(geoLocation)

            receiver.receiveData(mercatorLocation, data, dataAgeSeconds, immediate)
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