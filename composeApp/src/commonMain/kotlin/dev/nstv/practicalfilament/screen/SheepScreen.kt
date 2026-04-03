package dev.nstv.practicalfilament.screen

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import dev.nstv.composablesheep.library.model.DefaultHeadRotationAngle
import dev.nstv.composablesheep.library.model.FluffStyle
import dev.nstv.practicalfilament.components.ParameterInputField
import dev.nstv.practicalfilament.components.SheepFluffMaterial
import dev.nstv.practicalfilament.filament.AttributeDataType
import dev.nstv.practicalfilament.filament.BoundingBox
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.Color
import dev.nstv.practicalfilament.filament.CustomRenderableConfig
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.LightConfig
import dev.nstv.practicalfilament.filament.LightType
import dev.nstv.practicalfilament.filament.MaterialBlendingMode
import dev.nstv.practicalfilament.filament.PrimitiveType
import dev.nstv.practicalfilament.filament.VertexAttribute
import dev.nstv.practicalfilament.filament.VertexAttributeLayout
import dev.nstv.practicalfilament.filament.material.MaterialParameter
import dev.nstv.practicalfilament.filament.material.MaterialParameterDefinition
import dev.nstv.practicalfilament.filament.material.loadMaterialOnEngine
import dev.nstv.practicalfilament.filament.toByteArray
import dev.nstv.practicalfilament.theme.Grid
import practicalfilament.composeapp.generated.resources.Res
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

private const val SheepIndirectLightPath = "files/envs/pillars_2k/pillars_2k_ibl.ktx"
private const val SheepSkyboxPath = "files/envs/pillars_2k/pillars_2k_skybox.ktx"
private const val SheepBodyMaterialPath = "files/materials/sheepBody.filamat"
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

private data class FluffChunk(
    val center: Float3,
    val radius: Float,
)

private val SheepBaseLights = listOf(
    LightConfig(
        type = LightType.SUN,
        color = Color(0.98f, 0.94f, 0.90f),
        intensity = 100_000f,
        direction = Float3(0.45f, -1f, -0.6f),
        sunAngularRadius = 1.9f,
        sunHaloSize = 10f,
        sunHaloFalloff = 80f,
    ),
    LightConfig(
        type = LightType.DIRECTIONAL,
        color = Color(0.78f, 0.84f, 1f),
        intensity = 28_000f,
        direction = Float3(-1f, -0.35f, 0.8f),
    ),
)

