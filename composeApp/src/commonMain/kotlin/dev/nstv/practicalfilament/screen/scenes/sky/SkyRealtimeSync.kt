package dev.nstv.practicalfilament.screen.scenes.sky

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

fun resolveRealtimeSkyConfig(
    config: SkyWallpaperConfig,
    currentTimeMillis: Long,
): SkyWallpaperConfig {
    var resolved = config
    val sync = resolveRealtimeSkyValues(config, currentTimeMillis)
    if (sync != null) {
        resolved = resolved.copy(
            sunAzimuth = sync.sunAzimuth,
            sunHeight = sync.sunHeight,
            moonAzimuth = sync.moonAzimuth,
            moonHeight = sync.moonHeight,
            milkyWaySiderealTime = sync.siderealTimeHours,
            milkyWayLatitude = config.syncLatitude,
        )
    }
    return resolved
}

fun resolveRealtimeSkyValues(
    config: SkyWallpaperConfig,
    currentTimeMillis: Long,
): SkyRealtimeValues? {
    return if (config.syncEnabled || config.syncManualOverride) {
        if (config.syncManualOverride) {
            computeManualSkyValues(
                currentTimeMillis = currentTimeMillis,
                localTimeHours = config.manualTimeHours,
                latitudeDegrees = config.syncLatitude,
                longitudeDegrees = config.syncLongitude,
            )
        } else {
            computeRealtimeSkyValues(
                currentTimeMillis = currentTimeMillis,
                latitudeDegrees = config.syncLatitude,
                longitudeDegrees = config.syncLongitude,
            )
        }
    } else {
        null
    }
}

fun computeManualSkyValues(
    currentTimeMillis: Long,
    localTimeHours: Float,
    latitudeDegrees: Float,
    longitudeDegrees: Float,
): SkyRealtimeValues {
    val utcHours = localTimeHours - longitudeDegrees / 15f
    val utcDayStartMillis = floor(currentTimeMillis / MillisPerDay).toLong() * MillisPerDay.toLong()
    val manualTimeMillis = utcDayStartMillis + (utcHours * MillisPerHour).toLong()
    return computeRealtimeSkyValues(
        currentTimeMillis = manualTimeMillis,
        latitudeDegrees = latitudeDegrees,
        longitudeDegrees = longitudeDegrees,
    )
}

fun computeRealtimeSkyValues(
    currentTimeMillis: Long,
    latitudeDegrees: Float,
    longitudeDegrees: Float,
): SkyRealtimeValues {
    val julianDay = currentTimeMillis / MillisPerDay + JulianUnixEpoch
    val julianCentury = (julianDay - 2_451_545.0) / 36_525.0
    val daysSinceJ2000 = julianDay - JulianJ2000

    val meanLongitude = normalizeDegrees(
        280.46646 +
            julianCentury * (36_000.76983 + julianCentury * 0.0003032),
    )
    val meanAnomaly = normalizeDegrees(
        357.52911 +
            julianCentury * (35_999.05029 - 0.0001537 * julianCentury),
    )
    val equationOfCenter =
        sinDeg(meanAnomaly) * (1.914602 - julianCentury * (0.004817 + 0.000014 * julianCentury)) +
            sinDeg(2.0 * meanAnomaly) * (0.019993 - 0.000101 * julianCentury) +
            sinDeg(3.0 * meanAnomaly) * 0.000289
    val trueLongitude = meanLongitude + equationOfCenter
    val omega = 125.04 - 1934.136 * julianCentury
    val apparentLongitude = trueLongitude - 0.00569 - 0.00478 * sinDeg(omega)
    val meanObliquity =
        23.0 +
            (26.0 + (21.448 - julianCentury * (46.815 + julianCentury * (0.00059 - julianCentury * 0.001813))) / 60.0) /
            60.0
    val obliquity = meanObliquity + 0.00256 * cosDeg(omega)

    val rightAscension = normalizeDegrees(
        radiansToDegrees(
            atan2(
                cosDeg(obliquity) * sinDeg(apparentLongitude),
                cosDeg(apparentLongitude),
            ),
        ),
    )
    val declinationRadians = asin(sinDeg(obliquity) * sinDeg(apparentLongitude))
    val latitudeRadians = degreesToRadians(latitudeDegrees.toDouble())

    val gmstDegrees = normalizeDegrees(
        280.46061837 +
            360.98564736629 * (julianDay - 2_451_545.0) +
            julianCentury * julianCentury * 0.000387933 -
            julianCentury * julianCentury * julianCentury / 38_710_000.0,
    )
    val localSiderealDegrees = normalizeDegrees(gmstDegrees + longitudeDegrees)
    val sunCoords = computeSkyCoordinates(
        rightAscensionDegrees = rightAscension,
        declinationRadians = declinationRadians,
        latitudeRadians = latitudeRadians,
        localSiderealDegrees = localSiderealDegrees,
    )

    val moonCoords = computeMoonSkyCoordinates(
        daysSinceJ2000 = daysSinceJ2000,
        latitudeRadians = latitudeRadians,
        localSiderealDegrees = localSiderealDegrees,
    )

    return SkyRealtimeValues(
        sunAzimuth = sunCoords.azimuth,
        sunHeight = sunCoords.height,
        moonAzimuth = moonCoords.azimuth,
        moonHeight = moonCoords.height,
        siderealTimeHours = (localSiderealDegrees / 15.0).toFloat(),
    )
}

