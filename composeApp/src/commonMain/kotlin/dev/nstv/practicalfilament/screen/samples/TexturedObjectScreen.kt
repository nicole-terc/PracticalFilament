// Migrated sample from https://github.com/google/filament/tree/main/android/samples/sample-textured-object
package dev.nstv.practicalfilament.screen.samples

import androidx.compose.foundation.layout.fillMaxSize
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
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.FilamentColor
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.LightConfig
import dev.nstv.practicalfilament.filament.LightType
import dev.nstv.practicalfilament.filament.material.MaterialParameterType
import dev.nstv.practicalfilament.theme.components.SampleNotice
import dev.nstv.practicalfilament.theme.components.SampleScreenLayout
import practicalfilament.composeapp.generated.resources.Res

private const val TexturedObjectTexturePath = "files/textures/checker.png"
private const val TexturedObjectMaterialPath = "files/materials/texturedSample.filamat"

@Composable
fun TexturedObjectScreen(
    modifier: Modifier = Modifier,
) {
    var engine by remember { mutableStateOf<FilamentEngine?>(null) }
    var renderableHandle by remember { mutableIntStateOf(0) }
    var notice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(engine, renderableHandle) {
        val currentEngine = engine ?: return@LaunchedEffect
        if (renderableHandle == 0) return@LaunchedEffect
        while (true) {
            val time = withFrameNanos { it } / 1_000_000_000f
            currentEngine.setRenderableRotation(renderableHandle, 0f, time * 24f)
        }
    }

    SampleScreenLayout(
        modifier = modifier,
        title = "Textured Object",
        view = {
            FilamentView(
                modifier = Modifier.fillMaxSize(),
                camera = CameraConfig(
                    position = Float3(0f, 0f, 4f),
                    lookAt = Float3(0f, 0f, 0f),
                    fovDegrees = 26.0,
                ),
                lights = listOf(
                    LightConfig(
                        type = LightType.SUN,
                        intensity = 60_000f,
                        direction = Float3(0.3f, -1f, -0.4f),
                    ),
                ),
                backgroundColor = FilamentColor(0.06f, 0.05f, 0.04f, 1f),
                onEngineReady = { readyEngine ->
                    engine = readyEngine
                    val materialHandle =
                        readyEngine.loadMaterial(Res.getUri(TexturedObjectMaterialPath))
                    val definitions = readyEngine.getMaterialParameters(materialHandle)
                    val instanceHandle = readyEngine.createMaterialInstance(materialHandle)
                    readyEngine.applyImageMaterialDefaults(
                        instanceHandle = instanceHandle,
                        definitions = definitions,
                        backgroundColor = Float3(0.06f, 0.05f, 0.04f),
                    )
                    val samplerName = definitions.firstOrNull {
                        it.type is MaterialParameterType.Sampler2d ||
                                it.type is MaterialParameterType.SamplerExternal
                    }?.name
                    if (samplerName == null) {
                        notice = "No sampler parameter was found in texturedSample.filamat."
                        return@FilamentView
                    }
                    val textureHandle =
                        readyEngine.loadTexture(Res.getUri(TexturedObjectTexturePath))
                    if (textureHandle <= 0) {
                        notice = "Texture loading failed on this platform."
                        return@FilamentView
                    }
                    readyEngine.setTextureParameter(instanceHandle, samplerName, textureHandle)
                    renderableHandle = readyEngine.createPlaneRenderable(
                        instanceHandle,
                        width = 2.4f,
                        height = 2.4f
                    )
                },
            )
        },
        controls = {
            notice?.let { SampleNotice(it) }
            Text("Texture path: $TexturedObjectTexturePath")
        },
    )
}
