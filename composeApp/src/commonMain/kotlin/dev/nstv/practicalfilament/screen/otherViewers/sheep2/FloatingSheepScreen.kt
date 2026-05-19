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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import dev.nstv.practicalfilament.theme.TileColor
import dev.nstv.practicalfilament.theme.components.CheckBoxLabel

import practicalfilament.composeapp.generated.resources.Res
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private const val FloatingSheepDefaultCount = 14
private const val FloatingSheepMaxCount = 30
private const val FloatingSheepDefaultTapRespawnDelaySeconds = 2f
private const val FloatingSheepMaxTapRespawnDelaySeconds = 10f
private const val FloatingSheepMinSize = 0.65f
private const val FloatingSheepMaxSize = 1.45f
private const val FloatingSheepSlowBandMin = 0.7f
private const val FloatingSheepFastBandMin = 1.35f
private const val FloatingSheepSlowBandMax = 1.2f
private const val FloatingSheepFastBandMax = 2.2f
private const val FloatingSheepHiddenY = -1_000f
private const val FloatingSheepDriftAmplitude = 0.22f
private const val FloatingSheepBobAmplitude = 0.12f
private const val FloatingSheepDepthAmplitude = 0.08f
private const val FloatingSheepSpawnAttempts = 24
private const val FloatingSheepSpacingRadiusFactor = 0.45f
private const val FloatingSheepSpacingPadding = 0.14f
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

private data class FloatingSheepTrajectoryBounds(
    val minX: Float,
    val maxX: Float,
    val minZ: Float,
    val maxZ: Float,
    val spawnMinY: Float,
    val spawnMaxY: Float,
    val respawnTopY: Float,
)

private enum class FloatingSheepVisualMode {
    PROCEDURAL,
    GLTF,
}

private data class FloatingSheepVisualConfig(
    val mode: FloatingSheepVisualMode,
    val trajectoryBounds: FloatingSheepTrajectoryBounds,
    val removalDurationSeconds: Float,
    val removalSpinDegrees: Float,
    val avoidOverlap: Boolean = true,
    val spreadInitialPopulation: Boolean = false,
    val gltfAssetPath: String? = null,
    val gltfScaleFactor: Float = 1f,
)

private interface FloatingSheepMotionState {
    val sheepId: Int
    var rootPosition: Float3
    var sizeScale: Float
    var active: Boolean
    var respawnCooldownRemainingSeconds: Float
    val isInRemovalAnimation: Boolean
}

private data class FloatingSheepColorway(
    val baseColor: Float3,
    val sheenColor: Float3,
    val subsurfaceColor: Float3,
)

private data class FloatingSheepSharedMaterials(
    val fluffMaterialHandle: Int,
    val bodyInstanceHandle: Int,
    val eyeInstanceHandle: Int,
    val pupilInstanceHandle: Int,
    val glassesInstanceHandle: Int,
)

private class FloatingSheepState(
    override val sheepId: Int,
    val fluffMaterialInstanceHandle: Int,
    var renderables: List<FloatingSheepRenderable>,
    override var rootPosition: Float3,
    override var sizeScale: Float,
    var upwardSpeed: Float,
    var driftPhase: Float,
    var bobPhase: Float,
    var yRotationDegrees: Float,
    var noiseSeed: Int,
    override var active: Boolean,
    var exploding: Boolean,
    var explosionElapsedSeconds: Float,
    override var respawnCooldownRemainingSeconds: Float,
) : FloatingSheepMotionState {
    override val isInRemovalAnimation: Boolean
        get() = exploding
}

private class FloatingGltfSheepState(
    override val sheepId: Int,
    val assetHandle: Int,
    val renderableHandles: IntArray,
    val animationCount: Int,
    val animationDurationSeconds: Float,
    override var rootPosition: Float3,
    override var sizeScale: Float,
    var upwardSpeed: Float,
    var driftPhase: Float,
    var bobPhase: Float,
    var yRotationDegrees: Float,
    var noiseSeed: Int,
    override var active: Boolean,
    var shrinking: Boolean,
    var shrinkElapsedSeconds: Float,
    override var respawnCooldownRemainingSeconds: Float,
) : FloatingSheepMotionState {
    override val isInRemovalAnimation: Boolean
        get() = shrinking
}

private data class FloatingSheepSpawnConfig(
    val rootPosition: Float3,
    val sizeScale: Float,
    val upwardSpeed: Float,
    val fluffColorway: FloatingSheepColorway,
    val driftPhase: Float,
    val bobPhase: Float,
    val yRotationDegrees: Float,
    val noiseSeed: Int,
)

private val FloatingSheepColorPalette = listOf(
    TileColor.Blue,
    TileColor.Pink,
    TileColor.Purple,
    TileColor.Green,
    TileColor.Magenta,
    TileColor.Yellow,
    TileColor.Orange,
    TileColor.Red,
    TileColor.DarkGreen,
).map(::tileColorToFloatingSheepColorway)

private val FloatingSheepProceduralConfig = FloatingSheepVisualConfig(
    mode = FloatingSheepVisualMode.PROCEDURAL,
    trajectoryBounds = FloatingSheepTrajectoryBounds(
        minX = -4.5f,
        maxX = 4.5f,
        minZ = -2.2f,
        maxZ = 2.2f,
        spawnMinY = -8.2f,
        spawnMaxY = -7.5f,
        respawnTopY = 6.8f,
    ),
    removalDurationSeconds = 0.45f,
    removalSpinDegrees = 0f,
    avoidOverlap = true,
    spreadInitialPopulation = false,
)

