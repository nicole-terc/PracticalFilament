package dev.nstv.practicalfilament.filament

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Metal.MTLCreateSystemDefaultDevice
import platform.Metal.MTLPixelFormatBGRA8Unorm
import platform.QuartzCore.CAMetalLayer
import platform.UIKit.UIView
import platform.UIKit.UIScreen

/**
 * Singleton holder for the FilamentBridge instance.
 * Must be set from Swift before any FilamentView is composed.
 *
 * In your Swift code (e.g., ContentView.swift or iOSApp.swift):
 *   FilamentBridgeHolder.shared.bridge = FilamentBridge()
 */
object FilamentBridgeHolder {
    var bridge: FilamentBridgeProtocol? = null
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun FilamentView(
    modifier: Modifier,
    camera: CameraConfig,
    lights: List<LightConfig>,
    backgroundColor: Color,
    onEngineReady: (FilamentEngine) -> Unit,
) {
    val bridge = FilamentBridgeHolder.bridge
        ?: error("FilamentBridgeHolder.bridge must be set from Swift before using FilamentView")

    val engine = remember { IosFilamentEngine(bridge) }
    val metalLayerRef = remember { arrayOfNulls<CAMetalLayer>(1) }
    val isAttachedRef = remember { booleanArrayOf(false) }

    val syncSurface: (UIView, CAMetalLayer) -> Unit = { view, metalLayer ->
        val scale = UIScreen.mainScreen.scale
        metalLayer.frame = view.bounds
        metalLayer.contentsScale = scale

        view.bounds.useContents {
            val widthPx = (size.width * scale).toInt().coerceAtLeast(1)
            val heightPx = (size.height * scale).toInt().coerceAtLeast(1)
            metalLayer.drawableSize = CGSizeMake(widthPx.toDouble(), heightPx.toDouble())

            if (!isAttachedRef[0]) {
                engine.attachLayer(metalLayer, widthPx, heightPx)
                engine.updateCamera(camera)
                lights.forEach { engine.addLight(it) }
                onEngineReady(engine)
                isAttachedRef[0] = true
            } else {
                engine.updateViewport(widthPx, heightPx)
                engine.requestFrame()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            engine.destroy()
        }
    }

    UIKitView(
        modifier = modifier,
        factory = {
            val view = UIView(frame = UIScreen.mainScreen.bounds)
            val metalLayer = CAMetalLayer()
            @Suppress("USELESS_CAST")
            metalLayer.device = MTLCreateSystemDefaultDevice() as objcnames.protocols.MTLDeviceProtocol?
            metalLayer.setOpaque(true)
            metalLayer.setFramebufferOnly(true)
            metalLayer.pixelFormat = MTLPixelFormatBGRA8Unorm
            metalLayer.contentsScale = UIScreen.mainScreen.scale

            view.layer.addSublayer(metalLayer)
            metalLayerRef[0] = metalLayer
            view
        },
        update = { view ->
            val metalLayer = metalLayerRef[0] ?: return@UIKitView
            engine.setClearColor(backgroundColor)
            syncSurface(view, metalLayer)
        },
        onResize = { view, _ ->
            val metalLayer = metalLayerRef[0] ?: return@UIKitView
            engine.setClearColor(backgroundColor)
            syncSurface(view, metalLayer)
        },
    )
}
