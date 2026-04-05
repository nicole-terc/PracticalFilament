package dev.nstv.practicalfilament.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import dev.nstv.practicalfilament.components.textures.BrownMudLeavesMaterial
import dev.nstv.practicalfilament.components.textures.createTexturedSphere
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.Color
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.LightConfig
import dev.nstv.practicalfilament.filament.LightType
import dev.nstv.practicalfilament.theme.Grid
import dev.nstv.practicalfilament.theme.components.DropDownWithArrows
import practicalfilament.composeapp.generated.resources.Res
import kotlin.math.sqrt

private const val MarbleTextureEnvironmentIntensity = 30_000f

private data class EnvironmentOption(
    val label: String,
    val iblPath: String? = null,
    val skyboxPath: String? = null,
)

private data class LoadedEnvironment(
    val indirectLightHandle: Int,
    val skyboxHandle: Int,
)

private val MarbleTextureMaterials = listOf(
    BrownMudLeavesMaterial,
)

private val MarbleTextureBackgrounds = listOf(
    EnvironmentOption(label = "None"),
    environmentOption("flower_road_2k"),
    environmentOption("flower_road_no_sun_2k"),
    environmentOption("graffiti_shelter_2k"),
    environmentOption("lightroom_14b"),
    environmentOption("noon_grass_2k"),
    environmentOption("parking_garage_2k"),
    environmentOption("pillars_2k"),
    environmentOption("studio_small_02_2k"),
    environmentOption("syferfontein_18d_clear_2k"),
    environmentOption("the_sky_is_on_fire_2k"),
    environmentOption("venetian_crossroads_2k"),
)

