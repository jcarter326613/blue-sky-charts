package com.blueskycharts.app.coordinates

import com.blueskycharts.app.map.models.ExtentModel
import kotlin.math.*

class ProjectionLccDescription(
    val lat0: Double,
    val lat1: Double,
    val lat2: Double,
    val lon0: Double,
    val x0: Double,
    val y0: Double
) {
    private val a: Double
    private val ecc: Double
    private val E_F: Double
    private val N_F: Double
    private val rad_per_deg: Double
    private val phi_1_rad: Double
    private val phi_2_rad: Double
    private val phi_F_rad: Double
    private val lambda_F_rad: Double

    init {
        // Calculate some stuff     (semi-major and minor axis https://gisgeography.com/geodetic-datums-nad27-nad83-wgs84/)
        a = 6378137.0 // Semi-major axis of NAD83 based on GRS80 ellipsoid
        val b = 6356752.3 // Semi-minor axis of NAD83 based on GRS80 ellipsoid
        val f_inv = a / (a - b)
        val f = 1 / f_inv
        ecc = sqrt(f * (2 - f))

        val phi_1_deg = lat1
        val phi_2_deg = lat2
        val phi_F_deg = lat0
        val lambda_F_deg = lon0
        E_F = x0
        N_F = y0

        // Unit conversion
        rad_per_deg = PI / 180.0
        phi_1_rad = phi_1_deg * rad_per_deg
        phi_2_rad = phi_2_deg * rad_per_deg
        phi_F_rad = phi_F_deg * rad_per_deg
        lambda_F_rad = lambda_F_deg * rad_per_deg
    }

    fun createPointGeo(pointLcc: PointLcc): PointGeo {
        // Do the conversion
        val E = pointLcc.x
        val N = flipY(pointLcc.y)

        // Apply all the formulas from IOGP report
        val m_1 = calculateM(ecc, phi_1_rad)
        val m_2 = calculateM(ecc, phi_2_rad)

        val t_1 = calculateT(ecc, phi_1_rad)
        val t_2 = calculateT(ecc, phi_2_rad)
        val t_F = calculateT(ecc, phi_F_rad)

        val n = (ln(m_1) - ln(m_2))/(ln(t_1) - ln(t_2))
        var F = m_1 / (n * abs(t_1).pow(n))
        var r_F = a * F * abs(t_F).pow(n)

        if (t_1 < 0) {
            F = -F
        }
        if (t_F < 0) {
            r_F = -r_F
        }

        var r_dash = sqrt(  (E - E_F).pow(2) + (r_F - (N - N_F)).pow(2)  )
        var t_dash = abs(r_dash / (a * F)).pow(1/n)
        val theta_dash = atan2( (E - E_F), (r_F - (N - N_F)) )

        if (n < 0) {
            r_dash = -r_dash
        }
        if (r_dash/(a * F) < 0) {
            t_dash = -t_dash
        }

        // Iterative solution for the latitude
        var phi = PI / 2 - 2 * atan(t_dash)
        for (i in 0 ..100) {
            val newPhi = PI / 2 - 2 * atan(
                t_dash * ((1 - ecc * sin(phi)) / (1 + ecc * sin(phi))).pow(ecc / 2.0)
            )
            if (newPhi == phi) {
                break
            }
            phi = newPhi
        }

        // Longitude
        val lmbd = theta_dash / n + lambda_F_rad

        // Convert radians to degrees
        val lat_out  = phi  / rad_per_deg
        val lon_out = lmbd / rad_per_deg

        return PointGeo(lon_out, lat_out)
    }

    //https://github.com/vraida/Lambert-projection/blob/master/python_implementation/lib/lambert.py
    fun createPointLcc(pointGeo: PointGeo): PointLcc {
        // Convert angles to radians
        val lmbd_rad = pointGeo.longitude * rad_per_deg
        val phi_rad  = pointGeo.latitude * rad_per_deg

        // Apply all the formulas from IOGP report:
        val m_1 = calculateM(ecc, phi_1_rad)
        val m_2 = calculateM(ecc, phi_2_rad)

        val t = calculateT(ecc, phi_rad)
        val t_1 = calculateT(ecc, phi_1_rad)
        val t_2 = calculateT(ecc, phi_2_rad)
        val t_F = calculateT(ecc, phi_F_rad)

        val n = (ln(m_1) - ln(m_2)) / (ln(t_1) - ln(t_2))
        val F = m_1 / (n * t_1.pow(n))

        var r = a * F * abs(t).pow(n)
        var r_F = a * F * abs(t_F).pow(n)
        if ( t < 0 ) {
            r = -r
        }
        if ( t_F < 0 ) {
            r_F = -r_F
        }

        val theta = n * (lmbd_rad - lambda_F_rad)

        // Get the result
        val E = E_F + r * sin(theta)
        val N = N_F + r_F - r * cos(theta)

        return PointLcc(E, flipY(N), this)
    }

    private fun calculateM(ecc: Double, x: Double): Double {
        return cos(x) / sqrt(1 - ecc.pow(2) * sin(x).pow(2))
    }

    private fun calculateT(ecc: Double, x: Double): Double {
        return tan((PI / 4.0) - (x / 2.0)) / ((1 - ecc * sin(x)) / (1 + ecc * sin(x))).pow(ecc / 2)
    }

    private fun flipY(y: Double): Double {
        return y0 + (y0 - y)
    }
}