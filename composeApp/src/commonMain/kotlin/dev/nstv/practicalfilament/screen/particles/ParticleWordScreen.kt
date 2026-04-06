package dev.nstv.practicalfilament.screen.particles

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import dev.nstv.practicalfilament.theme.components.SampleNotice
import dev.nstv.practicalfilament.filament.AttributeDataType
import dev.nstv.practicalfilament.filament.BoundingBox
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.Color
import dev.nstv.practicalfilament.filament.CustomRenderableConfig
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.Float2
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.Float4
import dev.nstv.practicalfilament.filament.MaterialBlendingMode
import dev.nstv.practicalfilament.filament.ProjectionType
import dev.nstv.practicalfilament.filament.VertexAttribute
import dev.nstv.practicalfilament.filament.VertexAttributeLayout
import dev.nstv.practicalfilament.filament.material.MaterialParameter
import dev.nstv.practicalfilament.filament.material.MaterialParameterDefinition
import dev.nstv.practicalfilament.filament.material.MaterialParameterType
import dev.nstv.practicalfilament.theme.Grid
import kotlin.math.hypot
import kotlin.math.roundToInt

private const val ParticleVertexStrideBytes = 20
private const val ParticleOrthoZoom = 1.1f

private const val ParticleMaterialSource = """
void material(inout MaterialInputs material) {
    prepareMaterial(material);
    vec2 uv = getUV0() * 2.0 - 1.0;
    float d = length(uv);
    float alpha = 1.0 - smoothstep(0.5, 1.0, d);
    float glow = exp(-d * d * 2.5) * 1.4;
    vec4 col = materialParams.particleColor;
    vec3 rgb = col.rgb * glow * (col.a * alpha);
    material.baseColor = vec4(rgb, col.a * alpha);
}
"""

private val DefaultParticleWords = listOf("FILAMENT", "COMPOSE", "3D", "PARTICLES", "ANDROID")

private val ParticlePalettes = listOf(
    ParticlePalette("Ice", ComposeColor(0xFF9AF3FF)),
    ParticlePalette("Ember", ComposeColor(0xFFFF8A5B)),
    ParticlePalette("Lime", ComposeColor(0xFFA7FF7A)),
    ParticlePalette("Rose", ComposeColor(0xFFFF6FB5)),
    ParticlePalette("Gold", ComposeColor(0xFFFFD76B)),
)

private enum class ParticleWordPhase {
    SCATTER,
    FORMING,
    FORMED,
}

private enum class ParticleAnimationMode(
    val label: String,
) {
    FORM(
        label = "Form",
    ),
    PULSE(
        label = "Pulse",
    ),
    DRAW(
        label = "Draw",
    ),
}

private data class ParticlePalette(
    val label: String,
    val color: ComposeColor,
)