private val FloatingSheepGltfConfig = FloatingSheepVisualConfig(
    mode = FloatingSheepVisualMode.GLTF,
    trajectoryBounds = FloatingSheepTrajectoryBounds(
        minX = -3.06f,
        maxX = 3.06f,
        minZ = -1.21f,
        maxZ = 1.21f,
        spawnMinY = -6.9f,
        spawnMaxY = -6.2f,
        respawnTopY = 5.7f,
    ),
    removalDurationSeconds = 0.32f,
    removalSpinDegrees = 540f,
    avoidOverlap = false,
    spreadInitialPopulation = true,
    gltfAssetPath = "files/models/sheep/scene.gltf",
    gltfScaleFactor = 0.92f,
)

private fun FloatingSheepVisualMode.config(): FloatingSheepVisualConfig =
    when (this) {
        FloatingSheepVisualMode.PROCEDURAL -> FloatingSheepProceduralConfig
        FloatingSheepVisualMode.GLTF -> FloatingSheepGltfConfig
    }

@Composable
fun FloatingSheepScreen(
    modifier: Modifier = Modifier,
) {
    var engine by remember { mutableStateOf<FilamentEngine?>(null) }
    var sharedMaterials by remember { mutableStateOf<FloatingSheepSharedMaterials?>(null) }
    var speedScale by rememberSaveable { mutableFloatStateOf(1f) }
    var visualMode by rememberSaveable { mutableStateOf(FloatingSheepVisualMode.PROCEDURAL) }
    var tapRespawnDelaySeconds by rememberSaveable {
        mutableFloatStateOf(FloatingSheepDefaultTapRespawnDelaySeconds)
    }
    var maxSheep by rememberSaveable { mutableIntStateOf(FloatingSheepDefaultCount) }
    var selectedBackgroundIndex by rememberSaveable { mutableIntStateOf(0) }
    var wobbleEnabled by rememberSaveable { mutableStateOf(true) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val random = remember { Random.Default }
    val sheepStates = remember { mutableListOf<FloatingSheepState>() }
    val proceduralHandleToSheepId = remember { mutableMapOf<Int, Int>() }
    val gltfSheepStates = remember { mutableListOf<FloatingGltfSheepState>() }
    val gltfHandleToSheepId = remember { mutableMapOf<Int, Int>() }

    LaunchedEffect(engine, sharedMaterials, visualMode) {
        val currentEngine = engine ?: return@LaunchedEffect
        val visualConfig = visualMode.config()
        if (visualConfig.mode == FloatingSheepVisualMode.GLTF) {
            // Let the toggle paint before glTF creation does heavier main-thread work.
            withFrameNanos { }
        }
        if (visualMode == FloatingSheepVisualMode.PROCEDURAL) {
            destroyFloatingGltfSheepPopulation(
                engine = currentEngine,
                sheepStates = gltfSheepStates,
                handleToSheepId = gltfHandleToSheepId,
            )
            val materials = sharedMaterials ?: return@LaunchedEffect

            withFrameSeconds { elapsedSeconds, deltaSeconds ->
                synchronizeFloatingSheepPopulation(
                    engine = currentEngine,
                    sheepStates = sheepStates,
                    targetCount = maxSheep,
                    materials = materials,
                    handleToSheepId = proceduralHandleToSheepId,
                    config = visualConfig,
                    random = random,
                )

                sheepStates.forEach { sheep ->
                    if (!sheep.active || sheep.renderables.isEmpty()) return@forEach

                    if (sheep.exploding) {
                        sheep.explosionElapsedSeconds += deltaSeconds
                        if (sheep.explosionElapsedSeconds >= visualConfig.removalDurationSeconds) {
                            hideFloatingSheepForRespawn(
                                engine = currentEngine,
                                sheep = sheep,
                                respawnDelaySeconds = tapRespawnDelaySeconds,
                            )
                        }
                    } else if (sheep.respawnCooldownRemainingSeconds > 0f) {
                        sheep.respawnCooldownRemainingSeconds =
                            (sheep.respawnCooldownRemainingSeconds - deltaSeconds).coerceAtLeast(0f)
                        if (sheep.respawnCooldownRemainingSeconds <= 0f) {
                            recycleFloatingSheep(
                                engine = currentEngine,
                                sheep = sheep,
                                sheepStates = sheepStates,
                                config = visualConfig,
                                random = random,
                            )
                        }
                    } else {
                        sheep.rootPosition = sheep.rootPosition.copy(
                            y = sheep.rootPosition.y + sheep.upwardSpeed * speedScale * deltaSeconds,
                        )
                        if (sheep.rootPosition.y > visualConfig.trajectoryBounds.respawnTopY + sheep.sizeScale) {
                            recycleFloatingSheep(
                                engine = currentEngine,
                                sheep = sheep,
                                sheepStates = sheepStates,
                                config = visualConfig,
                                random = random,
                            )
                        }
                    }
                }

                resolveFloatingSheepSpacing(
                    sheepStates = sheepStates,
                    config = visualConfig,
                )

                sheepStates.forEach { sheep ->
                    if (!sheep.active || sheep.renderables.isEmpty()) return@forEach

                    val driftX = if (wobbleEnabled && sheep.respawnCooldownRemainingSeconds <= 0f) {
                        sin(elapsedSeconds * 0.58f + sheep.driftPhase) * FloatingSheepDriftAmplitude
                    } else {
                        0f
                    }
                    val bobY = if (wobbleEnabled && sheep.respawnCooldownRemainingSeconds <= 0f) {
                        sin(elapsedSeconds * 1.08f + sheep.bobPhase) * FloatingSheepBobAmplitude
                    } else {
                        0f
                    }
                    val swayZ = if (wobbleEnabled && sheep.respawnCooldownRemainingSeconds <= 0f) {
                        cos(elapsedSeconds * 0.41f + sheep.driftPhase * 0.7f) * FloatingSheepDepthAmplitude
                    } else {
                        0f
                    }
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
                        (sheep.explosionElapsedSeconds / visualConfig.removalDurationSeconds)
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
        } else {
            sheepStates.forEach { sheep ->
                deactivateFloatingSheep(currentEngine, sheep)
            }

            withFrameSeconds { elapsedSeconds, deltaSeconds ->
                synchronizeFloatingGltfSheepPopulation(
                    engine = currentEngine,
                    sheepStates = gltfSheepStates,
                    targetCount = maxSheep,
                    handleToSheepId = gltfHandleToSheepId,
                    config = visualConfig,
                    random = random,
                )

                gltfSheepStates.forEach { sheep ->
                    if (!sheep.active) return@forEach

                    if (sheep.shrinking) {
                        sheep.shrinkElapsedSeconds += deltaSeconds
                        if (sheep.shrinkElapsedSeconds >= visualConfig.removalDurationSeconds) {
                            hideFloatingGltfSheepForRespawn(
                                engine = currentEngine,
                                sheep = sheep,
                                respawnDelaySeconds = tapRespawnDelaySeconds,
                            )
                        }
                    } else if (sheep.respawnCooldownRemainingSeconds > 0f) {
                        sheep.respawnCooldownRemainingSeconds =
                            (sheep.respawnCooldownRemainingSeconds - deltaSeconds).coerceAtLeast(0f)
                        if (sheep.respawnCooldownRemainingSeconds <= 0f) {
                            recycleFloatingGltfSheep(
                                engine = currentEngine,
                                sheep = sheep,
                                sheepStates = gltfSheepStates,
                                config = visualConfig,
                                random = random,
                            )
                        }
                    } else {
                        sheep.rootPosition = sheep.rootPosition.copy(
                            y = sheep.rootPosition.y + sheep.upwardSpeed * speedScale * deltaSeconds,
                        )
                        if (sheep.rootPosition.y > visualConfig.trajectoryBounds.respawnTopY + sheep.sizeScale) {
                            recycleFloatingGltfSheep(
                                engine = currentEngine,
                                sheep = sheep,
                                sheepStates = gltfSheepStates,
                                config = visualConfig,
                                random = random,
                            )
                        }
                    }
                }

                resolveFloatingSheepSpacing(
                    sheepStates = gltfSheepStates,
                    config = visualConfig,
                )

                gltfSheepStates.forEach { sheep ->
                    if (!sheep.active) return@forEach

                    val driftX = if (wobbleEnabled && sheep.respawnCooldownRemainingSeconds <= 0f) {
                        sin(elapsedSeconds * 0.58f + sheep.driftPhase) * FloatingSheepDriftAmplitude
                    } else {
                        0f
                    }
                    val bobY = if (wobbleEnabled && sheep.respawnCooldownRemainingSeconds <= 0f) {
                        sin(elapsedSeconds * 1.08f + sheep.bobPhase) * FloatingSheepBobAmplitude
                    } else {
                        0f
                    }
                    val swayZ = if (wobbleEnabled && sheep.respawnCooldownRemainingSeconds <= 0f) {
                        cos(elapsedSeconds * 0.41f + sheep.driftPhase * 0.7f) * FloatingSheepDepthAmplitude
                    } else {
                        0f
                    }
                    val shrinkProgress = if (sheep.shrinking) {
                        (sheep.shrinkElapsedSeconds / visualConfig.removalDurationSeconds)
                            .coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    val visibleScale = (1f - shrinkProgress) * sheep.sizeScale * visualConfig.gltfScaleFactor
                    val shrinkSpinDegrees = shrinkProgress * visualConfig.removalSpinDegrees
                    val rootTransform = if (sheep.respawnCooldownRemainingSeconds > 0f) {
                        translationMatrix(0f, FloatingSheepHiddenY, 0f)
                    } else {
                        multiplyMatrix4(
                            translationMatrix(
                                x = sheep.rootPosition.x + driftX,
                                y = sheep.rootPosition.y + bobY,
                                z = sheep.rootPosition.z + swayZ,
                            ),
                            multiplyMatrix4(
                                rotationYMatrix(sheep.yRotationDegrees + shrinkSpinDegrees),
                                scaleMatrix(visibleScale, visibleScale, visibleScale),
                            ),
                        )
                    }

                    if (sheep.animationCount > 0) {
                        val animationTime = elapsedSeconds % sheep.animationDurationSeconds
                        currentEngine.applyGltfAnimation(sheep.assetHandle, 0, animationTime)
                        currentEngine.updateGltfBoneMatrices(sheep.assetHandle)
                    }
                    currentEngine.setGltfTransform(sheep.assetHandle, rootTransform)
                }
                currentEngine.requestFrame()
            }
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
                                when (visualMode) {
                                    FloatingSheepVisualMode.PROCEDURAL -> {
                                        val sheepId = proceduralHandleToSheepId[handle] ?: return@pickRenderable
                                        val sheep = sheepStates.firstOrNull { it.sheepId == sheepId }
                                            ?: return@pickRenderable
                                        if (!sheep.exploding && sheep.renderables.isNotEmpty()) {
                                            sheep.exploding = true
                                            sheep.explosionElapsedSeconds = 0f
                                        }
                                    }

                                    FloatingSheepVisualMode.GLTF -> {
                                        val sheepId = gltfHandleToSheepId[handle] ?: return@pickRenderable
                                        val sheep = gltfSheepStates.firstOrNull { it.sheepId == sheepId }
                                            ?: return@pickRenderable
                                        if (!sheep.shrinking && sheep.respawnCooldownRemainingSeconds <= 0f) {
                                            sheep.shrinking = true
                                            sheep.shrinkElapsedSeconds = 0f
                                        }
                                    }
                                }
                            }
                        },
                    )
                },
            camera = FloatingSheepCamera,
            lights = FloatingSheepLights,
            onEngineReady = { readyEngine ->
                engine = readyEngine
                proceduralHandleToSheepId.clear()
                gltfHandleToSheepId.clear()
                sheepStates.clear()
                gltfSheepStates.clear()

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
                selectedBackgroundIndex = selectedBackgroundIndex,
                onSelectedBackgroundIndexChange = { selectedBackgroundIndex = it },
                speedScale = speedScale,
                onSpeedScaleChange = { speedScale = it },
                visualMode = visualMode,
                onVisualModeChange = { visualMode = it },
                tapRespawnDelaySeconds = tapRespawnDelaySeconds,
                onTapRespawnDelaySecondsChange = { tapRespawnDelaySeconds = it },
                maxSheep = maxSheep,
                onMaxSheepChange = { maxSheep = it },
                wobbleEnabled = wobbleEnabled,
                onWobbleEnabledChange = { wobbleEnabled = it },
            )
        }
    }
}

