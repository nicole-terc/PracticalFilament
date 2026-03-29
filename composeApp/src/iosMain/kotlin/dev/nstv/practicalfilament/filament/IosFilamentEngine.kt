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

    private var _isInitialized = false
    override val isInitialized: Boolean get() = _isInitialized

    fun attachLayer(metalLayer: CAMetalLayer, width: Int, height: Int) {
        bridge.initializeWithMetalLayer(metalLayer, width = width, height = height)
        _isInitialized = true
    }

    override fun initialize() {
        // Handled by attachLayer — requires a CAMetalLayer
    }

    override fun destroy() {
        bridge.destroy()
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

    override fun createMaterialInstance(materialHandle: Int): Int {
        return bridge.createMaterialInstance(materialHandle)
    }

    override fun setMaterialParameter(instanceHandle: Int, param: MaterialParameter) {
        when (val value = param.value) {
            is Float -> bridge.setFloatParam(instanceHandle, name = param.name, value = value)
            is Int -> bridge.setIntParam(instanceHandle, name = param.name, value = value)
            is Float3 -> bridge.setFloat3Param(instanceHandle, name = param.name, x = value.x, y = value.y, z = value.z)
            is Float4 -> bridge.setFloat4Param(instanceHandle, name = param.name, x = value.x, y = value.y, z = value.z, w = value.w)
            is Color -> bridge.setFloat3Param(instanceHandle, name = param.name, x = value.r, y = value.g, z = value.b)
        }
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
    fun createMaterialInstance(materialHandle: Int): Int
    fun setFloatParam(instanceHandle: Int, name: String, value: Float)
    fun setIntParam(instanceHandle: Int, name: String, value: Int)
    fun setFloat3Param(instanceHandle: Int, name: String, x: Float, y: Float, z: Float)
    fun setFloat4Param(instanceHandle: Int, name: String, x: Float, y: Float, z: Float, w: Float)
    fun createPlaneWithMaterial(instanceHandle: Int, width: Float, height: Float): Int
    fun createSphereWithMaterial(instanceHandle: Int, radius: Float): Int
    fun removeRenderable(handle: Int)
    fun render()
    fun updateViewportWidth(width: Int, height: Int)
}
