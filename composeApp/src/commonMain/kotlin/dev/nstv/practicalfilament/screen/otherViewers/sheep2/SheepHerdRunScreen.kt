package dev.nstv.practicalfilament.screen.otherViewers.sheep2

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.nstv.practicalfilament.components.materials.sheepBodyMaterial
import dev.nstv.practicalfilament.components.materials.sheepFluffMaterial
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.FilamentColor
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentHostViewMode
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.LightConfig
import dev.nstv.practicalfilament.filament.LightType
import dev.nstv.practicalfilament.filament.ProjectionType
import dev.nstv.practicalfilament.filament.material.MaterialParameter
import dev.nstv.practicalfilament.filament.withFrameSeconds
import dev.nstv.practicalfilament.theme.Grid
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import practicalfilament.composeapp.generated.resources.Res

private const val SheepHerdCount = 8
private const val SheepHerdOrthoZoom = 2.7
private const val SheepHerdCameraDistance = 8f
private const val SheepOffstageMargin = 2.2f
private val SelectorSize = 78.dp

private val SheepHerdCamera = CameraConfig(
    position = Float3(0f, 0f, SheepHerdCameraDistance),
    lookAt = Float3(0f, 0f, 0f),
    projectionType = ProjectionType.ORTHO,
    orthoZoom = SheepHerdOrthoZoom,
    near = 0.1,
    far = 24.0,
)

private val SheepHerdLights = listOf(
    LightConfig(
        type = LightType.SUN,
        color = FilamentColor(0.98f, 0.94f, 0.9f),
        intensity = 150_000f,
        direction = Float3(0.4f, -1f, -0.55f),
        sunAngularRadius = 1.9f,
        sunHaloSize = 10f,
        sunHaloFalloff = 80f,
    ),
    LightConfig(
        type = LightType.DIRECTIONAL,
        color = FilamentColor(0.76f, 0.84f, 1f),
        intensity = 38_000f,
        direction = Float3(-0.8f, -0.28f, 0.8f),
    ),
)

private enum class SelectorHandle {
    START,
    END,
}

private data class StageRunPath(
    val screenStart: Offset,
    val screenEnd: Offset,
    val stageEntry: Offset,
    val stageExit: Offset,
    val worldStart: Float3,
    val worldEnd: Float3,
    val direction: Float3,
    val pathLength: Float,
)

private data class SheepActor(
    val pieces: List<SheepRigPiece>,
    val index: Int,
    val scale: Float,
    val laneOffset: Float,
    val depthOffset: Float,
    val arcHeight: Float,
    val bobAmplitude: Float,
    val speedMultiplier: Float,
    val startDelay: Float,
    val bobPhase: Float,
    val noiseSeed: Int,
)

