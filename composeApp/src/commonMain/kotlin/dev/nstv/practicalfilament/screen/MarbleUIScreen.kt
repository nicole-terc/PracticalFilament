package dev.nstv.practicalfilament.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.Color
import dev.nstv.practicalfilament.filament.AttributeDataType
import dev.nstv.practicalfilament.filament.BoundingBox
import dev.nstv.practicalfilament.filament.CustomRenderableConfig
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentClipShape
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.LightConfig
import dev.nstv.practicalfilament.filament.LightType
import dev.nstv.practicalfilament.filament.PrimitiveType
import dev.nstv.practicalfilament.filament.VertexAttribute
import dev.nstv.practicalfilament.filament.VertexAttributeLayout
import dev.nstv.practicalfilament.filament.material.Material
import dev.nstv.practicalfilament.filament.material.loadMaterialOnEngine
import dev.nstv.practicalfilament.filament.toByteArray
import dev.nstv.practicalfilament.theme.Grid
import dev.nstv.practicalfilament.theme.components.DropDownWithArrows
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private enum class DemoStep(
    val label: String,
    val title: String,
    val description: String,
) {
    FLAT_CIRCLE(
        label = "1. Flat Circle",
        title = "Step 1: Flat Circle",
        description = "A plain Compose circle establishes the UI starting point before any 3D rendering is introduced.",
    ),
    FILAMENT_SPHERE(
        label = "2. Filament Sphere",
        title = "Step 2: Filament Sphere",
        description = "The circle becomes a circular Filament viewport. It is clearly 3D now, but the material is still generic and neutral.",
    ),
    MATERIAL_MARBLE(
        label = "3. Add Material",
        title = "Step 3: Add Material",
        description = "A ceramic material preset turns the neutral sphere into a tactile marble with a glazed finish.",
    ),
    LIGHTING_ALIVE(
        label = "4. Add Light",
        title = "Step 4: Add Light",
        description = "Richer direct lighting and subtle rotation bring out highlights and form, making the marble feel alive.",
    ),
    UI_SYSTEM(
        label = "5. UI System",
        title = "Step 5: Add Interaction",
        description = "Tapping a marble selects a theme and applies that material treatment to a lit Filament-backed button surface.",
    ),
}

private val MarbleUiBackground = ComposeColor(0xFF101721)
private val MarbleUiText = ComposeColor(0xFFF4EEE4)
private val MarbleUiMuted = ComposeColor(0xFFB6C1CD)
private val MarbleUiAccent = ComposeColor(0xFFF6D28A)
private val MarbleUiBackgroundFilament = Color(0.0627f, 0.0902f, 0.1294f, 1f)
private val MarbleUiTransparentFilament = Color(0f, 0f, 0f, 0f)
private const val GlassMaterialPath = "materials/marbleGlass.filamat"

private val SingleMarbleCamera = CameraConfig(
    position = Float3(0f, 0.1f, 4.5f),
    lookAt = Float3(0f, 0f, 0f),
    fovDegrees = 34.0,
)

private val PickerMarbleCamera = CameraConfig(
    position = Float3(0f, 0.06f, 4.15f),
    lookAt = Float3(0f, 0f, 0f),
    fovDegrees = 32.0,
)

private val ButtonSurfaceCamera = CameraConfig(
    position = Float3(0f, 0f, 3.35f),
    lookAt = Float3(0f, 0f, 0f),
    fovDegrees = 31.0,
)

private val SphereStepLights = listOf(
    LightConfig(
        type = LightType.DIRECTIONAL,
        color = Color(1f, 0.97f, 0.93f),
        intensity = 72_000f,
        direction = Float3(-0.28f, -0.38f, -1f),
    ),
)

private val MarbleAliveLights = listOf(
    LightConfig(
        type = LightType.SUN,
        color = Color(1f, 0.95f, 0.89f),
        intensity = 110_000f,
        direction = Float3(0.45f, -1f, -0.72f),
        sunAngularRadius = 1.7f,
        sunHaloSize = 12f,
        sunHaloFalloff = 90f,
    ),
    LightConfig(
        type = LightType.POINT,
        color = Color(0.88f, 0.94f, 1f),
        intensity = 120_000f,
        position = Float3(-1.6f, 1.2f, 2.6f),
        falloffRadius = 8f,
    ),
)

