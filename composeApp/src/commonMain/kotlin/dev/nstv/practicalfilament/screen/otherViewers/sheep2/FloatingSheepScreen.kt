package dev.nstv.practicalfilament.screen.otherViewers.sheep2

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nstv.practicalfilament.components.materials.sheepBodyMaterial
import dev.nstv.practicalfilament.components.materials.sheepFluffMaterial
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.FilamentColor
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.LightConfig
import dev.nstv.practicalfilament.filament.LightType
import dev.nstv.practicalfilament.filament.material.MaterialParameter
import dev.nstv.practicalfilament.filament.withFrameSeconds
import dev.nstv.practicalfilament.screen.marbles.components.EnvironmentSelectionField

import practicalfilament.composeapp.generated.resources.Res
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val FloatingSheepDefaultCount = 14
private const val FloatingSheepMaxCount = 30
private const val FloatingSheepExplosionDurationSeconds = 0.45f
private const val FloatingSheepMinSize = 0.65f
private const val FloatingSheepMaxSize = 1.45f
private const val FloatingSheepMinX = -4.5f
private const val FloatingSheepMaxX = 4.5f
private const val FloatingSheepMinZ = -2.2f
private const val FloatingSheepMaxZ = 2.2f
private const val FloatingSheepSpawnMinY = -8.2f
private const val FloatingSheepSpawnMaxY = -7.5f
private const val FloatingSheepRespawnTopY = 6.8f
private const val FloatingSheepSlowBandMin = 0.7f
private const val FloatingSheepFastBandMin = 1.35f
private const val FloatingSheepSlowBandMax = 1.2f
private const val FloatingSheepFastBandMax = 2.2f
private const val FloatingSheepDriftAmplitude = 0.22f
private const val FloatingSheepBobAmplitude = 0.12f
private const val FloatingSheepDepthAmplitude = 0.08f
private val FloatingSheepCamera = CameraConfig(
    position = Float3(0f, 0.15f, 18f),
    lookAt = Float3(0f, 0.15f, 0f),
    fovDegrees = 38.0,
)

private val FloatingSheepLights = listOf(
    LightConfig(
        type = LightType.SUN,
        color = FilamentColor(0.98f, 0.94f, 0.90f),
        intensity = 100_000f,
        direction = Float3(0.45f, -1f, -0.6f),
        sunAngularRadius = 1.9f,
        sunHaloSize = 10f,
        sunHaloFalloff = 80f,
    ),
    LightConfig(
        type = LightType.DIRECTIONAL,
        color = FilamentColor(0.78f, 0.84f, 1f),
        intensity = 28_000f,
        direction = Float3(-1f, -0.35f, 0.8f),
    ),
)

private data class FloatingSheepRenderable(
    val piece: SheepRigPiece,
    val explosionOffset: Float3,
)

private data class FloatingSheepSharedMaterials(
    val fluffMaterialHandle: Int,
    val bodyInstanceHandle: Int,
    val eyeInstanceHandle: Int,
    val pupilInstanceHandle: Int,
    val glassesInstanceHandle: Int,
)

private class FloatingSheepState(
    val sheepId: Int,
    val fluffMaterialInstanceHandle: Int,
    var renderables: List<FloatingSheepRenderable>,
    var rootPosition: Float3,
    var sizeScale: Float,
    var upwardSpeed: Float,
    var driftPhase: Float,
    var bobPhase: Float,
    var yRotationDegrees: Float,
    var noiseSeed: Int,
    var exploding: Boolean,
    var explosionElapsedSeconds: Float,
)

private data class FloatingSheepSpawnConfig(
    val rootPosition: Float3,
    val sizeScale: Float,
    val upwardSpeed: Float,
    val fluffColor: Float3,
    val driftPhase: Float,
    val bobPhase: Float,
    val yRotationDegrees: Float,
    val noiseSeed: Int,
)

