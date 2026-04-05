package dev.nstv.practicalfilament

import android.animation.ValueAnimator
import android.app.Service
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.view.Choreographer
import android.view.Surface
import android.view.SurfaceHolder
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import com.google.android.filament.Camera
import com.google.android.filament.EntityManager
import com.google.android.filament.Filament
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.Skybox
import com.google.android.filament.SwapChain
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.android.DisplayHelper
import com.google.android.filament.android.FilamentHelper
import com.google.android.filament.android.UiHelper

class FilamentLiveWallpaperService : WallpaperService() {
    companion object {
        init {
            Filament.init()
        }
    }

    override fun onCreateEngine(): WallpaperService.Engine = FilamentWallpaperEngine()

    private inner class FilamentWallpaperEngine : WallpaperService.Engine() {
        private lateinit var uiHelper: UiHelper
        private lateinit var displayHelper: DisplayHelper
        private lateinit var choreographer: Choreographer
        private lateinit var engine: com.google.android.filament.Engine
        private lateinit var renderer: Renderer
        private lateinit var scene: Scene
        private lateinit var view: View
        private lateinit var camera: Camera

        private var swapChain: SwapChain? = null
        private val frameScheduler = FrameCallback()
        private val animator = ValueAnimator.ofFloat(0f, 360f)

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
            engine = com.google.android.filament.Engine.create()
            renderer = engine.createRenderer()
            scene = engine.createScene()
            view = engine.createView()
            camera = engine.createCamera(engine.entityManager.create())
        }

        private fun setupView() {
            scene.skybox = Skybox.Builder().build(engine)
            view.camera = camera
            view.scene = scene
        }

        private fun setupScene() {
            camera.setExposure(16f, 1f / 125f, 100f)
            startAnimation()
        }

        private fun startAnimation() {
            animator.interpolator = LinearInterpolator()
            animator.duration = 10_000L
            animator.repeatMode = ValueAnimator.RESTART
            animator.repeatCount = ValueAnimator.INFINITE
            animator.addUpdateListener { animation ->
                val hue = animation.animatedValue as Float
                val color = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
                scene.skybox?.setColor(
                    floatArrayOf(
                        Color.red(color) / 255f,
                        Color.green(color) / 255f,
                        Color.blue(color) / 255f,
                        1f,
                    ),
                )
            }
            animator.start()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                choreographer.postFrameCallback(frameScheduler)
                animator.start()
            } else {
                choreographer.removeFrameCallback(frameScheduler)
                animator.cancel()
            }
        }

        override fun onDestroy() {
            super.onDestroy()

            choreographer.removeFrameCallback(frameScheduler)
            animator.cancel()
            uiHelper.detach()

            engine.destroyRenderer(renderer)
            engine.destroyView(view)
            engine.destroyScene(scene)
            engine.destroyCameraComponent(camera.entity)
            EntityManager.get().destroy(camera.entity)
            engine.destroy()
        }

        private inner class FrameCallback : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                choreographer.postFrameCallback(this)
                if (uiHelper.isReadyToRender && renderer.beginFrame(swapChain!!, frameTimeNanos)) {
                    renderer.render(view)
                    renderer.endFrame()
                }
            }
        }

        private inner class SurfaceCallback : UiHelper.RendererCallback {
            override fun onNativeWindowChanged(surface: Surface) {
                swapChain?.let(engine::destroySwapChain)
                swapChain = engine.createSwapChain(surface)

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
                    engine.destroySwapChain(it)
                    engine.flushAndWait()
                    swapChain = null
                }
            }

            override fun onResized(width: Int, height: Int) {
                val aspect = width.toDouble() / height.toDouble()
                camera.setProjection(45.0, aspect, 0.1, 20.0, Camera.Fov.VERTICAL)
                view.viewport = Viewport(0, 0, width, height)
                FilamentHelper.synchronizePendingFrames(engine)
            }
        }
    }

    private object Api30Impl {
        fun getDisplay(context: Context) = context.display
    }
}
