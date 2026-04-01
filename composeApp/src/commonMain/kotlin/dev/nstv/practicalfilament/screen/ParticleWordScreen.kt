package dev.nstv.practicalfilament.screen

import androidx.compose.foundation.background
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.input.pointer.pointerInput
import dev.nstv.practicalfilament.filament.AttributeDataType
import dev.nstv.practicalfilament.filament.BoundingBox
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.Color
import dev.nstv.practicalfilament.filament.CustomRenderableConfig
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentView
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
import kotlin.math.roundToInt

private const val ParticleVertexStrideBytes = 20

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
    var advanceSignal by remember { mutableIntStateOf(0) }

    val particleCount = particleCountSlider.roundToInt()
    val particleSystem = remember(particleCount) { ParticleSystem(particleCount, seed = particleCount) }
    val sampledWords = remember(particleCount) {
        DefaultParticleWords.associateWith { word ->
            sampleWordPositions(word, particleCount)
        }
    }

    val latestAdvanceSignal by rememberUpdatedState(advanceSignal)
    val latestAnimationSpeed by rememberUpdatedState(animationSpeed)
    val latestParticleSize by rememberUpdatedState(particleSize)
    val selectedPalette = ParticlePalettes[selectedPaletteIndex]

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

        currentWordIndex = 0
        particleSystem.setScatterTargets()
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

    LaunchedEffect(engine, renderableHandle, particleSystem, sampledWords) {
        val currentEngine = engine ?: return@LaunchedEffect
        if (renderableHandle == 0) return@LaunchedEffect

        val wordOrder = DefaultParticleWords
        var wordIndex = currentWordIndex.coerceIn(0, wordOrder.lastIndex)
        var phase = ParticleWordPhase.SCATTER
        var phaseElapsed = 0f
        var simulationTime = 0f
        var processedAdvanceSignal = latestAdvanceSignal
        var previousFrameNanos = 0L

        particleSystem.setScatterTargets()
        currentWordIndex = wordIndex

        while (true) {
            val frameNanos = withFrameNanos { it }
            if (previousFrameNanos == 0L) {
                previousFrameNanos = frameNanos
                continue
            }

            val rawDt = ((frameNanos - previousFrameNanos) / 1_000_000_000f).coerceAtMost(0.05f)
            previousFrameNanos = frameNanos
            val dt = rawDt * latestAnimationSpeed.coerceAtLeast(0.1f)
            simulationTime += dt
            phaseElapsed += dt

            if (latestAdvanceSignal != processedAdvanceSignal) {
                processedAdvanceSignal = latestAdvanceSignal
                wordIndex = (wordIndex + 1) % wordOrder.size
                currentWordIndex = wordIndex
                particleSystem.setScatterTargets()
                phase = ParticleWordPhase.SCATTER
                phaseElapsed = 0f
            }

            when (phase) {
                ParticleWordPhase.SCATTER -> {
                    if (phaseElapsed >= 0.9f) {
                        particleSystem.setTargets(sampledWords[wordOrder[wordIndex]].orEmpty())
                        phase = ParticleWordPhase.FORMING
                        phaseElapsed = 0f
                    }
                }

                ParticleWordPhase.FORMING -> {
                    if (phaseElapsed >= 1.15f) {
                        phase = ParticleWordPhase.FORMED
                        phaseElapsed = 0f
                    }
                }

                ParticleWordPhase.FORMED -> {
                    if (phaseElapsed >= 1.95f) {
                        wordIndex = (wordIndex + 1) % wordOrder.size
                        currentWordIndex = wordIndex
                        particleSystem.setScatterTargets()
                        phase = ParticleWordPhase.SCATTER
                        phaseElapsed = 0f
                    }
                }
            }

            val spring = when (phase) {
                ParticleWordPhase.SCATTER -> 4.5f
                ParticleWordPhase.FORMING -> 15f
                ParticleWordPhase.FORMED -> 11f
            }
            val damping = when (phase) {
                ParticleWordPhase.SCATTER -> 0.93f
                ParticleWordPhase.FORMING -> 0.86f
                ParticleWordPhase.FORMED -> 0.88f
            }

            particleSystem.step(
                dt = dt,
                spring = spring,
                damping = damping,
                time = simulationTime,
                pulseAmplitude = if (phase == ParticleWordPhase.FORMED) 0.055f else 0f,
            )
            currentEngine.updateVertexData(renderableHandle, particleSystem.buildVertexBuffer(latestParticleSize))
            currentEngine.requestFrame()
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        FilamentView(
            modifier = Modifier
                .fillMaxSize()
                .background(ComposeColor(0xFF061019))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            advanceSignal += 1
                        }
                    )
                },
            camera = CameraConfig(
                position = Float3(0f, 0f, 3f),
                lookAt = Float3(0f, 0f, 0f),
                projectionType = ProjectionType.ORTHO,
                orthoZoom = 1.1,
            ),
            lights = emptyList(),
            backgroundColor = Color(0.02f, 0.05f, 0.08f, 1f),
            onEngineReady = { readyEngine ->
                engine = readyEngine
            },
        )

        supportNotice?.let { notice ->
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(Grid.Two),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                tonalElevation = Grid.Quarter,
            ) {
                Text(
                    modifier = Modifier.padding(Grid.Two),
                    text = notice,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(Grid.Two),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
            tonalElevation = Grid.Quarter,
        ) {
            Column(
                modifier = Modifier.padding(Grid.Two),
                verticalArrangement = Arrangement.spacedBy(Grid.One),
            ) {
                Text(
                    text = DefaultParticleWords[currentWordIndex],
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Tap the scene or use Next Word to jump to the next formation.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                    OutlinedButton(onClick = { advanceSignal += 1 }) {
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
