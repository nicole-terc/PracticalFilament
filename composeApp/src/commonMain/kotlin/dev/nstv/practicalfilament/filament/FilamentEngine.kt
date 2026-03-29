package dev.nstv.practicalfilament.filament

import dev.nstv.practicalfilament.filament.material.MaterialParameter
import dev.nstv.practicalfilament.filament.material.MaterialParameterDefinition

interface FilamentEngine {
    fun initialize()
    fun destroy()
    val isInitialized: Boolean

    // Scene
    fun clearScene()

    // Camera
    fun updateCamera(config: CameraConfig)

    // Lighting
    fun addLight(config: LightConfig): Int
    fun removeLight(handle: Int)
    fun clearLights()

    // Materials
    fun loadMaterial(path: String): Int
    fun getMaterialParameters(materialHandle: Int): List<MaterialParameterDefinition>
    fun createMaterialInstance(materialHandle: Int): Int
    fun setMaterialParameter(instanceHandle: Int, param: MaterialParameter)

    // Textures
    fun createTexture(width: Int, height: Int, pixels: ByteArray): Int
    fun setTextureParameter(instanceHandle: Int, paramName: String, textureHandle: Int)

    // Renderables
    fun createPlaneRenderable(materialInstanceHandle: Int, width: Float = 2f, height: Float = 2f): Int
    fun createSphereRenderable(materialInstanceHandle: Int, radius: Float = 1f): Int
    fun setRenderableRotation(handle: Int, rotationXDegrees: Float, rotationYDegrees: Float)
    fun setRenderableTransform(handle: Int, transform: FloatArray)
    fun removeRenderable(handle: Int)

    // Rendering
    fun requestFrame()
}