private val ThemePickerLights = listOf(
    LightConfig(
        type = LightType.SUN,
        color = Color(1f, 0.95f, 0.9f),
        intensity = 100_000f,
        direction = Float3(0.35f, -1f, -0.9f),
        sunAngularRadius = 1.5f,
        sunHaloSize = 10f,
        sunHaloFalloff = 85f,
    ),
    LightConfig(
        type = LightType.POINT,
        color = Color(0.85f, 0.9f, 1f),
        intensity = 75_000f,
        position = Float3(0f, 1.3f, 3.8f),
        falloffRadius = 10f,
    ),
)

private val ButtonSurfaceLights = MarbleAliveLights

private val MarblePresets = listOf(
    Material(
        fileName = "materials/marbleClay.filamat",
        label = "Clay",
        overrides = mapOf(
            "baseColor" to Float3(0.76f, 0.42f, 0.26f),
            "roughness" to 0.85f,
        ),
    ),
    Material(
        fileName = GlassMaterialPath,
        label = "Glass",
        overrides = mapOf(
            "baseColor" to Float3(0.97f, 0.985f, 1f),
            "roughness" to 0.015f,
            "reflectance" to 0.72f,
            "alpha" to 0.18f,
            "clearCoat" to 1f,
            "clearCoatRoughness" to 0.015f,
        ),
    ),
    Material(
        fileName = "materials/marbleStone.filamat",
        label = "Stone",
        overrides = mapOf(
            "baseColor" to Float3(0.88f, 0.86f, 0.82f),
            "roughness" to 0.35f,
            "subsurfaceColor" to Float3(0.9f, 0.85f, 0.7f),
            "thickness" to 0.5f,
            "subsurfacePower" to 12f,
        ),
    ),
    Material(
        fileName = "materials/marbleMetal.filamat",
        label = "Metal",
        overrides = mapOf(
            "baseColor" to Float3(0.85f, 0.85f, 0.88f),
            "roughness" to 0.08f,
            "metallic" to 1f,
            "reflectance" to 0.9f,
        ),
    ),
    Material(
        fileName = "materials/marbleCeramic.filamat",
        label = "Ceramic",
        overrides = mapOf(
            "baseColor" to Float3(0.22f, 0.45f, 0.72f),
            "roughness" to 0.4f,
            "clearCoat" to 1f,
            "clearCoatRoughness" to 0.04f,
        ),
    ),
    Material(
        fileName = "materials/marbleVelvet.filamat",
        label = "Velvet",
        overrides = mapOf(
            "baseColor" to Float3(0.55f, 0.08f, 0.22f),
            "roughness" to 0.75f,
            "sheenColor" to Float3(0.8f, 0.3f, 0.4f),
            "subsurfaceColor" to Float3(0.7f, 0.15f, 0.25f),
        ),
    ),
)

private val NeutralSphereMaterial = Material(
    fileName = "materials/plastic.filamat",
    label = "Neutral Sphere",
    overrides = mapOf(
        "baseColor" to Float3(0.72f, 0.74f, 0.78f),
        "roughness" to 0.28f,
        "clearCoat" to 0.2f,
        "clearCoatRoughness" to 0.18f,
    ),
)

private const val CeramicPresetIndex = 4

@Composable
fun MarbleUIScreen(
    modifier: Modifier = Modifier,
) {
    var selectedStep by remember { mutableStateOf(DemoStep.FLAT_CIRCLE) }
    var selectedPresetIndex by remember { mutableIntStateOf(CeramicPresetIndex) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MarbleUiBackground),
    ) {
        CompositionLocalProvider(LocalContentColor provides MarbleUiText) {
            DropDownWithArrows(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Grid.Two, vertical = Grid.One),
                options = DemoStep.entries.map { it.label },
                selectedIndex = DemoStep.entries.indexOf(selectedStep),
                onSelectionChanged = { selectedStep = DemoStep.entries[it] },
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    color = MarbleUiText,
                    fontWeight = FontWeight.Medium,
                ),
                loopSelection = true,
            )
        }

        HorizontalDivider(color = MarbleUiMuted.copy(alpha = 0.2f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.1f)
                .padding(Grid.Two),
            contentAlignment = Alignment.Center,
        ) {
            when (selectedStep) {
                DemoStep.FLAT_CIRCLE -> FlatCircleView()
                DemoStep.FILAMENT_SPHERE -> key(selectedStep) {
                    SingleMarbleView(
                        material = NeutralSphereMaterial,
                        lights = SphereStepLights,
                    )
                }
                DemoStep.MATERIAL_MARBLE -> key(selectedStep) {
                    SingleMarbleView(
                        material = MarblePresets[CeramicPresetIndex],
                        lights = SphereStepLights,
                    )
                }
                DemoStep.LIGHTING_ALIVE -> key(selectedStep) {
                    SingleMarbleView(
                        material = MarblePresets[CeramicPresetIndex],
                        lights = MarbleAliveLights,
                        autoRotate = true,
                    )
                }
                DemoStep.UI_SYSTEM -> Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(Grid.Two),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    MarblePickerRow(
                        modifier = Modifier.fillMaxWidth(),
                        selectedIndex = selectedPresetIndex,
                        onSelectionChanged = { selectedPresetIndex = it },
                    )
                    SampleMarbleButton(
                        modifier = Modifier.fillMaxWidth(),
                        preset = MarblePresets[selectedPresetIndex],
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.9f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Grid.Two, vertical = Grid.One),
            verticalArrangement = Arrangement.spacedBy(Grid.One),
        ) {
            Text(
                text = selectedStep.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MarbleUiText,
            )
            Text(
                text = selectedStep.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MarbleUiMuted,
            )
        }
    }
}

