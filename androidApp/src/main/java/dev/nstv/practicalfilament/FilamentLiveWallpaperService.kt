package dev.nstv.practicalfilament

import android.app.Service
import android.content.Context
import android.graphics.PixelFormat
import android.opengl.Matrix
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.view.Choreographer
import android.view.Surface
import android.view.SurfaceHolder
import android.view.WindowManager
import com.google.android.filament.Camera
import com.google.android.filament.EntityManager
import com.google.android.filament.Filament
import com.google.android.filament.IndirectLight
import com.google.android.filament.LightManager
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.Skybox
import com.google.android.filament.SwapChain
import com.google.android.filament.Texture
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
import com.google.android.filament.Engine as FilamentEngine
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.ProjectionType
import dev.nstv.practicalfilament.screen.LiveWallpaperPreferences
import dev.nstv.practicalfilament.screen.LiveWallpaperPreset
import dev.nstv.practicalfilament.screen.liveWallpaperHueAt
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max

private const val ComposeResourcesRoot = "composeResources/practicalfilament.composeapp.generated.resources"
private const val WallpaperIndirectLightPath = "$ComposeResourcesRoot/files/envs/pillars_2k/pillars_2k_ibl.ktx"
private const val WallpaperSkyboxPath = "$ComposeResourcesRoot/files/envs/pillars_2k/pillars_2k_skybox.ktx"
private const val WallpaperEnvironmentIntensity = 30_000f

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
            if (!currentPreset.usesModel) {
                val hue = liveWallpaperHueAt(seconds)
                val color = android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
                scene.skybox?.setColor(
                    floatArrayOf(
                        android.graphics.Color.red(color) / 255f,
                        android.graphics.Color.green(color) / 255f,
                        android.graphics.Color.blue(color) / 255f,
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
                    val resourceBuffer = loadAssetBuffer(resolveRelativeAssetPath(basePath, resourceUri))
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

        private fun loadAssetBuffer(path: String): ByteBuffer? {
            val bytes = runCatching { this@FilamentLiveWallpaperService.assets.open(path).readBytes() }
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
                    (getSystemService(Service.WINDOW_SERVICE) as WindowManager).defaultDisplay
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
                applyCameraProjection(currentPreset.cameraAt(0f))
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

    private object Api30Impl {
        fun getDisplay(context: Context) = context.display
    }
}

private fun androidAssetPathFor(relativePath: String): String = "$ComposeResourcesRoot/$relativePath"