@Composable
fun FloatingSheepScreen(
    modifier: Modifier = Modifier,
) {
    var engine by remember { mutableStateOf<FilamentEngine?>(null) }
    var sharedMaterials by remember { mutableStateOf<FloatingSheepSharedMaterials?>(null) }
    var speedScale by remember { mutableFloatStateOf(1f) }
    var maxSheep by remember { mutableIntStateOf(FloatingSheepDefaultCount) }
    var showSettings by remember { mutableStateOf(false) }
    val random = remember { Random.Default }
    val sheepStates = remember { mutableListOf<FloatingSheepState>() }
    val handleToSheepId = remember { mutableMapOf<Int, Int>() }

    LaunchedEffect(engine, sharedMaterials) {
        val currentEngine = engine ?: return@LaunchedEffect
        val materials = sharedMaterials ?: return@LaunchedEffect

        withFrameSeconds { elapsedSeconds, deltaSeconds ->
            if (sheepStates.size < maxSheep) {
                val sheepIndex = sheepStates.size
                val fluffInstanceHandle = currentEngine.createMaterialInstance(materials.fluffMaterialHandle)
                if (fluffInstanceHandle > 0) {
                    val sheep = FloatingSheepState(
                        sheepId = sheepIndex,
                        fluffMaterialInstanceHandle = fluffInstanceHandle,
                        renderables = emptyList(),
                        rootPosition = Float3(0f, 0f, 0f),
                        sizeScale = 1f,
                        upwardSpeed = 1f,
                        driftPhase = 0f,
                        bobPhase = 0f,
                        yRotationDegrees = 0f,
                        noiseSeed = sheepIndex + 1,
                        exploding = false,
                        explosionElapsedSeconds = 0f,
                    )
                    buildFloatingSheep(
                        engine = currentEngine,
                        sheep = sheep,
                        materials = materials,
                        handleToSheepId = handleToSheepId,
                        random = random,
                    )
                    sheepStates += sheep
                }
            }

            sheepStates.forEach { sheep ->
                if (sheep.renderables.isEmpty()) return@forEach

                if (sheep.exploding) {
                    sheep.explosionElapsedSeconds += deltaSeconds
                    if (sheep.explosionElapsedSeconds >= FloatingSheepExplosionDurationSeconds) {
                        recycleFloatingSheep(
                            engine = currentEngine,
                            sheep = sheep,
                            random = random,
                        )
                    }
                } else {
                    sheep.rootPosition = sheep.rootPosition.copy(
                        y = sheep.rootPosition.y + sheep.upwardSpeed * speedScale * deltaSeconds,
                    )
                    if (sheep.rootPosition.y > FloatingSheepRespawnTopY + sheep.sizeScale) {
                        recycleFloatingSheep(
                            engine = currentEngine,
                            sheep = sheep,
                            random = random,
                        )
                    }
                }

                val driftX = sin(elapsedSeconds * 0.58f + sheep.driftPhase) * FloatingSheepDriftAmplitude
                val bobY = sin(elapsedSeconds * 1.08f + sheep.bobPhase) * FloatingSheepBobAmplitude
                val swayZ = cos(elapsedSeconds * 0.41f + sheep.driftPhase * 0.7f) * FloatingSheepDepthAmplitude
                val rootTransform = multiplyMatrix4(
                    translationMatrix(
                        x = sheep.rootPosition.x + driftX,
                        y = sheep.rootPosition.y + bobY,
                        z = sheep.rootPosition.z + swayZ,
                    ),
                    multiplyMatrix4(
                        rotationYMatrix(sheep.yRotationDegrees),
                        scaleMatrix(sheep.sizeScale, sheep.sizeScale, sheep.sizeScale),
                    ),
                )
                val explosionProgress = if (sheep.exploding) {
                    (sheep.explosionElapsedSeconds / FloatingSheepExplosionDurationSeconds)
                        .coerceIn(0f, 1f)
                } else {
                    0f
                }

                sheep.renderables.forEach { renderable ->
                    currentEngine.setRenderableTransform(
                        renderable.piece.handle,
                        multiplyMatrix4(
                            rootTransform,
                            multiplyMatrix4(
                                buildScatterAndShrinkTransform(
                                    explosionOffset = renderable.explosionOffset,
                                    progress = explosionProgress,
                                    distanceScale = 0.1f,
                                ),
                                renderable.piece.baseTransform,
                            ),
                        ),
                    )
                }
            }
            currentEngine.requestFrame()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        FilamentView(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(engine) {
                    detectTapGestures(
                        onPress = { offset ->
                            engine?.pickRenderable(offset.x.toInt(), offset.y.toInt()) { result ->
                                val handle = result?.renderableHandle ?: return@pickRenderable
                                val sheepId = handleToSheepId[handle] ?: return@pickRenderable
                                val sheep = sheepStates.firstOrNull { it.sheepId == sheepId }
                                    ?: return@pickRenderable
                                if (!sheep.exploding && sheep.renderables.isNotEmpty()) {
                                    sheep.exploding = true
                                    sheep.explosionElapsedSeconds = 0f
                                }
                            }
                        },
                    )
                },
            camera = FloatingSheepCamera,
            lights = FloatingSheepLights,
            onEngineReady = { readyEngine ->
                engine = readyEngine
                handleToSheepId.clear()
                sheepStates.clear()

                val fluffMaterialHandle = readyEngine.loadMaterial(
                    Res.getUri(sheepFluffMaterial().materialPath),
                )
                val bodyMaterialHandle = readyEngine.loadMaterial(
                    Res.getUri(sheepBodyMaterial().materialPath),
                )
                if (fluffMaterialHandle <= 0 || bodyMaterialHandle <= 0) {
                    sharedMaterials = null
                    return@FilamentView
                }

                sharedMaterials = FloatingSheepSharedMaterials(
                    fluffMaterialHandle = fluffMaterialHandle,
                    bodyInstanceHandle = createSheepBodyMaterialInstance(
                        engine = readyEngine,
                        materialHandle = bodyMaterialHandle,
                        color = Float3(0.27f, 0.27f, 0.27f),
                        roughness = 0.6f,
                    ),
                    eyeInstanceHandle = createSheepBodyMaterialInstance(
                        engine = readyEngine,
                        materialHandle = bodyMaterialHandle,
                        color = Float3(0.90f, 0.60f, 0.26f),
                        roughness = 0.72f,
                    ),
                    pupilInstanceHandle = createSheepBodyMaterialInstance(
                        engine = readyEngine,
                        materialHandle = bodyMaterialHandle,
                        color = Float3(0.05f, 0.05f, 0.05f),
                        roughness = 0.45f,
                    ),
                    glassesInstanceHandle = createSheepBodyMaterialInstance(
                        engine = readyEngine,
                        materialHandle = bodyMaterialHandle,
                        color = Float3(0.05f, 0.05f, 0.05f),
                        roughness = 0.3f,
                    ),
                )
                readyEngine.requestFrame()
            },
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable { showSettings = !showSettings },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "⚙",
                fontSize = 22.sp,
                color = Color.White.copy(alpha = 0.85f),
            )
        }

        if (showSettings) {
            FloatingSheepSettingsPanel(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 68.dp, end = 16.dp),
                engine = engine,
                speedScale = speedScale,
                onSpeedScaleChange = { speedScale = it },
                maxSheep = maxSheep,
                onMaxSheepChange = { maxSheep = it },
            )
        }
    }
}

