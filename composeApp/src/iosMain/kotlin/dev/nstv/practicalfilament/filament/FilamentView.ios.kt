package dev.nstv.practicalfilament.filament

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.platform.LocalDensity
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Metal.MTLCreateSystemDefaultDevice
import platform.Metal.MTLPixelFormatBGRA8Unorm
import platform.QuartzCore.CAMetalLayer
import platform.UIKit.UIColor
import platform.UIKit.UIScreen
import platform.UIKit.UIView

/**
 * Singleton holder for the FilamentBridge instance.
 * Must be set from Swift before any FilamentView is composed.
 *
 * In your Swift code (e.g., ContentView.swift or iOSApp.swift):
 *   FilamentBridgeHolder.shared.bridge = FilamentBridge()
 */
object FilamentBridgeHolder {
    var bridge: FilamentBridgeProtocol? = null
    var bridgeFactory: FilamentBridgeFactory? = null

    fun obtainBridge(): FilamentBridgeProtocol {
        return bridgeFactory?.createBridge()
            ?: bridge
            ?: error("FilamentBridgeHolder.bridgeFactory or bridge must be set from Swift before using FilamentView")
    }
}

interface FilamentBridgeFactory {
    fun createBridge(): FilamentBridgeProtocol
}

@OptIn(ExperimentalForeignApi::class)
private class FilamentHostView : UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    var onLayoutSubviews: ((FilamentHostView) -> Unit)? = null

    override fun layoutSubviews() {
        super.layoutSubviews()
        onLayoutSubviews?.invoke(this)
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun FilamentView(
    modifier: Modifier,
    camera: CameraConfig,
    lights: List<LightConfig>,
    backgroundColor: FilamentColor,
    clipShape: FilamentClipShape?,
    isOpaque: Boolean,
    hostViewMode: FilamentHostViewMode,
    onEngineReady: (FilamentEngine) -> Unit,
) {
    val density = LocalDensity.current
    val engine = remember { IosFilamentEngine(FilamentBridgeHolder.obtainBridge()) }
    val metalLayerRef = remember { arrayOfNulls<CAMetalLayer>(1) }
    val isAttachedRef = remember { booleanArrayOf(false) }
    val effectiveOpaque = isOpaque && backgroundColor.a >= 0.999f

    val syncSurface: (UIView, CAMetalLayer) -> Unit = { view, metalLayer ->
        val scale = UIScreen.mainScreen.scale
        metalLayer.setOpaque(effectiveOpaque)
        view.setOpaque(effectiveOpaque)
        metalLayer.frame = view.bounds
        metalLayer.contentsScale = scale
        applyClipShape(
            view = view,
            metalLayer = metalLayer,
            clipShape = clipShape,
            density = density,
            screenScale = scale,
        )

        view.bounds.useContents {
            val widthPx = (size.width * scale).toInt().coerceAtLeast(1)
            val heightPx = (size.height * scale).toInt().coerceAtLeast(1)
            metalLayer.drawableSize = CGSizeMake(widthPx.toDouble(), heightPx.toDouble())

            if (!isAttachedRef[0]) {
                engine.attachLayer(metalLayer, widthPx, heightPx, effectiveOpaque)
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
            val view = FilamentHostView()
            view.backgroundColor = UIColor.clearColor
            view.userInteractionEnabled = false
            val metalLayer = CAMetalLayer()
            @Suppress("USELESS_CAST")
            metalLayer.device = MTLCreateSystemDefaultDevice() as objcnames.protocols.MTLDeviceProtocol?
            metalLayer.setOpaque(effectiveOpaque)
            metalLayer.setFramebufferOnly(true)
            metalLayer.pixelFormat = MTLPixelFormatBGRA8Unorm
            metalLayer.contentsScale = UIScreen.mainScreen.scale

            view.layer.addSublayer(metalLayer)
            metalLayerRef[0] = metalLayer
            view.onLayoutSubviews = onLayoutSubviews@{ hostView ->
                val currentMetalLayer = metalLayerRef[0] ?: return@onLayoutSubviews
                engine.setClearColor(backgroundColor)
                syncSurface(hostView, currentMetalLayer)
            }
            view
        },
        update = { view ->
            val metalLayer = metalLayerRef[0] ?: return@UIKitView
            view.onLayoutSubviews = onLayoutSubviews@{ hostView ->
                val currentMetalLayer = metalLayerRef[0] ?: return@onLayoutSubviews
                engine.setClearColor(backgroundColor)
                syncSurface(hostView, currentMetalLayer)
            }
            engine.setClearColor(backgroundColor)
            syncSurface(view, metalLayer)
        },
        interactive = false,
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun applyClipShape(
    view: UIView,
    metalLayer: CAMetalLayer,
    clipShape: FilamentClipShape?,
    density: androidx.compose.ui.unit.Density,
    screenScale: Double,
) {
    val cornerRadiusPoints = when (clipShape) {
        null -> 0.0
        FilamentClipShape.Circle -> view.bounds.useContents { minOf(size.width, size.height) / 2.0 }
        is FilamentClipShape.RoundedRect -> with(density) {
            clipShape.cornerRadius.toPx().toDouble() / screenScale
        }
    }

    val shouldClip = clipShape != null
    view.clipsToBounds = shouldClip
    view.layer.setMasksToBounds(shouldClip)
    view.layer.cornerRadius = cornerRadiusPoints
    metalLayer.cornerRadius = cornerRadiusPoints
    metalLayer.setMasksToBounds(shouldClip)
}
