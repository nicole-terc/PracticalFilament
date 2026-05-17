package dev.nstv.practicalfilament.screen.otherViewers

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import dev.nstv.practicalfilament.filament.withFrameSeconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import dev.nstv.composablesheep.library.model.FluffStyle
import dev.nstv.practicalfilament.components.ParameterInputField
import dev.nstv.practicalfilament.components.materials.sheepBodyMaterial
import dev.nstv.practicalfilament.components.materials.sheepFluffMaterial
import dev.nstv.practicalfilament.components.utils.OrbitQuaternion
import dev.nstv.practicalfilament.components.utils.orbitCameraConfig
import dev.nstv.practicalfilament.components.utils.orbitCameraControls
import dev.nstv.practicalfilament.components.utils.rememberOrbitCameraState
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
import dev.nstv.practicalfilament.filament.VertexAttribute
import dev.nstv.practicalfilament.filament.VertexAttributeLayout
import dev.nstv.practicalfilament.filament.material.MaterialParameter
import dev.nstv.practicalfilament.filament.material.MaterialParameterDefinition
import dev.nstv.practicalfilament.filament.toByteArray
import dev.nstv.practicalfilament.screen.marbles.components.EnvironmentSelectionField
import dev.nstv.practicalfilament.theme.Grid
import dev.nstv.practicalfilament.theme.components.SampleNotice
import practicalfilament.composeapp.generated.resources.Res
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

private const val SheepIndirectLightPath = "files/envs/pillars_2k/pillars_2k_ibl.ktx"
private const val SheepSkyboxPath = "files/envs/pillars_2k/pillars_2k_skybox.ktx"
private const val SheepEnvironmentIntensity = 50_000f
private const val SheepFluffRadius = 1f
private const val SheepFluffCoreRadius = 0.42f
private const val SheepFluffShellRadius = 0.60f
private const val SheepFluffChunkRadius = 0.26f
private const val SheepHeadDepth = 0.16f
private const val SheepEyeDepthOffset = -0.12f
private const val SheepPupilDepthOffset = 0.045f
private const val SheepGlassesDepthOffset = -0.02f
private const val SheepBridgeDepthOffset = -0.02f
private const val SheepHeadRadiusX = 0.43f
private const val SheepHeadRadiusY = SheepHeadRadiusX * (2f / 3f)
private const val SheepHeadRadiusZ = SheepHeadRadiusX * 0.75f
private const val SheepDefaultCameraDistance = 4f
private const val SheepMinCameraDistance = 2f
private const val SheepMaxCameraDistance = 8f
private const val SheepExplosionDistance = 5.2f
private const val SheepExplosionUpBias = 0.55f
private const val SheepExplosionResponse = 9.5f
private const val SheepExplosionDistanceMin = 0f
private const val SheepExplosionDistanceMax = 4f
private const val SheepExplosionProgressMin = 0f
private const val SheepExplosionProgressMax = 1f
private val SheepBaseCamera = CameraConfig(
    position = Float3(0f, 0f, SheepDefaultCameraDistance),
    lookAt = Float3(0f, 0f, 0f),
    fovDegrees = 45.0,
)

private enum class SheepBuildGroup {
    FLUFF_CORE,
    FLUFF_SHELL,
    LEGS,
    HEAD,
    GLASSES,
}

private enum class SheepBuildStep {
    NONE,
    FLUFF_CORE,
    FLUFF_ALL,
    LEGS,
    HEAD,
    GLASSES;

    companion object {
        val entries_indexed = entries.toTypedArray()
        fun fromIndex(index: Int): SheepBuildStep =
            entries_indexed[index.coerceIn(0, entries_indexed.lastIndex)]
    }
}

private const val SheepBuildAnimationDuration = 400

private data class FluffChunk(
    val center: Float3,
    val radius: Float,
)