@Composable
fun ParticleWordScreen(
    modifier: Modifier = Modifier,
) {
    var engine by remember { mutableStateOf<FilamentEngine?>(null) }
    var materialInstanceHandle by remember { mutableIntStateOf(0) }
    var renderableHandle by remember { mutableIntStateOf(0) }
    var supportNotice by remember { mutableStateOf<String?>(null) }
    var particleCountSlider by remember { mutableFloatStateOf(800f) }
    var particleSize by remember { mutableFloatStateOf(0.015f) }
    var animationSpeed by remember { mutableFloatStateOf(1f) }
    var selectedPaletteIndex by remember { mutableIntStateOf(0) }
    var currentWordIndex by remember { mutableIntStateOf(0) }
    var animationMode by remember { mutableStateOf(ParticleAnimationMode.FORM) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var drawStrokes by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var customDrawTargets by remember { mutableStateOf<List<Float2>>(emptyList()) }
    var isDrawingGesture by remember { mutableStateOf(false) }

    val particleCount = particleCountSlider.roundToInt()
    val particleSystem = remember(particleCount) { ParticleSystem(particleCount, seed = particleCount) }
    val sampledWords = remember(particleCount) {
        DefaultParticleWords.associateWith { word ->
            sampleWordPositions(word, particleCount)
        }
    }
    val selectedPalette = ParticlePalettes[selectedPaletteIndex]
    val latestAnimationSpeed by rememberUpdatedState(animationSpeed)
    val latestParticleSize by rememberUpdatedState(particleSize)

    LaunchedEffect(engine) {
        val currentEngine = engine ?: return@LaunchedEffect
        materialInstanceHandle = 0
        renderableHandle = 0
        if (!currentEngine.supportsMaterialBuilder) {
            supportNotice = "Particle Word currently targets Android runtime materials only."
            return@LaunchedEffect
        }

        val materialHandle = currentEngine.buildMaterial(
            materialSource = ParticleMaterialSource,
            shadingModel = "unlit",
            requiredAttributes = listOf(VertexAttribute.UV0),
            parameters = listOf(
                MaterialParameterDefinition(
                    name = "particleColor",
                    type = MaterialParameterType.Float4(),
                ),
            ),
            blendingMode = MaterialBlendingMode.TRANSPARENT,
        )
        if (materialHandle <= 0) {
            supportNotice = "Runtime particle material compilation failed."
            return@LaunchedEffect
        }

        materialInstanceHandle = currentEngine.createMaterialInstance(materialHandle)
        supportNotice = null
    }

    LaunchedEffect(engine, materialInstanceHandle, particleCount) {
        val currentEngine = engine ?: return@LaunchedEffect
        if (materialInstanceHandle == 0) return@LaunchedEffect

        if (renderableHandle > 0) {
            currentEngine.removeRenderable(renderableHandle)
            renderableHandle = 0
        }

        particleSystem.setScatterTargets()
        particleSystem.snapToTargets()
        renderableHandle = currentEngine.createCustomRenderable(
            CustomRenderableConfig(
                vertexData = particleSystem.buildVertexBuffer(latestParticleSize),
                vertexCount = particleCount * 4,
                strideBytes = ParticleVertexStrideBytes,
                attributes = listOf(
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
                ),
                indices = particleSystem.fixedIndices,
                materialInstanceHandle = materialInstanceHandle,
                boundingBox = BoundingBox(
                    center = Float3(0f, 0f, 0f),
                    halfExtent = Float3(2f, 1.25f, 0.9f),
                ),
            )
        )

        if (renderableHandle <= 0) {
            supportNotice = "Custom particle renderables are not available on this platform yet."
        } else {
            supportNotice = null
            currentEngine.requestFrame()
        }
    }

    LaunchedEffect(engine, materialInstanceHandle, selectedPaletteIndex) {
        val currentEngine = engine ?: return@LaunchedEffect
        if (materialInstanceHandle == 0) return@LaunchedEffect

        currentEngine.setMaterialParameter(
            materialInstanceHandle,
            MaterialParameter(
                name = "particleColor",
                value = selectedPalette.color.toFilamentFloat4(),
            )
        )
        currentEngine.requestFrame()
    }

    LaunchedEffect(
        engine,
        renderableHandle,
        particleSystem,
        animationMode,
        currentWordIndex,
        customDrawTargets,
        isDrawingGesture,
    ) {
        val currentEngine = engine ?: return@LaunchedEffect
        if (renderableHandle == 0) return@LaunchedEffect

        when (animationMode) {
            ParticleAnimationMode.FORM -> {
                animateFormSequence(
                    engine = currentEngine,
                    renderableHandle = renderableHandle,
                    particleSystem = particleSystem,
                    targets = sampledWords[DefaultParticleWords[currentWordIndex]].orEmpty(),
                    pulseAfterForm = false,
                    animationSpeed = { latestAnimationSpeed },
                    particleSize = { latestParticleSize },
                )
            }

            ParticleAnimationMode.PULSE -> {
                runPulseSequence(
                    engine = currentEngine,
                    renderableHandle = renderableHandle,
                    particleSystem = particleSystem,
                    targets = sampledWords[DefaultParticleWords[currentWordIndex]].orEmpty(),
                    animationSpeed = { latestAnimationSpeed },
                    particleSize = { latestParticleSize },
                )
            }

            ParticleAnimationMode.DRAW -> {
                if (isDrawingGesture || customDrawTargets.isEmpty()) {
                    particleSystem.setScatterTargets()
                    particleSystem.snapToTargets()
                    pushParticleFrame(
                        engine = currentEngine,
                        renderableHandle = renderableHandle,
                        particleSystem = particleSystem,
                        particleSize = latestParticleSize,
                    )
                    return@LaunchedEffect
                }

                animateFormSequence(
                    engine = currentEngine,
                    renderableHandle = renderableHandle,
                    particleSystem = particleSystem,
                    targets = customDrawTargets,
                    pulseAfterForm = true,
                    animationSpeed = { latestAnimationSpeed },
                    particleSize = { latestParticleSize },
                )
            }
        }
    }

    val headline = when (animationMode) {
        ParticleAnimationMode.DRAW -> if (customDrawTargets.isEmpty()) "Draw On Screen" else "Custom Sketch"
        else -> DefaultParticleWords[currentWordIndex]
    }
    val interactionModifier = when (animationMode) {
        ParticleAnimationMode.DRAW -> Modifier
        else -> Modifier.pointerInput(currentWordIndex, animationMode) {
            detectTapGestures(
                onTap = {
                    currentWordIndex = (currentWordIndex + 1) % DefaultParticleWords.size
                }
            )
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            FilamentView(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ComposeColor(0xFF061019))
                    .onSizeChanged { viewportSize = it }
                    .then(interactionModifier),
                camera = CameraConfig(
                    position = Float3(0f, 0f, 3f),
                    lookAt = Float3(0f, 0f, 0f),
                    projectionType = ProjectionType.ORTHO,
                    orthoZoom = ParticleOrthoZoom.toDouble(),
                ),
                lights = emptyList(),
                backgroundColor = Color(0.02f, 0.05f, 0.08f, 1f),
                onEngineReady = { readyEngine ->
                    engine = readyEngine
                },
            )

            if (animationMode == ParticleAnimationMode.DRAW) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(animationMode, viewportSize, particleCount) {
                            detectDragGestures(
                                onDragStart = { start ->
                                    isDrawingGesture = true
                                    currentStroke = listOf(start)
                                },
                                onDragEnd = {
                                    val finalStroke = currentStroke
                                    if (finalStroke.isNotEmpty()) {
                                        val updatedStrokes = drawStrokes + listOf(finalStroke)
                                        drawStrokes = updatedStrokes
                                        customDrawTargets = sampleDrawTargets(
                                            strokes = updatedStrokes,
                                            viewportSize = viewportSize,
                                            maxParticles = particleCount,
                                        )
                                    }
                                    currentStroke = emptyList()
                                    isDrawingGesture = false
                                },
                                onDragCancel = {
                                    currentStroke = emptyList()
                                    isDrawingGesture = false
                                },
                            ) { change, _ ->
                                val previous = currentStroke.lastOrNull()
                                val next = change.position
                                if (previous == null || previous.distanceTo(next) >= 3f) {
                                    currentStroke = currentStroke + next
                                }
                                change.consume()
                            }
                        }
                ) {
                    val guideColor = selectedPalette.color.copy(alpha = 0.72f)
                    drawStrokes.forEach { stroke ->
                        drawStroke(stroke, guideColor)
                    }
                    drawStroke(
                        stroke = currentStroke,
                        color = selectedPalette.color.copy(alpha = 0.9f),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(Grid.Two),
            verticalArrangement = Arrangement.spacedBy(Grid.One),
        ) {
            Text(
                text = headline,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            supportNotice?.let { notice ->
                SampleNotice(notice)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Grid.One),
            ) {
                ParticleAnimationMode.entries.forEach { mode ->
                    FilterChip(
                        selected = animationMode == mode,
                        onClick = { animationMode = mode },
                        label = { Text(mode.label) },
                    )
                }
            }
            ParticleControlRow(
                label = "Speed",
                value = animationSpeed,
                displayValue = formatFloat(animationSpeed, decimals = 2, suffix = "x"),
                valueRange = 0.35f..2f,
                onValueChange = { animationSpeed = it },
            )
            ParticleControlRow(
                label = "Size",
                value = particleSize,
                displayValue = formatFloat(particleSize, decimals = 3),
                valueRange = 0.006f..0.03f,
                onValueChange = { particleSize = it },
            )
            ParticleControlRow(
                label = "Count",
                value = particleCountSlider,
                displayValue = particleCount.toString(),
                valueRange = 200f..2000f,
                onValueChange = { particleCountSlider = it.roundToInt().toFloat() },
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Grid.One),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ParticlePalettes.forEachIndexed { index, palette ->
                    FilterChip(
                        selected = selectedPaletteIndex == index,
                        onClick = { selectedPaletteIndex = index },
                        label = { Text(palette.label) },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Grid.One),
            ) {
                if (animationMode == ParticleAnimationMode.DRAW) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            drawStrokes = emptyList()
                            currentStroke = emptyList()
                            customDrawTargets = emptyList()
                            isDrawingGesture = false
                        }
                    ) {
                        Text("Clear Drawing")
                    }
                } else {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            currentWordIndex = (currentWordIndex + 1) % DefaultParticleWords.size
                        }
                    ) {
                        Text("Next Word")
                    }
                }
            }
        }
    }
}

