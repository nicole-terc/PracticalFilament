package dev.nstv.practicalfilament

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.opengl.Matrix
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.view.Choreographer
import android.view.Surface
import android.view.SurfaceHolder
import android.view.WindowManager
import com.google.android.filament.Box
import com.google.android.filament.Camera
import com.google.android.filament.EntityManager
import com.google.android.filament.Filament
import com.google.android.filament.IndexBuffer
import com.google.android.filament.IndirectLight
import com.google.android.filament.LightManager
import com.google.android.filament.Material
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.Skybox
import com.google.android.filament.SwapChain
import com.google.android.filament.Texture
import com.google.android.filament.TextureSampler
import com.google.android.filament.VertexBuffer
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.android.DisplayHelper
import com.google.android.filament.android.FilamentHelper
import com.google.android.filament.android.UiHelper
import com.google.android.filament.gltfio.Animator
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider
import com.google.android.filament.utils.KTX1Loader
import com.google.android.filament.utils.Utils
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.ProjectionType
import dev.nstv.practicalfilament.screen.sky.SkyWallpaperConfig
import dev.nstv.practicalfilament.screen.sky.resolveRealtimeSkyConfig
import dev.nstv.practicalfilament.screen.wallpaper.LiveWallpaperPreferences
import dev.nstv.practicalfilament.screen.wallpaper.LiveWallpaperPreset
import dev.nstv.practicalfilament.screen.wallpaper.liveWallpaperHueAt
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import com.google.android.filament.Engine as FilamentEngine

private const val ComposeResourcesRoot =
    "composeResources/practicalfilament.composeapp.generated.resources"
private const val WallpaperIndirectLightPath =
    "$ComposeResourcesRoot/files/envs/pillars_2k/pillars_2k_ibl.ktx"
private const val WallpaperSkyboxPath =
    "$ComposeResourcesRoot/files/envs/pillars_2k/pillars_2k_skybox.ktx"
private const val WallpaperEnvironmentIntensity = 30_000f
private const val WallpaperSkyMaterialPath =
    "$ComposeResourcesRoot/files/materials/simulated_skybox.filamat"
private const val WallpaperMoonDiskTexturePath =
    "$ComposeResourcesRoot/files/textures/moon_disk.png"
private const val WallpaperMoonNormalTexturePath =
    "$ComposeResourcesRoot/files/textures/moon_normal.png"
private const val WallpaperMilkyWayTexturePath = "$ComposeResourcesRoot/files/textures/milkyway.png"

class FilamentLiveWallpaperService : WallpaperService() {
    companion object {
        init {
            Filament.init()
            Utils.init()
        }
    }

    override fun onCreateEngine(): WallpaperService.Engine = FilamentWallpaperEngine()

    private inner class FilamentWallpaperEngine : WallpaperService.Engine() {
        private lateinit var uiHelper: UiHelper
        private lateinit var displayHelper: DisplayHelper
        private lateinit var choreographer: Choreographer
        private lateinit var filamentEngine: FilamentEngine
        private lateinit var renderer: Renderer
        private lateinit var scene: Scene
        private lateinit var view: View
        private lateinit var camera: Camera

        private var cameraEntity = 0
        private var sunLightEntity = 0
        private var swapChain: SwapChain? = null
        private val frameScheduler = FrameCallback()
        private var viewportWidth = 1
        private var viewportHeight = 1
        private var materialProvider: UbershaderProvider? = null
        private var assetLoader: AssetLoader? = null
        private var resourceLoader: ResourceLoader? = null
        private var indirectLightBundle: EnvironmentIndirectLight? = null
        private var environmentSkyboxBundle: EnvironmentSkybox? = null
        private var rainbowSkybox: Skybox? = null
        private var currentAsset: ManagedGltfAsset? = null
        private var currentPreset = LiveWallpaperPreset.default
        private var configuredSky: ManagedConfiguredSky? = null
        private var configuredSkyConfig = SkyWallpaperConfig.default

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            surfaceHolder.setSizeFromLayout()
            surfaceHolder.setFormat(PixelFormat.RGBA_8888)

            choreographer = Choreographer.getInstance()
            displayHelper = DisplayHelper(this@FilamentLiveWallpaperService)

            setupUiHelper()
            setupFilament()
            setupView()
            setupScene()
        }