private val SheepBaseLights = listOf(
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

private data class SheepRenderable(
    val handle: Int,
    val baseTransform: FloatArray,
    val explosionOffset: Float3,
    val buildGroup: SheepBuildGroup,
)

private class SheepExplosionState {
    var targetProgress = 0f
    var progress = 0f

    fun reset() {
        targetProgress = 0f
        progress = 0f
    }

    fun animateTo(value: Float) {
        targetProgress = value.coerceIn(SheepExplosionProgressMin, SheepExplosionProgressMax)
    }

    fun snapTo(value: Float) {
        val clamped = value.coerceIn(SheepExplosionProgressMin, SheepExplosionProgressMax)
        targetProgress = clamped
        progress = clamped
    }

    fun toggle() {
        animateTo(if (progress >= 0.5f) 0f else 1f)
    }

    fun step(deltaSeconds: Float) {
        if (deltaSeconds <= 0f) return
        val smoothing = 1f - exp(-SheepExplosionResponse * deltaSeconds)
        progress += (targetProgress - progress) * smoothing
        if (abs(targetProgress - progress) <= 0.0015f) {
            progress = targetProgress
        }
    }
}

@Composable
fun SheepScreen(
    modifier: Modifier = Modifier,
) {
    var filamentEngine by remember { mutableStateOf<FilamentEngine?>(null) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val cameraState = rememberOrbitCameraState(initialDistance = SheepDefaultCameraDistance)
    var animationSpeed by remember { mutableFloatStateOf(0.5f) }
    var explosionDistanceScale by remember { mutableFloatStateOf(1f) }
    var explosionProgressControl by remember { mutableFloatStateOf(0f) }
    var renderables by remember { mutableStateOf<List<SheepRenderable>>(emptyList()) }
    var fluffMaterialInstanceHandle by remember { mutableStateOf(0) }
    var materialParameterDefinitions by remember {
        mutableStateOf<List<MaterialParameterDefinition>>(emptyList())
    }
    var materialParameters by remember { mutableStateOf<Map<String, MaterialParameter>>(emptyMap()) }
    var supportNotice by remember { mutableStateOf<String?>(null) }
    val explosionState = remember { SheepExplosionState() }

    val buildScope = rememberCoroutineScope()
    var buildJob by remember { mutableStateOf<Job?>(null) }
    var buildStep by remember { mutableStateOf(SheepBuildStep.GLASSES) }
    var buildSliderIndex by remember { mutableIntStateOf(SheepBuildStep.GLASSES.ordinal) }
    val buildTransition = updateTransition(targetState = buildStep, label = "sheepBuild")
    val fluffCoreProgress by buildTransition.animateFloat(
        label = "fluffCore",
        transitionSpec = { tween(SheepBuildAnimationDuration) },
    ) { step -> if (step >= SheepBuildStep.FLUFF_CORE) 1f else 0f }
    val fluffShellProgress by buildTransition.animateFloat(
        label = "fluffShell",
        transitionSpec = { tween(SheepBuildAnimationDuration) },
    ) { step -> if (step >= SheepBuildStep.FLUFF_ALL) 1f else 0f }
    val legsProgress by buildTransition.animateFloat(
        label = "legs",
        transitionSpec = { tween(SheepBuildAnimationDuration) },
    ) { step -> if (step >= SheepBuildStep.LEGS) 1f else 0f }
    val headProgress by buildTransition.animateFloat(
        label = "head",
        transitionSpec = { tween(SheepBuildAnimationDuration) },
    ) { step -> if (step >= SheepBuildStep.HEAD) 1f else 0f }
    val glassesProgress by buildTransition.animateFloat(
        label = "glasses",
        transitionSpec = { tween(SheepBuildAnimationDuration) },
    ) { step -> if (step >= SheepBuildStep.GLASSES) 1f else 0f }

    SideEffect {
        val currentEngine = filamentEngine ?: return@SideEffect
        if (fluffMaterialInstanceHandle == 0) return@SideEffect
        materialParameters.values.forEach { parameter ->
            currentEngine.setMaterialParameter(fluffMaterialInstanceHandle, parameter)
        }
        currentEngine.requestFrame()
    }

    SideEffect {
        val currentEngine = filamentEngine ?: return@SideEffect
        currentEngine.updateCamera(
            orbitCameraConfig(
                baseCamera = SheepBaseCamera,
                orientation = cameraState.orientation,
                distance = cameraState.distance,
            )
        )
        currentEngine.requestFrame()
    }

    LaunchedEffect(
        filamentEngine,
        renderables,
        animationSpeed,
        explosionDistanceScale,
    ) {
        val currentEngine = filamentEngine ?: return@LaunchedEffect
        if (renderables.isEmpty()) return@LaunchedEffect

        withFrameSeconds { elapsedSeconds, deltaSeconds ->
            explosionState.step(deltaSeconds)
            val bobOffset = if (animationSpeed <= 0.01f) {
                0f
            } else {
                sin(elapsedSeconds * (1.6f + animationSpeed * 2.2f)) * 0.14f * animationSpeed
            }
            val swayDegrees = if (animationSpeed <= 0.01f) {
                0f
            } else {
                sin(elapsedSeconds * (1.1f + animationSpeed * 1.6f)) * 6f * animationSpeed
            }
            val rootTransform = multiplyMatrix4(
                translationMatrix(0f, bobOffset, 0f),
                rotationZMatrix(swayDegrees),
            )
            val explosionProgress = smoothStep(explosionState.progress)
            renderables.forEach { renderable ->
                val groupProgress = when (renderable.buildGroup) {
                    SheepBuildGroup.FLUFF_CORE -> fluffCoreProgress
                    SheepBuildGroup.FLUFF_SHELL -> fluffShellProgress
                    SheepBuildGroup.LEGS -> legsProgress
                    SheepBuildGroup.HEAD -> headProgress
                    SheepBuildGroup.GLASSES -> glassesProgress
                }
                currentEngine.setRenderableTransform(
                    renderable.handle,
                    sheepTransformForProgress(
                        rootTransform = rootTransform,
                        renderable = renderable,
                        explosionProgress = explosionProgress,
                        explosionDistanceScale = explosionDistanceScale,
                        buildProgress = groupProgress,
                    ),
                )
            }
            currentEngine.requestFrame()
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            FilamentView(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { viewportSize = it }
                    .pointerInput(renderables.size) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (renderables.isNotEmpty()) {
                                    explosionState.toggle()
                                    explosionProgressControl = explosionState.targetProgress
                                }
                            },
                        )
                    }
                    .orbitCameraControls(
                        viewportSize = viewportSize,
                        cameraState = cameraState,
                        baseCamera = SheepBaseCamera,
                        engine = filamentEngine,
                        minDistance = SheepMinCameraDistance,
                        maxDistance = SheepMaxCameraDistance,
                        enabled = renderables.isNotEmpty(),
                    ),
                camera = orbitCameraConfig(
                    baseCamera = SheepBaseCamera,
                    orientation = cameraState.orientation,
                    distance = cameraState.distance,
                ),
                lights = SheepBaseLights,
                onEngineReady = { engine ->
                    val fluffMaterial = engine.loadMaterial(sheepFluffMaterial())
                    val indirectLightHandle =
                        engine.loadIndirectLight(Res.getUri(SheepIndirectLightPath))
                    if (indirectLightHandle > 0) {
                        engine.setIndirectLight(
                            handle = indirectLightHandle,
                            intensity = SheepEnvironmentIntensity,
                        )
                    }
                    val skyboxHandle = engine.loadSkybox(Res.getUri(SheepSkyboxPath))
                    if (skyboxHandle > 0) {
                        engine.setSkybox(skyboxHandle)
                    }

                    filamentEngine = engine
                    cameraState.orientation = OrbitQuaternion.Identity
                    cameraState.distance = SheepDefaultCameraDistance
                    explosionState.reset()
                    explosionProgressControl = 0f
                    supportNotice = null
                    fluffMaterialInstanceHandle = fluffMaterial.instanceHandle
                    materialParameterDefinitions = fluffMaterial.definitions
                    materialParameters = fluffMaterial.parameters

                    val bodyMaterialHandle = engine.loadMaterial(
                        Res.getUri(sheepBodyMaterial().materialPath)
                    )
                    if (bodyMaterialHandle <= 0) {
                        renderables = emptyList()
                        supportNotice = "The sheep body material could not be loaded."
                        return@FilamentView
                    }

                    val bodyInstance = createBodyMaterialInstance(
                        engine = engine,
                        materialHandle = bodyMaterialHandle,
                        color = Float3(0.27f, 0.27f, 0.27f),
                        roughness = 0.6f,
                    )
                    val eyeInstance = createBodyMaterialInstance(
                        engine = engine,
                        materialHandle = bodyMaterialHandle,
                        color = Float3(0.90f, 0.60f, 0.26f),
                        roughness = 0.72f,
                    )
                    val pupilInstance = createBodyMaterialInstance(
                        engine = engine,
                        materialHandle = bodyMaterialHandle,
                        color = Float3(0.05f, 0.05f, 0.05f),
                        roughness = 0.45f,
                    )
                    val glassesInstance = createBodyMaterialInstance(
                        engine = engine,
                        materialHandle = bodyMaterialHandle,
                        color = Float3(0.05f, 0.05f, 0.05f),
                        roughness = 0.3f,
                    )

                    val createdRenderables = buildSheepRenderables(
                        engine = engine,
                        fluffInstanceHandle = fluffMaterial.instanceHandle,
                        bodyInstanceHandle = bodyInstance,
                        eyeInstanceHandle = eyeInstance,
                        pupilInstanceHandle = pupilInstance,
                        glassesInstanceHandle = glassesInstance,
                    )
                    renderables = createdRenderables.filter { it.handle > 0 }
                    supportNotice = if (renderables.size != createdRenderables.size) {
                        "Some sheep geometry could not be created on this platform."
                    } else {
                        null
                    }
                    renderables.forEach { renderable ->
                        engine.setRenderableTransform(
                            renderable.handle,
                            sheepTransformForProgress(
                                rootTransform = identityMatrix4(),
                                renderable = renderable,
                                explosionProgress = explosionState.progress,
                                explosionDistanceScale = explosionDistanceScale,
                                buildProgress = 1f,
                            ),
                        )
                    }
                    engine.requestFrame()
                },
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(Grid.Two),
        ) {
            supportNotice?.let { SampleNotice(it) }
            EnvironmentSelectionField(
                filamentEngine = filamentEngine,
            )
            Text("Fluff Material")
            materialParameterDefinitions.forEach { definition ->
                val parameter = materialParameters[definition.name] ?: return@forEach
                ParameterInputField(
                    name = definition.name,
                    type = definition.type,
                    value = parameter.value,
                ) { updatedValue ->
                    materialParameters = materialParameters + (
                            definition.name to MaterialParameter(definition.name, updatedValue)
                            )
                }
            }

            Text(
                modifier = Modifier.padding(top = Grid.Two),
                text = "Animations",
                style = MaterialTheme.typography.headlineSmall,
            )
            HorizontalDivider(Modifier.fillMaxWidth())


            Text(
                modifier = Modifier.padding(top = Grid.Two),
                text = "Animation Speed",
            )
            Slider(
                value = animationSpeed,
                valueRange = 0f..2f,
                onValueChange = { animationSpeed = it },
            )

            Text(
                modifier = Modifier.padding(top = Grid.Two),
                text = "Explosion Distance",
            )
            Slider(
                value = explosionDistanceScale,
                valueRange = SheepExplosionDistanceMin..SheepExplosionDistanceMax,
                onValueChange = { explosionDistanceScale = it },
            )

            Text(
                modifier = Modifier.padding(top = Grid.Two),
                text = "Explosion Separation",
            )
            Slider(
                value = explosionProgressControl,
                valueRange = SheepExplosionProgressMin..SheepExplosionProgressMax,
                onValueChange = { value ->
                    explosionProgressControl = value
                    explosionState.snapTo(value)
                },
            )

            Row(
                modifier = Modifier.height(intrinsicSize = IntrinsicSize.Max),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Build Animation",
                    modifier = Modifier.padding(end = Grid.One),
                )
                Button(
                    onClick = {
                        buildJob?.cancel()
                        val goingDown = buildStep == SheepBuildStep.GLASSES
                        buildJob = buildScope.launch {
                            if (goingDown) {
                                for (i in buildStep.ordinal - 1 downTo 0) {
                                    delay(SheepBuildAnimationDuration.toLong() + 100)
                                    val step = SheepBuildStep.fromIndex(i)
                                    buildStep = step
                                    buildSliderIndex = i
                                }
                            } else {
                                for (i in buildStep.ordinal + 1..SheepBuildStep.GLASSES.ordinal) {
                                    delay(SheepBuildAnimationDuration.toLong() + 100)
                                    val step = SheepBuildStep.fromIndex(i)
                                    buildStep = step
                                    buildSliderIndex = i
                                }
                            }
                            buildJob = null
                        }
                    },
                ) {
                    Text(if (buildStep == SheepBuildStep.GLASSES) "Deconstruct" else "Build")
                }
            }

            Slider(
                value = buildSliderIndex.toFloat(),
                valueRange = 0f..SheepBuildStep.GLASSES.ordinal.toFloat(),
                steps = SheepBuildStep.GLASSES.ordinal - 1,
                onValueChange = { value ->
                    val index = value.toInt()
                    buildSliderIndex = index
                    buildStep = SheepBuildStep.fromIndex(index)
                },
            )
        }
    }
}