private data class SheepRenderable(
    val handle: Int,
    val baseTransform: FloatArray,
    val explosionOffset: Float3,
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

private data class SheepQuaternion(
    val x: Float,
    val y: Float,
    val z: Float,
    val w: Float,
) {
    operator fun times(other: SheepQuaternion): SheepQuaternion {
        return multiplyRaw(other).normalized()
    }

    private fun multiplyRaw(other: SheepQuaternion): SheepQuaternion {
        return SheepQuaternion(
            x = w * other.x + x * other.w + y * other.z - z * other.y,
            y = w * other.y - x * other.z + y * other.w + z * other.x,
            z = w * other.z + x * other.y - y * other.x + z * other.w,
            w = w * other.w - x * other.x - y * other.y - z * other.z,
        )
    }

    fun normalized(): SheepQuaternion {
        val magnitude = sqrt(x * x + y * y + z * z + w * w)
        if (magnitude <= 1e-6f) return SheepQuaternion(0f, 0f, 0f, 1f)
        return SheepQuaternion(x / magnitude, y / magnitude, z / magnitude, w / magnitude)
    }

    fun conjugate(): SheepQuaternion = SheepQuaternion(-x, -y, -z, w)

    fun rotate(vector: Float3): Float3 {
        val quaternionVector = SheepQuaternion(vector.x, vector.y, vector.z, 0f)
        val rotated = multiplyRaw(quaternionVector).multiplyRaw(conjugate())
        return Float3(rotated.x, rotated.y, rotated.z)
    }
}

@Composable
fun SheepScreen(
    modifier: Modifier = Modifier,
) {
    var filamentEngine by remember { mutableStateOf<FilamentEngine?>(null) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var orientation by remember { mutableStateOf(initialSheepOrientation()) }
    var cameraDistance by remember { mutableFloatStateOf(SheepDefaultCameraDistance) }
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
            sheepCameraForOrientation(
                orientation = orientation,
                distance = cameraDistance,
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

        var previousFrameNanos = 0L
        while (true) {
            val frameNanos = withFrameNanos { it }
            val seconds = frameNanos / 1_000_000_000f
            val deltaSeconds = if (previousFrameNanos == 0L) {
                1f / 60f
            } else {
                ((frameNanos - previousFrameNanos) / 1_000_000_000f).coerceAtMost(0.1f)
            }
            previousFrameNanos = frameNanos
            explosionState.step(deltaSeconds)
            val bobOffset = if (animationSpeed <= 0.01f) {
                0f
            } else {
                sin(seconds * (1.6f + animationSpeed * 2.2f)) * 0.14f * animationSpeed
            }
            val swayDegrees = if (animationSpeed <= 0.01f) {
                0f
            } else {
                sin(seconds * (1.1f + animationSpeed * 1.6f)) * 6f * animationSpeed
            }
            val rootTransform = multiplyMatrix4(
                translationMatrix(0f, bobOffset, 0f),
                rotationZMatrix(swayDegrees),
            )
            val explosionProgress = smoothStep(explosionState.progress)
            renderables.forEach { renderable ->
                currentEngine.setRenderableTransform(
                    renderable.handle,
                    sheepTransformForProgress(
                        rootTransform = rootTransform,
                        renderable = renderable,
                        explosionProgress = explosionProgress,
                        explosionDistanceScale = explosionDistanceScale,
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
                    .pointerInput(renderables.size, viewportSize) {
                        detectTransformGestures(
                            panZoomLock = true,
                        ) { centroid, pan, zoom, _ ->
                            val previousCentroid = centroid - pan
                            val previousPoint = previousCentroid.toSheepArcballPoint(viewportSize)
                            val currentPoint = centroid.toSheepArcballPoint(viewportSize)
                            if (previousPoint != null && currentPoint != null) {
                                orientation =
                                    sheepArcballDelta(previousPoint, currentPoint) * orientation
                            }

                            if (zoom > 0f) {
                                cameraDistance = (cameraDistance / zoom).coerceIn(
                                    SheepMinCameraDistance,
                                    SheepMaxCameraDistance,
                                )
                            }
                        }
                    },
                camera = sheepCameraForOrientation(
                    orientation = orientation,
                    distance = cameraDistance,
                ),
                lights = SheepBaseLights,
                backgroundColor = Color(0f, 0f, 0f, 1f),
                onEngineReady = { engine ->
                    val (fluffInstanceHandle, definitions, parameters) = loadMaterialOnEngine(
                        engine = engine,
                        material = SheepFluffMaterial,
                    )
                    val indirectLightHandle = engine.loadIndirectLight(Res.getUri(SheepIndirectLightPath))
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
                    orientation = initialSheepOrientation()
                    cameraDistance = SheepDefaultCameraDistance
                    explosionState.reset()
                    explosionProgressControl = 0f
                    supportNotice = null
                    fluffMaterialInstanceHandle = fluffInstanceHandle
                    materialParameterDefinitions = definitions
                    materialParameters = parameters

                    val bodyMaterialHandle = engine.loadMaterial(Res.getUri(SheepBodyMaterialPath))
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
                        fluffInstanceHandle = fluffInstanceHandle,
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
    val headTopLeftX = -radius - headWidth /3f
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
        leftPupilCenter = headFrontSurfacePoint(
            facePoint = leftPupilFace,
            headCenter = headCenter,
            headRotationDegrees = faceRotation,
            forwardOffset = SheepEyeDepthOffset + SheepPupilDepthOffset,
        ),
        rightPupilCenter = headFrontSurfacePoint(
            facePoint = rightPupilFace,
            headCenter = headCenter,
            headRotationDegrees = faceRotation,
            forwardOffset = SheepEyeDepthOffset + SheepPupilDepthOffset,
        ),
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
        addAll(fluffBandSpecs(sampleAngles(boundaryAngles, 5, PI.toFloat() / 10f), shellRadius, -44f, 3))
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

private fun initialSheepOrientation(): SheepQuaternion = SheepQuaternion(0f, 0f, 0f, 1f)

private fun sheepCameraForOrientation(
    orientation: SheepQuaternion,
    distance: Float = SheepDefaultCameraDistance,
): CameraConfig {
    val orbit = orientation.conjugate()
    return CameraConfig(
        position = orbit.rotate(Float3(0f, 0f, distance)),
        lookAt = Float3(0f, 0f, 0f),
        up = orbit.rotate(Float3(0f, 1f, 0f)),
        fovDegrees = 45.0,
    )
}

private fun sheepArcballDelta(from: Float3, to: Float3): SheepQuaternion {
    val dot = (from sheepDot to).coerceIn(-1f, 1f)
    val cross = from sheepCross to
    val magnitude = cross.lengthSheep()
    if (magnitude <= 1e-6f) {
        return if (dot < 0f) {
            val fallbackAxis = if (kotlin.math.abs(from.x) < 0.9f) {
                Float3(1f, 0f, 0f) sheepCross from
            } else {
                Float3(0f, 1f, 0f) sheepCross from
            }.normalizedSheep()
            SheepQuaternion(fallbackAxis.x, fallbackAxis.y, fallbackAxis.z, 0f)
        } else {
            SheepQuaternion(0f, 0f, 0f, 1f)
        }
    }
    return SheepQuaternion(
        x = cross.x,
        y = cross.y,
        z = cross.z,
        w = 1f + dot,
    ).normalized()
}

private fun Offset.toSheepArcballPoint(size: IntSize): Float3? {
    if (size.width <= 0 || size.height <= 0) return null
    val scale = minOf(size.width, size.height).toFloat()
    val x = (2f * this.x - size.width) / scale
    val y = (size.height - 2f * this.y) / scale
    val lengthSquared = x * x + y * y
    return if (lengthSquared <= 1f) {
        Float3(x, y, sqrt(1f - lengthSquared))
    } else {
        val inverseLength = 1f / sqrt(lengthSquared)
        Float3(x * inverseLength, y * inverseLength, 0f)
    }
}

private infix fun Float3.sheepDot(other: Float3): Float = x * other.x + y * other.y + z * other.z

private infix fun Float3.sheepCross(other: Float3): Float3 = Float3(
    x = y * other.z - z * other.y,
    y = z * other.x - x * other.z,
    z = x * other.y - y * other.x,
)

private fun Float3.lengthSheep(): Float = sqrt(x * x + y * y + z * z)

private fun Float3.normalizedSheep(): Float3 {
    val length = lengthSheep()
    if (length <= 1e-6f) return Float3(0f, 0f, 1f)
    return Float3(x / length, y / length, z / length)
}

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
): FloatArray {
    if (explosionProgress <= 1e-4f) {
        return multiplyMatrix4(rootTransform, renderable.baseTransform)
    }
    val scaledProgress = explosionProgress * explosionDistanceScale
    val explosionTransform = translationMatrix(
        x = renderable.explosionOffset.x * scaledProgress,
        y = renderable.explosionOffset.y * scaledProgress,
        z = renderable.explosionOffset.z * scaledProgress,
    )
    return multiplyMatrix4(
        rootTransform,
        multiplyMatrix4(explosionTransform, renderable.baseTransform),
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
