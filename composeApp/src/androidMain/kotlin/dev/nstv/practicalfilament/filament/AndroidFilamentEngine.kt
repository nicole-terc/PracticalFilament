package dev.nstv.practicalfilament.filament

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.Matrix
import android.util.Log
import android.view.Choreographer
import android.view.Surface
import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.IndexBuffer
import com.google.android.filament.IndirectLight
import com.google.android.filament.LightManager
import com.google.android.filament.Material
import com.google.android.filament.MaterialInstance
import com.google.android.filament.MorphTargetBuffer
import com.google.android.filament.RenderableManager
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.SurfaceOrientation
import com.google.android.filament.SwapChain
import com.google.android.filament.Texture
import com.google.android.filament.TextureSampler
import com.google.android.filament.VertexBuffer
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.android.UiHelper
import com.google.android.filament.filamat.MaterialBuilder
import com.google.android.filament.gltfio.Animator
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider
import com.google.android.filament.utils.KTX1Loader
import com.google.android.filament.utils.Utils
import dev.nstv.practicalfilament.filament.material.MaterialParameter
import dev.nstv.practicalfilament.filament.material.MaterialParameterDefinition
import dev.nstv.practicalfilament.filament.material.MaterialParameterPrecision
import dev.nstv.practicalfilament.filament.material.MaterialParameterType
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private const val MaxDecodedTextureDimension = 2048

