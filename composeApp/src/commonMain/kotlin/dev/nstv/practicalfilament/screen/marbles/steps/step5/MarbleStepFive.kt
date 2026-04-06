package dev.nstv.practicalfilament.screen.marbles.steps.step5

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.nstv.practicalfilament.filament.AttributeDataType
import dev.nstv.practicalfilament.filament.BoundingBox
import dev.nstv.practicalfilament.filament.CustomRenderableConfig
import dev.nstv.practicalfilament.filament.FilamentClipShape
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentHostViewMode
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.PrimitiveType
import dev.nstv.practicalfilament.filament.VertexAttribute
import dev.nstv.practicalfilament.filament.VertexAttributeLayout
import dev.nstv.practicalfilament.filament.material.Material
import dev.nstv.practicalfilament.filament.toByteArray
import dev.nstv.practicalfilament.screen.marbles.components.ButtonSurfaceCamera
import dev.nstv.practicalfilament.screen.marbles.components.CeramicPresetIndex
import dev.nstv.practicalfilament.screen.marbles.components.MarbleAliveLights
import dev.nstv.practicalfilament.screen.marbles.components.MarblePresets
import dev.nstv.practicalfilament.screen.marbles.components.MarbleUiAccent
import dev.nstv.practicalfilament.screen.marbles.components.MarbleUiBackground
import dev.nstv.practicalfilament.screen.marbles.components.MarbleUiBackgroundFilament
import dev.nstv.practicalfilament.screen.marbles.components.MarbleUiMuted
import dev.nstv.practicalfilament.screen.marbles.components.MarbleUiText
import dev.nstv.practicalfilament.screen.marbles.components.PickerMarbleCamera
import dev.nstv.practicalfilament.screen.marbles.components.ThemePickerLights
import dev.nstv.practicalfilament.screen.marbles.steps.components.SphereMaterialView
import dev.nstv.practicalfilament.theme.Grid
import dev.nstv.practicalfilament.theme.components.CheckBoxLabel
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val DefaultButtonCurvature = 0.44f
private val ButtonCornerRadius = 24.dp
private val ButtonHeight = 88.dp
private val ButtonShape = RoundedCornerShape(ButtonCornerRadius)
private val ButtonClipShape = FilamentClipShape.RoundedRect(ButtonCornerRadius)
private const val ButtonMeshWidth = 8.6f
private const val ButtonMeshHeight = 2.2f
private const val ButtonMeshDepth = 0.52f
private const val ButtonRimThickness = 0.14f
private const val ButtonCurvatureRange = 1f
private val ButtonCornerRadiusFraction = ButtonCornerRadius.value / ButtonHeight.value

private val ButtonSurfaceLights = MarbleAliveLights