@Composable
fun SheepHerdRunScreen(
    modifier: Modifier = Modifier,
) {
    var engine by remember { mutableStateOf<FilamentEngine?>(null) }
    var herdActors by remember { mutableStateOf<List<SheepActor>>(emptyList()) }
    var startYFraction by remember { mutableStateOf(0.5f) }
    var endYFraction by remember { mutableStateOf(0.5f) }
    var runToken by remember { mutableIntStateOf(0) }
    var runInProgress by remember { mutableStateOf(false) }
    var activeRunPath by remember { mutableStateOf<StageRunPath?>(null) }
    var activeSelector by remember { mutableStateOf<SelectorHandle?>(null) }
    val latestRunToken by rememberUpdatedState(runToken)
    val latestRunInProgress by rememberUpdatedState(runInProgress)
    val latestRunPath by rememberUpdatedState(activeRunPath)

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val containerSize = IntSize(constraints.maxWidth, constraints.maxHeight)
        val stageRect = remember(containerSize) {
            Rect(0f, 0f, containerSize.width.toFloat(), containerSize.height.toFloat())
        }
        val startPoint = remember(startYFraction, containerSize) {
            Offset(0f, startYFraction * containerSize.height.toFloat())
        }
        val endPoint = remember(endYFraction, containerSize) {
            Offset(containerSize.width.toFloat(), endYFraction * containerSize.height.toFloat())
        }
        val candidateRunPath = remember(startYFraction, endYFraction, containerSize) {
            computeRunPath(start = startPoint, end = endPoint, stageRect = stageRect)
        }
        val buttonEnabled = candidateRunPath != null && !runInProgress && herdActors.isNotEmpty()

        LaunchedEffect(engine, herdActors) {
            val currentEngine = engine ?: return@LaunchedEffect
            val actors = herdActors
            if (actors.isEmpty()) return@LaunchedEffect

            val animationControls = List(actors.size) { i ->
                val actor = actors[i]
                Sheep2AnimationControls(
                    animationEnabled = true,
                    pulseAmount = 0.66f,
                    noiseAmount = 0.26f,
                    noiseFrequency = 0.9f + actor.index * 0.04f,
                    driftAmount = 0.16f,
                    followThroughAmount = 0.78f,
                    headAndLegMotionEnabled = true,
                    noiseSeed = actor.noiseSeed,
                )
            }
            var localRunToken = runToken
            var runElapsed = 0f
            var hiddenTransformsApplied = false

            withFrameSeconds { _, deltaSeconds ->
                if (latestRunToken != localRunToken) {
                    localRunToken = latestRunToken
                    runElapsed = 0f
                    hiddenTransformsApplied = false
                }

                val currentRunPath = latestRunPath
                if (!latestRunInProgress || currentRunPath == null) {
                    if (!hiddenTransformsApplied) {
                        actors.forEachIndexed { index, actor ->
                            val hiddenTransform = hiddenActorTransform(index)
                            actor.pieces.forEach { piece ->
                                currentEngine.setRenderableTransform(piece.handle, hiddenTransform)
                            }
                        }
                        currentEngine.requestFrame()
                        hiddenTransformsApplied = true
                    }
                    return@withFrameSeconds
                }

                hiddenTransformsApplied = false
                val runPath = currentRunPath
                runElapsed += deltaSeconds
                val lateral = Float3(-runPath.direction.y, runPath.direction.x, 0f)
                val backward = Float3(-runPath.direction.x, -runPath.direction.y, 0f)
                var anySheepOnStage = false
                var allSheepFinished = true

                actors.forEachIndexed { actorIndex, actor ->
                    val actorTime = max(0f, runElapsed - actor.startDelay)
                    val speed = (2.45f + actor.speedMultiplier * 0.55f).coerceAtLeast(1.7f)
                    val totalDistance = runPath.pathLength + SheepOffstageMargin * 2f + actor.scale * 0.8f
                    val distanceAlongPath = (actorTime * speed).coerceIn(0f, totalDistance)
                    if (distanceAlongPath < totalDistance) {
                        allSheepFinished = false
                    }
                    val centerDistance = distanceAlongPath - SheepOffstageMargin
                    val progressOnLine = (centerDistance / runPath.pathLength).coerceIn(0f, 1f)
                    val linePoint = if (runPath.pathLength <= 1e-6f) {
                        runPath.worldStart
                    } else {
                        val basePoint = lerp(runPath.worldStart, runPath.worldEnd, progressOnLine)
                        when {
                            centerDistance < 0f -> Float3(
                                x = basePoint.x + backward.x * abs(centerDistance),
                                y = basePoint.y + backward.y * abs(centerDistance),
                                z = basePoint.z,
                            )

                            centerDistance > runPath.pathLength -> Float3(
                                x = basePoint.x - backward.x * (centerDistance - runPath.pathLength),
                                y = basePoint.y - backward.y * (centerDistance - runPath.pathLength),
                                z = basePoint.z,
                            )

                            else -> basePoint
                        }
                    }
                    val arc = sin(progressOnLine * PI.toFloat()) * actor.arcHeight
                    val bob = sin(actorTime * 8.2f + actor.bobPhase) * actor.bobAmplitude
                    val worldCenter = Float3(
                        x = linePoint.x + lateral.x * actor.laneOffset,
                        y = linePoint.y + lateral.y * actor.laneOffset + arc + bob,
                        z = actor.depthOffset,
                    )
                    val worldTransform = actorWorldTransform(
                        center = worldCenter,
                        direction = runPath.direction,
                        scale = actor.scale,
                    )
                    val localTime = actorTime * (0.92f + actor.speedMultiplier * 0.14f) + actor.bobPhase
                    val controls = animationControls[actorIndex]
                    actor.pieces.forEach { piece ->
                        currentEngine.setRenderableTransform(
                            piece.handle,
                            buildSheep2PieceTransform(
                                piece = piece,
                                timeSeconds = localTime,
                                controls = controls,
                                worldTransform = worldTransform,
                            ),
                        )
                    }

                    if (distanceAlongPath in SheepOffstageMargin..(runPath.pathLength + SheepOffstageMargin)) {
                        anySheepOnStage = true
                    }
                }

                currentEngine.requestFrame()

                if (allSheepFinished && !anySheepOnStage) {
                    runInProgress = false
                    activeRunPath = null
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            FilamentView(
                modifier = Modifier.fillMaxSize(),
                camera = SheepHerdCamera,
                lights = SheepHerdLights,
                backgroundColor = FilamentColor(0f, 0f, 0f, 0f),
                isOpaque = false,
                hostViewMode = FilamentHostViewMode.Texture,
                onEngineReady = { readyEngine ->
                    engine = readyEngine
                    herdActors = emptyList()

                    val indirectLightHandle = readyEngine.loadIndirectLight(Res.getUri(Sheep2IndirectLightPath))
                    if (indirectLightHandle > 0) {
                        readyEngine.setIndirectLight(indirectLightHandle, intensity = 65_000f)
                    }

                    val fluffMaterialHandle = readyEngine.loadMaterial(Res.getUri(sheepFluffMaterial().materialPath))
                    val bodyMaterialHandle = readyEngine.loadMaterial(Res.getUri(sheepBodyMaterial().materialPath))
                    if (fluffMaterialHandle <= 0 || bodyMaterialHandle <= 0) {
                        return@FilamentView
                    }

                    val bodyInstance = createSheepBodyMaterialInstance(
                        engine = readyEngine,
                        materialHandle = bodyMaterialHandle,
                        color = Float3(0.27f, 0.27f, 0.27f),
                        roughness = 0.6f,
                    )
                    val eyeInstance = createSheepBodyMaterialInstance(
                        engine = readyEngine,
                        materialHandle = bodyMaterialHandle,
                        color = Float3(0.9f, 0.6f, 0.26f),
                        roughness = 0.72f,
                    )
                    val pupilInstance = createSheepBodyMaterialInstance(
                        engine = readyEngine,
                        materialHandle = bodyMaterialHandle,
                        color = Float3(0.05f, 0.05f, 0.05f),
                        roughness = 0.45f,
                    )
                    val glassesInstance = createSheepBodyMaterialInstance(
                        engine = readyEngine,
                        materialHandle = bodyMaterialHandle,
                        color = Float3(0.05f, 0.05f, 0.05f),
                        roughness = 0.3f,
                    )

                    val hueOffset = Random.nextFloat()
                    herdActors = List(SheepHerdCount) { index ->
                        val hue = (index.toFloat() / SheepHerdCount + hueOffset) % 1f
                        val fluffInstance = readyEngine.createMaterialInstance(fluffMaterialHandle)
                        readyEngine.setMaterialParameter(fluffInstance, MaterialParameter("baseColor", hsvToFloat3(hue, 0.75f, 0.55f)))
                        readyEngine.setMaterialParameter(fluffInstance, MaterialParameter("roughness", 0.27f))
                        readyEngine.setMaterialParameter(fluffInstance, MaterialParameter("sheenColor", hsvToFloat3((hue + 0.33f) % 1f, 0.6f, 0.65f)))
                        readyEngine.setMaterialParameter(fluffInstance, MaterialParameter("subsurfaceColor", hsvToFloat3((hue + 0.17f) % 1f, 0.5f, 0.4f)))

                        val createdPieces = buildSheepRigPieces(
                            engine = readyEngine,
                            fluffInstanceHandle = fluffInstance,
                            bodyInstanceHandle = bodyInstance,
                            eyeInstanceHandle = eyeInstance,
                            pupilInstanceHandle = pupilInstance,
                            glassesInstanceHandle = glassesInstance,
                        )
                        SheepActor(
                            pieces = createdPieces.filter { it.handle > 0 },
                            index = index,
                            scale = 0.3f + (index % 3) * 0.045f,
                            laneOffset = ((index % 4) - 1.5f) * 0.28f + if (index >= 4) 0.11f else -0.05f,
                            depthOffset = ((index % 3) - 1f) * 0.22f,
                            arcHeight = 0.04f + (index % 2) * 0.03f,
                            bobAmplitude = 0.03f + (index % 3) * 0.012f,
                            speedMultiplier = 0.85f + index * 0.06f,
                            startDelay = index * 0.26f,
                            bobPhase = index * 0.65f,
                            noiseSeed = 17 + index * 9,
                        )
                    }
                },
            )

            EdgeSelector(
                label = "S",
                color = Color(0xFF1E3DFF),
                yFraction = startYFraction,
                containerSize = containerSize,
                isLeftEdge = true,
                selected = activeSelector == SelectorHandle.START,
                onDragStateChanged = { activeSelector = if (it) SelectorHandle.START else null },
                onYFractionChanged = { startYFraction = it },
            )
            EdgeSelector(
                label = "E",
                color = Color(0xFFFF3B12),
                yFraction = endYFraction,
                containerSize = containerSize,
                isLeftEdge = false,
                selected = activeSelector == SelectorHandle.END,
                onDragStateChanged = { activeSelector = if (it) SelectorHandle.END else null },
                onYFractionChanged = { endYFraction = it },
            )

            Button(
                onClick = {
                    val runPath = candidateRunPath ?: return@Button
                    activeRunPath = runPath
                    runToken += 1
                    runInProgress = true
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = Grid.Two),
                enabled = buttonEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE636F4),
                    contentColor = Color.White,
                ),
            ) {
                Text("HERD!")
            }
        }
    }
}

