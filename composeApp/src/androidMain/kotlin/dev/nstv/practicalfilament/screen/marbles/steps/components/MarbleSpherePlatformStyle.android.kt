package dev.nstv.practicalfilament.screen.marbles.steps.components

import androidx.compose.ui.graphics.Color
import dev.nstv.practicalfilament.filament.FilamentClipShape
import dev.nstv.practicalfilament.filament.FilamentColor

private val TransparentFilamentBackground = FilamentColor(0f, 0f, 0f, 0f)

internal actual fun marbleSphereUseComposeClip(): Boolean = false

internal actual fun marbleSphereComposeBackgroundColor(): Color? = null

internal actual fun marbleSphereNativeClipShape(): FilamentClipShape? = FilamentClipShape.Circle

internal actual fun marbleSphereBackgroundColor(): FilamentColor = TransparentFilamentBackground

internal actual fun marbleSphereIsOpaque(): Boolean = false
