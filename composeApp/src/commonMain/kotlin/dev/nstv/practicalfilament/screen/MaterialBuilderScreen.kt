// Migrated sample from https://github.com/google/filament/tree/main/android/samples/sample-material-builder
package dev.nstv.practicalfilament.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.Color
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.LightConfig
import dev.nstv.practicalfilament.filament.LightType
import practicalfilament.composeapp.generated.resources.Res

private const val BuilderIndirectLightPath = "files/envs/pillars_2k/pillars_2k_ibl.ktx"
private const val BuilderSkyboxPath = "files/envs/pillars_2k/pillars_2k_skybox.ktx"

private const val DefaultBuilderShader = """
void material(inout MaterialInputs material) {
    prepareMaterial(material);
    material.baseColor = vec4(0.18, 0.62, 0.93, 1.0);
    material.roughness = 0.18;
    material.metallic = 0.0;
    material.clearCoat = 1.0;
    material.clearCoatRoughness = 0.06;
}
"""

@Composable
fun MaterialBuilderScreen(
    modifier: Modifier = Modifier,
) {
    var engine by remember { mutableStateOf<FilamentEngine?>(null) }
    var renderableHandle by remember { mutableIntStateOf(0) }
    var shaderSource by remember { mutableStateOf(DefaultBuilderShader.trimIndent()) }
    var compileVersion by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(engine, compileVersion) {
        val currentEngine = engine ?: return@LaunchedEffect
        if (!currentEngine.supportsMaterialBuilder) {
            status = "Runtime material compilation is not supported on this platform."
            return@LaunchedEffect
        }
        val materialHandle = currentEngine.buildMaterial(shaderSource, shadingModel = "lit")
        if (materialHandle <= 0) {
            status = "Compilation failed. Filamat returned an invalid material package."
            return@LaunchedEffect
        }
        val instanceHandle = currentEngine.createMaterialInstance(materialHandle)
        if (renderableHandle > 0) {
            currentEngine.removeRenderable(renderableHandle)
        }
        renderableHandle = currentEngine.createSphereRenderable(instanceHandle, radius = 1f)
        status = "Compiled successfully."
    }

    SampleScreenLayout(
        modifier = modifier,
        title = "Material Builder",
        description = "Android uses Filamat to compile a material package at runtime. On iOS this screen stays in a not-supported state by design.",
        view = {
            FilamentView(
                modifier = Modifier.fillMaxSize(),
                camera = CameraConfig(
                    position = Float3(0f, 0.2f, 4f),
                    lookAt = Float3(0f, 0f, 0f),
                    fovDegrees = 28.0,
                ),
                lights = listOf(
                    LightConfig(
                        type = LightType.SUN,
                        intensity = 90_000f,
                        direction = Float3(0.5f, -1f, -0.7f),
                    ),
                ),
                backgroundColor = Color(0.05f, 0.05f, 0.06f, 1f),
                onEngineReady = { readyEngine ->
                    readyEngine.setIndirectLight(
                        readyEngine.loadIndirectLight(Res.getUri(BuilderIndirectLightPath)),
                        intensity = 45_000f,
                    )
                    readyEngine.setSkybox(readyEngine.loadSkybox(Res.getUri(BuilderSkyboxPath)))
                    engine = readyEngine
                    compileVersion += 1
                },
            )
        },
        controls = {
            status?.let { SampleNotice(it) }
            OutlinedTextField(
                modifier = Modifier.fillMaxSize(),
                value = shaderSource,
                onValueChange = { shaderSource = it },
                minLines = 12,
                label = { Text("Material Source") },
            )
            Button(
                onClick = { compileVersion += 1 },
            ) {
                Text("Recompile")
            }
        },
    )
}
