// Migrated sample from https://github.com/google/filament/tree/main/android/samples/sample-material-builder
package dev.nstv.practicalfilament.screen.samples

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import dev.nstv.practicalfilament.theme.components.SampleNotice
import dev.nstv.practicalfilament.theme.components.SampleScreenLayout
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.FilamentColor
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.LightConfig
import dev.nstv.practicalfilament.filament.LightType
import dev.nstv.practicalfilament.theme.Grid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    var environmentLoaded by remember { mutableStateOf(false) }
    var loadingMessage by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(engine, compileVersion) {
        val currentEngine = engine ?: return@LaunchedEffect
        if (!currentEngine.supportsMaterialBuilder) {
            status = "Runtime material compilation is not supported on this platform."
            return@LaunchedEffect
        }
        loadingMessage = "Compiling material..."
        status = null
        withFrameNanos { }
        val materialPackage = withContext(Dispatchers.Default) {
            currentEngine.compileMaterialPackage(shaderSource, shadingModel = "lit")
        }
        if (materialPackage == null) {
            loadingMessage = null
            status = "Compilation failed. Filamat returned an invalid material package."
            return@LaunchedEffect
        }
        val materialHandle = currentEngine.createMaterialFromPackage(materialPackage)
        loadingMessage = null
        if (materialHandle <= 0) {
            status = "Material creation failed after compilation."
            return@LaunchedEffect
        }
        val instanceHandle = currentEngine.createMaterialInstance(materialHandle)
        if (renderableHandle > 0) {
            currentEngine.removeRenderable(renderableHandle)
        }
        renderableHandle = currentEngine.createSphereRenderable(instanceHandle, radius = 1f)
        environmentLoaded = false
        status = "Compiled successfully. Loading environment..."
    }

    LaunchedEffect(engine, renderableHandle, environmentLoaded) {
        val currentEngine = engine ?: return@LaunchedEffect
        if (renderableHandle <= 0 || environmentLoaded) return@LaunchedEffect

        withFrameNanos { }
        val indirectLightHandle = currentEngine.loadIndirectLight(Res.getUri(BuilderIndirectLightPath))
        withFrameNanos { }
        val skyboxHandle = currentEngine.loadSkybox(Res.getUri(BuilderSkyboxPath))
        if (indirectLightHandle > 0) {
            currentEngine.setIndirectLight(indirectLightHandle, intensity = 45_000f)
        }
        if (skyboxHandle > 0) {
            currentEngine.setSkybox(skyboxHandle)
        }
        environmentLoaded = true
        currentEngine.requestFrame()
        status = "Compiled successfully."
    }

    SampleScreenLayout(
        modifier = modifier,
        title = "Material Builder",
        view = {
            Box(modifier = Modifier.fillMaxSize()) {
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
                    backgroundColor = FilamentColor(0.05f, 0.05f, 0.06f, 1f),
                    onEngineReady = { readyEngine ->
                        engine = readyEngine
                        compileVersion += 1
                    },
                )
                loadingMessage?.let { message ->
                    MaterialBuilderLoadingState(
                        modifier = Modifier.align(Alignment.Center),
                        message = message,
                    )
                }
            }
        },
        controls = {
            status?.let { SampleNotice(it) }
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = shaderSource,
                onValueChange = { shaderSource = it },
                minLines = 12,
                label = { Text("Material Source") },
            )
            Button(
                enabled = loadingMessage == null,
                onClick = { compileVersion += 1 },
            ) {
                Text(if (loadingMessage == null) "Recompile" else "Working...")
            }
        },
    )
}

@Composable
private fun MaterialBuilderLoadingState(
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(Grid.Two),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            modifier = Modifier.padding(top = Grid.One),
            text = message,
        )
    }
}