private fun computeMoonSkyCoordinates(
    daysSinceJ2000: Double,
    latitudeRadians: Double,
    localSiderealDegrees: Double,
): HorizontalSkyCoordinates {
    val eclipticLongitude = degreesToRadians(218.316 + 13.176396 * daysSinceJ2000)
    val meanAnomaly = degreesToRadians(134.963 + 13.064993 * daysSinceJ2000)
    val meanDistance = degreesToRadians(93.272 + 13.229350 * daysSinceJ2000)

    val longitude = eclipticLongitude + degreesToRadians(6.289) * sin(meanAnomaly)
    val latitude = degreesToRadians(5.128) * sin(meanDistance)
    val rightAscensionRadians = rightAscension(longitude, latitude)
    val declinationRadians = declination(longitude, latitude)
    val rightAscensionDegrees = normalizeDegrees(radiansToDegrees(rightAscensionRadians))

    return computeSkyCoordinates(
        rightAscensionDegrees = rightAscensionDegrees,
        declinationRadians = declinationRadians,
        latitudeRadians = latitudeRadians,
        localSiderealDegrees = localSiderealDegrees,
        altitudeOffsetRadians = astroRefraction(
            altitudeRadians = altitude(
                hourAngleRadians = degreesToRadians(normalizeSignedDegrees(localSiderealDegrees - rightAscensionDegrees)),
                latitudeRadians = latitudeRadians,
                declinationRadians = declinationRadians,
            ),
        ),
    )
}

private fun computeSkyCoordinates(
    rightAscensionDegrees: Double,
    declinationRadians: Double,
    latitudeRadians: Double,
    localSiderealDegrees: Double,
    altitudeOffsetRadians: Double = 0.0,
): HorizontalSkyCoordinates {
    val hourAngleRadians = degreesToRadians(normalizeSignedDegrees(localSiderealDegrees - rightAscensionDegrees))

    val east = cos(declinationRadians) * sin(hourAngleRadians)
    val north =
        cos(latitudeRadians) * sin(declinationRadians) -
            sin(latitudeRadians) * cos(declinationRadians) * cos(hourAngleRadians)
    var up =
        sin(latitudeRadians) * sin(declinationRadians) +
            cos(latitudeRadians) * cos(declinationRadians) * cos(hourAngleRadians)

    if (altitudeOffsetRadians != 0.0) {
        val altitude = asin(up.coerceIn(-1.0, 1.0)) + altitudeOffsetRadians
        up = sin(altitude)
    }

    val sampleX = -north
    val sampleZ = east
    val azimuth = normalizeDegrees(radiansToDegrees(atan2(sampleZ, sampleX)))

    return HorizontalSkyCoordinates(
        azimuth = azimuth.toFloat(),
        height = up.toFloat().coerceIn(-1f, 1f),
    )
}

private fun normalizeDegrees(value: Double): Double {
    val normalized = value % 360.0
    return if (normalized < 0.0) normalized + 360.0 else normalized
}

private fun normalizeSignedDegrees(value: Double): Double {
    val normalized = normalizeDegrees(value)
    return if (normalized > 180.0) normalized - 360.0 else normalized
}

private fun degreesToRadians(value: Double): Double = value * PI / 180.0

private fun radiansToDegrees(value: Double): Double = value * 180.0 / PI

private fun rightAscension(eclipticLongitude: Double, eclipticLatitude: Double): Double {
    return atan2(
        sin(eclipticLongitude) * cos(EarthObliquityRadians) -
            tan(eclipticLatitude) * sin(EarthObliquityRadians),
        cos(eclipticLongitude),
    )
}

private fun declination(eclipticLongitude: Double, eclipticLatitude: Double): Double {
    return asin(
        sin(eclipticLatitude) * cos(EarthObliquityRadians) +
            cos(eclipticLatitude) * sin(EarthObliquityRadians) * sin(eclipticLongitude),
    )
}

private fun altitude(
    hourAngleRadians: Double,
    latitudeRadians: Double,
    declinationRadians: Double,
): Double {
    return asin(
        sin(latitudeRadians) * sin(declinationRadians) +
            cos(latitudeRadians) * cos(declinationRadians) * cos(hourAngleRadians),
    )
}

private fun astroRefraction(altitudeRadians: Double): Double {
    val clampedAltitude = if (altitudeRadians < 0.0) 0.0 else altitudeRadians
    return 0.0002967 / tan(clampedAltitude + 0.00312536 / (clampedAltitude + 0.08901179))
}

private fun sinDeg(degrees: Double): Double = sin(degrees * PI / 180.0)

private fun cosDeg(degrees: Double): Double = cos(degrees * PI / 180.0)

data class SkyRealtimeValues(
    val sunAzimuth: Float,
    val sunHeight: Float,
    val moonAzimuth: Float,
    val moonHeight: Float,
    val siderealTimeHours: Float,
)

private data class HorizontalSkyCoordinates(
    val azimuth: Float,
    val height: Float,
)

private const val MillisPerDay = 86_400_000.0

private const val MillisPerHour = 3_600_000L

private const val JulianUnixEpoch = 2_440_587.5

private const val JulianJ2000 = 2_451_545.0

private val EarthObliquityRadians = degreesToRadians(23.4397)

expect fun platformCurrentTimeMillis(): Long

expect fun platformCurrentLocalTimeHours(currentTimeMillis: Long = platformCurrentTimeMillis()): Float

@Composable
expect fun SkyLocationSyncEffect(
    enabled: Boolean,
    onLocationUpdated: (latitude: Float, longitude: Float) -> Unit,
    onStatusChanged: (String?) -> Unit,
)

@Composable
expect fun ConfiguredSkyRealtimeStatus(
    modifier: Modifier = Modifier,
)
