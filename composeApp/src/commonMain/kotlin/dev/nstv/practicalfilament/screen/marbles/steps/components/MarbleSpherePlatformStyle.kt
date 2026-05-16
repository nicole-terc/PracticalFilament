package dev.nstv.practicalfilament.screen.marbles.steps.components

import androidx.compose.ui.graphics.Color
import dev.nstv.practicalfilament.filament.FilamentClipShape
import dev.nstv.practicalfilament.filament.FilamentColor

internal expect fun marbleSphereUseComposeClip(): Boolean

internal expect fun marbleSphereComposeBackgroundColor(): Color?

internal expect fun marbleSphereNativeClipShape(): FilamentClipShape?

internal expect fun marbleSphereBackgroundColor(): FilamentColor

internal expect fun marbleSphereIsOpaque(): Boolean