        private fun setupUiHelper() {
            uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)
            uiHelper.renderCallback = SurfaceCallback()
            uiHelper.attachTo(surfaceHolder)
        }

        private fun setupFilament() {
            filamentEngine = FilamentEngine.create()
            renderer = filamentEngine.createRenderer()
            scene = filamentEngine.createScene()
            view = filamentEngine.createView()
            cameraEntity = EntityManager.get().create()
            camera = filamentEngine.createCamera(cameraEntity)
            materialProvider = UbershaderProvider(filamentEngine)
            assetLoader = AssetLoader(filamentEngine, materialProvider!!, EntityManager.get())
            resourceLoader = ResourceLoader(filamentEngine, true)
        }

        private fun setupView() {
            rainbowSkybox = Skybox.Builder().build(filamentEngine)
            scene.skybox = rainbowSkybox
            view.camera = camera
            view.scene = scene
        }

        private fun setupScene() {
            camera.setExposure(16f, 1f / 125f, 100f)
            addSunLight()
            applyPreset(LiveWallpaperPreferences.loadSelectedPreset(this@FilamentLiveWallpaperService))
        }

        private fun addSunLight() {
            if (sunLightEntity != 0) return
            sunLightEntity = EntityManager.get().create()
            LightManager.Builder(LightManager.Type.SUN)
                .direction(0.4f, -1f, -0.6f)
                .intensity(85_000f)
                .build(filamentEngine, sunLightEntity)
            scene.addEntity(sunLightEntity)
        }

        private fun applyPreset(preset: LiveWallpaperPreset) {
            currentPreset = preset
            destroyCurrentAsset()
            destroyConfiguredSky()

            if (preset == LiveWallpaperPreset.CONFIGURED_SKY) {
                configuredSkyConfig =
                    LiveWallpaperPreferences.loadSkyConfig(this@FilamentLiveWallpaperService)
                ensureConfiguredSky()
                applyConfiguredSky(configuredSkyConfig, System.currentTimeMillis())
                return
            }

            if (!preset.usesModel) {
                scene.setIndirectLight(null)
                rainbowSkybox?.let(scene::setSkybox)
                applyCameraPose(preset.cameraAt(0f))
                return
            }

            ensureEnvironment()
            indirectLightBundle?.let { bundle ->
                bundle.indirectLight.intensity = WallpaperEnvironmentIntensity
                scene.setIndirectLight(bundle.indirectLight)
            }
            environmentSkyboxBundle?.let { bundle ->
                scene.setSkybox(bundle.skybox)
            }

            val asset = loadGltfAsset(androidAssetPathFor(preset.assetPath ?: ""))
            if (asset == null) {
                currentPreset = LiveWallpaperPreset.default
                scene.setIndirectLight(null)
                rainbowSkybox?.let(scene::setSkybox)
                applyCameraPose(currentPreset.cameraAt(0f))
                return
            }

            currentAsset = asset
            transformGltfToUnitCube(asset.asset)
            scene.addEntities(asset.asset.entities)
            updatePresetFrame(0L)
        }

        private fun ensureEnvironment() {
            if (indirectLightBundle == null) {
                loadAssetBuffer(WallpaperIndirectLightPath)?.let { buffer ->
                    val bundle = KTX1Loader.createIndirectLight(filamentEngine, buffer)
                    val indirectLight = bundle.indirectLight
                    val cubemap = bundle.cubemap
                    if (indirectLight != null && cubemap != null) {
                        indirectLightBundle = EnvironmentIndirectLight(indirectLight, cubemap)
                    }
                }
            }

            if (environmentSkyboxBundle == null) {
                loadAssetBuffer(WallpaperSkyboxPath)?.let { buffer ->
                    val bundle = KTX1Loader.createSkybox(filamentEngine, buffer)
                    val skybox = bundle.skybox
                    val cubemap = bundle.cubemap
                    if (skybox != null) {
                        environmentSkyboxBundle = EnvironmentSkybox(skybox, cubemap)
                    }
                }
            }
        }

        private fun updatePresetFrame(frameTimeNanos: Long) {
            val seconds = frameTimeNanos / 1_000_000_000f
            if (currentPreset == LiveWallpaperPreset.CONFIGURED_SKY) {
                if (configuredSkyConfig.syncEnabled || configuredSkyConfig.syncManualOverride) {
                    applyConfiguredSky(configuredSkyConfig, System.currentTimeMillis())
                }
                return
            }
            if (!currentPreset.usesModel) {
                val hue = liveWallpaperHueAt(seconds)
                val color = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
                scene.skybox?.setColor(
                    floatArrayOf(
                        Color.red(color) / 255f,
                        Color.green(color) / 255f,
                        Color.blue(color) / 255f,
                        1f,
                    ),
                )
                return
            }

            applyCameraPose(currentPreset.cameraAt(seconds))
            val asset = currentAsset ?: return
            if (asset.animator.animationCount > 0) {
                val duration = asset.animator.getAnimationDuration(0).coerceAtLeast(0.01f)
                val animationTime = seconds % duration
                asset.animator.applyAnimation(0, animationTime)
                asset.animator.updateBoneMatrices()
            }
        }

        private fun applyCameraPose(config: CameraConfig) {
            camera.lookAt(
                config.position.x.toDouble(),
                config.position.y.toDouble(),
                config.position.z.toDouble(),
                config.lookAt.x.toDouble(),
                config.lookAt.y.toDouble(),
                config.lookAt.z.toDouble(),
                config.up.x.toDouble(),
                config.up.y.toDouble(),
                config.up.z.toDouble(),
            )
            applyCameraProjection(config)
        }

        private fun applyCameraProjection(config: CameraConfig) {
            val aspect = viewportWidth.toDouble() / viewportHeight.coerceAtLeast(1).toDouble()
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

        private fun loadGltfAsset(path: String): ManagedGltfAsset? {
            val loader = assetLoader ?: return null
            val currentResourceLoader = resourceLoader ?: return null
            val buffer = loadAssetBuffer(path) ?: return null
            val asset = loader.createAsset(buffer) ?: return null
            if (path.endsWith(".gltf", ignoreCase = true)) {
                val basePath = path.substringBeforeLast('/', "")
                asset.resourceUris.forEach { resourceUri ->
                    val resourceBuffer =
                        loadAssetBuffer(resolveRelativeAssetPath(basePath, resourceUri))
                            ?: return null
                    currentResourceLoader.addResourceData(resourceUri, resourceBuffer)
                }
            }
            currentResourceLoader.loadResources(asset)
            asset.releaseSourceData()
            return ManagedGltfAsset(
                asset = asset,
                animator = asset.instance.animator,
            )
        }

        private fun transformGltfToUnitCube(asset: FilamentAsset) {
            val boundingBox = asset.boundingBox
            val center = boundingBox.center
            val halfExtent = boundingBox.halfExtent
            val maxExtent = max(max(halfExtent[0], halfExtent[1]), halfExtent[2]) * 2f
            if (maxExtent <= 0f) return

            val scale = 2f / maxExtent
            val transform = FloatArray(16)
            Matrix.setIdentityM(transform, 0)
            Matrix.scaleM(transform, 0, scale, scale, scale)
            Matrix.translateM(transform, 0, -center[0], -center[1], -center[2])

            val transformManager = filamentEngine.transformManager
            val instance = transformManager.getInstance(asset.root)
            if (instance != 0) {
                transformManager.setTransform(instance, transform)
            }
        }

        private fun destroyCurrentAsset() {
            val asset = currentAsset ?: return
            scene.removeEntities(asset.asset.entities)
            assetLoader?.destroyAsset(asset.asset)
            currentAsset = null
        }

        private fun ensureConfiguredSky() {
            if (configuredSky != null) return
            val materialBuffer = loadAssetBuffer(WallpaperSkyMaterialPath) ?: return
            val material = Material.Builder()
                .payload(materialBuffer, materialBuffer.remaining())
                .build(filamentEngine)
            val materialInstance = material.createInstance()

            val moonTexture = loadTexture(WallpaperMoonDiskTexturePath) ?: return
            val moonNormal = loadTexture(WallpaperMoonNormalTexturePath) ?: return
            val milkyWayTexture = loadTexture(WallpaperMilkyWayTexturePath) ?: return

            materialInstance.setParameter(
                "moonTexture",
                moonTexture,
                TextureSampler(
                    TextureSampler.MinFilter.LINEAR,
                    TextureSampler.MagFilter.LINEAR,
                    TextureSampler.WrapMode.CLAMP_TO_EDGE,
                ),
            )
            materialInstance.setParameter(
                "moonNormal",
                moonNormal,
                TextureSampler(
                    TextureSampler.MinFilter.LINEAR,
                    TextureSampler.MagFilter.LINEAR,
                    TextureSampler.WrapMode.CLAMP_TO_EDGE,
                ),
            )
            materialInstance.setParameter(
                "milkyWayTexture",
                milkyWayTexture,
                TextureSampler(
                    TextureSampler.MinFilter.LINEAR,
                    TextureSampler.MagFilter.LINEAR,
                    TextureSampler.WrapMode.REPEAT,
                ),
            )

            val vertexData = ByteBuffer.allocateDirect(3 * 2 * Float.SIZE_BYTES).apply {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().put(floatArrayOf(-1f, -1f, 3f, -1f, -1f, 3f))
            }
            val vertexBuffer = VertexBuffer.Builder()
                .vertexCount(3)
                .bufferCount(1)
                .attribute(
                    VertexBuffer.VertexAttribute.POSITION,
                    0,
                    VertexBuffer.AttributeType.FLOAT2,
                    0,
                    8
                )
                .build(filamentEngine)
            vertexBuffer.setBufferAt(filamentEngine, 0, vertexData)

            val indexBufferData = ByteBuffer.allocateDirect(3 * Short.SIZE_BYTES).apply {
                order(ByteOrder.nativeOrder())
                asShortBuffer().put(shortArrayOf(0, 1, 2))
            }
            val indexBuffer = IndexBuffer.Builder()
                .indexCount(3)
                .bufferType(IndexBuffer.Builder.IndexType.USHORT)
                .build(filamentEngine)
            indexBuffer.setBuffer(filamentEngine, indexBufferData)

            val entity = EntityManager.get().create()
            RenderableManager.Builder(1)
                .geometry(0, RenderableManager.PrimitiveType.TRIANGLES, vertexBuffer, indexBuffer)
                .material(0, materialInstance)
                .boundingBox(Box(0f, 0f, 0f, 10_000f, 10_000f, 10_000f))
                .culling(false)
                .castShadows(false)
                .receiveShadows(false)
                .priority(7)
                .build(filamentEngine, entity)
            scene.addEntity(entity)

            configuredSky = ManagedConfiguredSky(
                material = material,
                materialInstance = materialInstance,
                entity = entity,
                vertexBuffer = vertexBuffer,
                indexBuffer = indexBuffer,
                textures = listOf(moonTexture, moonNormal, milkyWayTexture),
            )
        }

        private fun applyConfiguredSky(
            config: SkyWallpaperConfig,
            currentTimeMillis: Long,
        ) {
            val sky = configuredSky ?: return
            val resolvedConfig = resolveRealtimeSkyConfig(config, currentTimeMillis)
            scene.setIndirectLight(null)
            scene.setSkybox(null)
            camera.setExposure(
                resolvedConfig.aperture,
                1f / resolvedConfig.shutterSpeed.coerceAtLeast(0.05f),
                resolvedConfig.iso,
            )
            applyCameraPose(
                CameraConfig(
                    position = Float3(0f, 0f, 0f),
                    lookAt = Float3(1f, 0f, 0f),
                    up = Float3(0f, 1f, 0f),
                    fovDegrees = computeVerticalFovDegrees(resolvedConfig.focalLength).toDouble(),
                    near = 0.1,
                    far = 5000.0,
                ),
            )
            applySkyConfigToMaterial(
                sky.materialInstance,
                resolvedConfig,
                viewportHeight = viewportHeight
            )
        }

        private fun destroyConfiguredSky() {
            val sky = configuredSky ?: return
            scene.removeEntity(sky.entity)
            filamentEngine.destroyEntity(sky.entity)
            EntityManager.get().destroy(sky.entity)
            sky.textures.forEach(filamentEngine::destroyTexture)
            filamentEngine.destroyIndexBuffer(sky.indexBuffer)
            filamentEngine.destroyVertexBuffer(sky.vertexBuffer)
            filamentEngine.destroyMaterialInstance(sky.materialInstance)
            filamentEngine.destroyMaterial(sky.material)
            configuredSky = null
        }

        private fun loadTexture(path: String): Texture? {
            val bytes = runCatching {
                this@FilamentLiveWallpaperService.assets.open(path).readBytes()
            }.getOrNull()
                ?: return null
            val bitmap =
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val pixelBuffer = ByteBuffer.allocateDirect(bitmap.byteCount)
            bitmap.copyPixelsToBuffer(pixelBuffer)
            pixelBuffer.flip()
            val texture = Texture.Builder()
                .width(bitmap.width)
                .height(bitmap.height)
                .sampler(Texture.Sampler.SAMPLER_2D)
                .format(Texture.InternalFormat.RGBA8)
                .levels(1)
                .build(filamentEngine)
            texture.setImage(
                filamentEngine,
                0,
                Texture.PixelBufferDescriptor(pixelBuffer, Texture.Format.RGBA, Texture.Type.UBYTE),
            )
            bitmap.recycle()
            return texture
        }

        private fun loadAssetBuffer(path: String): ByteBuffer? {
            val bytes =
                runCatching { this@FilamentLiveWallpaperService.assets.open(path).readBytes() }
                    .getOrNull()
                    ?: return null
            return ByteBuffer.allocateDirect(bytes.size).apply {
                order(ByteOrder.nativeOrder())
                put(bytes)
                flip()
            }
        }

        private fun resolveRelativeAssetPath(basePath: String, relativePath: String): String {
            if (relativePath.startsWith("file://")) return relativePath
            if (relativePath.startsWith("/")) return relativePath
            return if (basePath.isEmpty()) relativePath else "$basePath/$relativePath"
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                applyPreset(LiveWallpaperPreferences.loadSelectedPreset(this@FilamentLiveWallpaperService))
                choreographer.postFrameCallback(frameScheduler)
            } else {
                choreographer.removeFrameCallback(frameScheduler)
            }
        }

        override fun onDestroy() {
            super.onDestroy()

            choreographer.removeFrameCallback(frameScheduler)
            uiHelper.detach()
            displayHelper.detach()
            destroyCurrentAsset()
            destroyConfiguredSky()
            scene.setIndirectLight(null)
            scene.setSkybox(null)
            indirectLightBundle?.let { bundle ->
                filamentEngine.destroyIndirectLight(bundle.indirectLight)
                filamentEngine.destroyTexture(bundle.cubemap)
            }
            environmentSkyboxBundle?.let { bundle ->
                filamentEngine.destroySkybox(bundle.skybox)
                bundle.cubemap?.let(filamentEngine::destroyTexture)
            }
            rainbowSkybox?.let(filamentEngine::destroySkybox)
            resourceLoader?.destroy()
            assetLoader?.destroy()
            materialProvider?.destroyMaterials()
            if (sunLightEntity != 0) {
                scene.removeEntity(sunLightEntity)
                filamentEngine.destroyEntity(sunLightEntity)
                EntityManager.get().destroy(sunLightEntity)
            }
            swapChain?.let(filamentEngine::destroySwapChain)
            filamentEngine.destroyRenderer(renderer)
            filamentEngine.destroyView(view)
            filamentEngine.destroyScene(scene)
            filamentEngine.destroyCameraComponent(cameraEntity)
            EntityManager.get().destroy(cameraEntity)
            filamentEngine.destroy()
        }

        private inner class FrameCallback : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                choreographer.postFrameCallback(this)
                val currentSwapChain = swapChain ?: return
                if (!uiHelper.isReadyToRender) return
                updatePresetFrame(frameTimeNanos)
                if (!renderer.beginFrame(currentSwapChain, frameTimeNanos)) return
                renderer.render(view)
                renderer.endFrame()
            }
        }

        private inner class SurfaceCallback : UiHelper.RendererCallback {
            override fun onNativeWindowChanged(surface: Surface) {
                swapChain?.let(filamentEngine::destroySwapChain)
                swapChain = filamentEngine.createSwapChain(surface)

                @Suppress("deprecation")
                val display = if (Build.VERSION.SDK_INT >= 30) {
                    displayContext?.let(Api30Impl::getDisplay) ?: return
                } else {
                    (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay
                }

                displayHelper.attach(renderer, display)
            }

            override fun onDetachedFromSurface() {
                displayHelper.detach()
                swapChain?.let {
                    filamentEngine.destroySwapChain(it)
                    filamentEngine.flushAndWait()
                    swapChain = null
                }
            }

            override fun onResized(width: Int, height: Int) {
                viewportWidth = width.coerceAtLeast(1)
                viewportHeight = height.coerceAtLeast(1)
                if (currentPreset == LiveWallpaperPreset.CONFIGURED_SKY) {
                    applyConfiguredSky(configuredSkyConfig, System.currentTimeMillis())
                } else {
                    applyCameraProjection(currentPreset.cameraAt(0f))
                }
                view.viewport = Viewport(0, 0, width, height)
                FilamentHelper.synchronizePendingFrames(filamentEngine)
            }
        }
    }

    private data class ManagedGltfAsset(
        val asset: FilamentAsset,
        val animator: Animator,
    )

    private data class EnvironmentIndirectLight(
        val indirectLight: IndirectLight,
        val cubemap: Texture,
    )

    private data class EnvironmentSkybox(
        val skybox: Skybox,
        val cubemap: Texture?,
    )

    private data class ManagedConfiguredSky(
        val material: Material,
        val materialInstance: MaterialInstance,
        val entity: Int,
        val vertexBuffer: VertexBuffer,
        val indexBuffer: IndexBuffer,
        val textures: List<Texture>,
    )

    private object Api30Impl {
        fun getDisplay(context: Context) = context.display
    }
}

