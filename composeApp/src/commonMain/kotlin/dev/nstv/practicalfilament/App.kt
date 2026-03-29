@file:OptIn(ExperimentalUnsignedTypes::class)

package dev.nstv.practicalfilament

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.nstv.practicalfilament.components.ParameterInputField
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.Color
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.LightConfig
import dev.nstv.practicalfilament.filament.LightType
import dev.nstv.practicalfilament.filament.MaterialParameter
import dev.nstv.practicalfilament.filament.MaterialParameterDefinition
import dev.nstv.practicalfilament.filament.defaultMaterialParameter
import dev.nstv.practicalfilament.screen.MaterialViewerScreen
import practicalfilament.composeapp.generated.resources.Res

@Composable
@Preview
fun App() {
    MaterialTheme {
        MaterialViewerScreen()
    }
}