private fun buildFloatingSheep(
    engine: FilamentEngine,
    sheep: FloatingSheepState,
    materials: FloatingSheepSharedMaterials,
    handleToSheepId: MutableMap<Int, Int>,
    random: Random,
) {
    val spawn = randomFloatingSheepSpawn(random)
    applySheepFluffColor(
        engine = engine,
        instanceHandle = sheep.fluffMaterialInstanceHandle,
        color = spawn.fluffColor,
    )

    val createdPieces = buildSheepRigPieces(
        engine = engine,
        fluffInstanceHandle = sheep.fluffMaterialInstanceHandle,
        bodyInstanceHandle = materials.bodyInstanceHandle,
        eyeInstanceHandle = materials.eyeInstanceHandle,
        pupilInstanceHandle = materials.pupilInstanceHandle,
        glassesInstanceHandle = materials.glassesInstanceHandle,
    )
    sheep.renderables = createdPieces
        .filter { it.handle > 0 }
        .mapIndexed { index, piece ->
            handleToSheepId[piece.handle] = sheep.sheepId
            FloatingSheepRenderable(
                piece = piece,
                explosionOffset = sheepScatterOffset(piece.anchor, index),
            )
        }

    sheep.rootPosition = spawn.rootPosition
    sheep.sizeScale = spawn.sizeScale
    sheep.upwardSpeed = spawn.upwardSpeed
    sheep.driftPhase = spawn.driftPhase
    sheep.bobPhase = spawn.bobPhase
    sheep.yRotationDegrees = spawn.yRotationDegrees
    sheep.noiseSeed = spawn.noiseSeed
    sheep.exploding = false
    sheep.explosionElapsedSeconds = 0f
}

