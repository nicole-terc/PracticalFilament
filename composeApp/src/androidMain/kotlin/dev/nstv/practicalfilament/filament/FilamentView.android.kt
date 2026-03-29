package dev.nstv.practicalfilament.filament

import android.view.Surface
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.filament.android.UiHelper

@Composable
actual fun FilamentView(
    modifier: Modifier,
    camera: CameraConfig,
    lights: List<LightConfig>,
    onEngineReady: (FilamentEngine) -> Unit,
) {
    val context = LocalContext.current
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
            SurfaceView(context).also { surfaceView ->
                engine.initialize()

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
                    attachTo(surfaceView)
                }

                engine.setUiHelper(uiHelper)
                engine.updateCamera(camera)
                lights.forEach { engine.addLight(it) }
                onEngineReady(engine)
                engine.startRenderLoop()
            }
        }
    )
}