private fun createBodyMaterialInstance(
    engine: FilamentEngine,
    materialHandle: Int,
    color: Float3,
    roughness: Float,
    metallic: Float = 0f,
): Int {
    val instanceHandle = engine.createMaterialInstance(materialHandle)
    engine.setMaterialParameter(
        instanceHandle,
        MaterialParameter("baseColor", color),
    )
    engine.setMaterialParameter(
        instanceHandle,
        MaterialParameter("roughness", roughness),
    )
    engine.setMaterialParameter(
        instanceHandle,
        MaterialParameter("metallic", metallic),
    )
    return instanceHandle
}

private fun buildSheepRenderables(
    engine: FilamentEngine,
    fluffInstanceHandle: Int,
    bodyInstanceHandle: Int,
    eyeInstanceHandle: Int,
    pupilInstanceHandle: Int,
    glassesInstanceHandle: Int,
): List<SheepRenderable> {
    val renderables = mutableListOf<SheepRenderable>()

    val fluffCoreHandle = engine.createSphereRenderable(
        materialInstanceHandle = fluffInstanceHandle,
        radius = SheepFluffCoreRadius,
    )
    renderables += SheepRenderable(
        handle = fluffCoreHandle,
        baseTransform = identityMatrix4(),
        explosionOffset = Float3(0f, 0f, 0f),
        buildGroup = SheepBuildGroup.FLUFF_CORE,
    )

    fluffSphereSpecs(FluffStyle.Uniform(10), SheepFluffRadius).forEach { chunk ->
        val handle = engine.createSphereRenderable(
            materialInstanceHandle = fluffInstanceHandle,
            radius = chunk.radius,
        )
        renderables += SheepRenderable(
            handle = handle,
            baseTransform = translationMatrix(chunk.center.x, chunk.center.y, chunk.center.z),
            explosionOffset = Float3(0f, 0f, 0f),
            buildGroup = SheepBuildGroup.FLUFF_SHELL,
        )
    }

    val headLayout = buildHeadLayout()
    val headHandle = engine.createSphereRenderable(
        materialInstanceHandle = bodyInstanceHandle,
        radius = SheepHeadRadiusX,
    )
    renderables += SheepRenderable(
        handle = headHandle,
        baseTransform = multiplyMatrix4(
            multiplyMatrix4(
                translationMatrix(headLayout.center.x, headLayout.center.y, headLayout.center.z),
                rotationZMatrix(headLayout.rotationDegrees),
            ),
            scaleMatrix(1f, 2f / 3f, 0.75f),
        ),
        explosionOffset = Float3(0f, 0f, 0f),
        buildGroup = SheepBuildGroup.HEAD,
    )

    listOf(
        headLayout.leftEyeCenter to headLayout.leftPupilCenter,
        headLayout.rightEyeCenter to headLayout.rightPupilCenter,
    ).forEach { (eyePosition, pupilPosition) ->
        val eyeHandle = engine.createSphereRenderable(
            materialInstanceHandle = eyeInstanceHandle,
            radius = headLayout.eyeRadius,
        )
        renderables += SheepRenderable(
            handle = eyeHandle,
            baseTransform = translationMatrix(eyePosition.x, eyePosition.y, eyePosition.z),
            explosionOffset = Float3(0f, 0f, 0f),
            buildGroup = SheepBuildGroup.HEAD,
        )
        val pupilHandle = engine.createSphereRenderable(
            materialInstanceHandle = pupilInstanceHandle,
            radius = headLayout.eyeRadius * 0.6f,
        )
        renderables += SheepRenderable(
            handle = pupilHandle,
            baseTransform = multiplyMatrix4(
                translationMatrix(pupilPosition.x, pupilPosition.y, pupilPosition.z),
                scaleMatrix(0.28f, 0.45f, 1f),
            ),
            explosionOffset = Float3(0f, 0f, 0f),
            buildGroup = SheepBuildGroup.HEAD,
        )
    }

    val legAttachments = listOf(
        Float3(-0.26f, -0.36f, 0.18f),
        Float3(-0.26f, -0.36f, -0.18f),
        Float3(0.18f, -0.36f, 0.18f),
        Float3(0.18f, -0.36f, -0.18f),
    )
    legAttachments.forEach { legAttachment ->
        val legHeight = 0.56f
        val legRadius = 0.072f
        val (vertexData, indexData) = buildCylinderGeometry(
            radius = legRadius,
            height = legHeight,
            centered = false,
        )
        val handle = engine.createCustomRenderableWithGeneratedTangents(
            CustomRenderableConfig(
                vertexData = vertexData,
                vertexCount = vertexData.size / (5 * Float.SIZE_BYTES),
                strideBytes = 20,
                attributes = sheepGeometryAttributes(),
                indices = indexData,
                materialInstanceHandle = bodyInstanceHandle,
                boundingBox = BoundingBox(
                    center = Float3(0f, -legHeight * 0.5f, 0f),
                    halfExtent = Float3(legRadius, legHeight * 0.5f, legRadius),
                ),
                primitiveType = PrimitiveType.TRIANGLES,
            ),
        )
        renderables += SheepRenderable(
            handle = handle,
            baseTransform = translationMatrix(legAttachment.x, legAttachment.y, legAttachment.z),
            explosionOffset = Float3(0f, 0f, 0f),
            buildGroup = SheepBuildGroup.LEGS,
        )
    }

    val (glassesVertexData, glassesIndexData) = buildSemiDiskGeometry(
        radius = headLayout.glassRadius,
        thickness = 0.03f,
    )
    val glassesBounds = BoundingBox(
        center = Float3(0f, -headLayout.glassRadius * 0.5f, 0f),
        halfExtent = Float3(headLayout.glassRadius, headLayout.glassRadius * 0.5f, 0.03f),
    )
    listOf(headLayout.leftGlassCenter, headLayout.rightGlassCenter).forEach { glassCenter ->
        val handle = engine.createCustomRenderableWithGeneratedTangents(
            CustomRenderableConfig(
                vertexData = glassesVertexData,
                vertexCount = glassesVertexData.size / (5 * Float.SIZE_BYTES),
                strideBytes = 20,
                attributes = sheepGeometryAttributes(),
                indices = glassesIndexData,
                materialInstanceHandle = glassesInstanceHandle,
                boundingBox = glassesBounds,
                primitiveType = PrimitiveType.TRIANGLES,
            ),
        )
        renderables += SheepRenderable(
            handle = handle,
            baseTransform = multiplyMatrix4(
                translationMatrix(glassCenter.x, glassCenter.y, glassCenter.z),
                multiplyMatrix4(
                    rotationZMatrix(headLayout.glassesRotationDegrees),
                    rotationYMatrix(90f),
                ),
            ),
            explosionOffset = Float3(0f, 0f, 0f),
            buildGroup = SheepBuildGroup.GLASSES,
        )
    }

    val bridgeLength = distance(headLayout.bridgeStart, headLayout.bridgeEnd)
    val (bridgeVertexData, bridgeIndexData) = buildCylinderGeometry(
        radius = 0.018f,
        height = bridgeLength,
        centered = true,
    )
    val bridgeMidpoint = Float3(
        x = (headLayout.bridgeStart.x + headLayout.bridgeEnd.x) * 0.5f,
        y = (headLayout.bridgeStart.y + headLayout.bridgeEnd.y) * 0.5f,
        z = (headLayout.bridgeStart.z + headLayout.bridgeEnd.z) * 0.5f,
    )
    val bridgeHandle = engine.createCustomRenderableWithGeneratedTangents(
        CustomRenderableConfig(
            vertexData = bridgeVertexData,
            vertexCount = bridgeVertexData.size / (5 * Float.SIZE_BYTES),
            strideBytes = 20,
            attributes = sheepGeometryAttributes(),
            indices = bridgeIndexData,
            materialInstanceHandle = glassesInstanceHandle,
            boundingBox = BoundingBox(
                center = Float3(0f, 0f, 0f),
                halfExtent = Float3(bridgeLength * 0.5f, 0.03f, 0.03f),
            ),
            primitiveType = PrimitiveType.TRIANGLES,
        ),
    )
    renderables += SheepRenderable(
        handle = bridgeHandle,
        baseTransform = multiplyMatrix4(
            translationMatrix(bridgeMidpoint.x, bridgeMidpoint.y, bridgeMidpoint.z),
            rotationXMatrix(90f),
        ),
        explosionOffset = Float3(0f, 0f, 0f),
        buildGroup = SheepBuildGroup.GLASSES,
    )

    return renderables.mapIndexed { index, renderable ->
        renderable.copy(explosionOffset = sheepExplosionOffset(renderable.baseTransform, index))
    }
}

