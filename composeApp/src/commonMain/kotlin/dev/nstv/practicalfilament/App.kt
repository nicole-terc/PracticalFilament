package dev.nstv.practicalfilament

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.Color
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.Float4
import dev.nstv.practicalfilament.filament.LightConfig
import dev.nstv.practicalfilament.filament.LightType
import dev.nstv.practicalfilament.filament.MaterialParameter
import practicalfilament.composeapp.generated.resources.Res

@Composable
@Preview
fun App() {
    MaterialTheme {
        var alpha by remember { mutableFloatStateOf(1f) }
        var baseColorR by remember { mutableFloatStateOf(0.8f) }
        var baseColorG by remember { mutableFloatStateOf(0.2f) }
        var baseColorB by remember { mutableFloatStateOf(0.2f) }
        var roughness by remember { mutableFloatStateOf(0.4f) }
        var metallic by remember { mutableFloatStateOf(0.0f) }
        var reflectance by remember { mutableFloatStateOf(0.5f) }
        var sheenColorR by remember { mutableFloatStateOf(0f) }
        var sheenColorG by remember { mutableFloatStateOf(0f) }
        var sheenColorB by remember { mutableFloatStateOf(0f) }
        var sheenRoughness by remember { mutableFloatStateOf(0f) }
        var clearCoat by remember { mutableFloatStateOf(0f) }
        var clearCoatRoughness by remember { mutableFloatStateOf(0f) }
        var anisotropy by remember { mutableFloatStateOf(0f) }
        var emissiveR by remember { mutableFloatStateOf(0f) }
        var emissiveG by remember { mutableFloatStateOf(0f) }
        var emissiveB by remember { mutableFloatStateOf(0f) }
        var emissiveA by remember { mutableFloatStateOf(0f) }
        var filamentEngine by remember { mutableStateOf<dev.nstv.practicalfilament.filament.FilamentEngine?>(null) }
        var materialInstanceHandle by remember { mutableIntStateOf(0) }

        LaunchedEffect(
            filamentEngine,
            materialInstanceHandle,
            alpha,
            baseColorR,
            baseColorG,
            baseColorB,
            roughness,
            metallic,
            reflectance,
            sheenColorR,
            sheenColorG,
            sheenColorB,
            sheenRoughness,
            clearCoat,
            clearCoatRoughness,
            anisotropy,
            emissiveR,
            emissiveG,
            emissiveB,
            emissiveA,
        ) {
            val currentEngine = filamentEngine ?: return@LaunchedEffect
            if (materialInstanceHandle == 0) return@LaunchedEffect

            currentEngine.setMaterialParameter(materialInstanceHandle, MaterialParameter("alpha", alpha))
            currentEngine.setMaterialParameter(
                materialInstanceHandle,
                MaterialParameter("baseColor", Float3(baseColorR, baseColorG, baseColorB))
            )
            currentEngine.setMaterialParameter(materialInstanceHandle, MaterialParameter("roughness", roughness))
            currentEngine.setMaterialParameter(materialInstanceHandle, MaterialParameter("metallic", metallic))
            currentEngine.setMaterialParameter(materialInstanceHandle, MaterialParameter("reflectance", reflectance))
            currentEngine.setMaterialParameter(
                materialInstanceHandle,
                MaterialParameter("sheenColor", Float3(sheenColorR, sheenColorG, sheenColorB))
            )
            currentEngine.setMaterialParameter(materialInstanceHandle, MaterialParameter("sheenRoughness", sheenRoughness))
            currentEngine.setMaterialParameter(materialInstanceHandle, MaterialParameter("clearCoat", clearCoat))
            currentEngine.setMaterialParameter(
                materialInstanceHandle,
                MaterialParameter("clearCoatRoughness", clearCoatRoughness)
            )
            currentEngine.setMaterialParameter(materialInstanceHandle, MaterialParameter("anisotropy", anisotropy))
            currentEngine.setMaterialParameter(
                materialInstanceHandle,
                MaterialParameter("emissive", Float4(emissiveR, emissiveG, emissiveB, emissiveA))
            )
            currentEngine.requestFrame()
        }

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
                        val materialHandle = engine.loadMaterial(Res.getUri("files/sandboxLitFade.filamat"))
                        val instanceHandle = engine.createMaterialInstance(materialHandle)
                        filamentEngine = engine
                        materialInstanceHandle = instanceHandle
                        engine.createPlaneRenderable(instanceHandle)
                    },
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                LabeledSlider("Alpha", alpha) { alpha = it }
                LabeledSlider("Base Color R", baseColorR) { baseColorR = it }
                LabeledSlider("Base Color G", baseColorG) { baseColorG = it }
                LabeledSlider("Base Color B", baseColorB) { baseColorB = it }
                LabeledSlider("Roughness", roughness) { roughness = it }
                LabeledSlider("Metallic", metallic) { metallic = it }
                LabeledSlider("Reflectance", reflectance) { reflectance = it }
                LabeledSlider("Sheen Color R", sheenColorR) { sheenColorR = it }
                LabeledSlider("Sheen Color G", sheenColorG) { sheenColorG = it }
                LabeledSlider("Sheen Color B", sheenColorB) { sheenColorB = it }
                LabeledSlider("Sheen Roughness", sheenRoughness) { sheenRoughness = it }
                LabeledSlider("Clear Coat", clearCoat) { clearCoat = it }
                LabeledSlider("Clear Coat Roughness", clearCoatRoughness) { clearCoatRoughness = it }
                LabeledSlider("Anisotropy", anisotropy) { anisotropy = it }
                LabeledSlider("Emissive R", emissiveR) { emissiveR = it }
                LabeledSlider("Emissive G", emissiveG) { emissiveG = it }
                LabeledSlider("Emissive B", emissiveB) { emissiveB = it }
                LabeledSlider("Emissive A", emissiveA) { emissiveA = it }
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