@Composable
private fun EdgeSelector(
    label: String,
    color: Color,
    yFraction: Float,
    containerSize: IntSize,
    isLeftEdge: Boolean,
    selected: Boolean,
    onDragStateChanged: (Boolean) -> Unit,
    onYFractionChanged: (Float) -> Unit,
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val sizePx = with(density) { SelectorSize.roundToPx() }
    val halfSize = sizePx / 2
    val height = containerSize.height.toFloat().coerceAtLeast(1f)
    val centerY = (yFraction * height).roundToInt()
    val offsetX = if (isLeftEdge) 0 else containerSize.width - sizePx
    val currentYFraction by rememberUpdatedState(yFraction)

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX, centerY - halfSize) }
            .size(SelectorSize)
            .pointerInput(isLeftEdge, containerSize) {
                var dragY = 0f
                detectDragGestures(
                    onDragStart = {
                        dragY = currentYFraction * height
                        onDragStateChanged(true)
                    },
                    onDragEnd = { onDragStateChanged(false) },
                    onDragCancel = { onDragStateChanged(false) },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragY = (dragY + dragAmount.y).coerceIn(0f, height)
                        onYFractionChanged(dragY / height)
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawEdgeSemiCircle(
                color = color,
                selected = selected,
                isLeftEdge = isLeftEdge,
            )
        }
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.offset {
                IntOffset(if (isLeftEdge) -halfSize / 2 else halfSize / 2, 0)
            },
        )
    }
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