@Composable
private fun FlatCircleView(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .aspectRatio(1f),
        ) {
            val radius = size.minDimension * 0.38f
            drawCircle(
                color = ComposeColor(0xFF5779AB),
                radius = radius,
                center = center,
            )
            drawCircle(
                color = ComposeColor.White.copy(alpha = 0.08f),
                radius = radius * 0.72f,
                center = center + Offset(-radius * 0.18f, -radius * 0.16f),
            )
        }
    }
}

@Composable
private fun SingleMarbleView(
    material: Material,
    lights: List<LightConfig>,
    modifier: Modifier = Modifier,
    autoRotate: Boolean = false,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .aspectRatio(1f),
        ) {
            SphereMaterialView(
                modifier = Modifier.fillMaxSize(),
                material = material,
                camera = SingleMarbleCamera,
                lights = lights,
                radius = 1.1f,
                initialRotationX = -12f,
                initialRotationY = 24f,
                autoRotate = autoRotate,
            )
        }
    }
}

@Composable
private fun MarblePickerRow(
    selectedIndex: Int,
    onSelectionChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Grid.Half),
        verticalAlignment = Alignment.Top,
    ) {
        MarblePresets.forEachIndexed { index, preset ->
            MarbleOption(
                modifier = Modifier.weight(1f),
                preset = preset,
                selected = index == selectedIndex,
                onClick = { onSelectionChanged(index) },
            )
        }
    }
}

@Composable
private fun MarbleOption(
    preset: Material,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Grid.One),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clickable(onClick = onClick),
        ) {
            key(preset.fileName) {
                SphereMaterialView(
                    modifier = Modifier.fillMaxSize(),
                    material = preset,
                    camera = PickerMarbleCamera,
                    lights = ThemePickerLights,
                    radius = 0.92f,
                    initialRotationX = -10f,
                    initialRotationY = 20f,
                )
            }
            if (selected) {
                SelectionRingOverlay()
            }
        }

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = preset.label,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) MarbleUiAccent else MarbleUiMuted,
        )
    }
}

@Composable
private fun SphereMaterialView(
    material: Material,
    camera: CameraConfig,
    lights: List<LightConfig>,
    radius: Float,
    initialRotationX: Float,
    initialRotationY: Float,
    modifier: Modifier = Modifier,
    autoRotate: Boolean = false,
) {
    var engineReady by remember { mutableStateOf<FilamentEngine?>(null) }
    var renderableHandle by remember { mutableIntStateOf(0) }

    LaunchedEffect(engineReady, renderableHandle, initialRotationX, initialRotationY) {
        val engine = engineReady ?: return@LaunchedEffect
        if (renderableHandle == 0) return@LaunchedEffect
        engine.setRenderableRotation(renderableHandle, initialRotationX, initialRotationY)
        engine.requestFrame()
    }

    LaunchedEffect(engineReady, renderableHandle, autoRotate, initialRotationX, initialRotationY) {
        val engine = engineReady ?: return@LaunchedEffect
        if (renderableHandle == 0 || !autoRotate) return@LaunchedEffect
        while (true) {
            val timeSeconds = withFrameNanos { it } / 1_000_000_000f
            engine.setRenderableRotation(
                renderableHandle,
                initialRotationX + 2f,
                initialRotationY + timeSeconds * 18f,
            )
            engine.requestFrame()
        }
    }

    FilamentView(
        modifier = modifier,
        camera = camera,
        lights = lights,
        backgroundColor = filamentBackgroundForMaterial(material),
        clipShape = FilamentClipShape.Circle,
        onEngineReady = { engine ->
            val (instanceHandle, _, parameters) = loadMaterialOnEngine(engine, material)
            parameters.values.forEach { engine.setMaterialParameter(instanceHandle, it) }
            engineReady = engine
            renderableHandle = engine.createSphereRenderable(
                materialInstanceHandle = instanceHandle,
                radius = radius,
            )
        },
    )
}