@Composable
fun MarbleTextureScreen(
    modifier: Modifier = Modifier,
) {
    var filamentEngine by remember { mutableStateOf<FilamentEngine?>(null) }
    var selectedMaterialIndex by remember { mutableIntStateOf(0) }
    var selectedBackgroundIndex by remember { mutableIntStateOf(0) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var renderableHandle by remember { mutableIntStateOf(0) }
    var gestureLightHandle by remember { mutableIntStateOf(0) }
    var gestureLightPosition by remember { mutableStateOf<Float3?>(null) }
    var gestureLightScreenPosition by remember { mutableStateOf<Offset?>(null) }
    var gestureLightVersion by remember { mutableLongStateOf(0L) }
    var textureHandles by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var environmentHandles by remember { mutableStateOf<Map<String, LoadedEnvironment>>(emptyMap()) }
    var colorSkyboxHandle by remember { mutableIntStateOf(0) }
    var activeIndirectLightHandle by remember { mutableIntStateOf(0) }
    var notice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(filamentEngine, gestureLightVersion) {
        val currentEngine = filamentEngine ?: return@LaunchedEffect
        if (gestureLightHandle != 0) {
            currentEngine.removeLight(gestureLightHandle)
            gestureLightHandle = 0
        }
        val position = gestureLightPosition ?: return@LaunchedEffect
        gestureLightHandle = currentEngine.addLight(
            LightConfig(
                type = LightType.POINT,
                color = Color(1f, 1f, 1f),
                intensity = 600_000f,
                position = position,
                falloffRadius = 10f,
            ),
        )
        currentEngine.requestFrame()
    }

    LaunchedEffect(filamentEngine, selectedBackgroundIndex) {
        val engine = filamentEngine ?: return@LaunchedEffect

        var currentColorSkyboxHandle = colorSkyboxHandle
        if (currentColorSkyboxHandle == 0) {
            currentColorSkyboxHandle = engine.createColorSkybox()
            if (currentColorSkyboxHandle > 0) {
                engine.setSkyboxColor(
                    currentColorSkyboxHandle,
                    r = 0.16f,
                    g = 0.18f,
                    b = 0.27f,
                    a = 1f,
                )
                colorSkyboxHandle = currentColorSkyboxHandle
            }
        }

        val background = MarbleTextureBackgrounds[selectedBackgroundIndex]
        if (background.iblPath == null || background.skyboxPath == null) {
            if (activeIndirectLightHandle > 0) {
                engine.setIndirectLight(activeIndirectLightHandle, 0f)
            }
            if (currentColorSkyboxHandle > 0) {
                engine.setSkybox(currentColorSkyboxHandle)
            }
            notice = null
            engine.requestFrame()
            return@LaunchedEffect
        }

        var updatedEnvironmentHandles = environmentHandles
        val loadedEnvironment = updatedEnvironmentHandles[background.label] ?: run {
            val indirectLightHandle = engine.loadIndirectLight(Res.getUri(background.iblPath))
            val skyboxHandle = engine.loadSkybox(Res.getUri(background.skyboxPath))
            if (indirectLightHandle <= 0 || skyboxHandle <= 0) {
                notice = "The ${background.label} background could not be loaded."
                return@LaunchedEffect
            }
            LoadedEnvironment(
                indirectLightHandle = indirectLightHandle,
                skyboxHandle = skyboxHandle,
            ).also { loaded ->
                updatedEnvironmentHandles = updatedEnvironmentHandles + (background.label to loaded)
            }
        }

        environmentHandles = updatedEnvironmentHandles
        if (activeIndirectLightHandle > 0 &&
            activeIndirectLightHandle != loadedEnvironment.indirectLightHandle
        ) {
            engine.setIndirectLight(activeIndirectLightHandle, 0f)
        }
        engine.setIndirectLight(
            loadedEnvironment.indirectLightHandle,
            intensity = MarbleTextureEnvironmentIntensity,
        )
        engine.setSkybox(loadedEnvironment.skyboxHandle)
        activeIndirectLightHandle = loadedEnvironment.indirectLightHandle
        notice = null
        engine.requestFrame()
    }

    SampleScreenLayout(
        modifier = modifier,
        title = "Marble Texture",
        view = {
            FilamentView(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { viewportSize = it }
                    .pointerInput(viewportSize) {
                        detectTapGestures(
                            onDoubleTap = { tapOffset ->
                                tapOffset.toGestureLightPosition(viewportSize)?.let { worldLightPosition ->
                                    gestureLightScreenPosition = tapOffset
                                    gestureLightPosition = worldLightPosition
                                    gestureLightVersion += 1L
                                }
                            },
                        )
                    },
                camera = CameraConfig(
                    position = Float3(0f, 0.05f, 4.25f),
                    lookAt = Float3(0f, 0f, 0f),
                ),
                lights = listOf(
                    LightConfig(
                        type = LightType.DIRECTIONAL,
                        color = Color(1f, 0.98f, 0.95f),
                        intensity = 75_000f,
                        direction = Float3(-0.24f, -0.38f, -1f),
                    ),
                ),
                backgroundColor = Color(0.16f, 0.18f, 0.27f, 1f),
                onEngineReady = { engine ->
                    filamentEngine = engine
                    val createdSphere = createTexturedSphere(
                        engine = engine,
                        material = MarbleTextureMaterials[selectedMaterialIndex],
                        existingTextureHandles = textureHandles,
                        radius = 1f,
                    )
                    textureHandles = createdSphere.textureHandles
                    notice = createdSphere.notice
                    renderableHandle = createdSphere.renderableHandle
                },
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                val marker = gestureLightScreenPosition ?: return@Canvas
                drawCircle(
                    color = ComposeColor.White.copy(alpha = 0.2f),
                    radius = 26f,
                    center = marker,
                )
                drawCircle(
                    color = ComposeColor.White.copy(alpha = 0.95f),
                    radius = 12f,
                    center = marker,
                )
                drawCircle(
                    color = ComposeColor.White.copy(alpha = 0.8f),
                    radius = 18f,
                    center = marker,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f),
                )
            }
        },
        controls = {
            notice?.let { SampleNotice(it) }
            DropDownWithArrows(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Grid.One, bottom = Grid.One),
                options = MarbleTextureMaterials.map { it.label },
                selectedIndex = selectedMaterialIndex,
                label = "Material",
                onSelectionChanged = { index ->
                    val engine = filamentEngine ?: run {
                        selectedMaterialIndex = index
                        return@DropDownWithArrows
                    }
                    selectedMaterialIndex = index

                    if (renderableHandle != 0) {
                        engine.removeRenderable(renderableHandle)
                        renderableHandle = 0
                    }

                    val createdSphere = createTexturedSphere(
                        engine = engine,
                        material = MarbleTextureMaterials[index],
                        existingTextureHandles = textureHandles,
                        radius = 1f,
                    )
                    textureHandles = createdSphere.textureHandles
                    notice = createdSphere.notice
                    if (createdSphere.renderableHandle <= 0) {
                        return@DropDownWithArrows
                    }
                    renderableHandle = createdSphere.renderableHandle
                    if (gestureLightHandle != 0) {
                        engine.removeLight(gestureLightHandle)
                        gestureLightHandle = 0
                    }
                    if (gestureLightPosition != null) {
                        gestureLightVersion += 1L
                    }
                    engine.requestFrame()
                },
            )
            DropDownWithArrows(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Grid.One),
                options = MarbleTextureBackgrounds.map { it.label },
                selectedIndex = selectedBackgroundIndex,
                label = "Background",
                onSelectionChanged = { selectedBackgroundIndex = it },
            )
            androidx.compose.material3.Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Grid.One),
                text = "Double-tap the sphere to place a gesture light.",
            )
        },
    )
}

private fun environmentOption(name: String): EnvironmentOption {
    return EnvironmentOption(
        label = name,
        iblPath = "files/envs/$name/${name}_ibl.ktx",
        skyboxPath = "files/envs/$name/${name}_skybox.ktx",
    )
}

private fun Offset.toGestureLightPosition(size: IntSize): Float3? {
    if (size.width <= 0 || size.height <= 0) return null
    val scale = minOf(size.width, size.height).toFloat()
    val normalizedX = ((2f * x) - size.width) / scale
    val normalizedY = (size.height - (2f * y)) / scale
    val radial = normalizedX * normalizedX + normalizedY * normalizedY
    val hemisphereZ = if (radial <= 1f) {
        sqrt(1f - radial)
    } else {
        0f
    }
    val length =
        sqrt(normalizedX * normalizedX + normalizedY * normalizedY + hemisphereZ * hemisphereZ)
            .coerceAtLeast(1e-6f)
    val radius = 3.6f
    return Float3(
        x = (normalizedX / length) * radius,
        y = (normalizedY / length) * radius,
        z = (hemisphereZ / length) * radius,
    )
}
