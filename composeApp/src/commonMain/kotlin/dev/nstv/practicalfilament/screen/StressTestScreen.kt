// Migrated sample from https://github.com/google/filament/tree/main/android/samples/sample-material-instance-stress
package dev.nstv.practicalfilament.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.Color
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.LightConfig
import dev.nstv.practicalfilament.filament.LightType
import dev.nstv.practicalfilament.filament.material.Material
import dev.nstv.practicalfilament.filament.material.MaterialParameter
import dev.nstv.practicalfilament.filament.material.loadMaterialOnEngine
import kotlin.math.sin
import kotlin.random.Random

private data class StressCube(
    val handle: Int,
    val baseX: Float,
    val baseY: Float,
    val baseZ: Float,
    val phase: Float,
)

@Composable
fun StressTestScreen(
    modifier: Modifier = Modifier,
) {
    var engine by remember { mutableStateOf<FilamentEngine?>(null) }
    var cubes by remember { mutableStateOf<List<StressCube>>(emptyList()) }
    var supportNotice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(engine, cubes) {
        val currentEngine = engine ?: return@LaunchedEffect
        if (cubes.isEmpty()) return@LaunchedEffect
        while (true) {
            val timeSeconds = withFrameNanos { it } / 1_000_000_000f
            cubes.forEach { cube ->
                val scale = 0.72f + 0.18f * sin(timeSeconds * 1.7f + cube.phase)
                currentEngine.setRenderableTransform(
                    cube.handle,
                    stressTransform(
                        x = cube.baseX,
                        y = cube.baseY + (0.08f * sin(timeSeconds * 1.2f + cube.phase)),
                        z = cube.baseZ,
                        scale = scale,
                    ),
                )
            }
        }
    }

    SampleScreenLayout(
        modifier = modifier,
        title = "Stress Test",
        description = "A dense field of independently transformed cubes with randomized materials. The sample leans on repeated material instances and transform updates rather than a special scene graph.",
        view = {
            FilamentView(
                modifier = Modifier.fillMaxSize(),
                camera = CameraConfig(
                    position = Float3(8f, 6f, 12f),
                    lookAt = Float3(0f, 0f, 0f),
                    fovDegrees = 32.0,
                ),
                lights = listOf(
                    LightConfig(
                        type = LightType.SUN,
                        intensity = 115_000f,
                        direction = Float3(-0.8f, -1f, -0.4f),
                    ),
                ),
                backgroundColor = Color(0.02f, 0.02f, 0.03f, 1f),
                onEngineReady = { readyEngine ->
                    engine = readyEngine
                    val random = Random(7)
                    val createdCubes = buildList {
                        for (x in -3..3) {
                            for (y in -3..3) {
                                for (z in -3..3) {
                                    val material = Material(
                                        fileName = "plastic.filamat",
                                        label = "stress-$x-$y-$z",
                                        overrides = emptyMap(),
                                    )
                                    val (instanceHandle, _, _) = loadMaterialOnEngine(readyEngine, material)
                                    readyEngine.setMaterialParameter(
                                        instanceHandle,
                                        MaterialParameter(
                                            "baseColor",
                                            Float3(
                                                random.nextFloat() * 0.8f + 0.2f,
                                                random.nextFloat() * 0.8f + 0.2f,
                                                random.nextFloat() * 0.8f + 0.2f,
                                            ),
                                        ),
                                    )
                                    readyEngine.setMaterialParameter(
                                        instanceHandle,
                                        MaterialParameter("roughness", random.nextFloat() * 0.85f),
                                    )
                                    val handle = readyEngine.createCubeRenderable(instanceHandle, size = 0.55f)
                                    if (handle <= 0) {
                                        supportNotice = "This sample needs cube geometry support from the platform engine."
                                        return@buildList
                                    }
                                    add(
                                        StressCube(
                                            handle = handle,
                                            baseX = x * 1.2f,
                                            baseY = y * 1.2f,
                                            baseZ = z * 1.2f,
                                            phase = random.nextFloat() * 6.28f,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                    cubes = createdCubes
                },
            )
        },
        controls = {
            supportNotice?.let { SampleNotice(it) }
            Text("Renderable count: ${cubes.size}")
        },
    )
}

private fun stressTransform(
    x: Float,
    y: Float,
    z: Float,
    scale: Float,
): FloatArray {
    return floatArrayOf(
        scale, 0f, 0f, 0f,
        0f, scale, 0f, 0f,
        0f, 0f, scale, 0f,
        x, y, z, 1f,
    )
}