private data class HeadLayout(
    val center: Float3,
    val rotationDegrees: Float,
    val glassesRotationDegrees: Float,
    val leftEyeCenter: Float3,
    val rightEyeCenter: Float3,
    val leftPupilCenter: Float3,
    val rightPupilCenter: Float3,
    val eyeRadius: Float,
    val leftGlassCenter: Float3,
    val rightGlassCenter: Float3,
    val glassRadius: Float,
    val bridgeStart: Float3,
    val bridgeEnd: Float3,
)

private fun buildHeadLayout(radius: Float = SheepFluffRadius): HeadLayout {
    val headWidth = SheepHeadRadiusX * 2f
    val headHeight = SheepHeadRadiusY * 2f
    val headTopLeftX = -radius - headWidth / 3f
    val headTopLeftY = -headHeight / 4f
    val headCenter2DX = headTopLeftX + headWidth / 2f
    val headCenter2DY = headTopLeftY + headHeight / 2f
    val eyeDiameter = headWidth * 0.15f
    val eyeRadius = eyeDiameter / 2f
    val glassWidth = headWidth * 0.45f
    val glassRadius = glassWidth / 2f
    val headCenter2D = Offset(headCenter2DX, headCenter2DY)
    val headTiltDegrees = 14f
    val faceRotation = headTiltDegrees
    val eyeHeightOffset = headHeight * 0.48f
    val pupilHeightOffset = eyeHeightOffset
    val eyeSideOffset = eyeDiameter * 0.72f
    val glassHeightOffset = headHeight * 0.62f
    val glassSideOffset = glassRadius * 0.82f
    val headCenter = Float3(headCenter2D.x, -headCenter2D.y, SheepHeadDepth)
    val leftEyeFace = FacePointSpec(sideOffset = -eyeSideOffset, upOffset = eyeHeightOffset)
    val rightEyeFace = FacePointSpec(sideOffset = eyeSideOffset, upOffset = eyeHeightOffset)
    val leftPupilFace = FacePointSpec(sideOffset = -eyeSideOffset, upOffset = pupilHeightOffset)
    val rightPupilFace = FacePointSpec(sideOffset = eyeSideOffset, upOffset = pupilHeightOffset)
    val leftGlassFace = FacePointSpec(sideOffset = -glassSideOffset, upOffset = glassHeightOffset)
    val rightGlassFace = FacePointSpec(sideOffset = glassSideOffset, upOffset = glassHeightOffset)
    val bridgeStartFace = FacePointSpec(
        sideOffset = -glassRadius * 0.34f,
        upOffset = glassHeightOffset - glassRadius * 0.36f,
    )
    val bridgeEndFace = FacePointSpec(
        sideOffset = glassRadius * 0.34f,
        upOffset = glassHeightOffset - glassRadius * 0.36f,
    )

    return HeadLayout(
        center = headCenter,
        rotationDegrees = headTiltDegrees,
        glassesRotationDegrees = faceRotation,
        leftEyeCenter = headFrontSurfacePoint(
            facePoint = leftEyeFace,
            headCenter = headCenter,
            headRotationDegrees = faceRotation,
            forwardOffset = SheepEyeDepthOffset,
        ),
        rightEyeCenter = headFrontSurfacePoint(
            facePoint = rightEyeFace,
            headCenter = headCenter,
            headRotationDegrees = faceRotation,
            forwardOffset = SheepEyeDepthOffset,
        ),
        leftPupilCenter = run {
            val eye = headFrontSurfacePoint(
                facePoint = leftEyeFace,
                headCenter = headCenter,
                headRotationDegrees = faceRotation,
                forwardOffset = SheepEyeDepthOffset,
            )
            // Push pupil forward from eye center (negative X = forward)
            Float3(x = eye.x - eyeRadius * 0.85f, y = eye.y, z = eye.z)
        },
        rightPupilCenter = run {
            val eye = headFrontSurfacePoint(
                facePoint = rightEyeFace,
                headCenter = headCenter,
                headRotationDegrees = faceRotation,
                forwardOffset = SheepEyeDepthOffset,
            )
            Float3(x = eye.x - eyeRadius * 0.85f, y = eye.y, z = eye.z)
        },
        eyeRadius = eyeRadius,
        leftGlassCenter = headFrontSurfacePoint(
            facePoint = leftGlassFace,
            headCenter = headCenter,
            headRotationDegrees = faceRotation,
            forwardOffset = SheepGlassesDepthOffset,
        ),
        rightGlassCenter = headFrontSurfacePoint(
            facePoint = rightGlassFace,
            headCenter = headCenter,
            headRotationDegrees = faceRotation,
            forwardOffset = SheepGlassesDepthOffset,
        ),
        glassRadius = glassRadius,
        bridgeStart = headFrontSurfacePoint(
            facePoint = bridgeStartFace,
            headCenter = headCenter,
            headRotationDegrees = faceRotation,
            forwardOffset = SheepBridgeDepthOffset,
        ),
        bridgeEnd = headFrontSurfacePoint(
            facePoint = bridgeEndFace,
            headCenter = headCenter,
            headRotationDegrees = faceRotation,
            forwardOffset = SheepBridgeDepthOffset,
        ),
    )
}

