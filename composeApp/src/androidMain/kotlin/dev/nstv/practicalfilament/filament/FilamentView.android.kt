package dev.nstv.practicalfilament.filament

import android.graphics.Outline
import android.view.Surface
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.ViewOutlineProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.filament.android.UiHelper
import kotlin.math.min

@Composable
actual fun FilamentView(
    modifier: Modifier,
    camera: CameraConfig,
    lights: List<LightConfig>,
    backgroundColor: Color,
    clipShape: FilamentClipShape?,
    onEngineReady: (FilamentEngine) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val engine = remember { AndroidFilamentEngine(context) }

    DisposableEffect(Unit) {
        onDispose {
            engine.stopRenderLoop()
            engine.destroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val filamentHostView: View = if (clipShape == null) {
                SurfaceView(context)
            } else {
                TextureView(context)
            }
            filamentHostView.also { hostView ->
                engine.initialize()
                engine.setClearColor(backgroundColor)

                val uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK).apply {
                    renderCallback = object : UiHelper.RendererCallback {
                        override fun onNativeWindowChanged(surface: Surface) {
                            engine.attachSurface(surface)
                        }

                        override fun onDetachedFromSurface() {
                            engine.detachSurface()
                        }

                        override fun onResized(width: Int, height: Int) {
                            engine.updateViewport(width, height)
                        }
                    }
                    when (hostView) {
                        is SurfaceView -> attachTo(hostView)
                        is TextureView -> attachTo(hostView)
                    }
                }

                applyClipShape(hostView, clipShape, density)
                engine.setUiHelper(uiHelper)
                engine.updateCamera(camera)
                lights.forEach { engine.addLight(it) }
                onEngineReady(engine)
                engine.startRenderLoop()
            }
        },
        update = { hostView ->
            applyClipShape(hostView, clipShape, density)
            engine.setClearColor(backgroundColor)
        },
    )
}

private fun applyClipShape(
    view: View,
    clipShape: FilamentClipShape?,
    density: androidx.compose.ui.unit.Density,
) {
    if (clipShape == null) {
        view.clipToOutline = false
        view.outlineProvider = ViewOutlineProvider.BOUNDS
        if (view is TextureView) {
            view.isOpaque = true
        }
        return
    }

    if (view is TextureView) {
        view.isOpaque = false
    }

    view.outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(host: View, outline: Outline) {
            val width = host.width
            val height = host.height
            if (width <= 0 || height <= 0) return
            when (clipShape) {
                FilamentClipShape.Circle -> outline.setOval(0, 0, width, height)
                is FilamentClipShape.RoundedRect -> outline.setRoundRect(
                    0,
                    0,
                    width,
                    height,
                    with(density) { clipShape.cornerRadius.toPx() },
                )
            }
        }
    }
    view.clipToOutline = true
    view.invalidateOutline()
}