private fun synchronizeFloatingSheepPopulation(
    engine: FilamentEngine,
    sheepStates: MutableList<FloatingSheepState>,
    targetCount: Int,
    materials: FloatingSheepSharedMaterials,
    handleToSheepId: MutableMap<Int, Int>,
    config: FloatingSheepVisualConfig,
    random: Random,
) {
    sheepStates
        .filter { it.active }
        .drop(targetCount)
        .forEach { sheep ->
            deactivateFloatingSheep(engine, sheep)
        }

    while (sheepStates.count { it.active } < targetCount) {
        val inactiveSheep = sheepStates.firstOrNull { !it.active }
        if (inactiveSheep != null) {
            recycleFloatingSheep(
                engine = engine,
                sheep = inactiveSheep,
                sheepStates = sheepStates,
                config = config,
                random = random,
            )
            inactiveSheep.active = true
            continue
        }

        val sheep = createFloatingSheepState(
            engine = engine,
            sheepId = sheepStates.size,
            materials = materials,
            handleToSheepId = handleToSheepId,
            sheepStates = sheepStates,
            config = config,
            random = random,
        ) ?: break
        seedFloatingSheepInitialPlacement(
            sheep = sheep,
            placementIndex = sheepStates.count { it.active },
            targetCount = targetCount,
            config = config,
            random = random,
        )
        sheepStates += sheep
    }
}