private fun fluffSphereSpecs(
    fluffStyle: FluffStyle,
    radius: Float,
): List<FluffChunk> {
    val percentages = fluffStyle.fluffChunksPercentages
    if (percentages.isEmpty()) return emptyList()

    val boundaryAngles = buildList {
        var total = 0f
        percentages.forEach { percentage ->
            total += (percentage / 100.0).toFloat() * (PI.toFloat() * 2f)
            add(total)
        }
    }
    val centeredAngles = boundaryAngles.mapIndexed { index, angle ->
        val previousAngle = if (index == 0) 0f else boundaryAngles[index - 1]
        previousAngle + (angle - previousAngle) * 0.5f
    }
    val shellRadius = radius * SheepFluffShellRadius
    return buildList {
        add(FluffChunk(Float3(0f, shellRadius * 0.84f, 0f), SheepFluffChunkRadius * 1.10f))
        addAll(fluffBandSpecs(sampleAngles(centeredAngles, 5), shellRadius, 44f, 0))
        addAll(fluffBandSpecs(boundaryAngles, shellRadius, 16f, 1))
        addAll(fluffBandSpecs(centeredAngles, shellRadius, -16f, 2))
        addAll(
            fluffBandSpecs(
                sampleAngles(boundaryAngles, 5, PI.toFloat() / 10f),
                shellRadius,
                -44f,
                3
            )
        )
        add(FluffChunk(Float3(0f, -shellRadius * 0.84f, 0f), SheepFluffChunkRadius * 1.06f))
    }
}

