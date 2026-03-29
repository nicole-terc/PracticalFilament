package dev.nstv.practicalfilament.filament

import android.content.Context
import android.view.Choreographer
import android.view.Surface
import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.Filament
import com.google.android.filament.IndexBuffer
import com.google.android.filament.LightManager
import com.google.android.filament.Material
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.SwapChain
import com.google.android.filament.VertexBuffer
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.android.UiHelper
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class AndroidFilamentEngine(
    private val context: Context
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

    private val lights = mutableMapOf<Int, Int>()
    private val renderables = mutableMapOf<Int, Int>()
    private val materials = mutableMapOf<Int, Material>()
    private val materialInstances = mutableMapOf<Int, MaterialInstance>()
    private val vertexBuffers = mutableListOf<VertexBuffer>()
    private val indexBuffers = mutableListOf<IndexBuffer>()

    private var nextHandle = 1
    private var _isInitialized = false
    private var rendering = false

    override val isInitialized: Boolean get() = _isInitialized

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!rendering) return
            choreographer?.postFrameCallback(this)

            if (uiHelper?.isReadyToRender == true) {
                val sc = swapChain ?: return
                val r = renderer ?: return
                val v = filamentView ?: return

                if (r.beginFrame(sc, frameTimeNanos)) {
                    r.render(v)
                    r.endFrame()
                }
            }
        }
    }

    override fun initialize() {
        if (_isInitialized) return
        Filament.init()
        engine = Engine.create().also { eng ->
            renderer = eng.createRenderer()
            scene = eng.createScene()
            filamentView = eng.createView().apply {
                this.scene = this@AndroidFilamentEngine.scene
            }
            cameraEntity = EntityManager.get().create()
            camera = eng.createCamera(cameraEntity).also {
                filamentView?.camera = it
            }
        }
        choreographer = Choreographer.getInstance()
        _isInitialized = true
    }

    override fun destroy() {
        stopRenderLoop()
        val eng = engine ?: return

        renderables.values.forEach { entity ->
            eng.destroyEntity(entity)
            EntityManager.get().destroy(entity)
        }
        renderables.clear()

        lights.values.forEach { entity ->
            eng.destroyEntity(entity)
            EntityManager.get().destroy(entity)
        }
        lights.clear()

        materialInstances.values.forEach { eng.destroyMaterialInstance(it) }
        materialInstances.clear()

        materials.values.forEach { eng.destroyMaterial(it) }
        materials.clear()

        vertexBuffers.forEach { eng.destroyVertexBuffer(it) }
        vertexBuffers.clear()
        indexBuffers.forEach { eng.destroyIndexBuffer(it) }
        indexBuffers.clear()

        filamentView?.let { eng.destroyView(it) }
        scene?.let { eng.destroyScene(it) }
        camera?.let { eng.destroyCameraComponent(cameraEntity) }
        renderer?.let { eng.destroyRenderer(it) }
        swapChain?.let { eng.destroySwapChain(it) }

        eng.destroy()
        engine = null
        _isInitialized = false
    }

    fun attachSurface(surface: Surface) {
        swapChain?.let { engine?.destroySwapChain(it) }
        swapChain = engine?.createSwapChain(surface)
    }

    fun detachSurface() {
        swapChain?.let { engine?.destroySwapChain(it) }
        swapChain = null
    }

    fun updateViewport(width: Int, height: Int) {
        filamentView?.viewport = Viewport(0, 0, width, height)
        val aspect = width.toDouble() / height.toDouble()
        camera?.setProjection(
            45.0,
            aspect,
            0.1,
            100.0,
            Camera.Fov.VERTICAL
        )
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
        val s = scene ?: return
        val eng = engine ?: return
        renderables.values.forEach { entity ->
            s.removeEntity(entity)
            eng.destroyEntity(entity)
            EntityManager.get().destroy(entity)
        }
        renderables.clear()
    }

    override fun updateCamera(config: CameraConfig) {
        camera?.lookAt(
            config.position.x.toDouble(), config.position.y.toDouble(), config.position.z.toDouble(),
            config.lookAt.x.toDouble(), config.lookAt.y.toDouble(), config.lookAt.z.toDouble(),
            config.up.x.toDouble(), config.up.y.toDouble(), config.up.z.toDouble()
        )
        camera?.setProjection(
            config.fovDegrees,
            1.0,
            config.near,
            config.far,
            Camera.Fov.VERTICAL
        )
    }

    override fun addLight(config: LightConfig): Int {
        val eng = engine ?: return -1
        val entity = EntityManager.get().create()

        val type = when (config.type) {
            LightType.DIRECTIONAL -> LightManager.Type.DIRECTIONAL
            LightType.POINT -> LightManager.Type.POINT
            LightType.SPOT -> LightManager.Type.SPOT
        }

        LightManager.Builder(type)
            .color(config.color.r, config.color.g, config.color.b)
            .intensity(config.intensity)
            .position(config.position.x, config.position.y, config.position.z)
            .direction(config.direction.x, config.direction.y, config.direction.z)
            .apply {
                if (config.type == LightType.SPOT) {
                    spotLightCone(config.innerConeAngle, config.outerConeAngle)
                }
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

    override fun loadMaterial(path: String): Int {
        val assetPath = path.removePrefix("file:///android_asset/")

        val eng = engine ?: return -1
        val bytes = context.assets.open(assetPath).readBytes()
        val buffer = ByteBuffer.allocateDirect(bytes.size).apply {
            order(ByteOrder.nativeOrder())
            put(bytes)
            flip()
        }
        val material = Material.Builder()
            .payload(buffer, buffer.remaining())
            .build(eng)

        val handle = nextHandle++
        materials[handle] = material
        return handle
    }

    override fun createMaterialInstance(materialHandle: Int): Int {
        val material = materials[materialHandle] ?: return -1
        val instance = material.createInstance()
        val handle = nextHandle++
        materialInstances[handle] = instance
        return handle
    }

    override fun setMaterialParameter(instanceHandle: Int, param: MaterialParameter) {
        val instance = materialInstances[instanceHandle] ?: return
        when (val value = param.value) {
            is Float -> instance.setParameter(param.name, value)
            is Int -> instance.setParameter(param.name, value)
            is Boolean -> instance.setParameter(param.name, value)
            is Float3 -> instance.setParameter(param.name, value.x, value.y, value.z)
            is Float4 -> instance.setParameter(param.name, value.x, value.y, value.z, value.w)
            is Color -> instance.setParameter(param.name, value.r, value.g, value.b)
        }
    }

    override fun createPlaneRenderable(materialInstanceHandle: Int, width: Float, height: Float): Int {
        val eng = engine ?: return -1
        val instance = materialInstances[materialInstanceHandle] ?: return -1

        val hw = width / 2f
        val hh = height / 2f

        // Positions (x, y, z) + Normals (nx, ny, nz) + UVs (u, v) = 8 floats per vertex
        val vertices = floatArrayOf(
            -hw, -hh, 0f, 0f, 0f, 1f, 0f, 0f,
             hw, -hh, 0f, 0f, 0f, 1f, 1f, 0f,
             hw,  hh, 0f, 0f, 0f, 1f, 1f, 1f,
            -hw,  hh, 0f, 0f, 0f, 1f, 0f, 1f,
        )

        val indices = shortArrayOf(0, 1, 2, 0, 2, 3)

        val vertexBuffer = buildVertexBuffer(eng, vertices, 4)
        val indexBuffer = buildIndexBuffer(eng, indices)

        val entity = EntityManager.get().create()
        RenderableManager.Builder(1)
            .geometry(0, RenderableManager.PrimitiveType.TRIANGLES, vertexBuffer, indexBuffer)
            .material(0, instance)
            .boundingBox(
                com.google.android.filament.Box(
                    0f, 0f, 0f,
                    hw, hh, 0.01f
                )
            )
            .build(eng, entity)

        scene?.addEntity(entity)
        val handle = nextHandle++
        renderables[handle] = entity
        return handle
    }

    override fun createSphereRenderable(materialInstanceHandle: Int, radius: Float): Int {
        val eng = engine ?: return -1
        val instance = materialInstances[materialInstanceHandle] ?: return -1

        val stacks = 24
        val slices = 24
        val vertexCount = (stacks + 1) * (slices + 1)
        val vertexData = FloatArray(vertexCount * 8)
        var vi = 0

        for (i in 0..stacks) {
            val phi = PI * i.toDouble() / stacks
            val sinPhi = sin(phi).toFloat()
            val cosPhi = cos(phi).toFloat()

            for (j in 0..slices) {
                val theta = 2.0 * PI * j.toDouble() / slices
                val sinTheta = sin(theta).toFloat()
                val cosTheta = cos(theta).toFloat()

                val x = cosTheta * sinPhi
                val y = cosPhi
                val z = sinTheta * sinPhi

                vertexData[vi++] = x * radius  // position
                vertexData[vi++] = y * radius
                vertexData[vi++] = z * radius
                vertexData[vi++] = x            // normal
                vertexData[vi++] = y
                vertexData[vi++] = z
                vertexData[vi++] = j.toFloat() / slices   // uv
                vertexData[vi++] = i.toFloat() / stacks
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
                indexData[ii++] = second.toShort()
                indexData[ii++] = (first + 1).toShort()
                indexData[ii++] = second.toShort()
                indexData[ii++] = (second + 1).toShort()
                indexData[ii++] = (first + 1).toShort()
            }
        }

        val vertexBuffer = buildVertexBuffer(eng, vertexData, vertexCount)
        val indexBuffer = buildIndexBuffer(eng, indexData)

        val entity = EntityManager.get().create()
        RenderableManager.Builder(1)
            .geometry(0, RenderableManager.PrimitiveType.TRIANGLES, vertexBuffer, indexBuffer)
            .material(0, instance)
            .boundingBox(
                com.google.android.filament.Box(
                    0f, 0f, 0f,
                    radius, radius, radius
                )
            )
            .build(eng, entity)

        scene?.addEntity(entity)
        val handle = nextHandle++
        renderables[handle] = entity
        return handle
    }

    override fun removeRenderable(handle: Int) {
        val entity = renderables.remove(handle) ?: return
        val eng = engine ?: return
        scene?.removeEntity(entity)
        eng.destroyEntity(entity)
        EntityManager.get().destroy(entity)
    }

    override fun requestFrame() {
        // The choreographer-driven render loop handles continuous rendering.
        // This is a no-op since we render every frame.
    }

    private fun buildVertexBuffer(eng: Engine, data: FloatArray, vertexCount: Int): VertexBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(data.size * 4).apply {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().put(data)
        }

        val vb = VertexBuffer.Builder()
            .vertexCount(vertexCount)
            .bufferCount(1)
            .attribute(VertexBuffer.VertexAttribute.POSITION, 0, VertexBuffer.AttributeType.FLOAT3, 0, 32)
            .attribute(VertexBuffer.VertexAttribute.TANGENTS, 0, VertexBuffer.AttributeType.FLOAT3, 12, 32)
            .attribute(VertexBuffer.VertexAttribute.UV0, 0, VertexBuffer.AttributeType.FLOAT2, 24, 32)
            .build(eng)

        vb.setBufferAt(eng, 0, byteBuffer)
        vertexBuffers.add(vb)
        return vb
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
}
