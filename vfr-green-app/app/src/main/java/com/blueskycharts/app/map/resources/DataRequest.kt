package com.blueskycharts.app.map.resources

import android.graphics.Canvas
import com.blueskycharts.app.assests.*
import com.blueskycharts.app.coordinates.BoxGeo
import com.blueskycharts.app.coordinates.PointGeo
import com.blueskycharts.app.map.models.WeatherConditionResponse
import com.blueskycharts.app.map.view.OverlayTypes
import com.blueskycharts.app.utility.Log
import java.net.URL
import kotlin.math.ceil

class DataRequest(private val provider: DataProvider, private var receiver: DataReceiver?, val area: BoxGeo,
                  val resolution: PointGeo, val type: OverlayTypes, private var receiverData: Any?) :
    CachedProviderRequest(provider, 1) {

    private var data: WeatherConditionResponse? = null
    private var timeReceived: Long = 0
    private var oldestDataAgeAtRetrievalSeconds: Int? = null
    private val maxAgeMillisecondsBeforeExpiration = 5 * 60 * 1000  /* 5 minute */
    private val maxIssueAgeSeconds = 120 * 60

    override val expired: Boolean
        get() {
            return if (this.loaded) {
                val now = System.currentTimeMillis()
                now - this.timeReceived > maxAgeMillisecondsBeforeExpiration;
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
        val url = "$urlBase?startLongitude=${tl.longitude}&endLongitude=${br.longitude}&startLatitude=${br.latitude}&endLatitude=${tl.latitude}&bufferLongitude=${this.resolution.longitude}&bufferLatitude=${this.resolution.latitude}&information=${DataRequest.getInformationForType(this.type)}"

        val provider = AssetProvider()

        val description = RemoteAssetDescription(URL(url), dataCacheVolatility, StorageLocation.Internal, true)
        description.forceExpireDiskCache = true
        provider.retrieveAsset(description) {
            try {
                if ( !it.errorLoading ) {
                    val conditionResponse: WeatherConditionResponse? = it.asJsonReader()?.let{ reader -> WeatherConditionResponse.readFromJsonReader(reader) }
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
        val data = this.data ?: return
        val dataConditions = data.conditions ?: return

        val secondsSinceRequest = ceil((System.currentTimeMillis() - this.timeReceived) / 1000.0).toInt()
        for ( condition in dataConditions ) {
            val longitude = condition.longitude ?: continue
            val latitude = condition.latitude ?: continue
            val issueAgeSeconds = condition.issueAgeSeconds ?: continue
            if (issueAgeSeconds > maxIssueAgeSeconds) {
                continue
            }

            val geoLocation = PointGeo(longitude.toDouble(), latitude.toDouble())
            receiver?.receiveData(geoLocation, condition, issueAgeSeconds + secondsSinceRequest, immediate, canvas, this.receiverData)
            //this.receiver = null
            //this.receiverData = null
        }
    }

    companion object {
        private val dataCacheVolatility = Volatility.HourCache
        private val urlBase = "https://api.blueskycharts.com/condition-v2/getConditions"
        val endpointDescriptor: RemoteAssetDescription
            get() {
                return RemoteAssetDescription(URL(urlBase), dataCacheVolatility, StorageLocation.Internal, requiresCors = true, isFolder = true)
            }

        fun createFromFileDescriptor(descriptor: DiskAssetDescription, provider: DataProvider): DataRequest? {
            val area: BoxGeo
            val resolution: PointGeo
            val type: OverlayTypes

            // Extract the attributes from the file path
            val localPath = descriptor.localPath
            val startLongitudeIndex = localPath.indexOf("startLongitude")
            if ( startLongitudeIndex < 0 ) {
                return null
            }
            val queryPortion = localPath.substring(startLongitudeIndex)
            val keyValueCombined = queryPortion.split('&')
            if (keyValueCombined.size != 7) {
                return null
            }

            // Extract the values from the attributes
            if (!keyValueCombined[0].startsWith("startLongitude=") ||
                !keyValueCombined[1].startsWith("endLongitude=") ||
                !keyValueCombined[2].startsWith("startLatitude=")||
                !keyValueCombined[3].startsWith("endLatitude=")||
                !keyValueCombined[4].startsWith("bufferLongitude=")||
                !keyValueCombined[5].startsWith("bufferLatitude=")||
                !keyValueCombined[6].startsWith("information=")
            ) {
                return null
            }

            try {
                area = BoxGeo(
                    PointGeo(
                        longitude = keyValueCombined[0].split("=")[1].toDouble(),
                        latitude = keyValueCombined[3].split("=")[1].toDouble()
                    ),
                    PointGeo(
                        longitude = keyValueCombined[1].split("=")[1].toDouble(),
                        latitude = keyValueCombined[2].split("=")[1].toDouble()
                    )
                )
                resolution = PointGeo(
                    longitude = keyValueCombined[4].split("=")[1].toDouble(),
                    latitude = keyValueCombined[5].split("=")[1].toDouble()
                )
                type = getTypeForInformation(keyValueCombined[6].split("=")[1])
                var dr = DataRequest(provider, null, area, resolution, type, null)

                // Populate the data with the contents of the file
                dr.timeReceived = descriptor.modDate

                val diskAsset = Asset(descriptor)
                DiskCacheFactory.instance.retrieveAssetBytes(diskAsset)
                val conditionResponse: WeatherConditionResponse? = diskAsset.asJsonReader()?.let{ reader -> WeatherConditionResponse.readFromJsonReader(reader) }
                if (conditionResponse != null) {
                    dr.data = conditionResponse
                    dr.oldestDataAgeAtRetrievalSeconds = conditionResponse.oldestDataAgeSeconds
                    dr.loaded = true
                    return dr
                }
                return null
            } catch (e: Throwable) {
                return null
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
                OverlayTypes.Gust -> "gust"
                else -> {
                    Log.error(null, "Invalid overlay type")
                    ""
                }
            }
        }

        private fun getTypeForInformation(information: String): OverlayTypes {
            return when( information ) {
                "flightCategory" -> OverlayTypes.Category
                "ceiling" -> OverlayTypes.Ceiling
                "cloudCover" -> OverlayTypes.CloudCover
                "dewpointSpreadCelcius" -> OverlayTypes.DewPointSpread
                "temperatureCelcius" -> OverlayTypes.Temperature
                "visibility" -> OverlayTypes.Visibility
                "wind" -> OverlayTypes.SurfaceWind
                "gust" -> OverlayTypes.Gust
                else -> {
                    throw Error("Bad overlay type name")
                }
            }
        }
    }
}