private data class FacePointSpec(
    val sideOffset: Float,
    val upOffset: Float,
)

private fun headFrontSurfacePoint(
    facePoint: FacePointSpec,
    headCenter: Float3,
    headRotationDegrees: Float,
    forwardOffset: Float,
): Float3 {
    val localY = facePoint.upOffset
    val localZ = facePoint.sideOffset
    val normalized =
        (localY * localY) / (SheepHeadRadiusY * SheepHeadRadiusY) +
                (localZ * localZ) / (SheepHeadRadiusZ * SheepHeadRadiusZ)
    val localX = -SheepHeadRadiusX * sqrt((1f - normalized).coerceAtLeast(0f)) - forwardOffset
    val radians = headRotationDegrees * PI.toFloat() / 180f
    val cosAngle = cos(radians)
    val sinAngle = sin(radians)
    return Float3(
        x = headCenter.x + localX * cosAngle - localY * sinAngle,
        y = headCenter.y + localX * sinAngle + localY * cosAngle,
        z = headCenter.z + localZ,
    )
}

private fun fluffBandSpecs(
    angles: List<Float>,
    shellRadius: Float,
    latitudeDegrees: Float,
    bandIndex: Int,
): List<FluffChunk> {
    val latitudeRadians = latitudeDegrees * PI.toFloat() / 180f
    val horizontalRadius = shellRadius * cos(latitudeRadians)
    val y = shellRadius * sin(latitudeRadians)
    return angles.mapIndexed { index, angle ->
        val radiusScale = 0.86f + 0.32f * (0.5f + 0.5f * sin(index * 1.7f + bandIndex * 0.9f))
        val radialInset = 0.88f - (radiusScale - 1f) * 0.38f
        FluffChunk(
            center = Float3(
                x = horizontalRadius * radialInset * cos(angle),
                y = y,
                z = horizontalRadius * radialInset * sin(angle),
            ),
            radius = SheepFluffChunkRadius * radiusScale,
        )
    }
}

private fun sampleAngles(
    sourceAngles: List<Float>,
    count: Int,
    offsetRadians: Float = 0f,
): List<Float> {
    if (sourceAngles.isEmpty() || count <= 0) return emptyList()
    if (count >= sourceAngles.size) return sourceAngles.map { it + offsetRadians }
    return List(count) { index ->
        val sourceIndex = index * sourceAngles.size / count
        sourceAngles[sourceIndex] + offsetRadians
    }
}

private fun rotationXMatrix(degrees: Float): FloatArray {
    val radians = degrees * PI.toFloat() / 180f
    val cosAngle = cos(radians)
    val sinAngle = sin(radians)
    return floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, cosAngle, sinAngle, 0f,
        0f, -sinAngle, cosAngle, 0f,
        0f, 0f, 0f, 1f,
    )
}

private fun rotationYMatrix(degrees: Float): FloatArray {
    val radians = degrees * PI.toFloat() / 180f
    val cosAngle = cos(radians)
    val sinAngle = sin(radians)
    return floatArrayOf(
        cosAngle, 0f, -sinAngle, 0f,
        0f, 1f, 0f, 0f,
        sinAngle, 0f, cosAngle, 0f,
        0f, 0f, 0f, 1f,
    )
}