private fun computeRunPath(
    start: Offset,
    end: Offset,
    stageRect: Rect,
): StageRunPath? {
    val worldStart = stagePointToWorld(start, stageRect)
    val worldEnd = stagePointToWorld(end, stageRect)
    val direction = Float3(
        x = worldEnd.x - worldStart.x,
        y = worldEnd.y - worldStart.y,
        z = 0f,
    ).normalized(Float3(1f, 0f, 0f))
    val pathLength = distance(worldStart, worldEnd)
    if (pathLength <= 1e-4f) return null
    return StageRunPath(
        screenStart = start,
        screenEnd = end,
        stageEntry = start,
        stageExit = end,
        worldStart = worldStart,
        worldEnd = worldEnd,
        direction = direction,
        pathLength = pathLength,
    )
}

private fun stagePointToWorld(
    point: Offset,
    stageRect: Rect,
): Float3 {
    val localX = ((point.x - stageRect.left) / stageRect.width).coerceIn(0f, 1f)
    val localY = ((point.y - stageRect.top) / stageRect.height).coerceIn(0f, 1f)
    val halfHeight = SheepHerdOrthoZoom.toFloat()
    val halfWidth = halfHeight * (stageRect.width / stageRect.height)
    return Float3(
        x = lerp(-halfWidth, halfWidth, localX),
        y = lerp(halfHeight, -halfHeight, localY),
        z = 0f,
    )
}

