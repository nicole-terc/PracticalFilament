package dev.nstv.practicalfilament.filament

import android.content.Context
import android.opengl.Matrix
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
import com.google.android.filament.SurfaceOrientation
import com.google.android.filament.Texture
import com.google.android.filament.TextureSampler
import com.google.android.filament.VertexBuffer
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.android.UiHelper
import dev.nstv.practicalfilament.filament.material.MaterialParameter
import dev.nstv.practicalfilament.filament.material.MaterialParameterDefinition
import dev.nstv.practicalfilament.filament.material.MaterialParameterPrecision
import dev.nstv.practicalfilament.filament.material.MaterialParameterType
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
    private val materialInstanceMaterials = mutableMapOf<Int, Int>()
    private val materialParameterDefinitions = mutableMapOf<Int, Map<String, MaterialParameterDefinition>>()
    private val textures = mutableMapOf<Int, Texture>()
    private val vertexBuffers = mutableListOf<VertexBuffer>()
    private val indexBuffers = mutableListOf<IndexBuffer>()

    private var nextHandle = 1
    private var _isInitialized = false
    private var rendering = false

    override val isInitialized: Boolean get() = _isInitialized

    fun setClearColor(color: Color) {
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
            setClearColor(Color(0f, 0f, 0f, 1f))
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
        materialInstanceMaterials.clear()

        materials.values.forEach { eng.destroyMaterial(it) }
        materials.clear()
        materialParameterDefinitions.clear()

        textures.values.forEach { eng.destroyTexture(it) }
        textures.clear()

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
            is Color -> instance.setParameter(param.name, value.r, value.g, value.b)
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

    override fun createTexture(width: Int, height: Int, pixels: ByteArray): Int {
        val eng = engine ?: return -1
        require(pixels.size == width * height * 4) {
            "Pixel data size ${pixels.size} does not match expected ${width * height * 4} for ${width}x${height} RGBA"
        }

        val texture = Texture.Builder()
            .width(width)
            .height(height)
            .sampler(Texture.Sampler.SAMPLER_2D)
            .format(Texture.InternalFormat.RGBA8)
            .levels(1)
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

        val handle = nextHandle++
        textures[handle] = texture
        return handle
    }

    override fun setTextureParameter(instanceHandle: Int, paramName: String, textureHandle: Int) {
        val instance = materialInstances[instanceHandle] ?: return
        val texture = textures[textureHandle] ?: return

        val sampler = TextureSampler(
            TextureSampler.MinFilter.LINEAR,
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

        val tangentQuaternions = buildSurfaceOrientationQuaternions(
            vertexCount = vertexCount,
            positions = positions,
            normals = normals,
            uvs = uvs,
            indices = indexData,
        )

        // Interleaved vertex format: position(3) + tangent quaternion(4) + uv(2)
        val vertexData = FloatArray(vertexCount * 9)
        var vi = 0
        var pi = 0
        var qi = 0
        var ui = 0
        repeat(vertexCount) {
            vertexData[vi++] = positions[pi++]
            vertexData[vi++] = positions[pi++]
            vertexData[vi++] = positions[pi++]
            vertexData[vi++] = tangentQuaternions[qi++]
            vertexData[vi++] = tangentQuaternions[qi++]
            vertexData[vi++] = tangentQuaternions[qi++]
            vertexData[vi++] = tangentQuaternions[qi++]
            vertexData[vi++] = uvs[ui++]
            vertexData[vi++] = uvs[ui++]
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
            .attribute(VertexBuffer.VertexAttribute.POSITION, 0, VertexBuffer.AttributeType.FLOAT3, 0, 36)
            .attribute(VertexBuffer.VertexAttribute.TANGENTS, 0, VertexBuffer.AttributeType.FLOAT4, 12, 36)
            .attribute(VertexBuffer.VertexAttribute.UV0, 0, VertexBuffer.AttributeType.FLOAT2, 28, 36)
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