@Composable
fun MarbleStepFive(
    modifier: Modifier = Modifier,
) {
    var selectedPresetIndex by remember { mutableIntStateOf(CeramicPresetIndex) }
    var buttonCurvature by remember { mutableFloatStateOf(DefaultButtonCurvature) }
    var surface3d by remember { mutableStateOf(true) }

    Column(
        modifier = modifier.fillMaxSize(),
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
            curvature = buttonCurvature,
            flatSurface = !surface3d,
        )
        CheckBoxLabel(
            text = "3D surface",
            modifier = Modifier.fillMaxWidth(),
            checked = surface3d,
            onCheckedChange = { surface3d = it },
        )
        if (surface3d) {
            CurvatureSlider(
                modifier = Modifier.fillMaxWidth(),
                value = buttonCurvature,
                onValueChange = { buttonCurvature = it },
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
            key(preset.materialPath) {
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
private fun SampleMarbleButton(
    preset: Material,
    curvature: Float,
    flatSurface: Boolean,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = {},
        modifier = modifier.height(ButtonHeight),
        shape = ButtonShape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MarbleUiText,
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            key(preset.materialPath) {
                ButtonMaterialBackground(
                    preset = preset,
                    curvature = curvature,
                    flatSurface = flatSurface,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Text(
                text = preset.label,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MarbleUiText,
            )
        }
    }
}

@Composable
private fun ButtonMaterialBackground(
    preset: Material,
    curvature: Float,
    flatSurface: Boolean,
    modifier: Modifier = Modifier,
) {
    var engineReady by remember { mutableStateOf<FilamentEngine?>(null) }
    var materialInstanceHandle by remember { mutableIntStateOf(0) }
    var renderableHandle by remember { mutableIntStateOf(0) }

    LaunchedEffect(engineReady, materialInstanceHandle, curvature, flatSurface) {
        val engine = engineReady ?: return@LaunchedEffect
        if (materialInstanceHandle == 0) return@LaunchedEffect
        if (renderableHandle != 0) {
            engine.removeRenderable(renderableHandle)
        }
        val geometry =
            if (flatSurface) buildFlatButtonGeometry() else buildRoundedButtonGeometry(curvature)
        renderableHandle = engine.createCustomRenderableWithGeneratedTangents(
            geometry.copy(materialInstanceHandle = materialInstanceHandle),
        )
        engine.requestFrame()
    }

    Box(modifier = modifier) {
        FilamentView(
            modifier = Modifier.fillMaxSize(),
            camera = ButtonSurfaceCamera,
            lights = ButtonSurfaceLights,
            backgroundColor = MarbleUiBackgroundFilament,
            clipShape = ButtonClipShape,
            hostViewMode = FilamentHostViewMode.Texture,
            onEngineReady = { engine ->
                val loaded = engine.loadMaterial(preset)
                loaded.parameters.values.forEach {
                    engine.setMaterialParameter(
                        loaded.instanceHandle,
                        it
                    )
                }
                materialInstanceHandle = loaded.instanceHandle
                engineReady = engine
            },
        )
    }
}

@Composable
private fun CurvatureSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Grid.Half),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Curvature",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MarbleUiText,
            )
            Text(
                text = curvatureLabel(value),
                style = MaterialTheme.typography.labelMedium,
                color = MarbleUiMuted,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = -ButtonCurvatureRange..ButtonCurvatureRange,
            colors = SliderDefaults.colors(
                thumbColor = MarbleUiAccent,
                activeTrackColor = MarbleUiAccent,
                inactiveTrackColor = MarbleUiMuted.copy(alpha = 0.28f),
            ),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Bowl",
                style = MaterialTheme.typography.labelSmall,
                color = MarbleUiMuted,
            )
            Text(
                text = "Flat",
                style = MaterialTheme.typography.labelSmall,
                color = MarbleUiMuted,
            )
            Text(
                text = "Dome",
                style = MaterialTheme.typography.labelSmall,
                color = MarbleUiMuted,
            )
        }
    }
}

private fun curvatureLabel(value: Float): String {
    val percent = (abs(value) * 100f).roundToInt()
    return when {
        value <= -0.08f -> "Bowl $percent%"
        value >= 0.08f -> "Dome $percent%"
        else -> "Flat"
    }
}