private fun androidAssetPathFor(relativePath: String): String =
    "$ComposeResourcesRoot/$relativePath"

private fun applySkyConfigToMaterial(
    materialInstance: MaterialInstance,
    config: SkyWallpaperConfig,
    viewportHeight: Int,
) {
    val sunDirection = skyDirection(config.sunAzimuth, config.sunHeight)
    val moonDirection = skyDirection(config.moonAzimuth, config.moonHeight)
    val exposure = skyExposure(config.aperture, config.shutterSpeed, config.iso)
    val preExposedSunIntensity = config.sunIntensity * exposure

    val lambda = doubleArrayOf(680e-9, 550e-9, 440e-9)
    val n = 1.0003
    val moleculeDensity = 2.545e25
    val rayleighTerm = (8.0 * PI.pow(3.0) * (n * n - 1.0).pow(2.0)) / (3.0 * moleculeDensity)
    val depthR = FloatArray(3) { index ->
        ((rayleighTerm / lambda[index].pow(4.0)) * 8000.0 * config.rayleigh).toFloat()
    }
    val mieBase = 2.0e-5 * config.turbidity
    val depthM = FloatArray(3) { index ->
        (
                mieBase *
                        (550e-9 / lambda[index]).pow(1.3) *
                        1200.0 *
                        config.mieCoefficient
                ).toFloat()
    }

    val cutoffAngle = 96.0 * PI / 180.0
    val steepness = 1.5
    val zenithFade = 1.0 - exp(-(cutoffAngle / steepness))
    val zenithAngle = acos(sunDirection[1].coerceIn(-1f, 1f).toDouble())
    val sunFade =
        (max(0.0, 1.0 - exp(-((cutoffAngle - zenithAngle) / steepness))) / zenithFade).toFloat()
    val physicalSunIntensity = preExposedSunIntensity * sunFade

    val sunHalo = buildHalo(
        config.sunRadius,
        config.sunLimbDarkening,
        config.sunDiskIntensityBoost,
        enabled = true
    )
    val moonHalo = buildMoonHalo(config.moonRadius, 1f, config.moonEnabled)

    val cloudHeightKm = config.cloudHeightMeters * 0.001f
    val cloudIntersectC = 6360f * 6360f - (6360f + cloudHeightKm) * (6360f + cloudHeightKm)
    val cloudControl = floatArrayOf(
        config.cloudCoverage.coerceIn(0f, 1f),
        config.cloudDensity.coerceAtLeast(0f),
        cloudIntersectC,
        config.cloudSpeed * (0.05f / 72f),
    )
    val shimmerControl = floatArrayOf(
        config.shimmerStrength,
        config.shimmerFrequency,
        config.shimmerMaskHeight,
        6360f,
    )
    val multiScatParams = floatArrayOf(
        (depthR[0] * config.msRayleigh + depthM[0] * config.msMie) * 0.25f,
        (depthR[1] * config.msRayleigh + depthM[1] * config.msMie) * 0.25f,
        (depthR[2] * config.msRayleigh + depthM[2] * config.msMie) * 0.25f,
        config.horizonGlow,
    )
    val g2 = config.mieG * config.mieG
    val starControl = buildStarControl(
        config.starDensity,
        config.starsEnabled,
        viewportHeight,
        config.focalLength
    )
    val milkyWayControl = floatArrayOf(
        if (config.milkyWayEnabled) config.milkyWayIntensity * 0.003f else 0f,
        config.milkyWaySaturation,
        config.milkyWayBlackPoint,
    )

    materialInstance.setParameter("sunDirection", sunDirection[0], sunDirection[1], sunDirection[2])
    materialInstance.setParameter(
        "sunDirection2",
        moonDirection[0],
        moonDirection[1],
        moonDirection[2]
    )
    materialInstance.setParameter("depthR", depthR[0], depthR[1], depthR[2])
    materialInstance.setParameter("depthM", depthM[0], depthM[1], depthM[2])
    materialInstance.setParameter("miePhaseParams", 1f + g2, -2f * config.mieG)
    materialInstance.setParameter("sunIntensity", physicalSunIntensity)
    materialInstance.setParameter("contrast", config.contrast)
    materialInstance.setParameter("nightColor", 0.0035f, 0.006f, 0.012f)
    materialInstance.setParameter("ozone", 0f, config.ozone * 0.1f, 0f)
    materialInstance.setParameter(
        "eclipseFactor",
        computeEclipseFactor(
            sunDirection = sunDirection,
            moonDirection = moonDirection,
            sunRadiusDegrees = config.sunRadius,
            moonRadiusDegrees = config.moonRadius,
            moonEnabled = config.moonEnabled,
        ),
    )
    materialInstance.setParameter(
        "multiScatParams",
        multiScatParams[0],
        multiScatParams[1],
        multiScatParams[2],
        multiScatParams[3]
    )
    materialInstance.setParameter("sunHalo", sunHalo[0], sunHalo[1], sunHalo[2], sunHalo[3])
    materialInstance.setParameter(
        "shimmerControl",
        shimmerControl[0],
        shimmerControl[1],
        shimmerControl[2],
        shimmerControl[3]
    )
    materialInstance.setParameter(
        "cloudControl",
        cloudControl[0],
        cloudControl[1],
        cloudControl[2],
        cloudControl[3]
    )
    materialInstance.setParameter(
        "cloudControl2",
        config.cloudEvolutionSpeed,
        if (config.cloudVolumetrics) 1f else 0f,
        0f,
        0f,
    )
    materialInstance.setParameter("sunIntensity2", config.moonIntensity)
    materialInstance.setParameter("sunHalo2", moonHalo[0], moonHalo[1], moonHalo[2], moonHalo[3])
    materialInstance.setParameter(
        "waterControl",
        config.waterStrength,
        config.waterSpeed,
        if (config.waterDerivativeTrick) 1f else 0f,
        config.waterOctaves,
    )
    materialInstance.setParameter(
        "starControl",
        starControl[0],
        starControl[1],
        starControl[2],
        starControl[3]
    )
    materialInstance.setParameter("starIntensity", 2f.pow(config.starIntensityExponent))
    materialInstance.setParameter("exposure", 1f)
    materialInstance.setParameter(
        "milkyWayControl",
        milkyWayControl[0],
        milkyWayControl[1],
        milkyWayControl[2]
    )
    materialInstance.setParameter(
        "milkyWayRotation",
        MaterialInstance.FloatElement.MAT3,
        buildMilkyWayRotation(config.milkyWaySiderealTime, config.milkyWayLatitude),
        0,
        1,
    )
}