@Composable
private fun ParticleControlRow(
    label: String,
    value: Float,
    displayValue: String,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Grid.Half),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = displayValue,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = value,
            valueRange = valueRange,
            onValueChange = onValueChange,
        )
    }
}

private suspend fun animateFormSequence(
    engine: FilamentEngine,
    renderableHandle: Int,
    particleSystem: ParticleSystem,
    targets: List<Float2>,
    pulseAfterForm: Boolean,
    animationSpeed: () -> Float,
    particleSize: () -> Float,
) {
    particleSystem.setScatterTargets()
    particleSystem.snapToTargets()
    pushParticleFrame(engine, renderableHandle, particleSystem, particleSize())

    var phase = ParticleWordPhase.SCATTER
    var phaseElapsed = 0f
    var simulationTime = 0f
    var previousFrameNanos = 0L

    while (true) {
        val frameNanos = withFrameNanos { it }
        if (previousFrameNanos == 0L) {
            previousFrameNanos = frameNanos
            continue
        }

        val dt = ((frameNanos - previousFrameNanos) / 1_000_000_000f)
            .coerceAtMost(0.05f) * animationSpeed().coerceAtLeast(0.1f)
        previousFrameNanos = frameNanos
        simulationTime += dt
        phaseElapsed += dt

        when (phase) {
            ParticleWordPhase.SCATTER -> {
                if (phaseElapsed >= 0.5f) {
                    particleSystem.setTargets(targets)
                    phase = ParticleWordPhase.FORMING
                    phaseElapsed = 0f
                }
            }

            ParticleWordPhase.FORMING -> {
                if (phaseElapsed >= 1.1f) {
                    phase = ParticleWordPhase.FORMED
                    phaseElapsed = 0f
                }
            }

            ParticleWordPhase.FORMED -> Unit
        }

        val spring = when (phase) {
            ParticleWordPhase.SCATTER -> 5f
            ParticleWordPhase.FORMING -> 15f
            ParticleWordPhase.FORMED -> 11f
        }
        val damping = when (phase) {
            ParticleWordPhase.SCATTER -> 0.93f
            ParticleWordPhase.FORMING -> 0.86f
            ParticleWordPhase.FORMED -> 0.89f
        }

        particleSystem.step(
            dt = dt,
            spring = spring,
            damping = damping,
            time = simulationTime,
            pulseAmplitude = if (phase == ParticleWordPhase.FORMED && pulseAfterForm) 0.055f else 0f,
        )
        pushParticleFrame(engine, renderableHandle, particleSystem, particleSize())
    }
}