@Composable
private fun SampleMarbleButton(
    preset: Material,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = {},
        modifier = modifier.height(88.dp),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ComposeColor.Transparent,
            contentColor = MarbleUiText,
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            key(preset.fileName) {
                ButtonMaterialBackground(
                    preset = preset,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Text(
                text = "Apply ${preset.label}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MarbleUiText,
            )
        }
    }
}

@Composable
private fun ButtonMaterialBackground(
    preset: Material,
    modifier: Modifier = Modifier,
) {
    val buttonGeometry = remember { buildRoundedButtonGeometry() }

    Box(modifier = modifier) {
        FilamentView(
            modifier = Modifier.fillMaxSize(),
            camera = ButtonSurfaceCamera,
            lights = ButtonSurfaceLights,
            backgroundColor = filamentBackgroundForMaterial(preset),
            clipShape = FilamentClipShape.RoundedRect(24.dp),
            onEngineReady = { engine ->
                val (instanceHandle, _, parameters) = loadMaterialOnEngine(engine, preset)
                parameters.values.forEach { engine.setMaterialParameter(instanceHandle, it) }
                engine.createCustomRenderableWithGeneratedTangents(
                    buttonGeometry.copy(materialInstanceHandle = instanceHandle),
                )
                engine.requestFrame()
            },
        )
    }
}

private fun filamentBackgroundForMaterial(material: Material): Color {
    return if (material.fileName == GlassMaterialPath) MarbleUiTransparentFilament else MarbleUiBackgroundFilament
}

@Composable
private fun CircularMaskOverlay(
    modifier: Modifier = Modifier,
    backgroundColor: ComposeColor = MarbleUiBackground,
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
    ) {
        drawRect(color = backgroundColor)
        drawCircle(
            color = ComposeColor.Transparent,
            radius = size.minDimension * 0.5f,
            center = center,
            blendMode = BlendMode.Clear,
        )
    }
}

@Composable
private fun RoundedRectMaskOverlay(
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    backgroundColor: ComposeColor = MarbleUiBackground,
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
    ) {
        drawRect(color = backgroundColor)
        drawRoundRect(
            color = ComposeColor.Transparent,
            cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
            blendMode = BlendMode.Clear,
        )
    }
}

@Composable
private fun SelectionRingOverlay(
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawCircle(
            color = MarbleUiAccent,
            radius = size.minDimension * 0.5f - 3.dp.toPx(),
            center = center,
            style = Stroke(width = 3.dp.toPx()),
        )
    }
}

