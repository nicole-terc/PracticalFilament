package dev.nstv.practicalfilament.screen.otherViewers

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import dev.nstv.practicalfilament.components.materials.AllMaterialsList
import dev.nstv.practicalfilament.components.materials.aiDefaultMatMaterial
import dev.nstv.practicalfilament.components.utils.OrbitQuaternion
import dev.nstv.practicalfilament.components.utils.orbitCameraConfig
import dev.nstv.practicalfilament.components.utils.orbitCameraControls
import dev.nstv.practicalfilament.components.utils.orbitDistance
import dev.nstv.practicalfilament.filament.BlockTextConfig
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.FilamentColor
import dev.nstv.practicalfilament.filament.createBlockText
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.Float4
import dev.nstv.practicalfilament.filament.TextureColorFormat
import dev.nstv.practicalfilament.filament.material.BuiltInTexture
import dev.nstv.practicalfilament.filament.material.LoadedMaterial
import dev.nstv.practicalfilament.filament.material.LoadedTextureParameterValue
import dev.nstv.practicalfilament.filament.material.MaterialParameter
import dev.nstv.practicalfilament.filament.material.Material.TextureMaterial
import dev.nstv.practicalfilament.filament.material.generateTexturePixels
import dev.nstv.practicalfilament.screen.lights.components.LightPresets
import dev.nstv.practicalfilament.screen.lights.components.LightSelectionField
import dev.nstv.practicalfilament.screen.marbles.components.EnvironmentSelectionField
import dev.nstv.practicalfilament.theme.Grid
import dev.nstv.practicalfilament.theme.components.DropDownWithArrows
import dev.nstv.practicalfilament.theme.components.SampleNotice
import dev.nstv.practicalfilament.theme.components.SampleScreenLayout
import practicalfilament.composeapp.generated.resources.Res

private const val UiCardMaterialPath = "files/materials/filamentUiCardTexture.filamat"
private const val UiCardImagePrimaryPath = "files/textures/milkyway.png"
private const val UiCardImageSecondaryPath = "files/textures/moon_disk.png"
private const val UiCardPanelDepth = 0.18f
private const val UiCardImagePlateDepth = 0.12f

private val UiCardBaseCamera = CameraConfig(
    position = Float3(0.55f, 0.35f, 5.4f),
    lookAt = Float3(0f, 0.05f, 0f),
    fovDegrees = 26.0,
)
private val UiCardSurfaceMaterials = AllMaterialsList

private enum class UiCardState {
    Primary,
    Secondary,
}

private const val UiCardTextDefault = "Stars stay bright."