@Composable
private fun CircularMaskOverlay(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MarbleUiBackground,
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
    ) {
        drawRect(color = backgroundColor)
        drawCircle(
            color = Color.Transparent,
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
    backgroundColor: Color = MarbleUiBackground,
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
    ) {
        drawRect(color = backgroundColor)
        drawRoundRect(
            color = Color.Transparent,
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

private fun buildFlatButtonGeometry(): CustomRenderableConfig {
    val width = ButtonMeshWidth
    val height = ButtonMeshHeight
    val outerRadius =
        (height * ButtonCornerRadiusFraction).coerceAtMost(minOf(width, height) * 0.5f)
    val arcSegments = 18
    val ringCount = 8

    val vertexFloats = mutableListOf<Float>()
    val indices = mutableListOf<Short>()

    fun appendVertex(x: Float, y: Float, u: Float, v: Float): Short {
        val idx = vertexFloats.size / 5
        vertexFloats += x; vertexFloats += y; vertexFloats += 0f
        vertexFloats += u; vertexFloats += v
        return idx.toShort()
    }

    var prevRing = roundedRectRing(width, height, outerRadius, arcSegments).map { p ->
        appendVertex(p.x, p.y, (p.x / width) + 0.5f, (p.y / height) + 0.5f)
    }

    for (step in 1 until ringCount) {
        val t = step.toFloat() / ringCount.toFloat()
        val scale = 1f - t
        val terminalDiameter = minOf(width, height) * scale
        val centerBlend = easedProgress(((t - 0.4f) / 0.6f).coerceIn(0f, 1f))
        val rw = lerp(width * scale, terminalDiameter, centerBlend)
        val rh = lerp(height * scale, terminalDiameter, centerBlend)
        val rr = lerp(outerRadius * scale, terminalDiameter * 0.5f, centerBlend)
        if (rw < 0.035f || rh < 0.035f) break
        val ring = roundedRectRing(rw, rh, rr.coerceAtLeast(0.02f), arcSegments)
        val ringIndices = ring.map { p ->
            appendVertex(p.x, p.y, (p.x / width) + 0.5f, (p.y / height) + 0.5f)
        }
        for (i in prevRing.indices) {
            val j = (i + 1) % prevRing.size
            indices += prevRing[i]; indices += prevRing[j]; indices += ringIndices[j]
            indices += prevRing[i]; indices += ringIndices[j]; indices += ringIndices[i]
        }
        prevRing = ringIndices
    }

    val center = appendVertex(0f, 0f, 0.5f, 0.5f)
    for (i in prevRing.indices) {
        val j = (i + 1) % prevRing.size
        indices += center; indices += prevRing[i]; indices += prevRing[j]
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
            halfExtent = Float3(width * 0.5f, height * 0.5f, 0.01f),
        ),
        primitiveType = PrimitiveType.TRIANGLES,
    )
}

private fun buildRoundedButtonGeometry(
    curvature: Float,
): CustomRenderableConfig {
    val width = ButtonMeshWidth
    val height = ButtonMeshHeight
    val depth = ButtonMeshDepth
    val rimThickness = ButtonRimThickness
    val outerRadius =
        (height * ButtonCornerRadiusFraction).coerceAtMost(minOf(width, height) * 0.5f)
    val arcSegments = 18
    val wallRingCount = 6
    val floorRingCount = 8
    val signedCurvature = curvature.coerceIn(-1f, 1f)
    val bowlAmount = (-signedCurvature).coerceAtLeast(0f)
    val domeAmount = signedCurvature.coerceAtLeast(0f)

    // Z positions: rim top is the highest point, floor center is the lowest (bowl) or highest (dome)
    val rimTopZ = depth * 0.5f
    val rimBaseZ = rimTopZ - depth * 0.12f // slight thickness for the rim lip
    val innerSurfaceStartZ = lerp(rimBaseZ, rimTopZ, domeAmount)
    val floorCenterZ = if (signedCurvature >= 0f) {
        rimTopZ + domeAmount * depth * 0.28f
    } else {
        rimBaseZ - bowlAmount * depth * 0.7f
    }

    // Rim: outer edge and inner edge of the raised border
    val rimOuterRing = roundedRectRing(width, height, outerRadius, arcSegments)
    val innerWidth = (width - rimThickness * 2f).coerceAtLeast(0.8f)
    val innerHeight = (height - rimThickness * 2f).coerceAtLeast(0.4f)
    val innerRadius = (outerRadius - rimThickness).coerceAtLeast(0.08f)
    val rimInnerRing = roundedRectRing(innerWidth, innerHeight, innerRadius, arcSegments)

    // Floor: the inner bowl/dome surface, scaled down from rim inner edge
    val floorInset = innerHeight * 0.08f
    val floorWidth = (innerWidth - floorInset * 2f).coerceAtLeast(0.4f)
    val floorHeight = (innerHeight - floorInset * 2f).coerceAtLeast(0.2f)
    val floorRadius = (innerRadius - floorInset).coerceAtLeast(0.06f)
    val floorTerminalDiameter = minOf(floorWidth, floorHeight)

    val vertexFloats = mutableListOf<Float>()
    val indices = mutableListOf<Short>()

    fun appendVertex(x: Float, y: Float, z: Float, u: Float, v: Float): Short {
        val index = vertexFloats.size / 5
        vertexFloats += x; vertexFloats += y; vertexFloats += z
        vertexFloats += u; vertexFloats += v
        return index.toShort()
    }

    fun stitchRings(prevRing: List<Short>, nextRing: List<Short>, flipWinding: Boolean = false) {
        for (i in prevRing.indices) {
            val j = (i + 1) % prevRing.size
            if (flipWinding) {
                if (i % 2 == 0) {
                    indices += prevRing[i]; indices += nextRing[i]; indices += nextRing[j]
                    indices += prevRing[i]; indices += nextRing[j]; indices += prevRing[j]
                } else {
                    indices += prevRing[i]; indices += nextRing[i]; indices += prevRing[j]
                    indices += prevRing[j]; indices += nextRing[i]; indices += nextRing[j]
                }
            } else {
                // CCW winding for front-facing (+Z toward camera)
                if (i % 2 == 0) {
                    indices += prevRing[i]; indices += prevRing[j]; indices += nextRing[j]
                    indices += prevRing[i]; indices += nextRing[j]; indices += nextRing[i]
                } else {
                    indices += prevRing[i]; indices += prevRing[j]; indices += nextRing[i]
                    indices += prevRing[j]; indices += nextRing[j]; indices += nextRing[i]
                }
            }
        }
    }

    // 1. Rim top face: flat ring between outer and inner edges at rimTopZ
    val rimOuterTopIndices = rimOuterRing.map { p ->
        appendVertex(p.x, p.y, rimTopZ, (p.x / width) + 0.5f, (p.y / height) + 0.5f)
    }
    val rimInnerTopIndices = rimInnerRing.map { p ->
        appendVertex(p.x, p.y, rimTopZ, (p.x / width) + 0.5f, (p.y / height) + 0.5f)
    }
    stitchRings(rimOuterTopIndices, rimInnerTopIndices)

    var prevInnerRing = rimInnerTopIndices
    var centerSurfaceZ = floorCenterZ

    if (bowlAmount > 0f) {
        val bowlRingCount = wallRingCount + floorRingCount
        val bowlDepth = bowlAmount * depth * 0.64f
        for (step in 1..bowlRingCount) {
            val t = step.toFloat() / bowlRingCount.toFloat()
            val eased = easedProgress(t)
            val radialScale = cos(eased.toDouble() * PI * 0.5).toFloat()
            val circularBlend = easedProgress((eased * 1.25f).coerceIn(0f, 1f))
            val circularDiameter = floorTerminalDiameter * radialScale
            val ringWidth = lerp(innerWidth * radialScale, circularDiameter, circularBlend)
            val ringHeight = lerp(innerHeight * radialScale, circularDiameter, circularBlend)
            val ringRadius = lerp(
                (innerRadius * radialScale).coerceAtLeast(0.02f),
                circularDiameter * 0.5f,
                circularBlend,
            )
            val sink = sin(eased.toDouble() * PI * 0.5).toFloat()
            val ringZ = rimTopZ - bowlDepth * sink
            if (ringWidth < 0.035f || ringHeight < 0.035f) break
            val ring = roundedRectRing(ringWidth, ringHeight, ringRadius, arcSegments)
            val ringIndices = ring.map { p ->
                appendVertex(p.x, p.y, ringZ, (p.x / width) + 0.5f, (p.y / height) + 0.5f)
            }
            stitchRings(prevInnerRing, ringIndices)
            prevInnerRing = ringIndices
            centerSurfaceZ = ringZ
        }
        centerSurfaceZ = rimTopZ - bowlDepth
    } else {
        // 2. Inner wall: from rim inner edge curving into the dome or flat center.
        val rimInnerWallTopIndices = rimInnerRing.map { p ->
            appendVertex(p.x, p.y, innerSurfaceStartZ, (p.x / width) + 0.5f, (p.y / height) + 0.5f)
        }
        stitchRings(rimInnerTopIndices, rimInnerWallTopIndices)

        var prevWallRing: List<Short> = rimInnerWallTopIndices
        for (step in 1..wallRingCount) {
            val t = step.toFloat() / wallRingCount.toFloat()
            val eased = easedProgress(t)
            val ringWidth = lerp(innerWidth, floorWidth, eased)
            val ringHeight = lerp(innerHeight, floorHeight, eased)
            val ringRadius = lerp(innerRadius, floorRadius, eased)
            val ringZ = lerp(innerSurfaceStartZ, floorCenterZ, eased)
            val ring = roundedRectRing(ringWidth, ringHeight, ringRadius, arcSegments)
            val ringIndices = ring.map { p ->
                appendVertex(p.x, p.y, ringZ, (p.x / width) + 0.5f, (p.y / height) + 0.5f)
            }
            stitchRings(prevWallRing, ringIndices)
            prevWallRing = ringIndices
        }

        // 3. Floor surface: concentric rings from floor edge to center.
        var prevFloorRing = prevWallRing
        for (step in 1 until floorRingCount) {
            val t = step.toFloat() / floorRingCount.toFloat()
            val eased = easedProgress(t)
            val scale = 1f - eased
            val baseRingWidth = floorWidth * scale
            val baseRingHeight = floorHeight * scale
            val terminalDiameter = floorTerminalDiameter * scale
            val centerBlend = easedProgress(((t - 0.45f) / 0.55f).coerceIn(0f, 1f))
            val ringWidth = lerp(baseRingWidth, terminalDiameter, centerBlend)
            val ringHeight = lerp(baseRingHeight, terminalDiameter, centerBlend)
            val ringRadius = lerp(floorRadius * scale, terminalDiameter * 0.5f, centerBlend)
            val domeLift = domeAmount * depth * 0.12f * eased
            val ringZ = floorCenterZ + domeLift
            if (ringWidth < 0.035f || ringHeight < 0.035f) break
            val ring =
                roundedRectRing(ringWidth, ringHeight, ringRadius.coerceAtLeast(0.02f), arcSegments)
            val ringIndices = ring.map { p ->
                appendVertex(p.x, p.y, ringZ, (p.x / width) + 0.5f, (p.y / height) + 0.5f)
            }
            stitchRings(prevFloorRing, ringIndices)
            prevFloorRing = ringIndices
            centerSurfaceZ = ringZ
        }
        prevInnerRing = prevFloorRing
        centerSurfaceZ = floorCenterZ + domeAmount * depth * 0.12f
    }

    // Surface center vertex
    val floorCenter = appendVertex(0f, 0f, centerSurfaceZ, 0.5f, 0.5f)
    for (i in prevInnerRing.indices) {
        val j = (i + 1) % prevInnerRing.size
        indices += floorCenter
        indices += prevInnerRing[i]
        indices += prevInnerRing[j]
    }

    // 4. Outer side wall: connects rim top outer edge down to back face
    val backZ = -depth * 0.5f
    val outerBackIndices = rimOuterRing.map { p ->
        appendVertex(p.x, p.y, backZ, (p.x / width) + 0.5f, (p.y / height) + 0.5f)
    }
    stitchRings(rimOuterTopIndices, outerBackIndices, flipWinding = true)

    // 5. Back face: flat bottom
    val backCenter = appendVertex(0f, 0f, backZ, 0.5f, 0.5f)
    for (i in outerBackIndices.indices) {
        val j = (i + 1) % outerBackIndices.size
        indices += backCenter
        indices += outerBackIndices[j]
        indices += outerBackIndices[i]
    }

    val maxZ = maxOf(
        abs(rimTopZ),
        abs(centerSurfaceZ),
        depth * 0.5f,
    )

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
            halfExtent = Float3(width * 0.5f, height * 0.5f, maxZ),
        ),
        primitiveType = PrimitiveType.TRIANGLES,
    )
}

private fun easedProgress(value: Float): Float {
    return value * value * (3f - 2f * value)
}

private fun lerp(
    start: Float,
    end: Float,
    t: Float,
): Float {
    return start + (end - start) * t
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
            val angle =
                startAngles[cornerIndex] + (segment.toDouble() / arcSegments.toDouble()) * (PI * 0.5)
            points += Float3(
                x = center.x + cos(angle).toFloat() * clampedRadius,
                y = center.y + sin(angle).toFloat() * clampedRadius,
                z = 0f,
            )
        }
    }
    return points
}