private fun recycleFloatingSheep(
    engine: FilamentEngine,
    sheep: FloatingSheepState,
    random: Random,
) {
    val spawn = randomFloatingSheepSpawn(random)
    applySheepFluffColor(
        engine = engine,
        instanceHandle = sheep.fluffMaterialInstanceHandle,
        color = spawn.fluffColor,
    )

    sheep.rootPosition = spawn.rootPosition
    sheep.sizeScale = spawn.sizeScale
    sheep.upwardSpeed = spawn.upwardSpeed
    sheep.driftPhase = spawn.driftPhase
    sheep.bobPhase = spawn.bobPhase
    sheep.yRotationDegrees = spawn.yRotationDegrees
    sheep.noiseSeed = spawn.noiseSeed
    sheep.exploding = false
    sheep.explosionElapsedSeconds = 0f
}

private fun randomFloatingSheepSpawn(random: Random): FloatingSheepSpawnConfig {
    val sizeScale = random.nextFloat(FloatingSheepMinSize, FloatingSheepMaxSize)
    val sizeFactor =
        ((sizeScale - FloatingSheepMinSize) / (FloatingSheepMaxSize - FloatingSheepMinSize))
            .coerceIn(0f, 1f)
    val speedMin = lerp(FloatingSheepSlowBandMin, FloatingSheepFastBandMin, sizeFactor)
    val speedMax = lerp(FloatingSheepSlowBandMax, FloatingSheepFastBandMax, sizeFactor)
    val noiseSeed = random.nextInt(1, Int.MAX_VALUE)
    val noiseAnchor = Float3(noiseSeed * 0.1f, noiseSeed * 0.37f, noiseSeed * 0.71f)
    val noisePos = sheep2NoiseVector(anchor = noiseAnchor, timeSeconds = 0f, seed = noiseSeed, frequency = 1f)
    val xMin = FloatingSheepMinX + sizeScale
    val xMax = FloatingSheepMaxX - sizeScale
    return FloatingSheepSpawnConfig(
        rootPosition = Float3(
            x = lerp(xMin, xMax, (noisePos.x + 1f) * 0.5f),
            y = random.nextFloat(FloatingSheepSpawnMinY, FloatingSheepSpawnMaxY),
            z = lerp(FloatingSheepMinZ, FloatingSheepMaxZ, (noisePos.z + 1f) * 0.5f),
        ),
        sizeScale = sizeScale,
        upwardSpeed = random.nextFloat(speedMin, speedMax),
        fluffColor = randomFluffColor(random),
        driftPhase = random.nextFloat(0f, kotlin.math.PI.toFloat() * 2f),
        bobPhase = random.nextFloat(0f, kotlin.math.PI.toFloat() * 2f),
        yRotationDegrees = random.nextFloat(0f, 360f),
        noiseSeed = noiseSeed,
    )
}

