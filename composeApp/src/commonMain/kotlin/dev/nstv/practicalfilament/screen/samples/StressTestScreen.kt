// Migrated sample from https://github.com/google/filament/tree/main/android/samples/sample-material-instance-stress
package dev.nstv.practicalfilament.screen.samples

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.nstv.practicalfilament.filament.withFrameSeconds
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.FilamentColor
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.LightConfig
import dev.nstv.practicalfilament.filament.LightType
import dev.nstv.practicalfilament.filament.material.MaterialParameter
import dev.nstv.practicalfilament.theme.Grid
import dev.nstv.practicalfilament.theme.components.SampleNotice
import dev.nstv.practicalfilament.theme.components.SampleScreenLayout
import practicalfilament.composeapp.generated.resources.Res
import kotlin.math.sin
import kotlin.random.Random

private data class StressCube(
    val handle: Int,
    val baseX: Float,
    val baseY: Float,
    val baseZ: Float,
    val phase: Float,
)

private data class StressCubeSpec(
    val baseX: Float,
    val baseY: Float,
    val baseZ: Float,
    val phase: Float,
    val color: Float3,
    val roughness: Float,
)

private const val StressMaterialPath = "files/materials/plastic.filamat"
private const val StressChunkSize = 24

@Composable
fun StressTestScreen(
    modifier: Modifier = Modifier,
) {
    var engine by remember { mutableStateOf<FilamentEngine?>(null) }
    var cubes by remember { mutableStateOf<List<StressCube>>(emptyList()) }
    var loadedCubeCount by remember { mutableIntStateOf(0) }
    var loadingMessage by remember { mutableStateOf<String?>(null) }
    var supportNotice by remember { mutableStateOf<String?>(null) }
    val cubeSpecs = remember { buildStressCubeSpecs() }

    LaunchedEffect(engine, cubes) {
        val currentEngine = engine ?: return@LaunchedEffect
        if (cubes.isEmpty()) return@LaunchedEffect
        withFrameSeconds { elapsed, _ ->
            cubes.forEach { cube ->
                val scale = 0.72f + 0.18f * sin(elapsed * 1.7f + cube.phase)
                currentEngine.setRenderableTransform(
                    cube.handle,
                    stressTransform(
                        x = cube.baseX,
                        y = cube.baseY + (0.08f * sin(elapsed * 1.2f + cube.phase)),
                        z = cube.baseZ,
                        scale = scale,
                    ),
                )
            }
        }
    }

    LaunchedEffect(engine) {
        val currentEngine = engine ?: return@LaunchedEffect
        cubes = emptyList()
        loadedCubeCount = 0
        loadingMessage = "Loading stress material..."
        supportNotice = null

        val materialHandle = currentEngine.loadMaterial(Res.getUri(StressMaterialPath))
        if (materialHandle <= 0) {
            loadingMessage = null
            supportNotice = "Stress material failed to load."
            return@LaunchedEffect
        }

        val createdCubes = mutableListOf<StressCube>()
        cubeSpecs.chunked(StressChunkSize).forEachIndexed { chunkIndex, chunk ->
            loadingMessage = "Creating cubes ${chunkIndex * StressChunkSize + 1}-${(chunkIndex * StressChunkSize + chunk.size)} of ${cubeSpecs.size}..."
            chunk.forEach { spec ->
                val instanceHandle = currentEngine.createMaterialInstance(materialHandle)
                currentEngine.setMaterialParameter(
                    instanceHandle,
                    MaterialParameter("baseColor", spec.color),
                )
                currentEngine.setMaterialParameter(
                    instanceHandle,
                    MaterialParameter("roughness", spec.roughness),
                )
                val handle = currentEngine.createCubeRenderable(instanceHandle, size = 0.55f)
                if (handle <= 0) {
                    loadingMessage = null
                    supportNotice = "This sample needs cube geometry support from the platform engine."
                    cubes = createdCubes.toList()
                    loadedCubeCount = createdCubes.size
                    return@LaunchedEffect
                }
                createdCubes += StressCube(
                    handle = handle,
                    baseX = spec.baseX,
                    baseY = spec.baseY,
                    baseZ = spec.baseZ,
                    phase = spec.phase,
                )
            }
            cubes = createdCubes.toList()
            loadedCubeCount = createdCubes.size
            currentEngine.requestFrame()
        }

        loadingMessage = null
    }

    SampleScreenLayout(
        modifier = modifier,
        title = "Stress Test",
        view = {
            Box(modifier = Modifier.fillMaxSize()) {
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
                    onEngineReady = { readyEngine ->
                        engine = readyEngine
                    },
                )
                loadingMessage?.let { message ->
                    StressLoadingState(
                        modifier = Modifier.align(Alignment.Center),
                        message = message,
                    )
                }
            }
        },
        controls = {
            supportNotice?.let {
                SampleNotice(
                    it
                )
            }
            Text("Renderable count: $loadedCubeCount / ${cubeSpecs.size}")
        },
    )
}

@Composable
private fun StressLoadingState(
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

private fun buildStressCubeSpecs(): List<StressCubeSpec> {
    val random = Random(7)
    return buildList {
        for (x in -3..3) {
            for (y in -3..3) {
                for (z in -3..3) {
                    add(
                        StressCubeSpec(
                            baseX = x * 1.2f,
                            baseY = y * 1.2f,
                            baseZ = z * 1.2f,
                            phase = random.nextFloat() * 6.28f,
                            color = Float3(
                                random.nextFloat() * 0.8f + 0.2f,
                                random.nextFloat() * 0.8f + 0.2f,
                                random.nextFloat() * 0.8f + 0.2f,
                            ),
                            roughness = random.nextFloat() * 0.85f,
                        ),
                    )
                }
            }
        }
    }
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
