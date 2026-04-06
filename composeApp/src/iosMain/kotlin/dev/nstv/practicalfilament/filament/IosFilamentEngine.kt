package dev.nstv.practicalfilament.filament

import dev.nstv.practicalfilament.filament.material.MaterialParameter
import dev.nstv.practicalfilament.filament.material.MaterialParameterDefinition
import dev.nstv.practicalfilament.filament.material.MaterialParameterPrecision
import dev.nstv.practicalfilament.filament.material.MaterialParameterType
import platform.QuartzCore.CAMetalLayer

/**
 * iOS FilamentEngine implementation that delegates to the Objective-C++ FilamentBridge.
 *
 * The bridge is passed in from Swift since the ObjC++ class lives in the Xcode project,
 * not in the KMP framework. See FilamentView.ios.kt for how it's wired up.
 */
class IosFilamentEngine(
    private val bridge: FilamentBridgeProtocol,
) : FilamentEngine {
    private val materialInstanceMaterials = mutableMapOf<Int, Int>()

    private var _isInitialized = false
    private var currentCameraConfig = CameraConfig()
    override val supportsMaterialBuilder: Boolean = false
    override val isInitialized: Boolean get() = _isInitialized

    fun setClearColor(color: FilamentColor) {
        bridge.setClearColorRGBA(color.r, g = color.g, b = color.b, a = color.a)
    }

    fun attachLayer(metalLayer: CAMetalLayer, width: Int, height: Int, isOpaque: Boolean) {
        bridge.initializeWithMetalLayer(metalLayer, width = width, height = height, isOpaque = isOpaque)
        _isInitialized = true
    }

    override fun initialize() {
        // Handled by attachLayer — requires a CAMetalLayer
    }

    override fun destroy() {
        bridge.destroy()
        materialInstanceMaterials.clear()
        _isInitialized = false
    }

    override fun clearScene() {
        bridge.clearScene()
    }

    override fun updateCamera(config: CameraConfig) {
        currentCameraConfig = config
        bridge.updateCameraEyeX(
            config.position.x, eyeY = config.position.y, eyeZ = config.position.z,
            targetX = config.lookAt.x, targetY = config.lookAt.y, targetZ = config.lookAt.z,
            upX = config.up.x, upY = config.up.y, upZ = config.up.z,
            fov = config.fovDegrees, near = config.near, far = config.far,
            projectionType = config.projectionType.ordinal, orthoZoom = config.orthoZoom,
        )
    }

    override fun setCameraExposure(aperture: Float, shutterSpeed: Float, sensitivity: Float) {
        bridge.setCameraExposure(aperture, shutterSpeed = shutterSpeed, sensitivity = sensitivity)
    }

    override fun addLight(config: LightConfig): Int {
        return bridge.addLightWithType(
            type = config.type.ordinal,
            r = config.color.r, g = config.color.g, b = config.color.b,
            intensity = config.intensity,
            posX = config.position.x, posY = config.position.y, posZ = config.position.z,
            falloffRadius = config.falloffRadius,
            dirX = config.direction.x, dirY = config.direction.y, dirZ = config.direction.z,
            innerCone = config.innerConeAngle, outerCone = config.outerConeAngle,
            sunAngularRadius = config.sunAngularRadius,
            sunHaloSize = config.sunHaloSize,
            sunHaloFalloff = config.sunHaloFalloff,
        )
    }

    override fun removeLight(handle: Int) {
        bridge.removeLight(handle)
    }

    override fun clearLights() {
        bridge.clearLights()
    }

    override fun loadIndirectLight(path: String): Int {
        return bridge.loadIndirectLightFromPath(path)
    }

    override fun setIndirectLight(handle: Int, intensity: Float) {
        bridge.setIndirectLight(handle, intensity = intensity)
    }

    override fun loadSkybox(path: String): Int {
        return bridge.loadSkyboxFromPath(path)
    }

    override fun createColorSkybox(): Int {
        return bridge.createColorSkybox()
    }

    override fun setSkybox(handle: Int) {
        bridge.setSkybox(handle)
    }

    override fun setSkyboxColor(handle: Int, r: Float, g: Float, b: Float, a: Float) {
        bridge.setSkyboxColorHandle(handle, r = r, g = g, b = b, a = a)
    }

    override fun loadMaterial(path: String): Int {
        return bridge.loadMaterialFromPath(path)
    }

    override fun buildMaterial(
        materialSource: String,
        shadingModel: String,
        requiredAttributes: List<VertexAttribute>,
        parameters: List<MaterialParameterDefinition>,
        blendingMode: MaterialBlendingMode,
    ): Int = -1

    override fun compileMaterialPackage(
        materialSource: String,
        shadingModel: String,
        requiredAttributes: List<VertexAttribute>,
        parameters: List<MaterialParameterDefinition>,
        blendingMode: MaterialBlendingMode,
    ): ByteArray? = null

    override fun createMaterialFromPackage(materialPackage: ByteArray): Int = -1

    override fun getMaterialParameters(materialHandle: Int): List<MaterialParameterDefinition> {
        val count = bridge.getMaterialParameterDefinitionCount(materialHandle)
        return List(count) { index ->
            MaterialParameterDefinition(
                name = bridge.getMaterialParameterName(materialHandle, index),
                type = MaterialParameterType.fromRawTypeName(
                    rawTypeName = bridge.getMaterialParameterTypeName(materialHandle, index),
                    arraySize = bridge.getMaterialParameterArraySize(materialHandle, index),
                ),
                precision = when (bridge.getMaterialParameterPrecisionName(materialHandle, index)) {
                    "LOW" -> MaterialParameterPrecision.LOW
                    "MEDIUM" -> MaterialParameterPrecision.MEDIUM
                    "HIGH" -> MaterialParameterPrecision.HIGH
                    else -> MaterialParameterPrecision.DEFAULT
                },
            )
        }
    }

    override fun createMaterialInstance(materialHandle: Int): Int {
        return bridge.createMaterialInstance(materialHandle).also { instanceHandle ->
            materialInstanceMaterials[instanceHandle] = materialHandle
        }
    }

    override fun setMaterialParameter(instanceHandle: Int, param: MaterialParameter) {
        val definition = materialInstanceMaterials[instanceHandle]
            ?.let(::getMaterialParameters)
            ?.firstOrNull { it.name == param.name }

        when (val value = param.value) {
            is Boolean -> bridge.setBoolParam(instanceHandle, name = param.name, value = value)
            is Float -> bridge.setFloatParam(instanceHandle, name = param.name, value = value)
            is Int -> bridge.setIntParam(instanceHandle, name = param.name, value = value)
            is UInt -> bridge.setIntParam(instanceHandle, name = param.name, value = value.toInt())
            is Bool2 -> bridge.setBool2Param(instanceHandle, name = param.name, x = value.x, y = value.y)
            is Bool3 -> bridge.setBool3Param(instanceHandle, name = param.name, x = value.x, y = value.y, z = value.z)
            is Bool4 -> bridge.setBool4Param(instanceHandle, name = param.name, x = value.x, y = value.y, z = value.z, w = value.w)
            is Float2 -> bridge.setFloat2Param(instanceHandle, name = param.name, x = value.x, y = value.y)
            is Float3 -> bridge.setFloat3Param(instanceHandle, name = param.name, x = value.x, y = value.y, z = value.z)
            is Float4 -> bridge.setFloat4Param(instanceHandle, name = param.name, x = value.x, y = value.y, z = value.z, w = value.w)
            is Int2 -> bridge.setInt2Param(instanceHandle, name = param.name, x = value.x, y = value.y)
            is Int3 -> bridge.setInt3Param(instanceHandle, name = param.name, x = value.x, y = value.y, z = value.z)
            is Int4 -> bridge.setInt4Param(instanceHandle, name = param.name, x = value.x, y = value.y, z = value.z, w = value.w)
            is UInt2 -> bridge.setInt2Param(instanceHandle, name = param.name, x = value.x.toInt(), y = value.y.toInt())
            is UInt3 -> bridge.setInt3Param(instanceHandle, name = param.name, x = value.x.toInt(), y = value.y.toInt(), z = value.z.toInt())
            is UInt4 -> bridge.setInt4Param(instanceHandle, name = param.name, x = value.x.toInt(), y = value.y.toInt(), z = value.z.toInt(), w = value.w.toInt())
            is FloatArray -> setFloatArrayParam(instanceHandle, param.name, value, definition)
            is FilamentColor -> bridge.setFloat3Param(instanceHandle, name = param.name, x = value.r, y = value.g, z = value.b)
            is IntArray, is UIntArray, is BooleanArray -> error("Array material parameters are not yet supported by the iOS bridge for ${param.name}")
            else -> error("Unsupported material parameter value for ${param.name}: ${value::class.simpleName}")
        }
    }

    private fun setFloatArrayParam(
        instanceHandle: Int,
        name: String,
        value: FloatArray,
        definition: MaterialParameterDefinition?,
    ) {
        val type = definition?.type ?: error("Missing material definition for float array parameter $name")
        require(type.arraySize == 1) { "Array material parameters are not yet supported by the iOS bridge for $name" }
        when (type) {
            is MaterialParameterType.Float -> {
                require(value.size == 1) { "Parameter $name expects 1 value but received ${value.size}" }
                bridge.setFloatParam(instanceHandle, name = name, value = value[0])
            }
            is MaterialParameterType.Float2 -> {
                require(value.size == 2) { "Parameter $name expects 2 values but received ${value.size}" }
                bridge.setFloat2Param(instanceHandle, name = name, x = value[0], y = value[1])
            }
            is MaterialParameterType.Float3 -> {
                require(value.size == 3) { "Parameter $name expects 3 values but received ${value.size}" }
                bridge.setFloat3Param(instanceHandle, name = name, x = value[0], y = value[1], z = value[2])
            }
            is MaterialParameterType.Float4 -> {
                require(value.size == 4) { "Parameter $name expects 4 values but received ${value.size}" }
                bridge.setFloat4Param(instanceHandle, name = name, x = value[0], y = value[1], z = value[2], w = value[3])
            }
            is MaterialParameterType.Float3x3 -> {
                require(value.size == 9) { "Parameter $name expects 9 values but received ${value.size}" }
                bridge.setMat3Param(instanceHandle, name, value[0], value[1], value[2], value[3], value[4], value[5], value[6], value[7], value[8])
            }
            is MaterialParameterType.Float4x4 -> {
                require(value.size == 16) { "Parameter $name expects 16 values but received ${value.size}" }
                bridge.setMat4Param(
                    instanceHandle,
                    name,
                    value[0], value[1], value[2], value[3],
                    value[4], value[5], value[6], value[7],
                    value[8], value[9], value[10], value[11],
                    value[12], value[13], value[14], value[15],
                )
            }
            else -> error("Float array does not match material parameter type for $name: $type")
        }
    }

    override fun createTexture(width: Int, height: Int, pixels: ByteArray): Int {
        return bridge.createTextureWithWidth(width, height = height, pixels = pixels)
    }

    override fun loadTexture(path: String, colorFormat: TextureColorFormat): Int {
        return bridge.loadTextureFromPath(path, colorFormat = colorFormat.ordinal)
    }

    override fun setTextureParameter(instanceHandle: Int, paramName: String, textureHandle: Int) {
        bridge.setTextureParam(instanceHandle, name = paramName, textureHandle = textureHandle)
    }

    override fun createPlaneRenderable(materialInstanceHandle: Int, width: Float, height: Float): Int {
        return bridge.createPlaneWithMaterial(materialInstanceHandle, width = width, height = height)
    }

    override fun createSphereRenderable(materialInstanceHandle: Int, radius: Float): Int {
        return bridge.createSphereWithMaterial(materialInstanceHandle, radius = radius)
    }

    override fun createCubeRenderable(materialInstanceHandle: Int, size: Float): Int = -1

    override fun createCustomRenderableWithGeneratedTangents(config: CustomRenderableConfig): Int {
        val attributeMetadata = IntArray(config.attributes.size * 4)
        config.attributes.forEachIndexed { index, attribute ->
            val base = index * 4
            attributeMetadata[base] = attribute.attribute.ordinal
            attributeMetadata[base + 1] = attribute.type.ordinal
            attributeMetadata[base + 2] = attribute.offsetBytes
            attributeMetadata[base + 3] = if (attribute.normalized) 1 else 0
        }
        return bridge.createCustomRenderableWithMaterial(
            instanceHandle = config.materialInstanceHandle,
            vertexData = config.vertexData,
            vertexCount = config.vertexCount,
            strideBytes = config.strideBytes,
            attributes = attributeMetadata,
            indices = config.indices,
            primitiveType = config.primitiveType.ordinal,
            centerX = config.boundingBox.center.x,
            centerY = config.boundingBox.center.y,
            centerZ = config.boundingBox.center.z,
            halfExtentX = config.boundingBox.halfExtent.x,
            halfExtentY = config.boundingBox.halfExtent.y,
            halfExtentZ = config.boundingBox.halfExtent.z,
        )
    }

    override fun createCustomRenderable(config: CustomRenderableConfig): Int {
        return bridge.createCustomRenderable(
            vertexData = config.vertexData,
            vertexCount = config.vertexCount,
            strideBytes = config.strideBytes,
            attributeKinds = config.attributes.map { it.attribute.ordinal }.toIntArray(),
            attributeTypes = config.attributes.map { it.type.ordinal }.toIntArray(),
            attributeOffsets = config.attributes.map(VertexAttributeLayout::offsetBytes).toIntArray(),
            attributeNormalized = config.attributes.map(VertexAttributeLayout::normalized).toBooleanArray(),
            indices = config.indices,
            materialInstanceHandle = config.materialInstanceHandle,
            bboxCX = config.boundingBox.center.x,
            bboxCY = config.boundingBox.center.y,
            bboxCZ = config.boundingBox.center.z,
            bboxHX = config.boundingBox.halfExtent.x,
            bboxHY = config.boundingBox.halfExtent.y,
            bboxHZ = config.boundingBox.halfExtent.z,
            primitiveType = config.primitiveType.ordinal,
        )
    }

    override fun loadMesh(path: String, materialInstanceHandle: Int, scale: Float): Int {
        return bridge.loadMeshFromPath(path, materialInstanceHandle = materialInstanceHandle, scale = scale)
    }

    override fun createMorphRenderable(
        materialInstanceHandle: Int,
        geometry: MorphRenderableGeometry,
    ): Int {
        return bridge.createMorphRenderableWithMaterial(
            instanceHandle = materialInstanceHandle,
            positions = geometry.positions,
            uvs = geometry.uvs,
            indices = geometry.indices,
            morphTargetPositions = geometry.flattenedMorphTargetPositions(),
            morphTargetCount = geometry.morphTargetCount,
        )
    }

    override fun setRenderableRotation(handle: Int, rotationXDegrees: Float, rotationYDegrees: Float) {
        bridge.setRenderableRotation(handle, rotationX = rotationXDegrees, rotationY = rotationYDegrees)
    }

    override fun setRenderableTransform(handle: Int, transform: FloatArray) {
        require(transform.size == 16) { "Renderable transform must have 16 values" }
        bridge.setRenderableTransform(
            handle,
            transform[0], transform[1], transform[2], transform[3],
            transform[4], transform[5], transform[6], transform[7],
            transform[8], transform[9], transform[10], transform[11],
            transform[12], transform[13], transform[14], transform[15],
        )
    }

    override fun setShadowsEnabled(renderableHandle: Int, castShadows: Boolean, receiveShadows: Boolean) {
    }

    override fun updateVertexData(renderableHandle: Int, vertexData: ByteArray) {
    }

    override fun setMorphWeights(handle: Int, weights: FloatArray) {
        bridge.setMorphWeights(handle, weights = weights)
    }

    override fun removeRenderable(handle: Int) {
        bridge.removeRenderable(handle)
    }

    override fun createView(viewport: ViewportConfig): Int = -1

    override fun removeView(handle: Int) {
    }

    override fun setViewViewport(handle: Int, viewport: ViewportConfig) {
    }

    override fun setViewCamera(handle: Int, config: CameraConfig) {
    }

    override fun setViewBlendMode(handle: Int, translucent: Boolean) {
    }

    override fun setViewPostProcessing(handle: Int, enabled: Boolean) {
    }

    override fun loadGltfAsset(path: String): Int = -1

    override fun destroyGltfAsset(handle: Int) {
    }

    override fun getGltfAnimationCount(handle: Int): Int = 0

    override fun getGltfAnimationDuration(handle: Int, animationIndex: Int): Float = 0f

    override fun applyGltfAnimation(handle: Int, animationIndex: Int, timeSeconds: Float) {
    }

    override fun updateGltfBoneMatrices(handle: Int) {
    }

    override fun transformGltfToUnitCube(handle: Int) {
    }

    override fun addGltfToScene(handle: Int) {
    }

    override fun removeGltfFromScene(handle: Int) {
    }

    override fun requestFrame() {
        bridge.render()
    }

    fun updateViewport(width: Int, height: Int) {
        bridge.updateViewportWidth(width, height = height)
        updateCamera(currentCameraConfig)
    }
}