private fun skyDirection(azimuthDegrees: Float, heightCos: Float): FloatArray {
    val theta = acos(heightCos.coerceIn(-1f, 1f).toDouble())
    val phi = azimuthDegrees.toDouble() * PI / 180.0
    return floatArrayOf(
        (sin(theta) * cos(phi)).toFloat(),
        cos(theta).toFloat(),
        (sin(theta) * sin(phi)).toFloat(),
    )
}

private fun skyExposure(aperture: Float, shutterSpeed: Float, iso: Float): Float {
    val shutterSeconds = 1f / shutterSpeed.coerceAtLeast(0.05f)
    val ev100Linear = (aperture * aperture) / shutterSeconds * (100f / iso.coerceAtLeast(1f))
    return 1f / (1.2f * ev100Linear)
}

private fun buildHalo(
    radiusDegrees: Float,
    limbDarkening: Float,
    intensity: Float,
    enabled: Boolean
): FloatArray {
    val cosRadius = cos(radiusDegrees.toDouble() * PI / 180.0).toFloat()
    val solidAngle = (2.0 * PI * (1.0 - cosRadius)).toFloat()
    val radianceConversion = 1f / max(1e-9f, solidAngle)
    return floatArrayOf(
        cosRadius,
        limbDarkening,
        intensity * radianceConversion,
        if (enabled) 1f else 0f
    )
}

