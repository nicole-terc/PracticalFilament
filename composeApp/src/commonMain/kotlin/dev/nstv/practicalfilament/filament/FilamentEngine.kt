package dev.nstv.practicalfilament.filament

import dev.nstv.practicalfilament.filament.material.LoadedMaterial
import dev.nstv.practicalfilament.filament.material.Material
import dev.nstv.practicalfilament.filament.material.MaterialParameter
import dev.nstv.practicalfilament.filament.material.MaterialParameterDefinition
import dev.nstv.practicalfilament.filament.material.TextureMaterial
import dev.nstv.practicalfilament.filament.material.buildMaterialParameters
import practicalfilament.composeapp.generated.resources.Res

interface FilamentEngine {
    fun initialize()
    fun destroy()
    val isInitialized: Boolean
    val supportsMaterialBuilder: Boolean get() = false

    // Scene
    fun clearScene()

    // Camera
    fun updateCamera(config: CameraConfig)
    fun setCameraExposure(aperture: Float, shutterSpeed: Float, sensitivity: Float) {}

    // Lighting
    fun addLight(config: LightConfig): Int
    fun removeLight(handle: Int)
    fun clearLights()

    // Environment
    fun loadIndirectLight(path: String): Int
    fun setIndirectLight(handle: Int, intensity: Float = 30_000f)
    fun loadSkybox(path: String): Int
    fun createColorSkybox(): Int = -1
    fun setSkybox(handle: Int)
    fun setSkyboxColor(handle: Int, r: Float, g: Float, b: Float, a: Float) {}

    // Materials
    fun loadMaterial(path: String): Int
    fun loadMaterial(material: Material): LoadedMaterial {
        val materialHandle = loadMaterial(Res.getUri(material.materialPath))
        if (materialHandle <= 0) {
            return LoadedMaterial(
                instanceHandle = -1,
                definitions = emptyList(),
                parameters = emptyMap(),
            )
        }

        val definitions = getMaterialParameters(materialHandle)
        val instanceHandle = createMaterialInstance(materialHandle)
        if (instanceHandle <= 0) {
            return LoadedMaterial(
                instanceHandle = -1,
                definitions = definitions,
                parameters = emptyMap(),
            )
        }

        val parameters = buildMaterialParameters(definitions, material)
        val textureHandles = if (material is TextureMaterial) {
            buildMap {
                material.textureBindings.forEach { binding ->
                    val textureHandle = loadTexture(Res.getUri(binding.texturePath))
                    if (textureHandle > 0) {
                        setTextureParameter(instanceHandle, binding.parameterName, textureHandle)
                        put(binding.texturePath, textureHandle)
                    }
                }
            }
        } else {
            emptyMap()
        }

        return LoadedMaterial(
            instanceHandle = instanceHandle,
            definitions = definitions,
            parameters = parameters,
            textureHandles = textureHandles,
        )
    }
    fun getMaterialParameters(materialHandle: Int): List<MaterialParameterDefinition>
    fun createMaterialInstance(materialHandle: Int): Int
    fun setMaterialParameter(instanceHandle: Int, param: MaterialParameter)
    fun buildMaterial(
        materialSource: String,
        shadingModel: String = "lit",
        requiredAttributes: List<VertexAttribute> = emptyList(),
        parameters: List<MaterialParameterDefinition> = emptyList(),
        blendingMode: MaterialBlendingMode = MaterialBlendingMode.OPAQUE,
    ): Int = -1
    fun compileMaterialPackage(
        materialSource: String,
        shadingModel: String = "lit",
        requiredAttributes: List<VertexAttribute> = emptyList(),
        parameters: List<MaterialParameterDefinition> = emptyList(),
        blendingMode: MaterialBlendingMode = MaterialBlendingMode.OPAQUE,
    ): ByteArray? = null
    fun createMaterialFromPackage(materialPackage: ByteArray): Int = -1

    // Textures
    fun createTexture(width: Int, height: Int, pixels: ByteArray): Int
    fun loadTexture(path: String): Int = -1
    fun setTextureParameter(instanceHandle: Int, paramName: String, textureHandle: Int)

    // Renderables
    fun createPlaneRenderable(materialInstanceHandle: Int, width: Float = 2f, height: Float = 2f): Int
    fun createSphereRenderable(materialInstanceHandle: Int, radius: Float = 1f): Int
    fun createCubeRenderable(materialInstanceHandle: Int, size: Float = 1f): Int = -1
    fun createMorphRenderable(materialInstanceHandle: Int, geometry: MorphRenderableGeometry): Int
    fun createCustomRenderable(config: CustomRenderableConfig): Int = -1
    fun createCustomRenderableWithGeneratedTangents(config: CustomRenderableConfig): Int = -1
    fun loadMesh(path: String, materialInstanceHandle: Int): Int = -1
    fun setRenderableRotation(handle: Int, rotationXDegrees: Float, rotationYDegrees: Float)
    fun setRenderableTransform(handle: Int, transform: FloatArray)
    fun setShadowsEnabled(renderableHandle: Int, castShadows: Boolean, receiveShadows: Boolean) {}
    fun updateVertexData(renderableHandle: Int, vertexData: ByteArray) {}
    fun setMorphWeights(handle: Int, weights: FloatArray)
    fun removeRenderable(handle: Int)

    // Multi-view
    fun createView(viewport: ViewportConfig): Int = -1
    fun removeView(handle: Int) {}
    fun setViewViewport(handle: Int, viewport: ViewportConfig) {}
    fun setViewCamera(handle: Int, config: CameraConfig) {}
    fun setViewBlendMode(handle: Int, translucent: Boolean) {}
    fun setViewPostProcessing(handle: Int, enabled: Boolean) {}

    // glTF
    fun loadGltfAsset(path: String): Int = -1
    fun destroyGltfAsset(handle: Int) {}
    fun getGltfAnimationCount(handle: Int): Int = 0
    fun getGltfAnimationDuration(handle: Int, animationIndex: Int): Float = 0f
    fun applyGltfAnimation(handle: Int, animationIndex: Int, timeSeconds: Float) {}
    fun updateGltfBoneMatrices(handle: Int) {}
    fun transformGltfToUnitCube(handle: Int) {}
    fun addGltfToScene(handle: Int) {}
    fun removeGltfFromScene(handle: Int) {}

    // Rendering
    fun requestFrame()
}