private fun synchronizeFloatingGltfSheepPopulation(
    engine: FilamentEngine,
    sheepStates: MutableList<FloatingGltfSheepState>,
    targetCount: Int,
    handleToSheepId: MutableMap<Int, Int>,
    config: FloatingSheepVisualConfig,
    random: Random,
) {
    sheepStates
        .filter { it.active }
        .drop(targetCount)
        .forEach { sheep ->
            deactivateFloatingGltfSheep(engine, sheep)
        }

    while (sheepStates.count { it.active } < targetCount) {
        val inactiveSheep = sheepStates.firstOrNull { !it.active }
        if (inactiveSheep != null) {
            recycleFloatingGltfSheep(
                engine = engine,
                sheep = inactiveSheep,
                sheepStates = sheepStates,
                config = config,
                random = random,
            )
            inactiveSheep.active = true
            continue
        }

        val sheep = createFloatingGltfSheepState(
            engine = engine,
            sheepId = sheepStates.size,
            handleToSheepId = handleToSheepId,
            sheepStates = sheepStates,
            config = config,
            random = random,
        ) ?: break
        seedFloatingSheepInitialPlacement(
            sheep = sheep,
            placementIndex = sheepStates.count { it.active },
            targetCount = targetCount,
            config = config,
            random = random,
        )
        sheepStates += sheep
    }
}

private fun destroyFloatingGltfSheepPopulation(
    engine: FilamentEngine,
    sheepStates: MutableList<FloatingGltfSheepState>,
    handleToSheepId: MutableMap<Int, Int>,
) {
    sheepStates.forEach { sheep ->
        engine.removeGltfFromScene(sheep.assetHandle)
        engine.destroyGltfAsset(sheep.assetHandle)
    }
    sheepStates.clear()
    handleToSheepId.clear()
}

