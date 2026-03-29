package dev.nstv.practicalfilament.filament

import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile
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
    override val isInitialized: Boolean get() = _isInitialized

    fun setClearColor(color: Color) {
        bridge.setClearColorRGBA(color.r, g = color.g, b = color.b, a = color.a)
    }

    fun attachLayer(metalLayer: CAMetalLayer, width: Int, height: Int) {
        bridge.initializeWithMetalLayer(metalLayer, width = width, height = height)
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
        bridge.updateCameraEyeX(
            config.position.x, eyeY = config.position.y, eyeZ = config.position.z,
            targetX = config.lookAt.x, targetY = config.lookAt.y, targetZ = config.lookAt.z,
            upX = config.up.x, upY = config.up.y, upZ = config.up.z,
            fov = config.fovDegrees, near = config.near, far = config.far,
        )
    }

    override fun addLight(config: LightConfig): Int {
        return bridge.addLightWithType(
            type = config.type.ordinal,
            r = config.color.r, g = config.color.g, b = config.color.b,
            intensity = config.intensity,
            posX = config.position.x, posY = config.position.y, posZ = config.position.z,
            dirX = config.direction.x, dirY = config.direction.y, dirZ = config.direction.z,
            innerCone = config.innerConeAngle, outerCone = config.outerConeAngle,
        )
    }

    override fun removeLight(handle: Int) {
        bridge.removeLight(handle)
    }

    override fun clearLights() {
        bridge.clearLights()
    }

    override fun loadMaterial(path: String): Int {
        return bridge.loadMaterialFromPath(path)
    }

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
            is Color -> bridge.setFloat3Param(instanceHandle, name = param.name, x = value.r, y = value.g, z = value.b)
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

    override fun setTextureParameter(instanceHandle: Int, paramName: String, textureHandle: Int) {
        bridge.setTextureParam(instanceHandle, name = paramName, textureHandle = textureHandle)
    }

    override fun createPlaneRenderable(materialInstanceHandle: Int, width: Float, height: Float): Int {
        return bridge.createPlaneWithMaterial(materialInstanceHandle, width = width, height = height)
    }

    override fun createSphereRenderable(materialInstanceHandle: Int, radius: Float): Int {
        return bridge.createSphereWithMaterial(materialInstanceHandle, radius = radius)
    }

    override fun removeRenderable(handle: Int) {
        bridge.removeRenderable(handle)
    }

    override fun requestFrame() {
        bridge.render()
    }

    fun updateViewport(width: Int, height: Int) {
        bridge.updateViewportWidth(width, height = height)
    }
}

/**
 * Protocol that the Objective-C++ FilamentBridge must implement.
 * Defined here so Kotlin/Native can reference it; implemented in FilamentBridge.mm.
 */
interface FilamentBridgeProtocol {
    fun initializeWithMetalLayer(layer: CAMetalLayer, width: Int, height: Int)
    fun setClearColorRGBA(r: Float, g: Float, b: Float, a: Float)
    fun destroy()
    fun clearScene()
    fun updateCameraEyeX(
        eyeX: Float, eyeY: Float, eyeZ: Float,
        targetX: Float, targetY: Float, targetZ: Float,
        upX: Float, upY: Float, upZ: Float,
        fov: Double, near: Double, far: Double,
    )
    fun addLightWithType(
        type: Int,
        r: Float, g: Float, b: Float,
        intensity: Float,
        posX: Float, posY: Float, posZ: Float,
        dirX: Float, dirY: Float, dirZ: Float,
        innerCone: Float, outerCone: Float,
    ): Int
    fun removeLight(handle: Int)
    fun clearLights()
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
    fun setTextureParam(instanceHandle: Int, name: String, textureHandle: Int)
    fun createPlaneWithMaterial(instanceHandle: Int, width: Float, height: Float): Int
    fun createSphereWithMaterial(instanceHandle: Int, radius: Float): Int
    fun removeRenderable(handle: Int)
    fun render()
    fun updateViewportWidth(width: Int, height: Int)
}