private suspend fun runPulseSequence(
    engine: FilamentEngine,
    renderableHandle: Int,
    particleSystem: ParticleSystem,
    targets: List<Float2>,
    animationSpeed: () -> Float,
    particleSize: () -> Float,
) {
    particleSystem.setTargets(targets)
    particleSystem.snapToTargets()
    pushParticleFrame(engine, renderableHandle, particleSystem, particleSize())

    var simulationTime = 0f
    var previousFrameNanos = 0L

    while (true) {
        val frameNanos = withFrameNanos { it }
        if (previousFrameNanos == 0L) {
            previousFrameNanos = frameNanos
            continue
        }

        val dt = ((frameNanos - previousFrameNanos) / 1_000_000_000f)
            .coerceAtMost(0.05f) * animationSpeed().coerceAtLeast(0.1f)
        previousFrameNanos = frameNanos
        simulationTime += dt

        particleSystem.step(
            dt = dt,
            spring = 12f,
            damping = 0.9f,
            time = simulationTime,
            pulseAmplitude = 0.055f,
        )
        pushParticleFrame(engine, renderableHandle, particleSystem, particleSize())
    }
}

private fun pushParticleFrame(
    engine: FilamentEngine,
    renderableHandle: Int,
    particleSystem: ParticleSystem,
    particleSize: Float,
) {
    engine.updateVertexData(renderableHandle, particleSystem.buildVertexBuffer(particleSize))
    engine.requestFrame()
}

