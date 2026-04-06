package dev.nstv.practicalfilament.screen.sky

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitSecond
import platform.Foundation.NSDate
import platform.posix.time

@OptIn(ExperimentalForeignApi::class)
actual fun platformCurrentTimeMillis(): Long = time(null).toLong() * 1000L

actual fun platformCurrentLocalTimeHours(currentTimeMillis: Long): Float {
    val date = NSDate(
        timeIntervalSinceReferenceDate = currentTimeMillis.toDouble() / 1000.0 - AppleReferenceDateOffsetSeconds,
    )
    val components = NSCalendar.currentCalendar.components(
        unitFlags = NSCalendarUnitHour or NSCalendarUnitMinute or NSCalendarUnitSecond,
        fromDate = date,
    )
    return components.hour.toFloat() +
        components.minute.toFloat() / 60f +
        components.second.toFloat() / 3600f
}

@Composable
actual fun SkyLocationSyncEffect(
    enabled: Boolean,
    onLocationUpdated: (latitude: Float, longitude: Float) -> Unit,
    onStatusChanged: (String?) -> Unit,
) {
    LaunchedEffect(enabled) {
        onStatusChanged(
            if (enabled) "Device location sync is not implemented on iOS." else null,
        )
    }
}

@Composable
actual fun ConfiguredSkyRealtimeStatus(
    modifier: Modifier,
) {
}

private const val AppleReferenceDateOffsetSeconds = 978_307_200.0