class AndroidFilamentEngine(
    private val context: Context,
) : FilamentEngine {

    private var engine: Engine? = null
    private var renderer: Renderer? = null
    private var scene: Scene? = null
    private var filamentView: View? = null
    private var camera: Camera? = null
    private var cameraEntity: Int = 0
    private var swapChain: SwapChain? = null
    private var uiHelper: UiHelper? = null
    private var choreographer: Choreographer? = null
    private var assetLoader: AssetLoader? = null
    private var resourceLoader: ResourceLoader? = null
    private var materialProvider: UbershaderProvider? = null

    private val lights = mutableMapOf<Int, Int>()
    private val renderables = mutableMapOf<Int, Int>()
    private val materials = mutableMapOf<Int, Material>()
    private val materialInstances = mutableMapOf<Int, MaterialInstance>()
    private val materialInstanceMaterials = mutableMapOf<Int, Int>()
    private val materialParameterDefinitions = mutableMapOf<Int, Map<String, MaterialParameterDefinition>>()
    private val textures = mutableMapOf<Int, Texture>()
    private val morphTargetBuffers = mutableMapOf<Int, MorphTargetBuffer>()
    private val indirectLights = mutableMapOf<Int, EnvironmentIndirectLight>()
    private val skyboxes = mutableMapOf<Int, EnvironmentSkybox>()
    private val vertexBuffers = mutableListOf<VertexBuffer>()
    private val indexBuffers = mutableListOf<IndexBuffer>()
    private val additionalViews = mutableMapOf<Int, AuxiliaryView>()
    private val renderableBuffers = mutableMapOf<Int, RenderableBuffers>()
    private val gltfAssets = mutableMapOf<Int, ManagedGltfAsset>()

    private var nextHandle = 1
    private var _isInitialized = false
    private var rendering = false
    private var viewportWidth = 1
    private var viewportHeight = 1
    private var currentCameraConfig = CameraConfig()

    override val supportsMaterialBuilder: Boolean = true

    override val isInitialized: Boolean get() = _isInitialized

    fun setClearColor(color: FilamentColor) {
        renderer?.clearOptions = Renderer.ClearOptions().apply {
            clear = true
            clearColor = floatArrayOf(color.r, color.g, color.b, color.a)
        }
    }

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!rendering) return
            choreographer?.postFrameCallback(this)

            if (uiHelper?.isReadyToRender == true) {
                val sc = swapChain ?: return
                val r = renderer ?: return
                val baseView = filamentView ?: return

                if (r.beginFrame(sc, frameTimeNanos)) {
                    if (additionalViews.isEmpty()) {
                        r.render(baseView)
                    } else {
                        additionalViews.values.forEach { auxiliaryView ->
                            r.render(auxiliaryView.view)
                        }
                    }
                    r.endFrame()
                }
            }
        }
    }

    override fun initialize() {
        if (_isInitialized) return
        Utils.init()
        engine = Engine.create().also { eng ->
            renderer = eng.createRenderer()
            setClearColor(FilamentColor(0f, 0f, 0f, 1f))
            scene = eng.createScene()
            filamentView = eng.createView().apply {
                this.scene = this@AndroidFilamentEngine.scene
            }
            cameraEntity = EntityManager.get().create()
            camera = eng.createCamera(cameraEntity).also {
                filamentView?.camera = it
            }
            materialProvider = UbershaderProvider(eng)
            assetLoader = AssetLoader(eng, materialProvider!!, EntityManager.get())
            resourceLoader = ResourceLoader(eng, true)
        }
        choreographer = Choreographer.getInstance()
        MaterialBuilder.init()
        _isInitialized = true
    }

    override fun destroy() {
        stopRenderLoop()
        val eng = engine ?: return
        scene?.setIndirectLight(null)
        scene?.setSkybox(null)

        gltfAssets.values.forEach { managedAsset ->
            scene?.removeEntities(managedAsset.asset.entities)
            assetLoader?.destroyAsset(managedAsset.asset)
        }
        gltfAssets.clear()

        renderables.values.forEach { entity ->
            eng.destroyEntity(entity)
            EntityManager.get().destroy(entity)
        }
        renderables.clear()
        renderableBuffers.clear()
        morphTargetBuffers.values.forEach { eng.destroyMorphTargetBuffer(it) }
        morphTargetBuffers.clear()

        lights.values.forEach { entity ->
            eng.destroyEntity(entity)
            EntityManager.get().destroy(entity)
        }
        lights.clear()

        materialInstances.values.forEach { eng.destroyMaterialInstance(it) }
        materialInstances.clear()
        materialInstanceMaterials.clear()

        materials.values.forEach { eng.destroyMaterial(it) }
        materials.clear()
        materialParameterDefinitions.clear()

        indirectLights.values.forEach { bundle ->
            eng.destroyIndirectLight(bundle.indirectLight)
            eng.destroyTexture(bundle.cubemap)
        }
        indirectLights.clear()

        skyboxes.values.forEach { bundle ->
            eng.destroySkybox(bundle.skybox)
            bundle.cubemap?.let(eng::destroyTexture)
        }
        skyboxes.clear()

        textures.values.forEach { eng.destroyTexture(it) }
        textures.clear()

        vertexBuffers.forEach { eng.destroyVertexBuffer(it) }
        vertexBuffers.clear()
        indexBuffers.forEach { eng.destroyIndexBuffer(it) }
        indexBuffers.clear()

        additionalViews.values.forEach { auxiliaryView ->
            eng.destroyCameraComponent(auxiliaryView.cameraEntity)
            eng.destroyView(auxiliaryView.view)
            EntityManager.get().destroy(auxiliaryView.cameraEntity)
        }
        additionalViews.clear()

        filamentView?.let { eng.destroyView(it) }
        scene?.let { eng.destroyScene(it) }
        camera?.let { eng.destroyCameraComponent(cameraEntity) }
        renderer?.let { eng.destroyRenderer(it) }
        swapChain?.let { eng.destroySwapChain(it) }
        resourceLoader?.destroy()
        resourceLoader = null
        assetLoader?.destroy()
        assetLoader = null
        materialProvider?.destroyMaterials()
        materialProvider = null

        eng.destroy()
        engine = null
        _isInitialized = false
    }

    fun attachSurface(surface: Surface, swapChainFlags: Long = 0L) {
        swapChain?.let { engine?.destroySwapChain(it) }
        swapChain = engine?.createSwapChain(surface, swapChainFlags)
    }

    fun detachSurface() {
        swapChain?.let { engine?.destroySwapChain(it) }
        swapChain = null
    }

    fun updateViewport(width: Int, height: Int) {
        viewportWidth = width.coerceAtLeast(1)
        viewportHeight = height.coerceAtLeast(1)
        filamentView?.viewport = Viewport(0, 0, width, height)
        applyCameraConfig(camera ?: return, currentCameraConfig, viewportWidth, viewportHeight)
    }

    fun setUiHelper(helper: UiHelper) {
        uiHelper = helper
    }

    fun startRenderLoop() {
        rendering = true
        choreographer?.postFrameCallback(frameCallback)
    }

    fun stopRenderLoop() {
        rendering = false
        choreographer?.removeFrameCallback(frameCallback)
    }

    override fun clearScene() {
        renderables.keys.toList().forEach(::destroyRenderableEntity)
    }

    override fun updateCamera(config: CameraConfig) {
        currentCameraConfig = config
        camera?.lookAt(
            config.position.x.toDouble(), config.position.y.toDouble(), config.position.z.toDouble(),
            config.lookAt.x.toDouble(), config.lookAt.y.toDouble(), config.lookAt.z.toDouble(),
            config.up.x.toDouble(), config.up.y.toDouble(), config.up.z.toDouble()
        )
        applyCameraConfig(camera ?: return, config, viewportWidth, viewportHeight)
    }

    override fun setCameraExposure(aperture: Float, shutterSpeed: Float, sensitivity: Float) {
        camera?.setExposure(aperture, shutterSpeed, sensitivity)
    }

    override fun addLight(config: LightConfig): Int {
        val eng = engine ?: return -1
        val entity = EntityManager.get().create()

        val type = when (config.type) {
            LightType.DIRECTIONAL -> LightManager.Type.DIRECTIONAL
            LightType.POINT -> LightManager.Type.POINT
            LightType.SPOT -> LightManager.Type.SPOT
            LightType.SUN -> LightManager.Type.SUN
        }

        LightManager.Builder(type)
            .color(config.color.r, config.color.g, config.color.b)
            .intensity(config.intensity)
            .position(config.position.x, config.position.y, config.position.z)
            .direction(config.direction.x, config.direction.y, config.direction.z)
            .apply {
                if (config.type == LightType.POINT || config.type == LightType.SPOT) {
                    falloff(config.falloffRadius)
                }
                if (config.type == LightType.SPOT) {
                    spotLightCone(config.innerConeAngle, config.outerConeAngle)
                }
                if (config.type == LightType.SUN) {
                    sunAngularRadius(config.sunAngularRadius)
                    sunHaloSize(config.sunHaloSize)
                    sunHaloFalloff(config.sunHaloFalloff)
                }
                castShadows(config.castShadows)
            }
            .build(eng, entity)

        scene?.addEntity(entity)
        val handle = nextHandle++
        lights[handle] = entity
        return handle
    }

    override fun removeLight(handle: Int) {
        val entity = lights.remove(handle) ?: return
        val eng = engine ?: return
        scene?.removeEntity(entity)
        eng.destroyEntity(entity)
        EntityManager.get().destroy(entity)
    }

    override fun clearLights() {
        val eng = engine ?: return
        lights.values.forEach { entity ->
            scene?.removeEntity(entity)
            eng.destroyEntity(entity)
            EntityManager.get().destroy(entity)
        }
        lights.clear()
    }

    override fun loadIndirectLight(path: String): Int {
        val eng = engine ?: return -1
        val buffer = loadAssetBuffer(path) ?: return -1
        val bundle = KTX1Loader.createIndirectLight(eng, buffer)
        val indirectLight = bundle.indirectLight ?: return -1
        val cubemap = bundle.cubemap ?: return -1
        val handle = nextHandle++
        indirectLights[handle] = EnvironmentIndirectLight(indirectLight, cubemap)
        return handle
    }

    override fun setIndirectLight(handle: Int, intensity: Float) {
        val bundle = indirectLights[handle] ?: return
        bundle.indirectLight.intensity = intensity
        scene?.setIndirectLight(bundle.indirectLight)
    }

    override fun loadSkybox(path: String): Int {
        val eng = engine ?: return -1
        val buffer = loadAssetBuffer(path) ?: return -1
        val bundle = KTX1Loader.createSkybox(eng, buffer)
        val skybox = bundle.skybox ?: return -1
        val cubemap = bundle.cubemap ?: return -1
        val handle = nextHandle++
        skyboxes[handle] = EnvironmentSkybox(skybox, cubemap)
        return handle
    }

    override fun createColorSkybox(): Int {
        val eng = engine ?: return -1
        val skybox = com.google.android.filament.Skybox.Builder().build(eng)
        val handle = nextHandle++
        skyboxes[handle] = EnvironmentSkybox(skybox, cubemap = null)
        return handle
    }

    override fun setSkybox(handle: Int) {
        val bundle = skyboxes[handle] ?: return
        scene?.setSkybox(bundle.skybox)
    }

    override fun setSkyboxColor(handle: Int, r: Float, g: Float, b: Float, a: Float) {
        val bundle = skyboxes[handle] ?: return
        bundle.skybox.setColor(floatArrayOf(r, g, b, a))
    }

    override fun loadMaterial(path: String): Int {
        val eng = engine ?: return -1
        val buffer = loadAssetBuffer(path) ?: return -1
        val material = Material.Builder()
            .payload(buffer, buffer.remaining())
            .build(eng)

        val handle = nextHandle++
        materials[handle] = material
        return handle
    }

    override fun buildMaterial(
        materialSource: String,
        shadingModel: String,
        requiredAttributes: List<dev.nstv.practicalfilament.filament.VertexAttribute>,
        parameters: List<MaterialParameterDefinition>,
        blendingMode: dev.nstv.practicalfilament.filament.MaterialBlendingMode,
    ): Int {
        val materialPackage = compileMaterialPackage(
            materialSource = materialSource,
            shadingModel = shadingModel,
            requiredAttributes = requiredAttributes,
            parameters = parameters,
            blendingMode = blendingMode,
        ) ?: return -1
        return createMaterialFromPackage(materialPackage)
    }

    override fun compileMaterialPackage(
        materialSource: String,
        shadingModel: String,
        requiredAttributes: List<dev.nstv.practicalfilament.filament.VertexAttribute>,
        parameters: List<MaterialParameterDefinition>,
        blendingMode: dev.nstv.practicalfilament.filament.MaterialBlendingMode,
    ): ByteArray? {
        val materialBuilder = createRuntimeMaterialBuilder(
            materialSource = materialSource,
            shadingModel = shadingModel,
            requiredAttributes = requiredAttributes,
            parameters = parameters,
            blendingMode = blendingMode,
        )
        val materialPackage = materialBuilder.build()
        if (!materialPackage.isValid) {
            return null
        }
        val buffer = materialPackage.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return bytes
    }

    override fun createMaterialFromPackage(materialPackage: ByteArray): Int {
        val eng = engine ?: return -1
        val material = runCatching {
            Material.Builder()
                .payload(ByteBuffer.wrap(materialPackage), materialPackage.size)
                .build(eng)
        }.getOrElse {
            return -1
        }
        val handle = nextHandle++
        materials[handle] = material
        return handle
    }

    override fun getMaterialParameters(materialHandle: Int): List<MaterialParameterDefinition> {
        return getMaterialParameterDefinitionMap(materialHandle).values.toList()
    }

    private fun getMaterialParameterDefinitionMap(materialHandle: Int): Map<String, MaterialParameterDefinition> {
        materialParameterDefinitions[materialHandle]?.let { return it }

        val material = materials[materialHandle] ?: return emptyMap()
        val definitions = material.parameters.map { parameter ->
            MaterialParameterDefinition(
                name = parameter.name,
                type = MaterialParameterType.fromRawTypeName(
                    rawTypeName = parameter.type.name,
                    arraySize = parameter.count,
                ),
                precision = when (parameter.precision) {
                    Material.Parameter.Precision.LOW -> MaterialParameterPrecision.LOW
                    Material.Parameter.Precision.MEDIUM -> MaterialParameterPrecision.MEDIUM
                    Material.Parameter.Precision.HIGH -> MaterialParameterPrecision.HIGH
                    Material.Parameter.Precision.DEFAULT -> MaterialParameterPrecision.DEFAULT
                },
            )
        }.associateBy(MaterialParameterDefinition::name)
        materialParameterDefinitions[materialHandle] = definitions
        return definitions
    }

    private fun createRuntimeMaterialBuilder(
        materialSource: String,
        shadingModel: String,
        requiredAttributes: List<dev.nstv.practicalfilament.filament.VertexAttribute>,
        parameters: List<MaterialParameterDefinition>,
        blendingMode: dev.nstv.practicalfilament.filament.MaterialBlendingMode,
    ): MaterialBuilder {
        val shading = when (shadingModel.lowercase()) {
            "unlit" -> MaterialBuilder.Shading.UNLIT
            "cloth" -> MaterialBuilder.Shading.CLOTH
            "subsurface" -> MaterialBuilder.Shading.SUBSURFACE
            "specular_glossiness" -> MaterialBuilder.Shading.SPECULAR_GLOSSINESS
            else -> MaterialBuilder.Shading.LIT
        }
        return MaterialBuilder()
            .name("RuntimeMaterial${nextHandle}")
            .platform(MaterialBuilder.Platform.MOBILE)
            .targetApi(MaterialBuilder.TargetApi.ALL)
            .shading(shading)
            .require(MaterialBuilder.VertexAttribute.POSITION)
            .apply {
                if (shading != MaterialBuilder.Shading.UNLIT) {
                    require(MaterialBuilder.VertexAttribute.TANGENTS)
                }
            }
            .material(materialSource)
            .blending(blendingMode.toFilamentBlendingMode())
            .also { materialBuilder ->
                requiredAttributes
                    .map { it.toFilamatVertexAttribute() }
                    .distinct()
                    .forEach(materialBuilder::require)
                parameters.forEach(materialBuilder::addRuntimeParameter)
            }
    }

    override fun createMaterialInstance(materialHandle: Int): Int {
        val material = materials[materialHandle] ?: return -1
        val instance = material.createInstance()
        val handle = nextHandle++
        materialInstances[handle] = instance
        materialInstanceMaterials[handle] = materialHandle
        return handle
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun setMaterialParameter(instanceHandle: Int, param: MaterialParameter) {
        val instance = materialInstances[instanceHandle] ?: return
        val definition = materialInstanceMaterials[instanceHandle]
            ?.let(::getMaterialParameterDefinitionMap)
            ?.get(param.name)

        when (val value = param.value) {
            is Float -> instance.setParameter(param.name, value)
            is Int -> instance.setParameter(param.name, value)
            is UInt -> instance.setParameter(param.name, value.toInt())
            is Boolean -> instance.setParameter(param.name, value)
            is Float2 -> instance.setParameter(param.name, value.x, value.y)
            is Float3 -> instance.setParameter(param.name, value.x, value.y, value.z)
            is Float4 -> instance.setParameter(param.name, value.x, value.y, value.z, value.w)
            is Int2 -> instance.setParameter(param.name, value.x, value.y)
            is Int3 -> instance.setParameter(param.name, value.x, value.y, value.z)
            is Int4 -> instance.setParameter(param.name, value.x, value.y, value.z, value.w)
            is UInt2 -> instance.setParameter(param.name, value.x.toInt(), value.y.toInt())
            is UInt3 -> instance.setParameter(param.name, value.x.toInt(), value.y.toInt(), value.z.toInt())
            is UInt4 -> instance.setParameter(param.name, value.x.toInt(), value.y.toInt(), value.z.toInt(), value.w.toInt())
            is Bool2 -> instance.setParameter(param.name, value.x, value.y)
            is Bool3 -> instance.setParameter(param.name, value.x, value.y, value.z)
            is Bool4 -> instance.setParameter(param.name, value.x, value.y, value.z, value.w)
            is FloatArray -> setFloatArrayParameter(instance, param.name, value, definition)
            is IntArray -> setIntArrayParameter(instance, param.name, value, definition)
            is UIntArray -> setIntArrayParameter(instance, param.name, value.map(UInt::toInt).toIntArray(), definition)
            is BooleanArray -> setBooleanArrayParameter(instance, param.name, value, definition)
            is FilamentColor -> instance.setParameter(param.name, value.r, value.g, value.b)
            else -> error("Unsupported material parameter value for ${param.name}: ${value::class.simpleName}")
        }
    }

    private fun setFloatArrayParameter(
        instance: MaterialInstance,
        name: String,
        value: FloatArray,
        definition: MaterialParameterDefinition?,
    ) {
        val type = definition?.type ?: error("Missing material definition for float array parameter $name")
        when (type) {
            is MaterialParameterType.Float -> setFloatArray(instance, name, MaterialInstance.FloatElement.FLOAT, value, type.arraySize)
            is MaterialParameterType.Float2 -> setFloatArray(instance, name, MaterialInstance.FloatElement.FLOAT2, value, type.arraySize)
            is MaterialParameterType.Float3 -> setFloatArray(instance, name, MaterialInstance.FloatElement.FLOAT3, value, type.arraySize)
            is MaterialParameterType.Float4 -> setFloatArray(instance, name, MaterialInstance.FloatElement.FLOAT4, value, type.arraySize)
            is MaterialParameterType.Float3x3 -> setFloatArray(instance, name, MaterialInstance.FloatElement.MAT3, value, type.arraySize)
            is MaterialParameterType.Float4x4 -> setFloatArray(instance, name, MaterialInstance.FloatElement.MAT4, value, type.arraySize)
            else -> error("Float array does not match material parameter type for $name: $type")
        }
    }

    private fun setIntArrayParameter(
        instance: MaterialInstance,
        name: String,
        value: IntArray,
        definition: MaterialParameterDefinition?,
    ) {
        val type = definition?.type ?: error("Missing material definition for int array parameter $name")
        when (type) {
            is MaterialParameterType.Int -> setIntArray(instance, name, MaterialInstance.IntElement.INT, value, type.arraySize)
            is MaterialParameterType.Int2 -> setIntArray(instance, name, MaterialInstance.IntElement.INT2, value, type.arraySize)
            is MaterialParameterType.Int3 -> setIntArray(instance, name, MaterialInstance.IntElement.INT3, value, type.arraySize)
            is MaterialParameterType.Int4 -> setIntArray(instance, name, MaterialInstance.IntElement.INT4, value, type.arraySize)
            is MaterialParameterType.UInt -> setIntArray(instance, name, MaterialInstance.IntElement.INT, value, type.arraySize)
            is MaterialParameterType.UInt2 -> setIntArray(instance, name, MaterialInstance.IntElement.INT2, value, type.arraySize)
            is MaterialParameterType.UInt3 -> setIntArray(instance, name, MaterialInstance.IntElement.INT3, value, type.arraySize)
            is MaterialParameterType.UInt4 -> setIntArray(instance, name, MaterialInstance.IntElement.INT4, value, type.arraySize)
            else -> error("Int array does not match material parameter type for $name: $type")
        }
    }

    private fun setBooleanArrayParameter(
        instance: MaterialInstance,
        name: String,
        value: BooleanArray,
        definition: MaterialParameterDefinition?,
    ) {
        val type = definition?.type ?: error("Missing material definition for boolean array parameter $name")
        when (type) {
            is MaterialParameterType.Bool -> setBooleanArray(instance, name, MaterialInstance.BooleanElement.BOOL, value, type.arraySize)
            is MaterialParameterType.Bool2 -> setBooleanArray(instance, name, MaterialInstance.BooleanElement.BOOL2, value, type.arraySize)
            is MaterialParameterType.Bool3 -> setBooleanArray(instance, name, MaterialInstance.BooleanElement.BOOL3, value, type.arraySize)
            is MaterialParameterType.Bool4 -> setBooleanArray(instance, name, MaterialInstance.BooleanElement.BOOL4, value, type.arraySize)
            else -> error("Boolean array does not match material parameter type for $name: $type")
        }
    }

    private fun setFloatArray(
        instance: MaterialInstance,
        name: String,
        element: MaterialInstance.FloatElement,
        value: FloatArray,
        count: Int,
    ) {
        validateArrayLength(name, value.size, elementComponentCount(element) * count)
        instance.setParameter(name, element, value, 0, count)
    }

    private fun setIntArray(
        instance: MaterialInstance,
        name: String,
        element: MaterialInstance.IntElement,
        value: IntArray,
        count: Int,
    ) {
        validateArrayLength(name, value.size, elementComponentCount(element) * count)
        instance.setParameter(name, element, value, 0, count)
    }

    private fun setBooleanArray(
        instance: MaterialInstance,
        name: String,
        element: MaterialInstance.BooleanElement,
        value: BooleanArray,
        count: Int,
    ) {
        validateArrayLength(name, value.size, elementComponentCount(element) * count)
        instance.setParameter(name, element, value, 0, count)
    }

    private fun elementComponentCount(element: MaterialInstance.FloatElement): Int = when (element) {
        MaterialInstance.FloatElement.FLOAT -> 1
        MaterialInstance.FloatElement.FLOAT2 -> 2
        MaterialInstance.FloatElement.FLOAT3 -> 3
        MaterialInstance.FloatElement.FLOAT4 -> 4
        MaterialInstance.FloatElement.MAT3 -> 9
        MaterialInstance.FloatElement.MAT4 -> 16
    }

    private fun elementComponentCount(element: MaterialInstance.IntElement): Int = when (element) {
        MaterialInstance.IntElement.INT -> 1
        MaterialInstance.IntElement.INT2 -> 2
        MaterialInstance.IntElement.INT3 -> 3
        MaterialInstance.IntElement.INT4 -> 4
    }

    private fun elementComponentCount(element: MaterialInstance.BooleanElement): Int = when (element) {
        MaterialInstance.BooleanElement.BOOL -> 1
        MaterialInstance.BooleanElement.BOOL2 -> 2
        MaterialInstance.BooleanElement.BOOL3 -> 3
        MaterialInstance.BooleanElement.BOOL4 -> 4
    }

    private fun validateArrayLength(name: String, actualSize: Int, expectedSize: Int) {
        require(actualSize == expectedSize) {
            "Parameter $name expects $expectedSize values but received $actualSize"
        }
    }

    private fun calculateMipLevelCount(width: Int, height: Int): Int {
        val maxDimension = max(width, height).coerceAtLeast(1)
        return Int.SIZE_BITS - Integer.numberOfLeadingZeros(maxDimension)
    }

    override fun createTexture(width: Int, height: Int, pixels: ByteArray): Int {
        val eng = engine ?: return -1
        require(pixels.size == width * height * 4) {
            "Pixel data size ${pixels.size} does not match expected ${width * height * 4} for ${width}x${height} RGBA"
        }

        val mipLevelCount = calculateMipLevelCount(width, height)

        val texture = Texture.Builder()
            .width(width)
            .height(height)
            .sampler(Texture.Sampler.SAMPLER_2D)
            .format(Texture.InternalFormat.RGBA8)
            .usage(Texture.Usage.DEFAULT or Texture.Usage.GEN_MIPMAPPABLE)
            .levels(mipLevelCount)
            .build(eng)

        val buffer = ByteBuffer.allocateDirect(pixels.size).apply {
            order(ByteOrder.nativeOrder())
            put(pixels)
            flip()
        }
        texture.setImage(
            eng, 0,
            Texture.PixelBufferDescriptor(
                buffer,
                Texture.Format.RGBA,
                Texture.Type.UBYTE,
            )
        )
        texture.generateMipmaps(eng)

        val handle = nextHandle++
        textures[handle] = texture
        return handle
    }

    override fun loadTexture(path: String, colorFormat: TextureColorFormat): Int {
        val eng = engine ?: return -1
        if (path.endsWith(".ktx", ignoreCase = true) || path.endsWith(".ktx1", ignoreCase = true)) {
            val buffer = loadAssetBuffer(path) ?: return -1
            val texture = KTX1Loader.createTexture(eng, buffer)
            val handle = nextHandle++
            textures[handle] = texture
            return handle
        }

        return try {
            val bytes = loadAssetBytes(path) ?: return -1
            val bounds = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return -1

            val decodeOptions = BitmapFactory.Options().apply {
                inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                inScaled = false
                inSampleSize = calculateInSampleSize(
                    width = bounds.outWidth,
                    height = bounds.outHeight,
                    maxDimension = MaxDecodedTextureDimension,
                )
            }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions) ?: return -1

            val pixelBuffer = ByteBuffer.allocateDirect(bitmap.byteCount)
            bitmap.copyPixelsToBuffer(pixelBuffer)
            pixelBuffer.flip()
            val mipLevelCount = calculateMipLevelCount(bitmap.width, bitmap.height)

            val texture = Texture.Builder()
                .width(bitmap.width)
                .height(bitmap.height)
                .sampler(Texture.Sampler.SAMPLER_2D)
                .format(
                    when (colorFormat) {
                        TextureColorFormat.RGBA8 -> Texture.InternalFormat.RGBA8
                        TextureColorFormat.SRGB8_A8 -> Texture.InternalFormat.SRGB8_A8
                    }
                )
                .usage(Texture.Usage.DEFAULT or Texture.Usage.GEN_MIPMAPPABLE)
                .levels(mipLevelCount)
                .build(eng)
            texture.setImage(
                eng,
                0,
                Texture.PixelBufferDescriptor(
                    pixelBuffer,
                    Texture.Format.RGBA,
                    Texture.Type.UBYTE,
                )
            )
            texture.generateMipmaps(eng)
            bitmap.recycle()

            val handle = nextHandle++
            textures[handle] = texture
            handle
        } catch (error: OutOfMemoryError) {
            Log.w("AndroidFilamentEngine", "Failed to load texture due to memory pressure: $path", error)
            -1
        }
    }

    override fun setTextureParameter(instanceHandle: Int, paramName: String, textureHandle: Int) {
        val instance = materialInstances[instanceHandle] ?: return
        val texture = textures[textureHandle] ?: return

        val sampler = TextureSampler(
            TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR,
            TextureSampler.MagFilter.LINEAR,
            TextureSampler.WrapMode.REPEAT,
        )
        instance.setParameter(paramName, texture, sampler)
    }

    override fun createPlaneRenderable(materialInstanceHandle: Int, width: Float, height: Float): Int {
        val eng = engine ?: return -1
        val instance = materialInstances[materialInstanceHandle] ?: return -1

        val hw = width / 2f
        val hh = height / 2f

        // Positions (x, y, z) + Tangent quaternion (x, y, z, w) + UVs (u, v) = 9 floats per vertex
        // Normal (0,0,1) → tangent quaternion (0,0,0,1)
        val vertices = floatArrayOf(
            -hw, -hh, 0f, 0f, 0f, 0f, 1f, 0f, 0f,
             hw, -hh, 0f, 0f, 0f, 0f, 1f, 1f, 0f,
             hw,  hh, 0f, 0f, 0f, 0f, 1f, 1f, 1f,
            -hw,  hh, 0f, 0f, 0f, 0f, 1f, 0f, 1f,
        )

        val indices = shortArrayOf(0, 1, 2, 0, 2, 3)

        val vertexBuffer = buildVertexBuffer(eng, vertices, 4)
        val indexBuffer = buildIndexBuffer(eng, indices)

        val entity = EntityManager.get().create()
        return createRenderable(
            entity = entity,
            primitiveType = RenderableManager.PrimitiveType.TRIANGLES,
            vertexBuffer = vertexBuffer,
            indexBuffer = indexBuffer,
            materialInstance = instance,
            boundingBox = com.google.android.filament.Box(0f, 0f, 0f, hw, hh, 0.01f),
        )
    }

    override fun createSphereRenderable(materialInstanceHandle: Int, radius: Float): Int {
        val eng = engine ?: return -1
        val instance = materialInstances[materialInstanceHandle] ?: return -1

        val stacks = 64
        val slices = 64
        val vertexCount = (stacks + 1) * (slices + 1)
        val positions = FloatArray(vertexCount * 3)
        val normals = FloatArray(vertexCount * 3)
        val uvs = FloatArray(vertexCount * 2)
        var positionIndex = 0
        var normalIndex = 0
        var uvIndex = 0

        for (i in 0..stacks) {
            val phi = PI * i.toDouble() / stacks
            val sinPhi = sin(phi).toFloat()
            val cosPhi = cos(phi).toFloat()

            for (j in 0..slices) {
                val theta = 2.0 * PI * j.toDouble() / slices
                val sinTheta = sin(theta).toFloat()
                val cosTheta = cos(theta).toFloat()

                val nx = cosTheta * sinPhi
                val ny = cosPhi
                val nz = sinTheta * sinPhi

                positions[positionIndex++] = nx * radius
                positions[positionIndex++] = ny * radius
                positions[positionIndex++] = nz * radius

                normals[normalIndex++] = nx
                normals[normalIndex++] = ny
                normals[normalIndex++] = nz

                uvs[uvIndex++] = j.toFloat() / slices
                uvs[uvIndex++] = i.toFloat() / stacks
            }
        }

        val indexCount = stacks * slices * 6
        val indexData = ShortArray(indexCount)
        var ii = 0
        for (i in 0 until stacks) {
            for (j in 0 until slices) {
                val first = i * (slices + 1) + j
                val second = first + slices + 1
                indexData[ii++] = first.toShort()
                indexData[ii++] = (first + 1).toShort()
                indexData[ii++] = second.toShort()
                indexData[ii++] = (first + 1).toShort()
                indexData[ii++] = (second + 1).toShort()
                indexData[ii++] = second.toShort()
            }
        }

        val tangentQuaternions = buildSurfaceOrientationShortQuaternions(
            vertexCount = vertexCount,
            positions = positions,
            normals = normals,
            uvs = uvs,
            indices = indexData,
        )
        val vertexBuffer = buildShortTangentVertexBuffer(
            eng = eng,
            positions = positions,
            tangentQuaternions = tangentQuaternions,
            uvs = uvs,
            vertexCount = vertexCount,
        )
        val indexBuffer = buildIndexBuffer(eng, indexData)

        val entity = EntityManager.get().create()
        return createRenderable(
            entity = entity,
            primitiveType = RenderableManager.PrimitiveType.TRIANGLES,
            vertexBuffer = vertexBuffer,
            indexBuffer = indexBuffer,
            materialInstance = instance,
            boundingBox = com.google.android.filament.Box(0f, 0f, 0f, radius, radius, radius),
        )
    }

    override fun createCubeRenderable(materialInstanceHandle: Int, size: Float): Int {
        val eng = engine ?: return -1
        val instance = materialInstances[materialInstanceHandle] ?: return -1
        val half = size / 2f

        val positions = floatArrayOf(
            -half, -half, half, half, -half, half, half, half, half, -half, half, half,
            half, -half, -half, -half, -half, -half, -half, half, -half, half, half, -half,
            -half, -half, -half, -half, -half, half, -half, half, half, -half, half, -half,
            half, -half, half, half, -half, -half, half, half, -half, half, half, half,
            -half, half, half, half, half, half, half, half, -half, -half, half, -half,
            -half, -half, -half, half, -half, -half, half, -half, half, -half, -half, half,
        )
        val normals = floatArrayOf(
            0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f,
            0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f,
            -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f,
            1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f,
            0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f,
            0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f,
        )
        val uvs = floatArrayOf(
            0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f,
            0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f,
            0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f,
            0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f,
            0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f,
            0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f,
        )
        val indices = shortArrayOf(
            0, 1, 2, 0, 2, 3,
            4, 5, 6, 4, 6, 7,
            8, 9, 10, 8, 10, 11,
            12, 13, 14, 12, 14, 15,
            16, 17, 18, 16, 18, 19,
            20, 21, 22, 20, 22, 23,
        )

        val tangents = buildSurfaceOrientationQuaternions(
            vertexCount = 24,
            positions = positions,
            normals = normals,
            uvs = uvs,
            indices = indices,
        )
        val vertexData = FloatArray(24 * 9)
        var positionIndex = 0
        var tangentIndex = 0
        var uvIndex = 0
        var vertexDataIndex = 0
        repeat(24) {
            vertexData[vertexDataIndex++] = positions[positionIndex++]
            vertexData[vertexDataIndex++] = positions[positionIndex++]
            vertexData[vertexDataIndex++] = positions[positionIndex++]
            vertexData[vertexDataIndex++] = tangents[tangentIndex++]
            vertexData[vertexDataIndex++] = tangents[tangentIndex++]
            vertexData[vertexDataIndex++] = tangents[tangentIndex++]
            vertexData[vertexDataIndex++] = tangents[tangentIndex++]
            vertexData[vertexDataIndex++] = uvs[uvIndex++]
            vertexData[vertexDataIndex++] = uvs[uvIndex++]
        }

        val vertexBuffer = buildVertexBuffer(eng, vertexData, 24)
        val indexBuffer = buildIndexBuffer(eng, indices)
        return createRenderable(
            entity = EntityManager.get().create(),
            primitiveType = RenderableManager.PrimitiveType.TRIANGLES,
            vertexBuffer = vertexBuffer,
            indexBuffer = indexBuffer,
            materialInstance = instance,
            boundingBox = com.google.android.filament.Box(0f, 0f, 0f, half, half, half),
        )
    }

    override fun createCustomRenderable(config: CustomRenderableConfig): Int {
        val eng = engine ?: return -1
        val instance = materialInstances[config.materialInstanceHandle] ?: return -1
        val vertexBuffer = buildCustomVertexBuffer(
            eng = eng,
            vertexData = config.vertexData,
            vertexCount = config.vertexCount,
            strideBytes = config.strideBytes,
            attributes = config.attributes,
        )
        val indexBuffer = buildIndexBuffer(eng, config.indices)
        return createRenderable(
            entity = EntityManager.get().create(),
            primitiveType = config.primitiveType.toFilamentPrimitiveType(),
            vertexBuffer = vertexBuffer,
            indexBuffer = indexBuffer,
            materialInstance = instance,
            boundingBox = com.google.android.filament.Box(
                config.boundingBox.center.x,
                config.boundingBox.center.y,
                config.boundingBox.center.z,
                config.boundingBox.halfExtent.x,
                config.boundingBox.halfExtent.y,
                config.boundingBox.halfExtent.z,
            ),
        )
    }

    override fun createCustomRenderableWithGeneratedTangents(config: CustomRenderableConfig): Int {
        val eng = engine ?: return -1
        val instance = materialInstances[config.materialInstanceHandle] ?: return -1
        val positionAttribute = config.attributes.firstOrNull { it.attribute == VertexAttribute.POSITION }
        val uvAttribute = config.attributes.firstOrNull { it.attribute == VertexAttribute.UV0 }
        val hasTangents = config.attributes.any { it.attribute == VertexAttribute.TANGENTS }
        if (
            config.primitiveType == PrimitiveType.TRIANGLES &&
            positionAttribute?.type == AttributeDataType.FLOAT3 &&
            uvAttribute?.type == AttributeDataType.FLOAT2 &&
            !hasTangents
        ) {
            val positions = extractFloatAttributeData(
                vertexData = config.vertexData,
                vertexCount = config.vertexCount,
                strideBytes = config.strideBytes,
                offsetBytes = positionAttribute.offsetBytes,
                componentCount = 3,
            )
            val uvs = extractFloatAttributeData(
                vertexData = config.vertexData,
                vertexCount = config.vertexCount,
                strideBytes = config.strideBytes,
                offsetBytes = uvAttribute.offsetBytes,
                componentCount = 2,
            )
            val tangentQuaternions = buildSurfaceOrientationShortQuaternions(
                vertexCount = config.vertexCount,
                positions = positions,
                uvs = uvs,
                indices = config.indices,
            )
            val vertexBuffer = buildShortTangentVertexBuffer(
                eng = eng,
                positions = positions,
                tangentQuaternions = tangentQuaternions,
                uvs = uvs,
                vertexCount = config.vertexCount,
            )
            val indexBuffer = buildIndexBuffer(eng, config.indices)
            return createRenderable(
                entity = EntityManager.get().create(),
                primitiveType = config.primitiveType.toFilamentPrimitiveType(),
                vertexBuffer = vertexBuffer,
                indexBuffer = indexBuffer,
                materialInstance = instance,
                boundingBox = com.google.android.filament.Box(
                    config.boundingBox.center.x,
                    config.boundingBox.center.y,
                    config.boundingBox.center.z,
                    config.boundingBox.halfExtent.x,
                    config.boundingBox.halfExtent.y,
                    config.boundingBox.halfExtent.z,
                ),
            )
        }
        return createCustomRenderable(config)
    }

    override fun loadMesh(path: String, materialInstanceHandle: Int): Int = -1

    override fun createMorphRenderable(
        materialInstanceHandle: Int,
        geometry: MorphRenderableGeometry,
    ): Int {
        val eng = engine ?: return -1
        val instance = materialInstances[materialInstanceHandle] ?: return -1

        val tangentQuaternions = buildSurfaceOrientationShortQuaternions(
            vertexCount = geometry.vertexCount,
            positions = geometry.positions,
            uvs = geometry.uvs,
            indices = geometry.indices,
        )
        val vertexBuffer = buildShortTangentVertexBuffer(
            eng = eng,
            positions = geometry.positions,
            tangentQuaternions = tangentQuaternions,
            uvs = geometry.uvs,
            vertexCount = geometry.vertexCount,
        )
        val indexBuffer = buildIndexBuffer(eng, geometry.indices)
        val morphTargetBuffer = MorphTargetBuffer.Builder()
            .vertexCount(geometry.vertexCount)
            .count(geometry.morphTargetCount)
            .withPositions(true)
            .withTangents(true)
            .build(eng)

        geometry.morphTargetPositions.forEachIndexed { index, targetPositions ->
            morphTargetBuffer.setPositionsAt(
                eng,
                index,
                targetPositions.toFloat4Array(),
                geometry.vertexCount,
            )
            morphTargetBuffer.setTangentsAt(
                eng,
                index,
                buildSurfaceOrientationShortQuaternions(
                    vertexCount = geometry.vertexCount,
                    positions = targetPositions,
                    uvs = geometry.uvs,
                    indices = geometry.indices,
                ),
                geometry.vertexCount,
            )
        }

        val bounds = geometry.calculateMorphBounds()

        val entity = EntityManager.get().create()
        val handle = createRenderable(
            entity = entity,
            primitiveType = RenderableManager.PrimitiveType.TRIANGLES,
            vertexBuffer = vertexBuffer,
            indexBuffer = indexBuffer,
            materialInstance = instance,
            boundingBox = com.google.android.filament.Box(
                bounds.center.x,
                bounds.center.y,
                bounds.center.z,
                bounds.halfExtent.x,
                bounds.halfExtent.y,
                bounds.halfExtent.z,
            ),
            morphTargetBuffer = morphTargetBuffer,
        )
        morphTargetBuffers[handle] = morphTargetBuffer
        return handle
    }

    override fun setRenderableRotation(handle: Int, rotationXDegrees: Float, rotationYDegrees: Float) {
        val eng = engine ?: return
        val entity = renderables[handle] ?: return
        val transformManager = eng.transformManager
        val instance = transformManager.getInstance(entity)
        if (instance == 0) return

        val transform = FloatArray(16)
        Matrix.setIdentityM(transform, 0)
        Matrix.rotateM(transform, 0, rotationXDegrees, 1f, 0f, 0f)
        Matrix.rotateM(transform, 0, rotationYDegrees, 0f, 1f, 0f)
        transformManager.setTransform(instance, transform)
    }

    override fun setRenderableTransform(handle: Int, transform: FloatArray) {
        val eng = engine ?: return
        val entity = renderables[handle] ?: return
        val transformManager = eng.transformManager
        val instance = transformManager.getInstance(entity)
        if (instance == 0) return
        require(transform.size == 16) { "Renderable transform must have 16 values" }
        transformManager.setTransform(instance, transform)
    }

    override fun setShadowsEnabled(renderableHandle: Int, castShadows: Boolean, receiveShadows: Boolean) {
        val eng = engine ?: return
        val entity = renderables[renderableHandle] ?: return
        val instance = eng.renderableManager.getInstance(entity)
        if (instance == 0) return
        eng.renderableManager.setCastShadows(instance, castShadows)
        eng.renderableManager.setReceiveShadows(instance, receiveShadows)
    }

    override fun updateVertexData(renderableHandle: Int, vertexData: ByteArray) {
        val eng = engine ?: return
        val renderableBuffer = renderableBuffers[renderableHandle] ?: return
        val byteBuffer = ByteBuffer.allocateDirect(vertexData.size).apply {
            order(ByteOrder.nativeOrder())
            put(vertexData)
            flip()
        }
        renderableBuffer.vertexBuffer.setBufferAt(eng, 0, byteBuffer)
    }

    override fun setMorphWeights(handle: Int, weights: FloatArray) {
        val eng = engine ?: return
        val entity = renderables[handle] ?: return
        val instance = eng.renderableManager.getInstance(entity)
        if (instance == 0) return
        eng.renderableManager.setMorphWeights(instance, weights, 0)
    }

    override fun removeRenderable(handle: Int) {
        destroyRenderableEntity(handle)
    }

    override fun createView(viewport: ViewportConfig): Int {
        val eng = engine ?: return -1
        val scene = scene ?: return -1
        val handle = nextHandle++
        val cameraEntity = EntityManager.get().create()
        val camera = eng.createCamera(cameraEntity)
        applyCameraLookAt(camera, currentCameraConfig)
        applyCameraConfig(camera, currentCameraConfig, viewport.width, viewport.height)
        val view = eng.createView().apply {
            this.scene = scene
            this.camera = camera
            this.viewport = Viewport(viewport.x, viewport.y, viewport.width, viewport.height)
        }
        additionalViews[handle] = AuxiliaryView(
            view = view,
            camera = camera,
            cameraEntity = cameraEntity,
            viewport = viewport,
        )
        return handle
    }

    override fun removeView(handle: Int) {
        val eng = engine ?: return
        val auxiliaryView = additionalViews.remove(handle) ?: return
        eng.destroyCameraComponent(auxiliaryView.cameraEntity)
        eng.destroyView(auxiliaryView.view)
        EntityManager.get().destroy(auxiliaryView.cameraEntity)
    }

    override fun setViewViewport(handle: Int, viewport: ViewportConfig) {
        val auxiliaryView = additionalViews[handle] ?: return
        auxiliaryView.viewport = viewport
        auxiliaryView.view.viewport = Viewport(viewport.x, viewport.y, viewport.width, viewport.height)
        applyCameraConfig(auxiliaryView.camera, auxiliaryView.cameraConfig, viewport.width, viewport.height)
    }

    override fun setViewCamera(handle: Int, config: CameraConfig) {
        val auxiliaryView = additionalViews[handle] ?: return
        auxiliaryView.cameraConfig = config
        applyCameraLookAt(auxiliaryView.camera, config)
        applyCameraConfig(auxiliaryView.camera, config, auxiliaryView.viewport.width, auxiliaryView.viewport.height)
    }

    override fun setViewBlendMode(handle: Int, translucent: Boolean) {
        val auxiliaryView = additionalViews[handle] ?: return
        auxiliaryView.view.blendMode = if (translucent) View.BlendMode.TRANSLUCENT else View.BlendMode.OPAQUE
    }

    override fun setViewPostProcessing(handle: Int, enabled: Boolean) {
        additionalViews[handle]?.view?.isPostProcessingEnabled = enabled
    }

    override fun loadGltfAsset(path: String): Int {
        val loader = assetLoader ?: return -1
        val resourceLoader = resourceLoader ?: return -1
        val buffer = loadAssetBuffer(path) ?: return -1
        val asset = loader.createAsset(buffer) ?: return -1
        if (path.endsWith(".gltf", ignoreCase = true)) {
            val basePath = path.substringBeforeLast('/', "")
            asset.resourceUris.forEach { resourceUri ->
                val resourceBuffer = loadAssetBuffer(resolveRelativeAssetPath(basePath, resourceUri)) ?: return -1
                resourceLoader.addResourceData(resourceUri, resourceBuffer)
            }
        }
        resourceLoader.loadResources(asset)
        asset.releaseSourceData()
        val handle = nextHandle++
        gltfAssets[handle] = ManagedGltfAsset(
            asset = asset,
            animator = asset.instance.animator,
            addedToScene = false,
        )
        return handle
    }

    override fun destroyGltfAsset(handle: Int) {
        val loader = assetLoader ?: return
        val managedAsset = gltfAssets.remove(handle) ?: return
        if (managedAsset.addedToScene) {
            scene?.removeEntities(managedAsset.asset.entities)
        }
        loader.destroyAsset(managedAsset.asset)
    }

    override fun getGltfAnimationCount(handle: Int): Int {
        return gltfAssets[handle]?.animator?.animationCount ?: 0
    }

    override fun getGltfAnimationDuration(handle: Int, animationIndex: Int): Float {
        return gltfAssets[handle]?.animator?.getAnimationDuration(animationIndex) ?: 0f
    }

    override fun applyGltfAnimation(handle: Int, animationIndex: Int, timeSeconds: Float) {
        gltfAssets[handle]?.animator?.applyAnimation(animationIndex, timeSeconds)
    }

    override fun updateGltfBoneMatrices(handle: Int) {
        gltfAssets[handle]?.animator?.updateBoneMatrices()
    }

    override fun transformGltfToUnitCube(handle: Int) {
        val eng = engine ?: return
        val managedAsset = gltfAssets[handle] ?: return
        val boundingBox = managedAsset.asset.boundingBox
        val center = boundingBox.center
        val halfExtent = boundingBox.halfExtent
        val maxExtent = max(max(halfExtent[0], halfExtent[1]), halfExtent[2]) * 2f
        if (maxExtent <= 0f) return
        val scale = 2f / maxExtent
        val transform = FloatArray(16)
        Matrix.setIdentityM(transform, 0)
        Matrix.scaleM(transform, 0, scale, scale, scale)
        Matrix.translateM(transform, 0, -center[0], -center[1], -center[2])
        val root = managedAsset.asset.root
        val transformManager = eng.transformManager
        val instance = transformManager.getInstance(root)
        if (instance != 0) {
            transformManager.setTransform(instance, transform)
        }
    }

    override fun addGltfToScene(handle: Int) {
        val managedAsset = gltfAssets[handle] ?: return
        if (managedAsset.addedToScene) return
        scene?.addEntities(managedAsset.asset.entities)
        managedAsset.addedToScene = true
    }

    override fun removeGltfFromScene(handle: Int) {
        val managedAsset = gltfAssets[handle] ?: return
        if (!managedAsset.addedToScene) return
        scene?.removeEntities(managedAsset.asset.entities)
        managedAsset.addedToScene = false
    }

    override fun requestFrame() {
        // The choreographer-driven render loop handles continuous rendering.
        // This is a no-op since we render every frame.
    }

    private fun loadAssetBuffer(path: String): ByteBuffer? {
        val bytes = loadAssetBytes(path) ?: return null
        return ByteBuffer.allocateDirect(bytes.size).apply {
            order(ByteOrder.nativeOrder())
            put(bytes)
            flip()
        }
    }

    private fun loadAssetBytes(path: String): ByteArray? {
        return runCatching {
            val normalizedAssetPath = path.toAndroidAssetPath()
            when {
                normalizedAssetPath != null -> context.assets.open(normalizedAssetPath).readBytes()
                path.startsWith("file://") -> File(path.removePrefix("file://")).readBytes()
                else -> context.assets.open(path).readBytes()
            }
        }.getOrNull()
    }

    private fun String.toAndroidAssetPath(): String? {
        val trimmed = trim()
        val candidates = listOf(
            "file:///android_asset/",
            "file:/android_asset/",
            "/android_asset/",
            "android_asset/",
        ).firstNotNullOfOrNull { prefix ->
            trimmed.takeIf { it.startsWith(prefix) }?.removePrefix(prefix)
        } ?: trimmed.substringAfter("android_asset/", missingDelimiterValue = trimmed)
            .takeIf { it != trimmed }

        return candidates
            ?.substringBefore('?')
            ?.substringBefore('#')
            ?.trimStart('/')
            ?.takeIf { it.isNotEmpty() }
    }

    private fun buildVertexBuffer(eng: Engine, data: FloatArray, vertexCount: Int): VertexBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(data.size * 4).apply {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().put(data)
        }

        val vb = VertexBuffer.Builder()
            .vertexCount(vertexCount)
            .bufferCount(1)
            .attribute(VertexBuffer.VertexAttribute.POSITION, 0, VertexBuffer.AttributeType.FLOAT3, 0, 36)
            .attribute(VertexBuffer.VertexAttribute.TANGENTS, 0, VertexBuffer.AttributeType.FLOAT4, 12, 36)
            .attribute(VertexBuffer.VertexAttribute.UV0, 0, VertexBuffer.AttributeType.FLOAT2, 28, 36)
            .build(eng)

        vb.setBufferAt(eng, 0, byteBuffer)
        vertexBuffers.add(vb)
        return vb
    }

    private fun buildCustomVertexBuffer(
        eng: Engine,
        vertexData: ByteArray,
        vertexCount: Int,
        strideBytes: Int,
        attributes: List<VertexAttributeLayout>,
    ): VertexBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(vertexData.size).apply {
            order(ByteOrder.nativeOrder())
            put(vertexData)
            flip()
        }
        val builder = VertexBuffer.Builder()
            .vertexCount(vertexCount)
            .bufferCount(1)
        attributes.forEach { attribute ->
            builder.attribute(
                attribute.attribute.toFilamentVertexAttribute(),
                0,
                attribute.type.toFilamentAttributeType(),
                attribute.offsetBytes,
                strideBytes,
            )
            if (attribute.normalized) {
                builder.normalized(attribute.attribute.toFilamentVertexAttribute())
            }
        }
        return builder.build(eng).also { vertexBuffer ->
            vertexBuffer.setBufferAt(eng, 0, byteBuffer)
            vertexBuffers.add(vertexBuffer)
        }
    }

    private fun buildShortTangentVertexBuffer(
        eng: Engine,
        positions: FloatArray,
        tangentQuaternions: ShortArray,
        uvs: FloatArray,
        vertexCount: Int,
    ): VertexBuffer {
        val stride = 28
        val byteBuffer = ByteBuffer.allocateDirect(vertexCount * stride).apply {
            order(ByteOrder.nativeOrder())
            repeat(vertexCount) { index ->
                val positionOffset = index * 3
                putFloat(positions[positionOffset])
                putFloat(positions[positionOffset + 1])
                putFloat(positions[positionOffset + 2])
                val tangentOffset = index * 4
                putShort(tangentQuaternions[tangentOffset])
                putShort(tangentQuaternions[tangentOffset + 1])
                putShort(tangentQuaternions[tangentOffset + 2])
                putShort(tangentQuaternions[tangentOffset + 3])
                val uvOffset = index * 2
                putFloat(uvs[uvOffset])
                putFloat(uvs[uvOffset + 1])
            }
            flip()
        }

        val vb = VertexBuffer.Builder()
            .vertexCount(vertexCount)
            .bufferCount(1)
            .attribute(VertexBuffer.VertexAttribute.POSITION, 0, VertexBuffer.AttributeType.FLOAT3, 0, stride)
            .attribute(VertexBuffer.VertexAttribute.TANGENTS, 0, VertexBuffer.AttributeType.SHORT4, 12, stride)
            .normalized(VertexBuffer.VertexAttribute.TANGENTS)
            .attribute(VertexBuffer.VertexAttribute.UV0, 0, VertexBuffer.AttributeType.FLOAT2, 20, stride)
            .build(eng)

        vb.setBufferAt(eng, 0, byteBuffer)
        vertexBuffers.add(vb)
        return vb
    }

    private fun buildSurfaceOrientationQuaternions(
        vertexCount: Int,
        positions: FloatArray,
        normals: FloatArray,
        uvs: FloatArray,
        indices: ShortArray,
    ): FloatArray {
        val positionsBuffer = ByteBuffer.allocateDirect(positions.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(positions)
                flip()
            }
        val normalsBuffer = ByteBuffer.allocateDirect(normals.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(normals)
                flip()
            }
        val uvsBuffer = ByteBuffer.allocateDirect(uvs.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(uvs)
                flip()
            }
        val indexBuffer = ByteBuffer.allocateDirect(indices.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .apply {
                put(indices)
                flip()
            }
        val quaternionsBuffer = ByteBuffer.allocateDirect(vertexCount * 4 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        val orientation = SurfaceOrientation.Builder()
            .vertexCount(vertexCount)
            .normals(normalsBuffer)
            .uvs(uvsBuffer)
            .positions(positionsBuffer)
            .triangleCount(indices.size / 3)
            .triangles_uint16(indexBuffer)
            .build()
        return try {
            orientation.getQuatsAsFloat(quaternionsBuffer)
            FloatArray(vertexCount * 4).also { quaternions ->
                quaternionsBuffer.rewind()
                quaternionsBuffer.get(quaternions)
            }
        } finally {
            orientation.destroy()
        }
    }

    private fun buildSurfaceOrientationShortQuaternions(
        vertexCount: Int,
        positions: FloatArray,
        normals: FloatArray? = null,
        uvs: FloatArray,
        indices: ShortArray,
    ): ShortArray {
        val positionsBuffer = ByteBuffer.allocateDirect(positions.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(positions)
                flip()
            }
        val uvsBuffer = ByteBuffer.allocateDirect(uvs.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(uvs)
                flip()
            }
        val indexBuffer = ByteBuffer.allocateDirect(indices.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .apply {
                put(indices)
                flip()
            }
        val quaternionsBuffer = ByteBuffer.allocateDirect(vertexCount * 4 * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()

        val orientation = SurfaceOrientation.Builder()
            .vertexCount(vertexCount)
            .apply {
                normals?.let { normals(normalsBufferFrom(it)) }
            }
            .uvs(uvsBuffer)
            .positions(positionsBuffer)
            .triangleCount(indices.size / 3)
            .triangles_uint16(indexBuffer)
            .build()
        return try {
            orientation.getQuatsAsShort(quaternionsBuffer)
            ShortArray(vertexCount * 4).also { quaternions ->
                quaternionsBuffer.rewind()
                quaternionsBuffer.get(quaternions)
            }
        } finally {
            orientation.destroy()
        }
    }

    private fun normalsBufferFrom(normals: FloatArray) = ByteBuffer.allocateDirect(normals.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(normals)
            flip()
        }

    private fun buildIndexBuffer(eng: Engine, data: ShortArray): IndexBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(data.size * 2).apply {
            order(ByteOrder.nativeOrder())
            asShortBuffer().put(data)
        }

        val ib = IndexBuffer.Builder()
            .indexCount(data.size)
            .bufferType(IndexBuffer.Builder.IndexType.USHORT)
            .build(eng)

        ib.setBuffer(eng, byteBuffer)
        indexBuffers.add(ib)
        return ib
    }

    private fun extractFloatAttributeData(
        vertexData: ByteArray,
        vertexCount: Int,
        strideBytes: Int,
        offsetBytes: Int,
        componentCount: Int,
    ): FloatArray {
        val output = FloatArray(vertexCount * componentCount)
        val buffer = ByteBuffer.wrap(vertexData).order(ByteOrder.nativeOrder())
        var outputIndex = 0
        repeat(vertexCount) { vertexIndex ->
            val attributeBase = vertexIndex * strideBytes + offsetBytes
            repeat(componentCount) { componentIndex ->
                output[outputIndex++] = buffer.getFloat(attributeBase + componentIndex * Float.SIZE_BYTES)
            }
        }
        return output
    }

    private fun createRenderable(
        entity: Int,
        primitiveType: RenderableManager.PrimitiveType,
        vertexBuffer: VertexBuffer,
        indexBuffer: IndexBuffer,
        materialInstance: MaterialInstance,
        boundingBox: com.google.android.filament.Box,
        morphTargetBuffer: MorphTargetBuffer? = null,
    ): Int {
        val eng = engine ?: return -1
        val builder = RenderableManager.Builder(1)
            .geometry(0, primitiveType, vertexBuffer, indexBuffer)
            .material(0, materialInstance)
            .boundingBox(boundingBox)
        if (morphTargetBuffer != null) {
            builder.morphing(morphTargetBuffer)
        }
        builder.build(eng, entity)
        scene?.addEntity(entity)
        val handle = nextHandle++
        renderables[handle] = entity
        renderableBuffers[handle] = RenderableBuffers(vertexBuffer)
        return handle
    }

    private fun applyCameraLookAt(camera: Camera, config: CameraConfig) {
        camera.lookAt(
            config.position.x.toDouble(), config.position.y.toDouble(), config.position.z.toDouble(),
            config.lookAt.x.toDouble(), config.lookAt.y.toDouble(), config.lookAt.z.toDouble(),
            config.up.x.toDouble(), config.up.y.toDouble(), config.up.z.toDouble(),
        )
    }

    private fun applyCameraConfig(camera: Camera, config: CameraConfig, width: Int, height: Int) {
        val aspect = width.toDouble() / height.coerceAtLeast(1).toDouble()
        when (config.projectionType) {
            ProjectionType.PERSPECTIVE -> camera.setProjection(
                config.fovDegrees,
                aspect,
                config.near,
                config.far,
                Camera.Fov.VERTICAL,
            )

            ProjectionType.ORTHO -> {
                val orthoHeight = config.orthoZoom
                val orthoWidth = orthoHeight * aspect
                camera.setProjection(
                    Camera.Projection.ORTHO,
                    -orthoWidth,
                    orthoWidth,
                    -orthoHeight,
                    orthoHeight,
                    config.near,
                    config.far,
                )
            }
        }
    }

    private fun destroyRenderableEntity(handle: Int) {
        val eng = engine ?: return
        val entity = renderables.remove(handle) ?: return
        scene?.removeEntity(entity)
        renderableBuffers.remove(handle)
        morphTargetBuffers.remove(handle)?.let(eng::destroyMorphTargetBuffer)
        eng.destroyEntity(entity)
        EntityManager.get().destroy(entity)
    }

    private fun resolveRelativeAssetPath(basePath: String, relativePath: String): String {
        if (relativePath.startsWith("file://")) return relativePath
        if (relativePath.startsWith("/")) return relativePath
        return if (basePath.isEmpty()) relativePath else "$basePath/$relativePath"
    }

    private data class EnvironmentIndirectLight(
        val indirectLight: IndirectLight,
        val cubemap: Texture,
    )

    private data class EnvironmentSkybox(
        val skybox: com.google.android.filament.Skybox,
        val cubemap: Texture?,
    )

    private data class RenderableBuffers(
        val vertexBuffer: VertexBuffer,
    )

    private data class AuxiliaryView(
        val view: View,
        val camera: Camera,
        val cameraEntity: Int,
        var viewport: ViewportConfig,
        var cameraConfig: CameraConfig = CameraConfig(),
    )

    private data class ManagedGltfAsset(
        val asset: FilamentAsset,
        val animator: Animator,
        var addedToScene: Boolean,
    )

}

private fun calculateInSampleSize(
    width: Int,
    height: Int,
    maxDimension: Int,
): Int {
    if (width <= 0 || height <= 0 || maxDimension <= 0) return 1
    var inSampleSize = 1
    var largestDimension = max(width, height)
    while (largestDimension > maxDimension) {
        largestDimension /= 2
        inSampleSize *= 2
    }
    return inSampleSize
}

private fun FloatArray.toFloat4Array(): FloatArray {
    val values = FloatArray(size / 3 * 4)
    var sourceIndex = 0
    var targetIndex = 0
    while (sourceIndex < size) {
        values[targetIndex++] = this[sourceIndex++]
        values[targetIndex++] = this[sourceIndex++]
        values[targetIndex++] = this[sourceIndex++]
        values[targetIndex++] = 1f
    }
    return values
}

private data class MorphBounds(
    val center: Float3,
    val halfExtent: Float3,
)

private fun MorphRenderableGeometry.calculateMorphBounds(): MorphBounds {
    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var minZ = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY
    var maxZ = Float.NEGATIVE_INFINITY

    fun include(values: FloatArray) {
        var index = 0
        while (index < values.size) {
            val x = values[index++]
            val y = values[index++]
            val z = values[index++]
            minX = minOf(minX, x)
            minY = minOf(minY, y)
            minZ = minOf(minZ, z)
            maxX = maxOf(maxX, x)
            maxY = maxOf(maxY, y)
            maxZ = maxOf(maxZ, z)
        }
    }

    include(positions)
    morphTargetPositions.forEach(::include)

    return MorphBounds(
        center = Float3(
            x = (minX + maxX) * 0.5f,
            y = (minY + maxY) * 0.5f,
            z = (minZ + maxZ) * 0.5f,
        ),
        halfExtent = Float3(
            x = (maxX - minX) * 0.5f,
            y = (maxY - minY) * 0.5f,
            z = (maxZ - minZ) * 0.5f,
        ),
    )
}

private fun VertexAttribute.toFilamentVertexAttribute(): VertexBuffer.VertexAttribute = when (this) {
    VertexAttribute.POSITION -> VertexBuffer.VertexAttribute.POSITION
    VertexAttribute.TANGENTS -> VertexBuffer.VertexAttribute.TANGENTS
    VertexAttribute.UV0 -> VertexBuffer.VertexAttribute.UV0
    VertexAttribute.COLOR -> VertexBuffer.VertexAttribute.COLOR
}

private fun AttributeDataType.toFilamentAttributeType(): VertexBuffer.AttributeType = when (this) {
    AttributeDataType.FLOAT2 -> VertexBuffer.AttributeType.FLOAT2
    AttributeDataType.FLOAT3 -> VertexBuffer.AttributeType.FLOAT3
    AttributeDataType.FLOAT4 -> VertexBuffer.AttributeType.FLOAT4
    AttributeDataType.UBYTE4 -> VertexBuffer.AttributeType.UBYTE4
}

private fun PrimitiveType.toFilamentPrimitiveType(): RenderableManager.PrimitiveType = when (this) {
    PrimitiveType.TRIANGLES -> RenderableManager.PrimitiveType.TRIANGLES
    PrimitiveType.LINES -> RenderableManager.PrimitiveType.LINES
    PrimitiveType.POINTS -> RenderableManager.PrimitiveType.POINTS
}

private fun VertexAttribute.toFilamatVertexAttribute(): MaterialBuilder.VertexAttribute = when (this) {
    VertexAttribute.POSITION -> MaterialBuilder.VertexAttribute.POSITION
    VertexAttribute.TANGENTS -> MaterialBuilder.VertexAttribute.TANGENTS
    VertexAttribute.UV0 -> MaterialBuilder.VertexAttribute.UV0
    VertexAttribute.COLOR -> MaterialBuilder.VertexAttribute.COLOR
}

private fun MaterialParameterDefinition.toUniformPrecision(): MaterialBuilder.ParameterPrecision = when (precision) {
    MaterialParameterPrecision.LOW -> MaterialBuilder.ParameterPrecision.LOW
    MaterialParameterPrecision.MEDIUM -> MaterialBuilder.ParameterPrecision.MEDIUM
    MaterialParameterPrecision.HIGH -> MaterialBuilder.ParameterPrecision.HIGH
    MaterialParameterPrecision.DEFAULT -> MaterialBuilder.ParameterPrecision.DEFAULT
}

private fun MaterialParameterType.toFilamatUniformType(): MaterialBuilder.UniformType? = when (this) {
    is MaterialParameterType.Bool -> MaterialBuilder.UniformType.BOOL
    is MaterialParameterType.Bool2 -> MaterialBuilder.UniformType.BOOL2
    is MaterialParameterType.Bool3 -> MaterialBuilder.UniformType.BOOL3
    is MaterialParameterType.Bool4 -> MaterialBuilder.UniformType.BOOL4
    is MaterialParameterType.Float -> MaterialBuilder.UniformType.FLOAT
    is MaterialParameterType.Float2 -> MaterialBuilder.UniformType.FLOAT2
    is MaterialParameterType.Float3 -> MaterialBuilder.UniformType.FLOAT3
    is MaterialParameterType.Float4 -> MaterialBuilder.UniformType.FLOAT4
    is MaterialParameterType.Int -> MaterialBuilder.UniformType.INT
    is MaterialParameterType.Int2 -> MaterialBuilder.UniformType.INT2
    is MaterialParameterType.Int3 -> MaterialBuilder.UniformType.INT3
    is MaterialParameterType.Int4 -> MaterialBuilder.UniformType.INT4
    is MaterialParameterType.UInt -> MaterialBuilder.UniformType.UINT
    is MaterialParameterType.UInt2 -> MaterialBuilder.UniformType.UINT2
    is MaterialParameterType.UInt3 -> MaterialBuilder.UniformType.UINT3
    is MaterialParameterType.UInt4 -> MaterialBuilder.UniformType.UINT4
    is MaterialParameterType.Float3x3 -> MaterialBuilder.UniformType.MAT3
    is MaterialParameterType.Float4x4 -> MaterialBuilder.UniformType.MAT4
    is MaterialParameterType.Sampler2d,
    is MaterialParameterType.Sampler2dArray,
    is MaterialParameterType.SamplerExternal,
    is MaterialParameterType.SamplerCubemap -> null
}

private fun MaterialParameterType.toFilamatSamplerType(): MaterialBuilder.SamplerType? = when (this) {
    is MaterialParameterType.Sampler2d -> MaterialBuilder.SamplerType.SAMPLER_2D
    is MaterialParameterType.Sampler2dArray -> MaterialBuilder.SamplerType.SAMPLER_2D_ARRAY
    is MaterialParameterType.SamplerExternal -> MaterialBuilder.SamplerType.SAMPLER_EXTERNAL
    is MaterialParameterType.SamplerCubemap -> MaterialBuilder.SamplerType.SAMPLER_CUBEMAP
    else -> null
}

private fun dev.nstv.practicalfilament.filament.material.SamplerFormat.toFilamatSamplerFormat(): MaterialBuilder.SamplerFormat = when (this) {
    dev.nstv.practicalfilament.filament.material.SamplerFormat.INT -> MaterialBuilder.SamplerFormat.INT
    dev.nstv.practicalfilament.filament.material.SamplerFormat.FLOAT -> MaterialBuilder.SamplerFormat.FLOAT
}

private fun MaterialParameterDefinition.addedAsSamplerOrUniform(builder: MaterialBuilder) {
    val samplerType = type.toFilamatSamplerType()
    if (samplerType != null) {
        val samplerFormat = when (type) {
            is MaterialParameterType.Sampler2d -> type.format
            is MaterialParameterType.Sampler2dArray -> type.format
            is MaterialParameterType.SamplerExternal -> type.format
            is MaterialParameterType.SamplerCubemap -> type.format
            else -> error("Unsupported sampler material parameter type for $name")
        }
        builder.samplerParameter(samplerType, samplerFormat.toFilamatSamplerFormat(), toUniformPrecision(), name)
        return
    }

    val uniformType = type.toFilamatUniformType()
        ?: error("Unsupported runtime material parameter type for $name: $type")
    if (type.arraySize > 1) {
        builder.uniformParameterArray(uniformType, type.arraySize, toUniformPrecision(), name)
    } else {
        builder.uniformParameter(uniformType, toUniformPrecision(), name)
    }
}

private fun MaterialBuilder.addRuntimeParameter(definition: MaterialParameterDefinition) {
    definition.addedAsSamplerOrUniform(this)
}

private fun MaterialBlendingMode.toFilamentBlendingMode(): MaterialBuilder.BlendingMode = when (this) {
    MaterialBlendingMode.OPAQUE -> MaterialBuilder.BlendingMode.OPAQUE
    MaterialBlendingMode.TRANSPARENT -> MaterialBuilder.BlendingMode.TRANSPARENT
    MaterialBlendingMode.ADD -> MaterialBuilder.BlendingMode.ADD
    MaterialBlendingMode.MASKED -> MaterialBuilder.BlendingMode.MASKED
    MaterialBlendingMode.FADE -> MaterialBuilder.BlendingMode.FADE
    MaterialBlendingMode.MULTIPLY -> MaterialBuilder.BlendingMode.MULTIPLY
    MaterialBlendingMode.SCREEN -> MaterialBuilder.BlendingMode.SCREEN
}