private fun actorWorldTransform(
    center: Float3,
    direction: Float3,
    scale: Float,
): FloatArray {
    val dirAngle = atan2(direction.y, direction.x) * 180f / PI.toFloat()
    val goingRight = direction.x >= 0
    val tiltAngle = if (goingRight) dirAngle else dirAngle - 180f
    val baseTransform = if (goingRight) {
        multiplyMatrix4(rotationYMatrix(180f), scaleMatrix(scale, scale, scale))
    } else {
        scaleMatrix(scale, scale, scale)
    }
    return multiplyMatrix4(
        translationMatrix(center.x, center.y, center.z),
        multiplyMatrix4(rotationZMatrix(tiltAngle), baseTransform),
    )
}

private fun hiddenActorTransform(index: Int): FloatArray = translationMatrix(
    x = -40f - index,
    y = -30f,
    z = -4f,
)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEdgeSemiCircle(
    color: Color,
    selected: Boolean,
    isLeftEdge: Boolean,
) {
    val diameter = size.minDimension
    val arcTopLeft = if (isLeftEdge) {
        Offset(-diameter / 2f, (size.height - diameter) / 2f)
    } else {
        Offset(size.width - diameter / 2f, (size.height - diameter) / 2f)
    }
    val arcSize = Size(diameter, diameter)
    val startAngle = if (isLeftEdge) -90f else 90f

    if (selected) {
        drawArc(
            color = color.copy(alpha = 0.28f),
            startAngle = startAngle,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = arcTopLeft,
            size = arcSize,
        )
        val inset = diameter * 0.07f
        drawArc(
            color = color.copy(alpha = 0.92f),
            startAngle = startAngle,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(arcTopLeft.x + inset, arcTopLeft.y + inset),
            size = Size(arcSize.width - inset * 2, arcSize.height - inset * 2),
        )
    } else {
        drawArc(
            color = color,
            startAngle = startAngle,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = arcTopLeft,
            size = arcSize,
        )
    }
}

private fun hsvToFloat3(hue: Float, saturation: Float, value: Float): Float3 {
    val h = ((hue % 1f) + 1f) % 1f * 6f
    val i = h.toInt()
    val f = h - i
    val p = value * (1f - saturation)
    val q = value * (1f - saturation * f)
    val t = value * (1f - saturation * (1f - f))
    return when (i) {
        0 -> Float3(value, t, p)
        1 -> Float3(q, value, p)
        2 -> Float3(p, value, t)
        3 -> Float3(p, q, value)
        4 -> Float3(t, p, value)
        else -> Float3(value, p, q)
    }
}

private fun lerp(
    start: Float,
    end: Float,
    progress: Float,
): Float = start + (end - start) * progress

private fun lerp(
    start: Float3,
    end: Float3,
    progress: Float,
): Float3 = Float3(
    x = lerp(start.x, end.x, progress),
    y = lerp(start.y, end.y, progress),
    z = lerp(start.z, end.z, progress),
)