private fun sampleDrawTargets(
    strokes: List<List<Offset>>,
    viewportSize: IntSize,
    maxParticles: Int,
): List<Float2> {
    if (viewportSize.width == 0 || viewportSize.height == 0) {
        return emptyList()
    }

    val doubled = buildDoubleLineTargets(strokes, maxParticles)
    if (doubled.isEmpty()) {
        return emptyList()
    }

    return doubled.map { point ->
        point.toWorldPoint(viewportSize)
    }
}

private fun buildDoubleLineTargets(
    strokes: List<List<Offset>>,
    maxParticles: Int,
): List<Offset> {
    if (maxParticles <= 0) return emptyList()
    val centerlineBudget = (maxParticles / 2).coerceAtLeast(1)
    val resampled = resampleStrokes(strokes, centerlineBudget)
    if (resampled.isEmpty()) {
        return emptyList()
    }

    val thickened = ArrayList<Offset>(resampled.sumOf { it.size } * 2)
    resampled.forEach { stroke ->
        thickened += buildParallelOffsets(stroke, offsetPixels = 9f)
    }

    return if (thickened.size <= maxParticles) {
        thickened
    } else {
        List(maxParticles) { index ->
            thickened[index * thickened.size / maxParticles]
        }
    }
}

private fun resampleStrokes(
    strokes: List<List<Offset>>,
    maxSamples: Int,
): List<List<Offset>> {
    if (maxSamples <= 0) return emptyList()

    var totalLength = 0f
    val lengths = strokes.map { stroke ->
        strokeLength(stroke).also { totalLength += it }
    }
    if (totalLength <= 0f) {
        return strokes.filter { it.isNotEmpty() }
    }

    return strokes.mapIndexedNotNull { index, stroke ->
        if (stroke.isEmpty()) {
            null
        } else {
            val strokeShare = (lengths[index] / totalLength).coerceAtLeast(0f)
            val strokeSamples = (strokeShare * maxSamples).roundToInt().coerceAtLeast(2)
            val spacing = (lengths[index] / strokeSamples.toFloat()).coerceAtLeast(2f)
            resampleStroke(stroke, spacing)
        }
    }
}