private fun buildSemiDiskGeometry(
    radius: Float,
    thickness: Float,
    segments: Int = 24,
): Pair<ByteArray, ShortArray> {
    val halfThickness = thickness * 0.5f
    val vertices = ArrayList<Float>()
    val indices = ArrayList<Short>()

    fun addVertex(x: Float, y: Float, z: Float, u: Float, v: Float): Short {
        val index = (vertices.size / 5).toShort()
        vertices += x
        vertices += y
        vertices += z
        vertices += u
        vertices += v
        return index
    }

    val topCenter = addVertex(0f, 0f, halfThickness, 0.5f, 0.5f)
    val topRim = ArrayList<Short>(segments + 1)
    for (segment in 0..segments) {
        val t = segment.toFloat() / segments
        val angle = t * PI.toFloat()
        val x = radius * cos(angle)
        val y = -radius * sin(angle)
        topRim += addVertex(
            x = x,
            y = y,
            z = halfThickness,
            u = 0.5f + x / (radius * 2f),
            v = 0.5f - y / (radius * 2f),
        )
    }
    for (segment in 0 until segments) {
        indices += topCenter
        indices += topRim[segment + 1]
        indices += topRim[segment]
    }

    val bottomCenter = addVertex(0f, 0f, -halfThickness, 0.5f, 0.5f)
    val bottomRim = ArrayList<Short>(segments + 1)
    for (segment in 0..segments) {
        val t = segment.toFloat() / segments
        val angle = t * PI.toFloat()
        val x = radius * cos(angle)
        val y = -radius * sin(angle)
        bottomRim += addVertex(
            x = x,
            y = y,
            z = -halfThickness,
            u = 0.5f + x / (radius * 2f),
            v = 0.5f - y / (radius * 2f),
        )
    }
    for (segment in 0 until segments) {
        indices += bottomCenter
        indices += bottomRim[segment]
        indices += bottomRim[segment + 1]
    }

    for (segment in 0 until segments) {
        val topCurrent = topRim[segment]
        val topNext = topRim[segment + 1]
        val bottomCurrent = bottomRim[segment]
        val bottomNext = bottomRim[segment + 1]
        indices += topCurrent
        indices += topNext
        indices += bottomCurrent
        indices += topNext
        indices += bottomNext
        indices += bottomCurrent
    }

    val rightTop = addVertex(radius, 0f, halfThickness, 1f, 1f)
    val rightBottom = addVertex(radius, 0f, -halfThickness, 1f, 0f)
    val leftTop = addVertex(-radius, 0f, halfThickness, 0f, 1f)
    val leftBottom = addVertex(-radius, 0f, -halfThickness, 0f, 0f)
    indices += rightTop
    indices += rightBottom
    indices += leftTop
    indices += leftTop
    indices += rightBottom
    indices += leftBottom

    return vertices.toFloatArray().toByteArray() to indices.toShortArray()
}

private fun buildCylinderGeometry(
    radius: Float,
    height: Float,
    segments: Int = 16,
    centered: Boolean = false,
): Pair<ByteArray, ShortArray> {
    val topY = if (centered) height * 0.5f else 0f
    val bottomY = if (centered) -height * 0.5f else -height
    val vertices = ArrayList<Float>((segments + 1) * 20)
    val indices = ArrayList<Short>(segments * 12)

    fun addVertex(x: Float, y: Float, z: Float, u: Float, v: Float): Short {
        val index = (vertices.size / 5).toShort()
        vertices += x
        vertices += y
        vertices += z
        vertices += u
        vertices += v
        return index
    }

    for (segment in 0..segments) {
        val t = segment.toFloat() / segments
        val angle = t * PI.toFloat() * 2f
        val x = cos(angle) * radius
        val z = sin(angle) * radius
        addVertex(x, topY, z, t, 1f)
        addVertex(x, bottomY, z, t, 0f)
    }
    for (segment in 0 until segments) {
        val base = (segment * 2).toShort()
        indices.add(base)
        indices.add((base + 2).toShort())
        indices.add((base + 1).toShort())
        indices.add((base + 2).toShort())
        indices.add((base + 3).toShort())
        indices.add((base + 1).toShort())
    }

    val topCenter = addVertex(0f, topY, 0f, 0.5f, 0.5f)
    val topStart = (vertices.size / 5).toShort()
    for (segment in 0..segments) {
        val t = segment.toFloat() / segments
        val angle = t * PI.toFloat() * 2f
        val x = cos(angle) * radius
        val z = sin(angle) * radius
        addVertex(
            x = x,
            y = topY,
            z = z,
            u = 0.5f + x / (radius * 2f),
            v = 0.5f + z / (radius * 2f),
        )
    }
    for (segment in 0 until segments) {
        val current = (topStart + segment.toShort()).toShort()
        val next = (current + 1).toShort()
        indices.add(topCenter)
        indices.add(next)
        indices.add(current)
    }

    val bottomCenter = addVertex(0f, bottomY, 0f, 0.5f, 0.5f)
    val bottomStart = (vertices.size / 5).toShort()
    for (segment in 0..segments) {
        val t = segment.toFloat() / segments
        val angle = t * PI.toFloat() * 2f
        val x = cos(angle) * radius
        val z = sin(angle) * radius
        addVertex(
            x = x,
            y = bottomY,
            z = z,
            u = 0.5f + x / (radius * 2f),
            v = 0.5f + z / (radius * 2f),
        )
    }
    for (segment in 0 until segments) {
        val current = (bottomStart + segment.toShort()).toShort()
        val next = (current + 1).toShort()
        indices.add(bottomCenter)
        indices.add(current)
        indices.add(next)
    }

    return vertices.toFloatArray().toByteArray() to indices.toShortArray()
}

private fun buildTorusSegmentGeometry(
    arcRadius: Float,
    tubeRadius: Float,
    sweepDegrees: Float = 180f,
    arcSegments: Int = 20,
    tubeSegments: Int = 10,
): Pair<ByteArray, ShortArray> {
    val vertices = ArrayList<Float>((arcSegments + 1) * (tubeSegments + 1) * 5)
    val indices = ArrayList<Short>(arcSegments * tubeSegments * 6)

    fun addVertex(x: Float, y: Float, z: Float, u: Float, v: Float) {
        vertices += x
        vertices += y
        vertices += z
        vertices += u
        vertices += v
    }

    for (arcStep in 0..arcSegments) {
        val arcT = arcStep.toFloat() / arcSegments
        val arcAngle = arcT * sweepDegrees * PI.toFloat() / 180f
        val cosArc = cos(arcAngle)
        val sinArc = sin(arcAngle)
        for (tubeStep in 0..tubeSegments) {
            val tubeT = tubeStep.toFloat() / tubeSegments
            val tubeAngle = tubeT * PI.toFloat() * 2f
            val cosTube = cos(tubeAngle)
            val sinTube = sin(tubeAngle)
            val radial = arcRadius + tubeRadius * cosTube
            addVertex(
                x = radial * cosArc,
                y = -radial * sinArc,
                z = tubeRadius * sinTube,
                u = arcT,
                v = tubeT,
            )
        }
    }

    val rowSize = tubeSegments + 1
    for (arcStep in 0 until arcSegments) {
        for (tubeStep in 0 until tubeSegments) {
            val current = (arcStep * rowSize + tubeStep).toShort()
            val nextRow = (current + rowSize.toShort()).toShort()
            indices.add(current)
            indices.add(nextRow)
            indices.add((current + 1).toShort())
            indices.add((current + 1).toShort())
            indices.add(nextRow)
            indices.add((nextRow + 1).toShort())
        }
    }

    return vertices.toFloatArray().toByteArray() to indices.toShortArray()
}

