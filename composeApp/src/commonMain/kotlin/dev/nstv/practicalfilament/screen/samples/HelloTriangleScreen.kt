// Migrated sample from https://github.com/google/filament/tree/main/android/samples/sample-hello-triangle
package dev.nstv.practicalfilament.screen.samples

import androidx.compose.foundation.layout.fillMaxSize
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
import dev.nstv.practicalfilament.filament.withFrameSeconds
import androidx.compose.ui.Modifier
import dev.nstv.practicalfilament.filament.AttributeDataType
import dev.nstv.practicalfilament.filament.BoundingBox
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.CustomRenderableConfig
import dev.nstv.practicalfilament.filament.FilamentColor
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.LightConfig
import dev.nstv.practicalfilament.filament.LightType
import dev.nstv.practicalfilament.filament.PrimitiveType
import dev.nstv.practicalfilament.filament.ProjectionType
import dev.nstv.practicalfilament.filament.VertexAttribute
import dev.nstv.practicalfilament.filament.VertexAttributeLayout
import dev.nstv.practicalfilament.filament.toByteArray
import dev.nstv.practicalfilament.theme.components.SampleNotice
import dev.nstv.practicalfilament.theme.components.SampleScreenLayout

private const val TriangleMaterialSource = """
void material(inout MaterialInputs material) {
    prepareMaterial(material);
    material.baseColor = vec4(0.19, 0.80, 0.65, 1.0);
}
"""

@Composable
fun HelloTriangleScreen(
    modifier: Modifier = Modifier,
) {
    var engine by remember { mutableStateOf<FilamentEngine?>(null) }
    var renderableHandle by remember { mutableIntStateOf(0) }
    var rotationSpeed by remember { mutableFloatStateOf(45f) }
    var supportNotice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(engine, renderableHandle, rotationSpeed) {
        if (renderableHandle == 0) return@LaunchedEffect
        withFrameSeconds { elapsed, _ ->
            engine?.setRenderableRotation(renderableHandle, 0f, elapsed * rotationSpeed)
        }
    }

    SampleScreenLayout(
        modifier = modifier,
        title = "Hello Triangle",
        view = {
            FilamentView(
                modifier = Modifier.fillMaxSize(),
                camera = CameraConfig(
                    position = Float3(0f, 0f, 4f),
                    lookAt = Float3(0f, 0f, 0f),
                    projectionType = ProjectionType.ORTHO,
                    orthoZoom = 1.3,
                ),
                lights = listOf(
                    LightConfig(
                        type = LightType.DIRECTIONAL,
                        intensity = 10_000f
                    )
                ),
                backgroundColor = FilamentColor(0.01f, 0.02f, 0.03f, 1f),
                onEngineReady = { readyEngine ->
                    engine = readyEngine
                    if (!readyEngine.supportsMaterialBuilder) {
                        supportNotice =
                            "Hello Triangle currently needs runtime material compilation on this platform."
                        return@FilamentView
                    }
                    val materialHandle =
                        readyEngine.buildMaterial(TriangleMaterialSource, shadingModel = "unlit")
                    if (materialHandle <= 0) {
                        supportNotice = "Runtime material compilation failed."
                        return@FilamentView
                    }
                    val instanceHandle = readyEngine.createMaterialInstance(materialHandle)
                    renderableHandle = readyEngine.createCustomRenderable(
                        CustomRenderableConfig(
                            vertexData = floatArrayOf(
                                0f, 0.8f, 0f,
                                -0.85f, -0.65f, 0f,
                                0.85f, -0.65f, 0f,
                            ).toByteArray(),
                            vertexCount = 3,
                            strideBytes = 12,
                            attributes = listOf(
                                VertexAttributeLayout(
                                    attribute = VertexAttribute.POSITION,
                                    type = AttributeDataType.FLOAT3,
                                    offsetBytes = 0,
                                ),
                            ),
                            indices = shortArrayOf(0, 1, 2),
                            materialInstanceHandle = instanceHandle,
                            boundingBox = BoundingBox(
                                center = Float3(0f, 0f, 0f),
                                halfExtent = Float3(1f, 1f, 0.1f),
                            ),
                            primitiveType = PrimitiveType.TRIANGLES,
                        ),
                    )
                    if (renderableHandle <= 0) {
                        supportNotice = "Custom renderables are not available on this platform yet."
                    }
                },
            )
        },
        controls = {
            supportNotice?.let {
                SampleNotice(
                    it
                )
            }
            Text("Rotation Speed")
            Slider(
                value = rotationSpeed,
                valueRange = 0f..120f,
                onValueChange = { rotationSpeed = it },
            )
        },
    )
}