private fun buildMoonHalo(radiusDegrees: Float, intensity: Float, enabled: Boolean): FloatArray {
    val radians = radiusDegrees.toDouble() * PI / 180.0
    val cosRadius = cos(radians).toFloat()
    val sinRadius = sin(radians).toFloat()
    val solidAngle = (2.0 * PI * (1.0 - cosRadius)).toFloat()
    val radianceConversion = 1f / max(1e-9f, solidAngle)
    return floatArrayOf(
        cosRadius,
        sinRadius,
        intensity * radianceConversion,
        if (enabled) 1f else 0f
    )
}

private fun buildStarControl(
    density: Float,
    enabled: Boolean,
    viewportHeightPx: Int,
    focalLengthMm: Float,
): FloatArray {
    val compensatedDensity = (density * 12f).coerceIn(0f, 1f)
    val pixelScale =
        (1f / viewportHeightPx.coerceAtLeast(1)) * (24f / focalLengthMm.coerceAtLeast(1f))
    return floatArrayOf(compensatedDensity, if (enabled) 1f else 0f, 100f, pixelScale * 1.3f)
}

private fun computeEclipseFactor(
    sunDirection: FloatArray,
    moonDirection: FloatArray,
    sunRadiusDegrees: Float,
    moonRadiusDegrees: Float,
    moonEnabled: Boolean,
): Float {
    if (!moonEnabled) return 1f
    val sunRadius = sunRadiusDegrees.toDouble() * PI / 180.0
    val moonRadius = moonRadiusDegrees.toDouble() * PI / 180.0
    val dot =
        (sunDirection[0] * moonDirection[0] + sunDirection[1] * moonDirection[1] + sunDirection[2] * moonDirection[2]).coerceIn(
            -1f,
            1f
        )
    val separation = acos(dot.toDouble())
    val overlap = areaIntersection(sunRadius, moonRadius, separation)
    val sunArea = PI * sunRadius * sunRadius
    val ratio = overlap / max(1e-9, sunArea)
    return (1.0 - min(1.0, max(0.0, ratio))).toFloat()
}