private fun sheepGeometryAttributes(): List<VertexAttributeLayout> = listOf(
    VertexAttributeLayout(
        attribute = VertexAttribute.POSITION,
        type = AttributeDataType.FLOAT3,
        offsetBytes = 0,
    ),
    VertexAttributeLayout(
        attribute = VertexAttribute.UV0,
        type = AttributeDataType.FLOAT2,
        offsetBytes = 12,
    ),
)


private fun point3(x: Float, y: Float, z: Float): Float3 = Float3(x, y, z)

private fun rotateAroundZ(point: Float3, center: Float3, degrees: Float): Float3 {
    val radians = degrees * PI.toFloat() / 180f
    val cosAngle = cos(radians)
    val sinAngle = sin(radians)
    val translatedX = point.x - center.x
    val translatedY = point.y - center.y
    return Float3(
        x = center.x + translatedX * cosAngle - translatedY * sinAngle,
        y = center.y + translatedX * sinAngle + translatedY * cosAngle,
        z = point.z,
    )
}

private fun identityMatrix4(): FloatArray = floatArrayOf(
    1f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f,
    0f, 0f, 1f, 0f,
    0f, 0f, 0f, 1f,
)

private fun translationMatrix(x: Float, y: Float, z: Float): FloatArray = floatArrayOf(
    1f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f,
    0f, 0f, 1f, 0f,
    x, y, z, 1f,
)

private fun scaleMatrix(x: Float, y: Float, z: Float): FloatArray = floatArrayOf(
    x, 0f, 0f, 0f,
    0f, y, 0f, 0f,
    0f, 0f, z, 0f,
    0f, 0f, 0f, 1f,
)

private fun rotationZMatrix(degrees: Float): FloatArray {
    val radians = degrees * PI.toFloat() / 180f
    val cosAngle = cos(radians)
    val sinAngle = sin(radians)
    return floatArrayOf(
        cosAngle, sinAngle, 0f, 0f,
        -sinAngle, cosAngle, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f,
    )
}

private fun multiplyMatrix4(left: FloatArray, right: FloatArray): FloatArray {
    val result = FloatArray(16)
    for (column in 0 until 4) {
        for (row in 0 until 4) {
            result[column * 4 + row] =
                left[row] * right[column * 4] +
                        left[4 + row] * right[column * 4 + 1] +
                        left[8 + row] * right[column * 4 + 2] +
                        left[12 + row] * right[column * 4 + 3]
        }
    }
    return result
}

private fun sheepTransformForProgress(
    rootTransform: FloatArray,
    renderable: SheepRenderable,
    explosionProgress: Float,
    explosionDistanceScale: Float,
    buildProgress: Float,
): FloatArray {
    val buildTransform = when {
        buildProgress >= 1f - 1e-4f -> identityMatrix4()
        buildProgress <= 1e-4f -> scaleMatrix(0f, 0f, 0f)
        renderable.buildGroup == SheepBuildGroup.LEGS -> {
            // Legs slide down from fluff: start at y=0 (inside fluff), end at base position
            val legDrop = 0.56f
            translationMatrix(0f, legDrop * (1f - buildProgress), 0f)
        }

        renderable.buildGroup == SheepBuildGroup.GLASSES -> {
            // Glasses appear (scale in) and slide down from above the head onto the eyes
            val t = smoothStep(buildProgress)
            val slideDistance = 0.5f
            multiplyMatrix4(
                translationMatrix(0f, slideDistance * (1f - t), 0f),
                scaleMatrix(t, t, t),
            )
        }

        else -> {
            // Scale in from center of the renderable's position
            val s = smoothStep(buildProgress)
            scaleMatrix(s, s, s)
        }
    }

    val explosionTransform = if (explosionProgress <= 1e-4f) {
        identityMatrix4()
    } else {
        val scaledProgress = explosionProgress * explosionDistanceScale
        translationMatrix(
            x = renderable.explosionOffset.x * scaledProgress,
            y = renderable.explosionOffset.y * scaledProgress,
            z = renderable.explosionOffset.z * scaledProgress,
        )
    }

    return multiplyMatrix4(
        rootTransform,
        multiplyMatrix4(
            explosionTransform,
            multiplyMatrix4(renderable.baseTransform, buildTransform),
        ),
    )
}

private fun sheepExplosionOffset(baseTransform: FloatArray, index: Int): Float3 {
    val center = extractTranslation(baseTransform)
    val angle = index * 2.3999631f
    val fallbackDirection = Float3(
        x = cos(angle),
        y = 0.4f + 0.2f * sin(angle * 1.7f),
        z = sin(angle),
    )
    val direction = Float3(
        x = center.x * 1.15f + fallbackDirection.x * 0.34f,
        y = center.y + SheepExplosionUpBias + fallbackDirection.y * 0.2f,
        z = center.z * 1.15f + fallbackDirection.z * 0.34f,
    ).normalizedSheep()
    val distanceScale = 1.05f + 0.5f * (0.5f + 0.5f * sin(index * 1.37f))
    return Float3(
        x = direction.x * SheepExplosionDistance * distanceScale,
        y = direction.y * SheepExplosionDistance * distanceScale,
        z = direction.z * SheepExplosionDistance * distanceScale,
    )
}

private fun Float3.normalizedSheep(): Float3 {
    val length = sqrt(x * x + y * y + z * z)
    if (length <= 1e-6f) return Float3(0f, 0f, 1f)
    return Float3(x / length, y / length, z / length)
}

private fun extractTranslation(matrix: FloatArray): Float3 = Float3(
    x = matrix[12],
    y = matrix[13],
    z = matrix[14],
)

private fun smoothStep(value: Float): Float {
    val clamped = value.coerceIn(0f, 1f)
    return clamped * clamped * (3f - 2f * clamped)
}

private fun distance(from: Float3, to: Float3): Float {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val dz = to.z - from.z
    return sqrt(dx * dx + dy * dy + dz * dz)
}