private fun createFloatingSheepState(
    engine: FilamentEngine,
    sheepId: Int,
    materials: FloatingSheepSharedMaterials,
    handleToSheepId: MutableMap<Int, Int>,
    sheepStates: List<FloatingSheepState>,
    config: FloatingSheepVisualConfig,
    random: Random,
): FloatingSheepState? {
    val fluffInstanceHandle = engine.createMaterialInstance(materials.fluffMaterialHandle)
    if (fluffInstanceHandle <= 0) {
        return null
    }

    return FloatingSheepState(
        sheepId = sheepId,
        fluffMaterialInstanceHandle = fluffInstanceHandle,
        renderables = emptyList(),
        rootPosition = Float3(0f, 0f, 0f),
        sizeScale = 1f,
        upwardSpeed = 1f,
        driftPhase = 0f,
        bobPhase = 0f,
        yRotationDegrees = 0f,
        noiseSeed = sheepId + 1,
        active = true,
        exploding = false,
        explosionElapsedSeconds = 0f,
        respawnCooldownRemainingSeconds = 0f,
    ).also { sheep ->
        buildFloatingSheep(
            engine = engine,
            sheep = sheep,
            materials = materials,
            handleToSheepId = handleToSheepId,
            sheepStates = sheepStates,
            config = config,
            random = random,
        )
    }
}

private fun createFloatingGltfSheepState(
    engine: FilamentEngine,
    sheepId: Int,
    handleToSheepId: MutableMap<Int, Int>,
    sheepStates: List<FloatingGltfSheepState>,
    config: FloatingSheepVisualConfig,
    random: Random,
): FloatingGltfSheepState? {
    val assetPath = config.gltfAssetPath ?: return null
    val assetHandle = engine.loadGltfAsset(Res.getUri(assetPath))
    if (assetHandle <= 0) {
        return null
    }
    engine.transformGltfToUnitCube(assetHandle)
    val animationCount = engine.getGltfAnimationCount(assetHandle)
    val animationDuration = if (animationCount > 0) {
        engine.getGltfAnimationDuration(assetHandle, 0).coerceAtLeast(0.01f)
    } else {
        0f
    }
    if (animationCount > 0) {
        engine.applyGltfAnimation(assetHandle, 0, 0f)
        engine.updateGltfBoneMatrices(assetHandle)
    }
    engine.setGltfTransform(
        assetHandle,
        translationMatrix(0f, FloatingSheepHiddenY, 0f),
    )
    engine.addGltfToScene(assetHandle)
    val renderableHandles = engine.getGltfRenderableHandles(assetHandle)
    renderableHandles.forEach { handle ->
        handleToSheepId[handle] = sheepId
    }

    return FloatingGltfSheepState(
        sheepId = sheepId,
        assetHandle = assetHandle,
        renderableHandles = renderableHandles,
        animationCount = animationCount,
        animationDurationSeconds = animationDuration,
        rootPosition = Float3(0f, FloatingSheepHiddenY, 0f),
        sizeScale = 1f,
        upwardSpeed = 1f,
        driftPhase = 0f,
        bobPhase = 0f,
        yRotationDegrees = 0f,
        noiseSeed = sheepId + 1,
        active = true,
        shrinking = false,
        shrinkElapsedSeconds = 0f,
        respawnCooldownRemainingSeconds = 0f,
    ).also { sheep ->
        recycleFloatingGltfSheep(
            engine = engine,
            sheep = sheep,
            sheepStates = sheepStates,
            config = config,
            random = random,
        )
    }
}