@Composable
fun FilamentUiCardScreen(
    modifier: Modifier = Modifier,
) {
    var engine by remember { mutableStateOf<FilamentEngine?>(null) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var notice by remember { mutableStateOf<String?>(null) }
    var orientation by remember { mutableStateOf(OrbitQuaternion.Identity) }
    var cameraDistance by remember { mutableStateOf(UiCardBaseCamera.orbitDistance()) }
    var sceneVersion by remember { mutableLongStateOf(0L) }
    var cardState by remember { mutableStateOf(UiCardState.Primary) }
    var cardText by remember { mutableStateOf(UiCardTextDefault) }
    var selectedSurfaceMaterialIndex by remember { mutableIntStateOf(0) }
    var selectedTextMaterialIndex by remember { mutableIntStateOf(5) }

    var panelHandle by remember { mutableIntStateOf(0) }
    var imagePlateHandle by remember { mutableIntStateOf(0) }
    var imageOverlayHandle by remember { mutableIntStateOf(0) }
    var textHandles by remember { mutableStateOf<List<Int>>(emptyList()) }
    var buttonBodyHandle by remember { mutableIntStateOf(0) }
    var buttonLetterHandles by remember { mutableStateOf<List<Int>>(emptyList()) }

    fun rebuildScene(currentEngine: FilamentEngine) {
        (
            listOf(
                panelHandle,
                imagePlateHandle,
                imageOverlayHandle,
                buttonBodyHandle,
            ) + textHandles + buttonLetterHandles
        ).filter { it > 0 }.forEach(currentEngine::removeRenderable)

        panelHandle = 0
        imagePlateHandle = 0
        imageOverlayHandle = 0
        textHandles = emptyList()
        buttonBodyHandle = 0
        buttonLetterHandles = emptyList()
        notice = null
        sceneVersion += 1L

        val selectedSurfaceMaterial = UiCardSurfaceMaterials[selectedSurfaceMaterialIndex]
        val selectedTextMaterial = UiCardSurfaceMaterials[selectedTextMaterialIndex]
        val loadedSurfaceMaterial = currentEngine.loadMaterial(selectedSurfaceMaterial)
        val loadedTextMaterial = currentEngine.loadMaterial(selectedTextMaterial)
        notice = when {
            loadedSurfaceMaterial.instanceHandle <= 0 -> "The selected material could not be loaded."
            selectedSurfaceMaterial is TextureMaterial &&
                loadedSurfaceMaterial.textureHandles.size != selectedSurfaceMaterial.textureBindings.size ->
                "Some textures for the selected surface material could not be loaded."
            loadedTextMaterial.instanceHandle <= 0 -> "The selected text material could not be loaded."
            selectedTextMaterial is TextureMaterial &&
                loadedTextMaterial.textureHandles.size != selectedTextMaterial.textureBindings.size ->
                "Some textures for the selected text material could not be loaded."

            else -> null
        }
        if (loadedSurfaceMaterial.instanceHandle <= 0 || loadedTextMaterial.instanceHandle <= 0) {
            currentEngine.requestFrame()
            return
        }
        applyLoadedMaterialParameters(currentEngine, loadedSurfaceMaterial)
        applyLoadedMaterialParameters(currentEngine, loadedTextMaterial)

        val texturedMaterialHandle = currentEngine.loadMaterial(Res.getUri(UiCardMaterialPath))
        val imagePrimary = currentEngine.loadTexture(
            Res.getUri(UiCardImagePrimaryPath),
            TextureColorFormat.SRGB8_A8,
        )
        val imageSecondary = currentEngine.loadTexture(
            Res.getUri(UiCardImageSecondaryPath),
            TextureColorFormat.SRGB8_A8,
        )

        if (
            texturedMaterialHandle <= 0 ||
            imagePrimary <= 0 ||
            imageSecondary <= 0
        ) {
            notice = "The UI card textures or decal material failed to load on this platform."
            currentEngine.requestFrame()
            return
        }

        val imageInstance = currentEngine.createMaterialInstance(texturedMaterialHandle)
        if (imageInstance <= 0) {
            notice = "The UI card decal material instances are not available on this platform."
            currentEngine.requestFrame()
            return
        }
        val imageTexture = when (cardState) {
            UiCardState.Primary -> imagePrimary
            UiCardState.Secondary -> imageSecondary
        }
        currentEngine.setTextureParameter(imageInstance, "albedo", imageTexture)
        currentEngine.setMaterialParameter(imageInstance, MaterialParameter("tint", Float4(1f, 1f, 1f, 1f)))

        panelHandle = currentEngine.createCubeRenderable(
            materialInstanceHandle = loadedSurfaceMaterial.instanceHandle,
            size = 1f,
        )
        imagePlateHandle = currentEngine.createCubeRenderable(
            materialInstanceHandle = loadedSurfaceMaterial.instanceHandle,
            size = 1f,
        )
        imageOverlayHandle = currentEngine.createPlaneRenderable(
            materialInstanceHandle = imageInstance,
            width = 2.54f,
            height = 1.44f,
        )
        textHandles = currentEngine.createBlockText(
            BlockTextConfig(
                text = cardText,
                materialInstanceHandle = loadedTextMaterial.instanceHandle,
                center = Float3(0f, -0.28f, 0.34f),
                cellSize = 0.018f,
                cellDepth = 0.045f,
                letterSpacing = 0.012f,
                wordSpacing = 0.03f,
                cubeFill = 0.88f,
            ),
        ).handles
        buttonBodyHandle = currentEngine.createCubeRenderable(
            materialInstanceHandle = loadedSurfaceMaterial.instanceHandle,
            size = 1f,
        )
        buttonLetterHandles = currentEngine.createBlockText(
            BlockTextConfig(
                text = "Change",
                materialInstanceHandle = loadedTextMaterial.instanceHandle,
                center = Float3(0f, -1.395f, 0.62f),
                cellSize = 0.038f,
                cellDepth = 0.06f,
                letterSpacing = 0.035f,
                cubeFill = 0.9f,
            ),
        ).handles

        if (
            panelHandle <= 0 ||
            imagePlateHandle <= 0 ||
            imageOverlayHandle <= 0 ||
            textHandles.any { it <= 0 } ||
            buttonBodyHandle <= 0 ||
            buttonLetterHandles.any { it <= 0 }
        ) {
            notice = "One or more UI planes failed to create on this platform."
            currentEngine.requestFrame()
            return
        }

        currentEngine.setRenderableTransform(
            panelHandle,
            multiplyMatrix4(
                translationMatrix(0f, 0f, 0f),
                scaleMatrix(3.45f, 4.6f, UiCardPanelDepth),
            ),
        )
        currentEngine.setRenderableTransform(
            imagePlateHandle,
            multiplyMatrix4(
                translationMatrix(0f, 0.92f, 0.18f),
                scaleMatrix(2.82f, 1.74f, UiCardImagePlateDepth),
            ),
        )
        currentEngine.setRenderableTransform(imageOverlayHandle, translationMatrix(0f, 0.92f, 0.31f))
        currentEngine.setRenderableTransform(
            buttonBodyHandle,
            multiplyMatrix4(
                translationMatrix(0f, -1.42f, 0.4f),
                scaleMatrix(1.62f, 0.5f, 0.18f),
            ),
        )

        currentEngine.setShadowsEnabled(panelHandle, castShadows = true, receiveShadows = true)
        currentEngine.setShadowsEnabled(imagePlateHandle, castShadows = true, receiveShadows = true)
        currentEngine.setShadowsEnabled(imageOverlayHandle, castShadows = false, receiveShadows = false)
        textHandles.forEach { handle ->
            currentEngine.setShadowsEnabled(handle, castShadows = true, receiveShadows = false)
        }
        currentEngine.setShadowsEnabled(buttonBodyHandle, castShadows = true, receiveShadows = true)
        buttonLetterHandles.forEach { handle ->
            currentEngine.setShadowsEnabled(handle, castShadows = true, receiveShadows = false)
        }
        currentEngine.requestFrame()
    }

    LaunchedEffect(engine, orientation, cameraDistance) {
        val currentEngine = engine ?: return@LaunchedEffect
        currentEngine.updateCamera(
            orbitCameraConfig(
                baseCamera = UiCardBaseCamera,
                orientation = orientation,
                distance = cameraDistance,
            ),
        )
        currentEngine.requestFrame()
    }

    LaunchedEffect(
        engine,
        selectedSurfaceMaterialIndex,
        selectedTextMaterialIndex,
        cardState,
        cardText,
    ) {
        val currentEngine = engine ?: return@LaunchedEffect
        rebuildScene(currentEngine)
    }

    SampleScreenLayout(
        modifier = modifier,
        title = "Filament UI Card",
        view = {
            FilamentView(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { viewportSize = it }
                    .pointerInput(buttonBodyHandle, buttonLetterHandles, sceneVersion) {
                        detectTapGestures { offset ->
                            val currentEngine = engine ?: return@detectTapGestures
                            val version = sceneVersion
                            currentEngine.pickRenderable(offset.x.toInt(), offset.y.toInt()) { result ->
                                if (sceneVersion != version) return@pickRenderable
                                val handle = result?.renderableHandle ?: return@pickRenderable
                                if (handle == buttonBodyHandle || handle in buttonLetterHandles) {
                                    cardState = when (cardState) {
                                        UiCardState.Primary -> UiCardState.Secondary
                                        UiCardState.Secondary -> UiCardState.Primary
                                    }
                                }
                            }
                        }
                    }
                    .orbitCameraControls(
                        viewportSize = viewportSize,
                        orientation = orientation,
                        onOrientationChange = { orientation = it },
                        distance = cameraDistance,
                        onDistanceChange = { cameraDistance = it },
                        minDistance = 3.8f,
                        maxDistance = 14f,
                        enabled = panelHandle != 0,
                    ),
                camera = orbitCameraConfig(
                    baseCamera = UiCardBaseCamera,
                    orientation = orientation,
                    distance = cameraDistance,
                ),
                lights = emptyList(),
                backgroundColor = FilamentColor(0.08f, 0.1f, 0.14f, 1f),
                onEngineReady = { readyEngine ->
                    engine = readyEngine
                    orientation = OrbitQuaternion.Identity
                    cameraDistance = UiCardBaseCamera.orbitDistance()
                    cardState = UiCardState.Primary
                    notice = null
                    panelHandle = 0
                    imagePlateHandle = 0
                    imageOverlayHandle = 0
                    textHandles = emptyList()
                    buttonBodyHandle = 0
                    buttonLetterHandles = emptyList()
                },
            )
        },
        controls = {
            notice?.let { SampleNotice(it) }
            EnvironmentSelectionField(
                filamentEngine = engine,
                selectedBackground = 7,
                updateNotice = { notice = it },
            )
            LightSelectionField(
                filamentEngine = engine,
                presets = LightPresets,
                initialSelectedIndex = 6,
                enabled = buttonBodyHandle > 0,
            )
            DropDownWithArrows(
                modifier = Modifier.padding(bottom = Grid.One),
                options = UiCardSurfaceMaterials.map { it.label },
                selectedIndex = selectedSurfaceMaterialIndex,
                label = "Surface Material",
                onSelectionChanged = { selectedSurfaceMaterialIndex = it },
            )
            DropDownWithArrows(
                modifier = Modifier.padding(bottom = Grid.One),
                options = UiCardSurfaceMaterials.map { it.label },
                selectedIndex = selectedTextMaterialIndex,
                label = "Text Material",
                onSelectionChanged = { selectedTextMaterialIndex = it },
            )
            OutlinedTextField(
                modifier = Modifier.padding(bottom = Grid.One),
                value = cardText,
                onValueChange = { updatedText -> cardText = updatedText },
                label = { androidx.compose.material3.Text("Card Text") },
                singleLine = true,
            )
        },
    )
}

private fun applyLoadedMaterialParameters(
    engine: FilamentEngine,
    loadedMaterial: LoadedMaterial,
) {
    val builtInTextureHandles = mutableMapOf<BuiltInTexture, Int>()
    loadedMaterial.parameters.values.forEach { parameter ->
        when (val value = parameter.value) {
            is BuiltInTexture -> {
                val textureHandle = builtInTextureHandles.getOrPut(value) {
                    engine.createTexture(
                        width = 256,
                        height = 256,
                        pixels = generateTexturePixels(value),
                    )
                }
                if (textureHandle > 0) {
                    engine.setTextureParameter(loadedMaterial.instanceHandle, parameter.name, textureHandle)
                }
            }

            is LoadedTextureParameterValue -> {
                engine.setTextureParameter(loadedMaterial.instanceHandle, parameter.name, value.textureHandle)
            }

            else -> engine.setMaterialParameter(loadedMaterial.instanceHandle, parameter)
        }
    }
}

private fun translationMatrix(x: Float, y: Float, z: Float): FloatArray = floatArrayOf(
    1f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f,
    0f, 0f, 1f, 0f,
    x, y, z, 1f,
)

private fun scaleMatrix(x: Float, y: Float, z: Float): FloatArray = floatArrayOf(
    x, 0f, 0f, 0f,
    0f, y, 0f, 0f,
    0f, 0f, z, 0f,
    0f, 0f, 0f, 1f,
)

private fun multiplyMatrix4(left: FloatArray, right: FloatArray): FloatArray {
    val result = FloatArray(16)
    for (column in 0 until 4) {
        for (row in 0 until 4) {
            result[column * 4 + row] =
                left[row] * right[column * 4] +
                    left[4 + row] * right[column * 4 + 1] +
                    left[8 + row] * right[column * 4 + 2] +
                    left[12 + row] * right[column * 4 + 3]
        }
    }
    return result
}
