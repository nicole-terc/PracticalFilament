package dev.nstv.practicalfilament.screen.otherViewers.sheep2

import androidx.compose.ui.geometry.Offset
import dev.nstv.composablesheep.library.model.FluffStyle
import dev.nstv.practicalfilament.filament.AttributeDataType
import dev.nstv.practicalfilament.filament.BoundingBox
import dev.nstv.practicalfilament.filament.CustomRenderableConfig
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.PrimitiveType
import dev.nstv.practicalfilament.filament.VertexAttribute
import dev.nstv.practicalfilament.filament.VertexAttributeLayout
import dev.nstv.practicalfilament.filament.toByteArray
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal const val Sheep2IndirectLightPath = "files/envs/pillars_2k/pillars_2k_ibl.ktx"
internal const val Sheep2SkyboxPath = "files/envs/pillars_2k/pillars_2k_skybox.ktx"
internal const val Sheep2EnvironmentIntensity = 50_000f
internal const val Sheep2FluffRadius = 1f
private const val Sheep2FluffCoreRadius = 0.42f
private const val Sheep2FluffShellRadius = 0.60f
private const val Sheep2FluffChunkRadius = 0.26f
private const val Sheep2HeadDepth = 0.16f
private const val Sheep2EyeDepthOffset = -0.12f
private const val Sheep2GlassesDepthOffset = -0.02f
private const val Sheep2BridgeDepthOffset = -0.02f
private const val Sheep2HeadRadiusX = 0.43f
private const val Sheep2HeadRadiusY = Sheep2HeadRadiusX * (2f / 3f)
private const val Sheep2HeadRadiusZ = Sheep2HeadRadiusX * 0.75f

internal enum class SheepPieceRole {
    FLUFF_CORE,
    FLUFF_SHELL,
    HEAD,
    EYE,
    PUPIL,
    LEG,
    GLASSES,
}

internal data class SheepRigPiece(
    val handle: Int,
    val role: SheepPieceRole,
    val baseTransform: FloatArray,
    val anchor: Float3,
    val phaseOffset: Float,
    val radialWeight: Float,
    val lagWeight: Float,
)

private data class FluffChunk(
    val center: Float3,
    val radius: Float,
)

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

private data class FacePointSpec(
    val sideOffset: Float,
    val upOffset: Float,
)