private fun buildFloatingSheep(
    engine: FilamentEngine,
    sheep: FloatingSheepState,
    materials: FloatingSheepSharedMaterials,
    handleToSheepId: MutableMap<Int, Int>,
    sheepStates: List<FloatingSheepState>,
    config: FloatingSheepVisualConfig,
    random: Random,
) {
    val spawn = randomFloatingSheepSpawn(
        random = random,
        sheepStates = sheepStates,
        config = config,
        sheepIdToIgnore = sheep.sheepId,
    )
    applySheepFluffColor(
        engine = engine,
        instanceHandle = sheep.fluffMaterialInstanceHandle,
        colorway = spawn.fluffColorway,
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
    sheep.active = true
    sheep.exploding = false
    sheep.explosionElapsedSeconds = 0f
    sheep.respawnCooldownRemainingSeconds = 0f
}

private fun recycleFloatingSheep(
    engine: FilamentEngine,
    sheep: FloatingSheepState,
    sheepStates: List<FloatingSheepState>,
    config: FloatingSheepVisualConfig,
    random: Random,
) {
    val spawn = randomFloatingSheepSpawn(
        random = random,
        sheepStates = sheepStates,
        config = config,
        sheepIdToIgnore = sheep.sheepId,
    )
    applySheepFluffColor(
        engine = engine,
        instanceHandle = sheep.fluffMaterialInstanceHandle,
        colorway = spawn.fluffColorway,
    )

    sheep.rootPosition = spawn.rootPosition
    sheep.sizeScale = spawn.sizeScale
    sheep.upwardSpeed = spawn.upwardSpeed
    sheep.driftPhase = spawn.driftPhase
    sheep.bobPhase = spawn.bobPhase
    sheep.yRotationDegrees = spawn.yRotationDegrees
    sheep.noiseSeed = spawn.noiseSeed
    sheep.active = true
    sheep.exploding = false
    sheep.explosionElapsedSeconds = 0f
    sheep.respawnCooldownRemainingSeconds = 0f
}

private fun recycleFloatingGltfSheep(
    engine: FilamentEngine,
    sheep: FloatingGltfSheepState,
    sheepStates: List<FloatingGltfSheepState>,
    config: FloatingSheepVisualConfig,
    random: Random,
) {
    val spawn = randomFloatingSheepSpawn(
        random = random,
        sheepStates = sheepStates,
        config = config,
        sheepIdToIgnore = sheep.sheepId,
    )
    sheep.rootPosition = spawn.rootPosition
    sheep.sizeScale = spawn.sizeScale
    sheep.upwardSpeed = spawn.upwardSpeed
    sheep.driftPhase = spawn.driftPhase
    sheep.bobPhase = spawn.bobPhase
    sheep.yRotationDegrees = spawn.yRotationDegrees
    sheep.noiseSeed = spawn.noiseSeed
    sheep.active = true
    sheep.shrinking = false
    sheep.shrinkElapsedSeconds = 0f
    sheep.respawnCooldownRemainingSeconds = 0f
    engine.setGltfTransform(
        sheep.assetHandle,
        translationMatrix(
            sheep.rootPosition.x,
            sheep.rootPosition.y,
            sheep.rootPosition.z,
        ),
    )
}

private fun hideFloatingSheepForRespawn(
    engine: FilamentEngine,
    sheep: FloatingSheepState,
    respawnDelaySeconds: Float,
) {
    sheep.exploding = false
    sheep.explosionElapsedSeconds = 0f
    sheep.respawnCooldownRemainingSeconds = respawnDelaySeconds.coerceAtLeast(0f)
    sheep.rootPosition = Float3(0f, FloatingSheepHiddenY, 0f)
    sheep.renderables.forEach { renderable ->
        engine.setRenderableTransform(
            renderable.piece.handle,
            multiplyMatrix4(
                translationMatrix(0f, FloatingSheepHiddenY, 0f),
                renderable.piece.baseTransform,
            ),
        )
    }
}

private fun hideFloatingGltfSheepForRespawn(
    engine: FilamentEngine,
    sheep: FloatingGltfSheepState,
    respawnDelaySeconds: Float,
) {
    sheep.shrinking = false
    sheep.shrinkElapsedSeconds = 0f
    sheep.respawnCooldownRemainingSeconds = respawnDelaySeconds.coerceAtLeast(0f)
    sheep.rootPosition = Float3(0f, FloatingSheepHiddenY, 0f)
    engine.setGltfTransform(
        sheep.assetHandle,
        translationMatrix(0f, FloatingSheepHiddenY, 0f),
    )
}

private fun deactivateFloatingSheep(
    engine: FilamentEngine,
    sheep: FloatingSheepState,
) {
    sheep.active = false
    sheep.exploding = false
    sheep.explosionElapsedSeconds = 0f
    sheep.respawnCooldownRemainingSeconds = 0f
    sheep.rootPosition = Float3(0f, FloatingSheepHiddenY, 0f)
    sheep.renderables.forEach { renderable ->
        engine.setRenderableTransform(
            renderable.piece.handle,
            multiplyMatrix4(
                translationMatrix(0f, FloatingSheepHiddenY, 0f),
                renderable.piece.baseTransform,
            ),
        )
    }
}

private fun deactivateFloatingGltfSheep(
    engine: FilamentEngine,
    sheep: FloatingGltfSheepState,
) {
    sheep.active = false
    sheep.shrinking = false
    sheep.shrinkElapsedSeconds = 0f
    sheep.respawnCooldownRemainingSeconds = 0f
    sheep.rootPosition = Float3(0f, FloatingSheepHiddenY, 0f)
    engine.setGltfTransform(
        sheep.assetHandle,
        translationMatrix(0f, FloatingSheepHiddenY, 0f),
    )
}

private fun randomFloatingSheepSpawn(
    random: Random,
    sheepStates: List<FloatingSheepMotionState>,
    config: FloatingSheepVisualConfig,
    sheepIdToIgnore: Int,
): FloatingSheepSpawnConfig {
    if (!config.avoidOverlap) {
        return randomFloatingSheepSpawnCandidate(
            random = random,
            bounds = config.trajectoryBounds,
        )
    }
    var bestSpawn: FloatingSheepSpawnConfig? = null
    var bestClearance = Float.NEGATIVE_INFINITY
    repeat(FloatingSheepSpawnAttempts) {
        val candidate = randomFloatingSheepSpawnCandidate(
            random = random,
            bounds = config.trajectoryBounds,
        )
        val clearance = floatingSheepSpawnClearance(
            candidate = candidate,
            sheepStates = sheepStates,
            sheepIdToIgnore = sheepIdToIgnore,
        )
        if (clearance > bestClearance) {
            bestClearance = clearance
            bestSpawn = candidate
        }
        if (clearance >= 0f) {
            return candidate
        }
    }
    return bestSpawn ?: randomFloatingSheepSpawnCandidate(
        random = random,
        bounds = config.trajectoryBounds,
    )
}

private fun randomFloatingSheepSpawnCandidate(
    random: Random,
    bounds: FloatingSheepTrajectoryBounds,
): FloatingSheepSpawnConfig {
    val sizeScale = random.nextFloat(FloatingSheepMinSize, FloatingSheepMaxSize)
    val sizeFactor =
        ((sizeScale - FloatingSheepMinSize) / (FloatingSheepMaxSize - FloatingSheepMinSize))
            .coerceIn(0f, 1f)
    val speedMin = lerp(FloatingSheepSlowBandMin, FloatingSheepFastBandMin, sizeFactor)
    val speedMax = lerp(FloatingSheepSlowBandMax, FloatingSheepFastBandMax, sizeFactor)
    val noiseSeed = random.nextInt(1, Int.MAX_VALUE)
    val noiseAnchor = Float3(noiseSeed * 0.1f, noiseSeed * 0.37f, noiseSeed * 0.71f)
    val noisePos = sheep2NoiseVector(anchor = noiseAnchor, timeSeconds = 0f, seed = noiseSeed, frequency = 1f)
    val xMin = bounds.minX + sizeScale
    val xMax = bounds.maxX - sizeScale
    return FloatingSheepSpawnConfig(
        rootPosition = Float3(
            x = lerp(xMin, xMax, (noisePos.x + 1f) * 0.5f),
            y = random.nextFloat(bounds.spawnMinY, bounds.spawnMaxY),
            z = lerp(bounds.minZ, bounds.maxZ, (noisePos.z + 1f) * 0.5f),
        ),
        sizeScale = sizeScale,
        upwardSpeed = random.nextFloat(speedMin, speedMax),
        fluffColorway = FloatingSheepColorPalette.random(random),
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
    colorway: FloatingSheepColorway,
) {
    engine.setMaterialParameter(instanceHandle, MaterialParameter("baseColor", colorway.baseColor))
    engine.setMaterialParameter(instanceHandle, MaterialParameter("roughness", 0.27f))
    engine.setMaterialParameter(
        instanceHandle,
        MaterialParameter("sheenColor", colorway.sheenColor),
    )
    engine.setMaterialParameter(
        instanceHandle,
        MaterialParameter("subsurfaceColor", colorway.subsurfaceColor),
    )
}

private fun floatingSheepSpawnClearance(
    candidate: FloatingSheepSpawnConfig,
    sheepStates: List<FloatingSheepMotionState>,
    sheepIdToIgnore: Int,
): Float {
    val candidateRadius = floatingSheepSpacingRadius(candidate.sizeScale)
    return sheepStates
        .asSequence()
        .filter { sheep ->
            sheep.active &&
                !sheep.isInRemovalAnimation &&
                sheep.respawnCooldownRemainingSeconds <= 0f &&
                sheep.sheepId != sheepIdToIgnore
        }
        .map { sheep ->
            distanceXZ(candidate.rootPosition, sheep.rootPosition) -
                (candidateRadius + floatingSheepSpacingRadius(sheep.sizeScale) + FloatingSheepSpacingPadding)
        }
        .minOrNull()
        ?: Float.POSITIVE_INFINITY
}

private fun resolveFloatingSheepSpacing(
    sheepStates: List<FloatingSheepMotionState>,
    config: FloatingSheepVisualConfig,
) {
    if (!config.avoidOverlap) {
        return
    }
    val visibleSheep = sheepStates.filter { sheep ->
        sheep.active &&
            !sheep.isInRemovalAnimation &&
            sheep.respawnCooldownRemainingSeconds <= 0f
    }

    for (i in 0 until visibleSheep.lastIndex) {
        val sheepA = visibleSheep[i]
        for (j in i + 1 until visibleSheep.size) {
            val sheepB = visibleSheep[j]
            val minDistance = floatingSheepSpacingRadius(sheepA.sizeScale) +
                floatingSheepSpacingRadius(sheepB.sizeScale) +
                FloatingSheepSpacingPadding
            val dx = sheepB.rootPosition.x - sheepA.rootPosition.x
            val dz = sheepB.rootPosition.z - sheepA.rootPosition.z
            val distanceSquared = dx * dx + dz * dz
            if (distanceSquared >= minDistance * minDistance) {
                continue
            }

            val distance = sqrt(distanceSquared)
            val direction = if (distance > 1e-4f) {
                Float3(dx / distance, 0f, dz / distance)
            } else {
                separationFallbackDirection(sheepA.sheepId, sheepB.sheepId)
            }
            val pushDistance = (minDistance - distance).coerceAtLeast(0f) * 0.5f
            sheepA.rootPosition = clampFloatingSheepPosition(
                sheepA.rootPosition.copy(
                    x = sheepA.rootPosition.x - direction.x * pushDistance,
                    z = sheepA.rootPosition.z - direction.z * pushDistance,
                ),
                sheepA.sizeScale,
                config.trajectoryBounds,
            )
            sheepB.rootPosition = clampFloatingSheepPosition(
                sheepB.rootPosition.copy(
                    x = sheepB.rootPosition.x + direction.x * pushDistance,
                    z = sheepB.rootPosition.z + direction.z * pushDistance,
                ),
                sheepB.sizeScale,
                config.trajectoryBounds,
            )
        }
    }
}

private fun seedFloatingSheepInitialPlacement(
    sheep: FloatingSheepMotionState,
    placementIndex: Int,
    targetCount: Int,
    config: FloatingSheepVisualConfig,
    random: Random,
) {
    if (!config.spreadInitialPopulation || targetCount <= 0) {
        return
    }
    val slotCount = targetCount.coerceAtLeast(1)
    val baseFraction = (placementIndex.toFloat() + 0.5f) / slotCount.toFloat()
    val jitter = (random.nextFloat() - 0.5f) * (0.35f / slotCount.toFloat())
    val yFraction = (baseFraction + jitter).coerceIn(0f, 1f)
    sheep.rootPosition = sheep.rootPosition.copy(
        y = lerp(
            config.trajectoryBounds.spawnMinY,
            config.trajectoryBounds.respawnTopY,
            yFraction,
        ),
    )
}

private fun Random.nextFloat(min: Float, max: Float): Float =
    min + nextFloat() * (max - min)

private fun lerp(start: Float, end: Float, fraction: Float): Float =
    start + (end - start) * fraction

private fun floatingSheepSpacingRadius(sizeScale: Float): Float =
    sizeScale * FloatingSheepSpacingRadiusFactor

private fun distanceXZ(first: Float3, second: Float3): Float {
    val dx = first.x - second.x
    val dz = first.z - second.z
    return sqrt(dx * dx + dz * dz)
}

private fun clampFloatingSheepPosition(
    position: Float3,
    sizeScale: Float,
    bounds: FloatingSheepTrajectoryBounds,
): Float3 {
    val xMin = bounds.minX + sizeScale
    val xMax = bounds.maxX - sizeScale
    return position.copy(
        x = position.x.coerceIn(xMin, xMax),
        z = position.z.coerceIn(bounds.minZ, bounds.maxZ),
    )
}

private fun separationFallbackDirection(firstSheepId: Int, secondSheepId: Int): Float3 {
    val angle = ((firstSheepId * 97 + secondSheepId * 57) % 360).toFloat()
    val radians = angle / 180f * kotlin.math.PI.toFloat()
    return Float3(cos(radians), 0f, sin(radians))
}

private fun tileColorToFloatingSheepColorway(color: Color): FloatingSheepColorway {
    val normalizedBase = normalizePeak(color.toFloat3(), minPeak = 0.42f, maxPeak = 0.82f)
    return FloatingSheepColorway(
        baseColor = normalizedBase,
        sheenColor = blendFloat3(normalizedBase, Float3(0.95f, 0.95f, 0.95f), 0.18f),
        subsurfaceColor = blendFloat3(normalizedBase, Float3(0.14f, 0.11f, 0.08f), 0.58f),
    )
}

private fun Color.toFloat3(): Float3 = Float3(red, green, blue)

private fun normalizePeak(color: Float3, minPeak: Float, maxPeak: Float): Float3 {
    val peak = maxOf(color.x, color.y, color.z)
    if (peak <= 1e-6f) return color
    val targetPeak = when {
        peak < minPeak -> minPeak
        peak > maxPeak -> maxPeak
        else -> peak
    }
    val scale = targetPeak / peak
    return scaleFloat3(color, scale)
}

private fun scaleFloat3(color: Float3, scale: Float): Float3 = Float3(
    x = (color.x * scale).coerceIn(0f, 1f),
    y = (color.y * scale).coerceIn(0f, 1f),
    z = (color.z * scale).coerceIn(0f, 1f),
)

private fun blendFloat3(start: Float3, end: Float3, amount: Float): Float3 {
    val fraction = amount.coerceIn(0f, 1f)
    return Float3(
        x = lerp(start.x, end.x, fraction),
        y = lerp(start.y, end.y, fraction),
        z = lerp(start.z, end.z, fraction),
    )
}

private fun formatSingleDecimal(value: Float): String {
    val rounded = ((value * 10f).toInt() / 10f)
    val integer = rounded.toInt()
    val decimal = ((rounded - integer) * 10).toInt()
    return "$integer.${abs(decimal)}"
}

@Composable
private fun FloatingSheepSettingsPanel(
    engine: FilamentEngine?,
    selectedBackgroundIndex: Int,
    onSelectedBackgroundIndexChange: (Int) -> Unit,
    speedScale: Float,
    onSpeedScaleChange: (Float) -> Unit,
    visualMode: FloatingSheepVisualMode,
    onVisualModeChange: (FloatingSheepVisualMode) -> Unit,
    tapRespawnDelaySeconds: Float,
    onTapRespawnDelaySecondsChange: (Float) -> Unit,
    maxSheep: Int,
    onMaxSheepChange: (Int) -> Unit,
    wobbleEnabled: Boolean,
    onWobbleEnabledChange: (Boolean) -> Unit,
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

            EnvironmentSelectionField(
                filamentEngine = engine,
                selectedBackground = selectedBackgroundIndex,
                onSelectedBackgroundChange = onSelectedBackgroundIndexChange,
            )

            CheckBoxLabel(
                text = "Wobble animation",
                checked = wobbleEnabled,
                onCheckedChange = onWobbleEnabledChange,
                textStyle = MaterialTheme.typography.bodyMedium,
            )

            CheckBoxLabel(
                text = "Use glTF sheep",
                checked = visualMode == FloatingSheepVisualMode.GLTF,
                onCheckedChange = { enabled ->
                    onVisualModeChange(
                        if (enabled) FloatingSheepVisualMode.GLTF else FloatingSheepVisualMode.PROCEDURAL
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium,
            )

            Text(
                text = "Speed: ${formatSingleDecimal(speedScale)}x",
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = speedScale,
                onValueChange = onSpeedScaleChange,
                valueRange = 0f..3f,
            )

            Text(
                text = "Tap Respawn Delay: ${formatSingleDecimal(tapRespawnDelaySeconds)}s",
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = tapRespawnDelaySeconds,
                onValueChange = onTapRespawnDelaySecondsChange,
                valueRange = 0f..FloatingSheepMaxTapRespawnDelaySeconds,
                steps = 19,
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
