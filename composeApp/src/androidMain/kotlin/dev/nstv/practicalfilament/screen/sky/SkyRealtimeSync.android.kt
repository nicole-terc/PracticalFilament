package dev.nstv.practicalfilament.screen.sky

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import dev.nstv.practicalfilament.screen.wallpaper.LiveWallpaperPreferences
import dev.nstv.practicalfilament.theme.components.SampleNotice
import java.util.Calendar

actual fun platformCurrentTimeMillis(): Long = System.currentTimeMillis()

actual fun platformCurrentLocalTimeHours(currentTimeMillis: Long): Float {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = currentTimeMillis
    }
    return calendar.get(Calendar.HOUR_OF_DAY) +
        calendar.get(Calendar.MINUTE) / 60f +
        calendar.get(Calendar.SECOND) / 3600f
}

@Composable
actual fun SkyLocationSyncEffect(
    enabled: Boolean,
    onLocationUpdated: (latitude: Float, longitude: Float) -> Unit,
    onStatusChanged: (String?) -> Unit,
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.values.any { it }) {
            fetchCurrentLocation(context, onLocationUpdated, onStatusChanged)
        } else {
            onStatusChanged("Location permission was denied.")
        }
    }

    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        if (hasLocationPermission(context)) {
            fetchCurrentLocation(context, onLocationUpdated, onStatusChanged)
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ),
            )
        }
    }
}

@Composable
actual fun ConfiguredSkyRealtimeStatus(
    modifier: Modifier,
) {
    val context = LocalContext.current
    val config = LiveWallpaperPreferences.loadSkyConfig(context)
    val effective = resolveRealtimeSkyConfig(config, System.currentTimeMillis())
    when {
        config.syncEnabled && config.syncManualOverride -> SampleNotice(
            "Location/Time: manual override, " +
                    "lat ${formatRealtimeValue(config.syncLatitude)}, lon ${
                        formatRealtimeValue(
                            config.syncLongitude
                        )
                    }, " +
                    "time ${formatRealtimeValue(config.manualTimeHours)} h."
        )
        config.syncEnabled && config.syncDeviceLocation -> SampleNotice(
            "Location/Time: device location and current time, " +
                    "lat ${formatRealtimeValue(config.syncLatitude)}, lon ${
                        formatRealtimeValue(
                            config.syncLongitude
                        )
                    }."
        )
        config.syncEnabled -> SampleNotice(
            "Location/Time: current time with manual coordinates, " +
                    "lat ${formatRealtimeValue(config.syncLatitude)}, lon ${
                        formatRealtimeValue(
                            config.syncLongitude
                        )
                    }."
        )
        else -> SampleNotice("Location/Time: using manual sun and Milky Way values.")
    }
    if (effective != config) {
        SampleNotice(
            "Live sun ${formatRealtimeValue(effective.sunAzimuth)} deg, " +
                    "height ${formatRealtimeValue(effective.sunHeight)}, " +
                    "sidereal ${formatRealtimeValue(effective.milkyWaySiderealTime)} h."
        )
    }
}

private fun fetchCurrentLocation(
    context: Context,
    onLocationUpdated: (latitude: Float, longitude: Float) -> Unit,
    onStatusChanged: (String?) -> Unit,
) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    if (locationManager == null) {
        onStatusChanged("Location services are unavailable on this device.")
        return
    }

    val provider = chooseLocationProvider(locationManager)
    if (provider == null) {
        fallbackLastKnownLocation(locationManager, onLocationUpdated, onStatusChanged)
        return
    }

    val currentBest = bestLastKnownLocation(locationManager)
    if (currentBest != null) {
        onLocationUpdated(currentBest.latitude.toFloat(), currentBest.longitude.toFloat())
        onStatusChanged("Using the last known device location.")
        return
    }

    onStatusChanged("No device location is available yet from $provider.")
}

private fun fallbackLastKnownLocation(
    locationManager: LocationManager,
    onLocationUpdated: (latitude: Float, longitude: Float) -> Unit,
    onStatusChanged: (String?) -> Unit,
) {
    val location = bestLastKnownLocation(locationManager)

    if (location != null) {
        onLocationUpdated(location.latitude.toFloat(), location.longitude.toFloat())
        onStatusChanged("Using the last known device location.")
    } else {
        onStatusChanged("No device location is available yet.")
    }
}

private fun bestLastKnownLocation(locationManager: LocationManager): Location? {
    return locationManager
        .getProviders(true)
        .asSequence()
        .mapNotNull { provider ->
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }
        .maxByOrNull(Location::getTime)
}

private fun chooseLocationProvider(locationManager: LocationManager): String? {
    val enabledProviders = locationManager.getProviders(true)
    return when {
        enabledProviders.contains(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        enabledProviders.contains(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        enabledProviders.contains(LocationManager.PASSIVE_PROVIDER) -> LocationManager.PASSIVE_PROVIDER
        else -> null
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
}

private fun formatRealtimeValue(value: Float): String = if (value == value.toInt().toFloat()) {
    value.toInt().toString()
} else {
    value.toString().take(6)
}