private fun areaIntersection(r1: Double, r2: Double, d: Double): Double {
    if (d >= r1 + r2) return 0.0
    if (d <= abs(r1 - r2)) {
        val radius = min(r1, r2)
        return PI * radius * radius
    }
    val r1Sq = r1 * r1
    val r2Sq = r2 * r2
    val c1 = ((d * d + r1Sq - r2Sq) / (2.0 * d * r1)).coerceIn(-1.0, 1.0)
    val c2 = ((d * d + r2Sq - r1Sq) / (2.0 * d * r2)).coerceIn(-1.0, 1.0)
    val part1 = r1Sq * acos(c1)
    val part2 = r2Sq * acos(c2)
    val triangleTerm = (-d + r1 + r2) * (d + r1 - r2) * (d - r1 + r2) * (d + r1 + r2)
    val part3 = 0.5 * sqrt(max(0.0, triangleTerm))
    return part1 + part2 - part3
}

private fun buildMilkyWayRotation(
    siderealTimeHours: Float,
    latitudeDegrees: Float,
): FloatArray {
    val eqToGal = floatArrayOf(
        -0.054876f, 0.494109f, -0.867666f,
        -0.873437f, -0.444830f, -0.198076f,
        -0.483835f, 0.746982f, 0.455984f,
    )
    val worldToEq = floatArrayOf(
        1f, 0f, 0f,
        0f, 1f, 0f,
        0f, 0f, 1f,
    )
    rotateX(worldToEq, (latitudeDegrees.toDouble() * PI / 180.0 - PI / 2.0).toFloat())
    rotateY(worldToEq, (siderealTimeHours * (PI / 12.0).toFloat()))
    return multiplyMat3(eqToGal, worldToEq)
}