private fun resampleStroke(
    stroke: List<Offset>,
    spacing: Float,
): List<Offset> {
    if (stroke.isEmpty()) return emptyList()
    if (stroke.size == 1) return stroke

    val samples = ArrayList<Offset>()
    samples += stroke.first()
    var distanceUntilNextSample = spacing

    for (index in 1 until stroke.size) {
        var segmentStart = stroke[index - 1]
        val segmentEnd = stroke[index]
        var segmentLength = segmentStart.distanceTo(segmentEnd)
        if (segmentLength == 0f) continue

        while (segmentLength >= distanceUntilNextSample) {
            val t = distanceUntilNextSample / segmentLength
            segmentStart = lerp(segmentStart, segmentEnd, t)
            samples += segmentStart
            segmentLength = segmentStart.distanceTo(segmentEnd)
            distanceUntilNextSample = spacing
        }

        distanceUntilNextSample -= segmentLength
        if (distanceUntilNextSample <= 0f) {
            distanceUntilNextSample = spacing
        }
    }

    if (samples.last() != stroke.last()) {
        samples += stroke.last()
    }
    return samples
}

private fun buildParallelOffsets(
    stroke: List<Offset>,
    offsetPixels: Float,
): List<Offset> {
    if (stroke.isEmpty()) return emptyList()
    if (stroke.size == 1) {
        val point = stroke.first()
        return listOf(
            Offset(point.x - offsetPixels, point.y),
            Offset(point.x + offsetPixels, point.y),
        )
    }

    val doubled = ArrayList<Offset>(stroke.size * 2)
    stroke.forEachIndexed { index, point ->
        val previous = stroke.getOrElse(index - 1) { point }
        val next = stroke.getOrElse(index + 1) { point }
        val tangent = Offset(next.x - previous.x, next.y - previous.y)
        val tangentLength = tangent.distanceTo(Offset.Zero).coerceAtLeast(0.001f)
        val normal = Offset(
            x = -tangent.y / tangentLength,
            y = tangent.x / tangentLength,
        )
        doubled += Offset(
            x = point.x + normal.x * offsetPixels,
            y = point.y + normal.y * offsetPixels,
        )
        doubled += Offset(
            x = point.x - normal.x * offsetPixels,
            y = point.y - normal.y * offsetPixels,
        )
    }
    return doubled
}

private fun strokeLength(stroke: List<Offset>): Float {
    var length = 0f
    for (index in 1 until stroke.size) {
        length += stroke[index - 1].distanceTo(stroke[index])
    }
    return length
}

private fun lerp(start: Offset, end: Offset, fraction: Float): Offset = Offset(
    x = start.x + (end.x - start.x) * fraction,
    y = start.y + (end.y - start.y) * fraction,
)

private fun Offset.toWorldPoint(viewportSize: IntSize): Float2 {
    val width = viewportSize.width.coerceAtLeast(1).toFloat()
    val height = viewportSize.height.coerceAtLeast(1).toFloat()
    val aspect = width / height
    val orthoWidth = ParticleOrthoZoom * aspect
    val x = ((this.x / width) - 0.5f) * 2f * orthoWidth
    val y = (0.5f - (this.y / height)) * 2f * ParticleOrthoZoom
    return Float2(x = x, y = y)
}

private fun DrawScope.drawStroke(
    stroke: List<Offset>,
    color: ComposeColor,
) {
    if (stroke.isEmpty()) return
    if (stroke.size == 1) {
        drawCircle(
            color = color,
            radius = 8f,
            center = stroke.first(),
        )
        return
    }

    for (index in 1 until stroke.size) {
        drawLine(
            color = color,
            start = stroke[index - 1],
            end = stroke[index],
            strokeWidth = 7f,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.cornerPathEffect(16f),
        )
    }
    drawCircle(
        color = color,
        radius = 5f,
        center = stroke.last(),
        style = Stroke(width = 2f),
    )
}

private fun Offset.distanceTo(other: Offset): Float = hypot(x - other.x, y - other.y)

private fun ComposeColor.toFilamentFloat4(): Float4 = Float4(
    x = red,
    y = green,
    z = blue,
    w = alpha,
)

private fun formatFloat(
    value: Float,
    decimals: Int,
    suffix: String = "",
): String {
    val multiplier = when (decimals) {
        0 -> 1
        1 -> 10
        2 -> 100
        else -> 1000
    }
    val rounded = (value * multiplier).roundToInt().toFloat() / multiplier.toFloat()
    return rounded.toString() + suffix
}