private fun createSheepBodyMaterialInstance(
    engine: FilamentEngine,
    materialHandle: Int,
    color: Float3,
    roughness: Float,
    metallic: Float = 0f,
): Int {
    val instanceHandle = engine.createMaterialInstance(materialHandle)
    engine.setMaterialParameter(instanceHandle, MaterialParameter("baseColor", color))
    engine.setMaterialParameter(instanceHandle, MaterialParameter("roughness", roughness))
    engine.setMaterialParameter(instanceHandle, MaterialParameter("metallic", metallic))
    return instanceHandle
}

private fun applySheepFluffColor(
    engine: FilamentEngine,
    instanceHandle: Int,
    color: Float3,
) {
    engine.setMaterialParameter(instanceHandle, MaterialParameter("baseColor", color))
    engine.setMaterialParameter(instanceHandle, MaterialParameter("roughness", 0.27f))
    engine.setMaterialParameter(
        instanceHandle,
        MaterialParameter("sheenColor", Float3(0.29f, 0.72f, 0.37f)),
    )
    engine.setMaterialParameter(
        instanceHandle,
        MaterialParameter("subsurfaceColor", Float3(0.41f, 0.42f, 0.23f)),
    )
}

private fun randomFluffColor(random: Random): Float3 {
    val hue = random.nextFloat(0f, 1f)
    val saturation = random.nextFloat(0.35f, 0.8f)
    val value = random.nextFloat(0.72f, 1f)
    return hsvToRgb(hue, saturation, value)
}

private fun hsvToRgb(hue: Float, saturation: Float, value: Float): Float3 {
    if (saturation <= 1e-6f) {
        return Float3(value, value, value)
    }
    val scaledHue = ((hue % 1f) + 1f) % 1f * 6f
    val sector = scaledHue.toInt()
    val fraction = scaledHue - sector
    val p = value * (1f - saturation)
    val q = value * (1f - saturation * fraction)
    val t = value * (1f - saturation * (1f - fraction))
    return when (sector % 6) {
        0 -> Float3(value, t, p)
        1 -> Float3(q, value, p)
        2 -> Float3(p, value, t)
        3 -> Float3(p, q, value)
        4 -> Float3(t, p, value)
        else -> Float3(value, p, q)
    }
}

private fun Random.nextFloat(min: Float, max: Float): Float =
    min + nextFloat() * (max - min)

private fun lerp(start: Float, end: Float, fraction: Float): Float =
    start + (end - start) * fraction

@Composable
private fun FloatingSheepSettingsPanel(
    engine: FilamentEngine?,
    speedScale: Float,
    onSpeedScaleChange: (Float) -> Unit,
    maxSheep: Int,
    onMaxSheepChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.widthIn(max = 280.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        tonalElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleMedium,
            )

            EnvironmentSelectionField(filamentEngine = engine)

            val speedDisplay = ((speedScale * 10f).toInt() / 10f).let { rounded ->
                val integer = rounded.toInt()
                val decimal = ((rounded - integer) * 10).toInt()
                "$integer.${kotlin.math.abs(decimal)}"
            }
            Text(
                text = "Speed: ${speedDisplay}x",
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = speedScale,
                onValueChange = onSpeedScaleChange,
                valueRange = 0f..3f,
            )

            Text(
                text = "Max Sheep: $maxSheep",
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = maxSheep.toFloat(),
                onValueChange = { onMaxSheepChange(it.toInt()) },
                valueRange = 1f..FloatingSheepMaxCount.toFloat(),
                steps = FloatingSheepMaxCount - 2,
            )
        }
    }
}