private fun buildRoundedButtonGeometry(): CustomRenderableConfig {
    val width = 8.6f
    val height = 2.2f
    val depth = 0.4f
    val bevel = 0.18f
    val outerRadius = 0.72f
    val topWidth = width - bevel * 2f
    val topHeight = height - bevel * 2f
    val topRadius = (outerRadius - bevel).coerceAtLeast(0.16f)
    val arcSegments = 18
    val domeRingCount = 10
    val domeDrop = depth * 0.14f
    val outerRing = roundedRectRing(width, height, outerRadius, arcSegments)

    val vertexFloats = mutableListOf<Float>()
    val indices = mutableListOf<Short>()

    fun appendVertex(x: Float, y: Float, z: Float, u: Float, v: Float): Short {
        val index = vertexFloats.size / 5
        vertexFloats += x
        vertexFloats += y
        vertexFloats += z
        vertexFloats += u
        vertexFloats += v
        return index.toShort()
    }

    val frontCenter = appendVertex(0f, 0f, depth * 0.5f, 0.5f, 0.5f)
    var previousRingIndices: List<Short>? = null
    var topOuterIndices: List<Short> = emptyList()

    for (ringStep in 1..domeRingCount) {
        val t = ringStep.toFloat() / domeRingCount.toFloat()
        val scale = easedProgress(t)
        val ringZ = depth * 0.5f - domeDrop * easedProgress(t)
        val ring = roundedRectRing(
            width = topWidth * scale,
            height = topHeight * scale,
            radius = topRadius * scale,
            arcSegments = arcSegments,
        )
        val ringIndices = ring.map { point ->
            appendVertex(
                x = point.x,
                y = point.y,
                z = ringZ,
                u = (point.x / width) + 0.5f,
                v = (point.y / height) + 0.5f,
            )
        }

        if (previousRingIndices == null) {
            for (index in ringIndices.indices) {
                val next = (index + 1) % ringIndices.size
                indices += frontCenter
                indices += ringIndices[index]
                indices += ringIndices[next]
            }
        } else {
            for (index in ringIndices.indices) {
                val next = (index + 1) % ringIndices.size
                if ((ringStep + index) % 2 == 0) {
                    indices += previousRingIndices[index]
                    indices += ringIndices[index]
                    indices += ringIndices[next]
                    indices += previousRingIndices[index]
                    indices += ringIndices[next]
                    indices += previousRingIndices[next]
                } else {
                    indices += previousRingIndices[index]
                    indices += ringIndices[index]
                    indices += previousRingIndices[next]
                    indices += previousRingIndices[next]
                    indices += ringIndices[index]
                    indices += ringIndices[next]
                }
            }
        }

        previousRingIndices = ringIndices
        topOuterIndices = ringIndices
    }

    val backCenter = appendVertex(0f, 0f, -depth * 0.5f, 0.5f, 0.5f)
    val outerIndices = outerRing.map { point ->
        appendVertex(
            x = point.x,
            y = point.y,
            z = -depth * 0.5f,
            u = (point.x / width) + 0.5f,
            v = (point.y / height) + 0.5f,
        )
    }

    val ringSize = outerRing.size
    for (index in 0 until ringSize) {
        val next = (index + 1) % ringSize
        indices += backCenter
        indices += outerIndices[next]
        indices += outerIndices[index]
    }

    for (index in 0 until ringSize) {
        val next = (index + 1) % ringSize
        indices += topOuterIndices[index]
        indices += outerIndices[index]
        indices += outerIndices[next]
        indices += topOuterIndices[index]
        indices += outerIndices[next]
        indices += topOuterIndices[next]
    }

    return CustomRenderableConfig(
        vertexData = vertexFloats.toFloatArray().toByteArray(),
        vertexCount = vertexFloats.size / 5,
        strideBytes = 20,
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
        indices = indices.toShortArray(),
        materialInstanceHandle = 0,
        boundingBox = BoundingBox(
            center = Float3(0f, 0f, 0f),
            halfExtent = Float3(width * 0.5f, height * 0.5f, depth * 0.5f),
        ),
        primitiveType = PrimitiveType.TRIANGLES,
    )
}

private fun easedProgress(value: Float): Float {
    return value * value * (3f - 2f * value)
}

private fun roundedRectRing(
    width: Float,
    height: Float,
    radius: Float,
    arcSegments: Int,
): List<Float3> {
    val clampedRadius = radius.coerceAtMost(minOf(width, height) * 0.5f)
    val halfWidth = width * 0.5f
    val halfHeight = height * 0.5f
    val centers = listOf(
        Float3(halfWidth - clampedRadius, halfHeight - clampedRadius, 0f),
        Float3(-(halfWidth - clampedRadius), halfHeight - clampedRadius, 0f),
        Float3(-(halfWidth - clampedRadius), -(halfHeight - clampedRadius), 0f),
        Float3(halfWidth - clampedRadius, -(halfHeight - clampedRadius), 0f),
    )
    val startAngles = listOf(0.0, PI * 0.5, PI, PI * 1.5)

    val points = mutableListOf<Float3>()
    centers.forEachIndexed { cornerIndex, center ->
        for (segment in 0..arcSegments) {
            if (cornerIndex > 0 && segment == 0) continue
            val angle = startAngles[cornerIndex] + (segment.toDouble() / arcSegments.toDouble()) * (PI * 0.5)
            points += Float3(
                x = center.x + cos(angle).toFloat() * clampedRadius,
                y = center.y + sin(angle).toFloat() * clampedRadius,
                z = 0f,
            )
        }
    }
    return points
}