private fun rotateX(matrix: FloatArray, angleRadians: Float) {
    val c = cos(angleRadians.toDouble()).toFloat()
    val s = sin(angleRadians.toDouble()).toFloat()
    val m1 = matrix[1]
    val m2 = matrix[2]
    val m4 = matrix[4]
    val m5 = matrix[5]
    val m7 = matrix[7]
    val m8 = matrix[8]
    matrix[1] = m1 * c - m2 * s
    matrix[2] = m1 * s + m2 * c
    matrix[4] = m4 * c - m5 * s
    matrix[5] = m4 * s + m5 * c
    matrix[7] = m7 * c - m8 * s
    matrix[8] = m7 * s + m8 * c
}

private fun rotateY(matrix: FloatArray, angleRadians: Float) {
    val c = cos(angleRadians.toDouble()).toFloat()
    val s = sin(angleRadians.toDouble()).toFloat()
    val m0 = matrix[0]
    val m2 = matrix[2]
    val m3 = matrix[3]
    val m5 = matrix[5]
    val m6 = matrix[6]
    val m8 = matrix[8]
    matrix[0] = m0 * c + m2 * s
    matrix[2] = -m0 * s + m2 * c
    matrix[3] = m3 * c + m5 * s
    matrix[5] = -m3 * s + m5 * c
    matrix[6] = m6 * c + m8 * s
    matrix[8] = -m6 * s + m8 * c
}

