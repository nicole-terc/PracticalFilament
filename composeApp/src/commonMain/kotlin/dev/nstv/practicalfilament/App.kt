package dev.nstv.practicalfilament

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.Color
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.LightConfig
import dev.nstv.practicalfilament.filament.LightType
import dev.nstv.practicalfilament.filament.MaterialParameter

@Composable
@Preview
fun App() {
    MaterialTheme {
        var roughness by remember { mutableFloatStateOf(0.4f) }
        var metallic by remember { mutableFloatStateOf(0.0f) }
        var reflectance by remember { mutableFloatStateOf(0.5f) }

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                FilamentView(
                    modifier = Modifier.fillMaxSize(),
                    camera = CameraConfig(
                        position = Float3(0f, 0f, 4f),
                        lookAt = Float3(0f, 0f, 0f),
                    ),
                    lights = listOf(
                        LightConfig(
                            type = LightType.DIRECTIONAL,
                            color = Color(1f, 1f, 1f),
                            intensity = 110_000f,
                            direction = Float3(0f, -1f, -1f),
                        ),
                        LightConfig(
                            type = LightType.POINT,
                            color = Color(1f, 0.9f, 0.8f),
                            intensity = 50_000f,
                            position = Float3(2f, 2f, 2f),
                        ),
                    ),
                    onEngineReady = { engine ->
                        // Material loading would go here once .filamat files are compiled:
                        // val materialHandle = engine.loadMaterial("/path/to/lit_material.filamat")
                        // val instanceHandle = engine.createMaterialInstance(materialHandle)
                        // engine.setMaterialParameter(instanceHandle, MaterialParameter("baseColor", Float3(0.8f, 0.2f, 0.2f)))
                        // engine.setMaterialParameter(instanceHandle, MaterialParameter("metallic", metallic))
                        // engine.setMaterialParameter(instanceHandle, MaterialParameter("roughness", roughness))
                        // engine.setMaterialParameter(instanceHandle, MaterialParameter("reflectance", reflectance))
                        // engine.createPlaneRenderable(instanceHandle)
                    },
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                LabeledSlider("Roughness", roughness) { roughness = it }
                LabeledSlider("Metallic", metallic) { metallic = it }
                LabeledSlider("Reflectance", reflectance) { reflectance = it }
            }
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            text = "$label: ${((value * 100).toInt() / 100f)}",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(2f),
        )
    }
}
