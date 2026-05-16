package dev.nstv.practicalfilament.screen.marbles.steps.components

import androidx.compose.ui.graphics.Color
import dev.nstv.practicalfilament.filament.FilamentClipShape
import dev.nstv.practicalfilament.filament.FilamentColor
import dev.nstv.practicalfilament.screen.marbles.components.MarbleUiBackground

private val StepMarbleBackground = FilamentColor(
    0x10 / 255f,
    0x17 / 255f,
    0x21 / 255f,
    1f,
)

internal actual fun marbleSphereUseComposeClip(): Boolean = true

internal actual fun marbleSphereComposeBackgroundColor(): Color? = MarbleUiBackground

internal actual fun marbleSphereNativeClipShape(): FilamentClipShape? = null

internal actual fun marbleSphereBackgroundColor(): FilamentColor = StepMarbleBackground

internal actual fun marbleSphereIsOpaque(): Boolean = true