internal fun buildSheepRigPieces(
    engine: FilamentEngine,
    fluffInstanceHandle: Int,
    bodyInstanceHandle: Int,
    eyeInstanceHandle: Int,
    pupilInstanceHandle: Int,
    glassesInstanceHandle: Int,
): List<SheepRigPiece> {
    val pieces = mutableListOf<SheepRigPiece>()

    val fluffCoreHandle = engine.createSphereRenderable(
        materialInstanceHandle = fluffInstanceHandle,
        radius = Sheep2FluffCoreRadius,
    )
    pieces += SheepRigPiece(
        handle = fluffCoreHandle,
        role = SheepPieceRole.FLUFF_CORE,
        baseTransform = identityMatrix4(),
        anchor = Float3(0f, 0f, 0f),
        phaseOffset = 0f,
        radialWeight = 0.18f,
        lagWeight = 0.1f,
    )

    fluffSphereSpecs(FluffStyle.Uniform(10), Sheep2FluffRadius).forEachIndexed { index, chunk ->
        val handle = engine.createSphereRenderable(
            materialInstanceHandle = fluffInstanceHandle,
            radius = chunk.radius,
        )
        pieces += SheepRigPiece(
            handle = handle,
            role = SheepPieceRole.FLUFF_SHELL,
            baseTransform = translationMatrix(chunk.center.x, chunk.center.y, chunk.center.z),
            anchor = chunk.center,
            phaseOffset = ringPhase(chunk.center, index),
            radialWeight = (0.45f + chunk.center.length() * 0.55f).coerceAtMost(1f),
            lagWeight = 0.22f + normalizedAbs(chunk.center.y, Sheep2FluffRadius) * 0.18f,
        )
    }

    val headLayout = buildHeadLayout()
    val headBaseTransform = multiplyMatrix4(
        multiplyMatrix4(
            translationMatrix(headLayout.center.x, headLayout.center.y, headLayout.center.z),
            rotationZMatrix(headLayout.rotationDegrees),
        ),
        scaleMatrix(1f, 2f / 3f, 0.75f),
    )
    val headHandle = engine.createSphereRenderable(
        materialInstanceHandle = bodyInstanceHandle,
        radius = Sheep2HeadRadiusX,
    )
    pieces += SheepRigPiece(
        handle = headHandle,
        role = SheepPieceRole.HEAD,
        baseTransform = headBaseTransform,
        anchor = headLayout.center,
        phaseOffset = 0.85f,
        radialWeight = 0.36f,
        lagWeight = 0.52f,
    )

    listOf(
        Triple(SheepPieceRole.EYE, headLayout.leftEyeCenter, headLayout.eyeRadius),
        Triple(SheepPieceRole.EYE, headLayout.rightEyeCenter, headLayout.eyeRadius),
        Triple(SheepPieceRole.PUPIL, headLayout.leftPupilCenter, headLayout.eyeRadius * 0.6f),
        Triple(SheepPieceRole.PUPIL, headLayout.rightPupilCenter, headLayout.eyeRadius * 0.6f),
    ).forEachIndexed { index, (role, center, radius) ->
        val handle = engine.createSphereRenderable(
            materialInstanceHandle = if (role == SheepPieceRole.EYE) eyeInstanceHandle else pupilInstanceHandle,
            radius = radius,
        )
        val baseTransform = if (role == SheepPieceRole.PUPIL) {
            multiplyMatrix4(
                translationMatrix(center.x, center.y, center.z),
                scaleMatrix(0.28f, 0.45f, 1f),
            )
        } else {
            translationMatrix(center.x, center.y, center.z)
        }
        pieces += SheepRigPiece(
            handle = handle,
            role = role,
            baseTransform = baseTransform,
            anchor = center,
            phaseOffset = 1.2f + index * 0.35f,
            radialWeight = 0.16f,
            lagWeight = if (role == SheepPieceRole.PUPIL) 0.84f else 0.68f,
        )
    }

    val legAttachments = listOf(
        Float3(-0.26f, -0.36f, 0.18f),
        Float3(-0.26f, -0.36f, -0.18f),
        Float3(0.18f, -0.36f, 0.18f),
        Float3(0.18f, -0.36f, -0.18f),
    )
    legAttachments.forEachIndexed { index, legAttachment ->
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
        pieces += SheepRigPiece(
            handle = handle,
            role = SheepPieceRole.LEG,
            baseTransform = translationMatrix(legAttachment.x, legAttachment.y, legAttachment.z),
            anchor = legAttachment,
            phaseOffset = 2f + index * 0.31f,
            radialWeight = 0.28f,
            lagWeight = 0.92f,
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
    listOf(headLayout.leftGlassCenter, headLayout.rightGlassCenter).forEachIndexed { index, glassCenter ->
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
        pieces += SheepRigPiece(
            handle = handle,
            role = SheepPieceRole.GLASSES,
            baseTransform = multiplyMatrix4(
                translationMatrix(glassCenter.x, glassCenter.y, glassCenter.z),
                multiplyMatrix4(
                    rotationZMatrix(headLayout.glassesRotationDegrees),
                    rotationYMatrix(90f),
                ),
            ),
            anchor = glassCenter,
            phaseOffset = 2.8f + index * 0.2f,
            radialWeight = 0.12f,
            lagWeight = 0.95f,
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
    pieces += SheepRigPiece(
        handle = bridgeHandle,
        role = SheepPieceRole.GLASSES,
        baseTransform = multiplyMatrix4(
            translationMatrix(bridgeMidpoint.x, bridgeMidpoint.y, bridgeMidpoint.z),
            rotationXMatrix(90f),
        ),
        anchor = bridgeMidpoint,
        phaseOffset = 3.1f,
        radialWeight = 0.08f,
        lagWeight = 1f,
    )

    return pieces
}

private fun buildHeadLayout(radius: Float = Sheep2FluffRadius): HeadLayout {
    val headWidth = Sheep2HeadRadiusX * 2f
    val headHeight = Sheep2HeadRadiusY * 2f
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
    val eyeHeightOffset = headHeight * 0.48f
    val eyeSideOffset = eyeDiameter * 0.72f
    val glassHeightOffset = headHeight * 0.62f
    val glassSideOffset = glassRadius * 0.82f
    val headCenter = Float3(headCenter2D.x, -headCenter2D.y, Sheep2HeadDepth)
    val leftEyeFace = FacePointSpec(sideOffset = -eyeSideOffset, upOffset = eyeHeightOffset)
    val rightEyeFace = FacePointSpec(sideOffset = eyeSideOffset, upOffset = eyeHeightOffset)
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

    val leftEyeCenter = headFrontSurfacePoint(
        facePoint = leftEyeFace,
        headCenter = headCenter,
        headRotationDegrees = headTiltDegrees,
        forwardOffset = Sheep2EyeDepthOffset,
    )
    val rightEyeCenter = headFrontSurfacePoint(
        facePoint = rightEyeFace,
        headCenter = headCenter,
        headRotationDegrees = headTiltDegrees,
        forwardOffset = Sheep2EyeDepthOffset,
    )
    return HeadLayout(
        center = headCenter,
        rotationDegrees = headTiltDegrees,
        glassesRotationDegrees = headTiltDegrees,
        leftEyeCenter = leftEyeCenter,
        rightEyeCenter = rightEyeCenter,
        leftPupilCenter = Float3(
            x = leftEyeCenter.x - eyeRadius * 0.85f,
            y = leftEyeCenter.y,
            z = leftEyeCenter.z,
        ),
        rightPupilCenter = Float3(
            x = rightEyeCenter.x - eyeRadius * 0.85f,
            y = rightEyeCenter.y,
            z = rightEyeCenter.z,
        ),
        eyeRadius = eyeRadius,
        leftGlassCenter = headFrontSurfacePoint(
            facePoint = leftGlassFace,
            headCenter = headCenter,
            headRotationDegrees = headTiltDegrees,
            forwardOffset = Sheep2GlassesDepthOffset,
        ),
        rightGlassCenter = headFrontSurfacePoint(
            facePoint = rightGlassFace,
            headCenter = headCenter,
            headRotationDegrees = headTiltDegrees,
            forwardOffset = Sheep2GlassesDepthOffset,
        ),
        glassRadius = glassRadius,
        bridgeStart = headFrontSurfacePoint(
            facePoint = bridgeStartFace,
            headCenter = headCenter,
            headRotationDegrees = headTiltDegrees,
            forwardOffset = Sheep2BridgeDepthOffset,
        ),
        bridgeEnd = headFrontSurfacePoint(
            facePoint = bridgeEndFace,
            headCenter = headCenter,
            headRotationDegrees = headTiltDegrees,
            forwardOffset = Sheep2BridgeDepthOffset,
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
    val shellRadius = radius * Sheep2FluffShellRadius
    return buildList {
        add(FluffChunk(Float3(0f, shellRadius * 0.84f, 0f), Sheep2FluffChunkRadius * 1.10f))
        addAll(fluffBandSpecs(sampleAngles(centeredAngles, 5), shellRadius, 44f, 0))
        addAll(fluffBandSpecs(boundaryAngles, shellRadius, 16f, 1))
        addAll(fluffBandSpecs(centeredAngles, shellRadius, -16f, 2))
        addAll(fluffBandSpecs(sampleAngles(boundaryAngles, 5, PI.toFloat() / 10f), shellRadius, -44f, 3))
        add(FluffChunk(Float3(0f, -shellRadius * 0.84f, 0f), Sheep2FluffChunkRadius * 1.06f))
    }
}

private fun headFrontSurfacePoint(
    facePoint: FacePointSpec,
    headCenter: Float3,
    headRotationDegrees: Float,
    forwardOffset: Float,
): Float3 {
    val localY = facePoint.upOffset
    val localZ = facePoint.sideOffset
    val normalized =
        (localY * localY) / (Sheep2HeadRadiusY * Sheep2HeadRadiusY) +
            (localZ * localZ) / (Sheep2HeadRadiusZ * Sheep2HeadRadiusZ)
    val localX = -Sheep2HeadRadiusX * sqrt((1f - normalized).coerceAtLeast(0f)) - forwardOffset
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
            radius = Sheep2FluffChunkRadius * radiusScale,
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
        indices += base
        indices += (base + 2).toShort()
        indices += (base + 1).toShort()
        indices += (base + 2).toShort()
        indices += (base + 3).toShort()
        indices += (base + 1).toShort()
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
        indices += topCenter
        indices += next
        indices += current
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
        indices += bottomCenter
        indices += current
        indices += next
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

internal fun identityMatrix4(): FloatArray = floatArrayOf(
    1f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f,
    0f, 0f, 1f, 0f,
    0f, 0f, 0f, 1f,
)

internal fun translationMatrix(x: Float, y: Float, z: Float): FloatArray = floatArrayOf(
    1f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f,
    0f, 0f, 1f, 0f,
    x, y, z, 1f,
)

internal fun scaleMatrix(x: Float, y: Float, z: Float): FloatArray = floatArrayOf(
    x, 0f, 0f, 0f,
    0f, y, 0f, 0f,
    0f, 0f, z, 0f,
    0f, 0f, 0f, 1f,
)

internal fun rotationXMatrix(degrees: Float): FloatArray {
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

internal fun rotationYMatrix(degrees: Float): FloatArray {
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

internal fun rotationZMatrix(degrees: Float): FloatArray {
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

internal fun multiplyMatrix4(left: FloatArray, right: FloatArray): FloatArray {
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

internal fun extractTranslation(matrix: FloatArray): Float3 = Float3(
    x = matrix[12],
    y = matrix[13],
    z = matrix[14],
)

internal fun distance(from: Float3, to: Float3): Float {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val dz = to.z - from.z
    return sqrt(dx * dx + dy * dy + dz * dz)
}

internal fun Float3.length(): Float = sqrt(x * x + y * y + z * z)

internal fun Float3.normalized(default: Float3 = Float3(0f, 1f, 0f)): Float3 {
    val length = length()
    if (length <= 1e-6f) return default
    return Float3(x / length, y / length, z / length)
}

private fun normalizedAbs(value: Float, max: Float): Float =
    (kotlin.math.abs(value) / max.coerceAtLeast(1e-6f)).coerceIn(0f, 1f)

private fun ringPhase(anchor: Float3, index: Int): Float =
    kotlin.math.atan2(anchor.z, anchor.x) + index * 0.13f