private fun multiplyMat3(a: FloatArray, b: FloatArray): FloatArray {
    val out = FloatArray(9)
    val a00 = a[0]
    val a10 = a[1]
    val a20 = a[2]
    val a01 = a[3]
    val a11 = a[4]
    val a21 = a[5]
    val a02 = a[6]
    val a12 = a[7]
    val a22 = a[8]
    val b00 = b[0]
    val b10 = b[1]
    val b20 = b[2]
    val b01 = b[3]
    val b11 = b[4]
    val b21 = b[5]
    val b02 = b[6]
    val b12 = b[7]
    val b22 = b[8]
    out[0] = a00 * b00 + a01 * b10 + a02 * b20
    out[1] = a10 * b00 + a11 * b10 + a12 * b20
    out[2] = a20 * b00 + a21 * b10 + a22 * b20
    out[3] = a00 * b01 + a01 * b11 + a02 * b21
    out[4] = a10 * b01 + a11 * b11 + a12 * b21
    out[5] = a20 * b01 + a21 * b11 + a22 * b21
    out[6] = a00 * b02 + a01 * b12 + a02 * b22
    out[7] = a10 * b02 + a11 * b12 + a12 * b22
    out[8] = a20 * b02 + a21 * b12 + a22 * b22
    return out
}

private fun computeVerticalFovDegrees(focalLengthMm: Float, sensorHeightMm: Float = 24f): Float {
    val focal = focalLengthMm.coerceAtLeast(1f).toDouble()
    val sensor = sensorHeightMm.toDouble()
    return ((2.0 * atan(sensor / (2.0 * focal))) * 180.0 / PI).toFloat()
}