/**
 * Protocol that the Objective-C++ FilamentBridge must implement.
 * Defined here so Kotlin/Native can reference it; implemented in FilamentBridge.mm.
 */
interface FilamentBridgeProtocol {
    fun initializeWithMetalLayer(layer: CAMetalLayer, width: Int, height: Int, isOpaque: Boolean)
    fun setClearColorRGBA(r: Float, g: Float, b: Float, a: Float)
    fun destroy()
    fun clearScene()
    fun updateCameraEyeX(
        eyeX: Float, eyeY: Float, eyeZ: Float,
        targetX: Float, targetY: Float, targetZ: Float,
        upX: Float, upY: Float, upZ: Float,
        fov: Double, near: Double, far: Double,
        projectionType: Int, orthoZoom: Double,
    )
    fun setCameraExposure(aperture: Float, shutterSpeed: Float, sensitivity: Float)
    fun addLightWithType(
        type: Int,
        r: Float, g: Float, b: Float,
        intensity: Float,
        posX: Float, posY: Float, posZ: Float,
        falloffRadius: Float,
        dirX: Float, dirY: Float, dirZ: Float,
        innerCone: Float, outerCone: Float,
        sunAngularRadius: Float,
        sunHaloSize: Float,
        sunHaloFalloff: Float,
    ): Int
    fun removeLight(handle: Int)
    fun clearLights()
    fun loadIndirectLightFromPath(path: String): Int
    fun setIndirectLight(handle: Int, intensity: Float)
    fun loadSkyboxFromPath(path: String): Int
    fun createColorSkybox(): Int
    fun setSkybox(handle: Int)
    fun setSkyboxColorHandle(handle: Int, r: Float, g: Float, b: Float, a: Float)
    fun loadMaterialFromPath(path: String): Int
    fun getMaterialParameterDefinitionCount(materialHandle: Int): Int
    fun getMaterialParameterName(materialHandle: Int, index: Int): String
    fun getMaterialParameterTypeName(materialHandle: Int, index: Int): String
    fun getMaterialParameterPrecisionName(materialHandle: Int, index: Int): String
    fun getMaterialParameterArraySize(materialHandle: Int, index: Int): Int
    fun createMaterialInstance(materialHandle: Int): Int
    fun setBoolParam(instanceHandle: Int, name: String, value: Boolean)
    fun setFloatParam(instanceHandle: Int, name: String, value: Float)
    fun setIntParam(instanceHandle: Int, name: String, value: Int)
    fun setBool2Param(instanceHandle: Int, name: String, x: Boolean, y: Boolean)
    fun setBool3Param(instanceHandle: Int, name: String, x: Boolean, y: Boolean, z: Boolean)
    fun setBool4Param(instanceHandle: Int, name: String, x: Boolean, y: Boolean, z: Boolean, w: Boolean)
    fun setFloat2Param(instanceHandle: Int, name: String, x: Float, y: Float)
    fun setFloat3Param(instanceHandle: Int, name: String, x: Float, y: Float, z: Float)
    fun setFloat4Param(instanceHandle: Int, name: String, x: Float, y: Float, z: Float, w: Float)
    fun setInt2Param(instanceHandle: Int, name: String, x: Int, y: Int)
    fun setInt3Param(instanceHandle: Int, name: String, x: Int, y: Int, z: Int)
    fun setInt4Param(instanceHandle: Int, name: String, x: Int, y: Int, z: Int, w: Int)
    fun setMat3Param(
        instanceHandle: Int,
        name: String,
        m00: Float, m01: Float, m02: Float,
        m10: Float, m11: Float, m12: Float,
        m20: Float, m21: Float, m22: Float,
    )
    fun setMat4Param(
        instanceHandle: Int,
        name: String,
        m00: Float, m01: Float, m02: Float, m03: Float,
        m10: Float, m11: Float, m12: Float, m13: Float,
        m20: Float, m21: Float, m22: Float, m23: Float,
        m30: Float, m31: Float, m32: Float, m33: Float,
    )
    fun createTextureWithWidth(width: Int, height: Int, pixels: ByteArray): Int
    fun loadTextureFromPath(path: String, colorFormat: Int): Int
    fun setTextureParam(instanceHandle: Int, name: String, textureHandle: Int)
    fun loadMeshFromPath(path: String, materialInstanceHandle: Int, scale: Float): Int
    fun createPlaneWithMaterial(instanceHandle: Int, width: Float, height: Float): Int
    fun createSphereWithMaterial(instanceHandle: Int, radius: Float): Int
    fun createCustomRenderableWithMaterial(
        instanceHandle: Int,
        vertexData: ByteArray,
        vertexCount: Int,
        strideBytes: Int,
        attributes: IntArray,
        indices: ShortArray,
        primitiveType: Int,
        centerX: Float,
        centerY: Float,
        centerZ: Float,
        halfExtentX: Float,
        halfExtentY: Float,
        halfExtentZ: Float,
    ): Int
    fun createCustomRenderable(
        vertexData: ByteArray,
        vertexCount: Int,
        strideBytes: Int,
        attributeKinds: IntArray,
        attributeTypes: IntArray,
        attributeOffsets: IntArray,
        attributeNormalized: BooleanArray,
        indices: ShortArray,
        materialInstanceHandle: Int,
        bboxCX: Float,
        bboxCY: Float,
        bboxCZ: Float,
        bboxHX: Float,
        bboxHY: Float,
        bboxHZ: Float,
        primitiveType: Int,
    ): Int
    fun createMorphRenderableWithMaterial(
        instanceHandle: Int,
        positions: FloatArray,
        uvs: FloatArray,
        indices: ShortArray,
        morphTargetPositions: FloatArray,
        morphTargetCount: Int,
    ): Int
    fun setRenderableRotation(handle: Int, rotationX: Float, rotationY: Float)
    fun setRenderableTransform(
        handle: Int,
        m00: Float, m01: Float, m02: Float, m03: Float,
        m10: Float, m11: Float, m12: Float, m13: Float,
        m20: Float, m21: Float, m22: Float, m23: Float,
        m30: Float, m31: Float, m32: Float, m33: Float,
    )
    fun setMorphWeights(handle: Int, weights: FloatArray)
    fun removeRenderable(handle: Int)
    fun render()
    fun updateViewportWidth(width: Int, height: Int)
}
