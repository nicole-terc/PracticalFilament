package dev.nstv.practicalfilament.screen.otherViewers.sheep2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import dev.nstv.practicalfilament.components.ParameterInputField
import dev.nstv.practicalfilament.components.materials.sheepBodyMaterial
import dev.nstv.practicalfilament.components.materials.sheepFluffMaterial
import dev.nstv.practicalfilament.components.utils.orbitCameraConfig
import dev.nstv.practicalfilament.components.utils.orbitCameraControls
import dev.nstv.practicalfilament.components.utils.rememberOrbitCameraState
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.FilamentColor
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.LightConfig
import dev.nstv.practicalfilament.filament.LightType
import dev.nstv.practicalfilament.filament.material.MaterialParameter
import dev.nstv.practicalfilament.filament.material.MaterialParameterDefinition
import dev.nstv.practicalfilament.filament.withFrameSeconds
import dev.nstv.practicalfilament.screen.marbles.components.EnvironmentSelectionField
import dev.nstv.practicalfilament.theme.Grid
import dev.nstv.practicalfilament.theme.components.SampleNotice
import dev.nstv.practicalfilament.theme.components.SampleScreenLayout
import practicalfilament.composeapp.generated.resources.Res
import kotlin.math.roundToInt

private const val Sheep2DefaultCameraDistance = 4f
private const val Sheep2MinCameraDistance = 2f
private const val Sheep2MaxCameraDistance = 8f

private val Sheep2BaseCamera = CameraConfig(
    position = Float3(0f, 0f, Sheep2DefaultCameraDistance),
    lookAt = Float3(0f, 0f, 0f),
    fovDegrees = 45.0,
)

private val Sheep2BaseLights = listOf(
    LightConfig(
        type = LightType.SUN,
        color = FilamentColor(0.98f, 0.94f, 0.90f),
        intensity = 100_000f,
        direction = Float3(0.45f, -1f, -0.6f),
        sunAngularRadius = 1.9f,
        sunHaloSize = 10f,
        sunHaloFalloff = 80f,
    ),
    LightConfig(
        type = LightType.DIRECTIONAL,
        color = FilamentColor(0.78f, 0.84f, 1f),
        intensity = 28_000f,
        direction = Float3(-1f, -0.35f, 0.8f),
    ),
)

@Composable
fun SheepScreen2(
    modifier: Modifier = Modifier,
) {
    var engine by remember { mutableStateOf<FilamentEngine?>(null) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val cameraState = rememberOrbitCameraState(initialDistance = Sheep2DefaultCameraDistance)
    var rigPieces by remember { mutableStateOf<List<SheepRigPiece>>(emptyList()) }
    var supportNotice by remember { mutableStateOf<String?>(null) }
    var fluffMaterialInstanceHandle by remember { mutableIntStateOf(0) }
    var materialParameterDefinitions by remember {
        mutableStateOf<List<MaterialParameterDefinition>>(emptyList())
    }
    var materialParameters by remember { mutableStateOf<Map<String, MaterialParameter>>(emptyMap()) }

    var animationEnabled by remember { mutableStateOf(true) }
    var masterSpeed by remember { mutableFloatStateOf(1f) }
    var pulseAmount by remember { mutableFloatStateOf(0.72f) }
    var noiseAmount by remember { mutableFloatStateOf(0.48f) }
    var noiseFrequency by remember { mutableFloatStateOf(1f) }
    var driftAmount by remember { mutableFloatStateOf(0.58f) }
    var followThroughAmount by remember { mutableFloatStateOf(0.7f) }
    var noiseSeed by remember { mutableIntStateOf(7) }

    SideEffect {
        val currentEngine = engine ?: return@SideEffect
        if (fluffMaterialInstanceHandle == 0) return@SideEffect
        materialParameters.values.forEach { parameter ->
            currentEngine.setMaterialParameter(fluffMaterialInstanceHandle, parameter)
        }
        currentEngine.requestFrame()
    }

    SideEffect {
        val currentEngine = engine ?: return@SideEffect
        currentEngine.updateCamera(
            orbitCameraConfig(
                baseCamera = Sheep2BaseCamera,
                orientation = cameraState.orientation,
                distance = cameraState.distance,
            ),
        )
        currentEngine.requestFrame()
    }

    LaunchedEffect(
        engine,
        rigPieces,
        animationEnabled,
        masterSpeed,
        pulseAmount,
        noiseAmount,
        noiseFrequency,
        driftAmount,
        followThroughAmount,
        noiseSeed,
    ) {
        val currentEngine = engine ?: return@LaunchedEffect
        if (rigPieces.isEmpty()) return@LaunchedEffect

        val controls = Sheep2AnimationControls(
            animationEnabled = animationEnabled,
            pulseAmount = pulseAmount,
            noiseAmount = noiseAmount,
            noiseFrequency = noiseFrequency,
            driftAmount = driftAmount,
            followThroughAmount = followThroughAmount,
            noiseSeed = noiseSeed,
        )
        if (!animationEnabled) {
            rigPieces.forEach { piece ->
                currentEngine.setRenderableTransform(piece.handle, piece.baseTransform)
            }
            currentEngine.requestFrame()
            return@LaunchedEffect
        }

        withFrameSeconds { elapsedSeconds, _ ->
            val timeSeconds = elapsedSeconds * masterSpeed
            rigPieces.forEach { piece ->
                currentEngine.setRenderableTransform(
                    piece.handle,
                    buildSheep2PieceTransform(
                        piece = piece,
                        timeSeconds = timeSeconds,
                        controls = controls,
                    ),
                )
            }
            currentEngine.requestFrame()
        }
    }

    SampleScreenLayout(
        modifier = modifier,
        title = "Sheep 2.0",
        view = {
            FilamentView(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { viewportSize = it }
                    .orbitCameraControls(
                        viewportSize = viewportSize,
                        cameraState = cameraState,
                        baseCamera = Sheep2BaseCamera,
                        engine = engine,
                        minDistance = Sheep2MinCameraDistance,
                        maxDistance = Sheep2MaxCameraDistance,
                        enabled = rigPieces.isNotEmpty(),
                    ),
                camera = orbitCameraConfig(
                    baseCamera = Sheep2BaseCamera,
                    orientation = cameraState.orientation,
                    distance = cameraState.distance,
                ),
                lights = Sheep2BaseLights,
                onEngineReady = { readyEngine ->
                    engine = readyEngine
                    cameraState.orientation = dev.nstv.practicalfilament.components.utils.OrbitQuaternion.Identity
                    cameraState.distance = Sheep2DefaultCameraDistance
                    supportNotice = null

                    val fluffMaterial = readyEngine.loadMaterial(sheepFluffMaterial())
                    fluffMaterialInstanceHandle = fluffMaterial.instanceHandle
                    materialParameterDefinitions = fluffMaterial.definitions
                    materialParameters = fluffMaterial.parameters

                    val indirectLightHandle = readyEngine.loadIndirectLight(Res.getUri(Sheep2IndirectLightPath))
                    if (indirectLightHandle > 0) {
                        readyEngine.setIndirectLight(
                            handle = indirectLightHandle,
                            intensity = Sheep2EnvironmentIntensity,
                        )
                    }
                    val skyboxHandle = readyEngine.loadSkybox(Res.getUri(Sheep2SkyboxPath))
                    if (skyboxHandle > 0) {
                        readyEngine.setSkybox(skyboxHandle)
                    }

                    val bodyMaterialHandle = readyEngine.loadMaterial(Res.getUri(sheepBodyMaterial().materialPath))
                    if (bodyMaterialHandle <= 0) {
                        rigPieces = emptyList()
                        supportNotice = "The sheep body material could not be loaded."
                        return@FilamentView
                    }

                    val bodyInstance = createBodyMaterialInstance(
                        engine = readyEngine,
                        materialHandle = bodyMaterialHandle,
                        color = Float3(0.27f, 0.27f, 0.27f),
                        roughness = 0.6f,
                    )
                    val eyeInstance = createBodyMaterialInstance(
                        engine = readyEngine,
                        materialHandle = bodyMaterialHandle,
                        color = Float3(0.90f, 0.60f, 0.26f),
                        roughness = 0.72f,
                    )
                    val pupilInstance = createBodyMaterialInstance(
                        engine = readyEngine,
                        materialHandle = bodyMaterialHandle,
                        color = Float3(0.05f, 0.05f, 0.05f),
                        roughness = 0.45f,
                    )
                    val glassesInstance = createBodyMaterialInstance(
                        engine = readyEngine,
                        materialHandle = bodyMaterialHandle,
                        color = Float3(0.05f, 0.05f, 0.05f),
                        roughness = 0.3f,
                    )

                    val createdPieces = buildSheepRigPieces(
                        engine = readyEngine,
                        fluffInstanceHandle = fluffMaterial.instanceHandle,
                        bodyInstanceHandle = bodyInstance,
                        eyeInstanceHandle = eyeInstance,
                        pupilInstanceHandle = pupilInstance,
                        glassesInstanceHandle = glassesInstance,
                    )
                    rigPieces = createdPieces.filter { it.handle > 0 }
                    if (rigPieces.size != createdPieces.size) {
                        supportNotice = "Some sheep geometry could not be created on this platform."
                    }
                    rigPieces.forEach { piece ->
                        readyEngine.setRenderableTransform(piece.handle, piece.baseTransform)
                    }
                    readyEngine.requestFrame()
                },
            )
        },
        controls = {
            supportNotice?.let { SampleNotice(it) }
            EnvironmentSelectionField(
                filamentEngine = engine,
            )

            ToggleRow(
                label = "Animation",
                checked = animationEnabled,
                onCheckedChange = { animationEnabled = it },
            )
            SliderField(
                label = "Master Speed",
                value = masterSpeed,
                valueRange = 0.2f..2f,
                onValueChange = { masterSpeed = it },
            )
            SliderField(
                label = "Pulse Amount",
                value = pulseAmount,
                valueRange = 0f..1f,
                onValueChange = { pulseAmount = it },
            )
            SliderField(
                label = "Noise Amount",
                value = noiseAmount,
                valueRange = 0f..1f,
                onValueChange = { noiseAmount = it },
            )
            SliderField(
                label = "Noise Frequency",
                value = noiseFrequency,
                valueRange = 0.2f..2f,
                onValueChange = { noiseFrequency = it },
            )
            SliderField(
                label = "Drift Amount",
                value = driftAmount,
                valueRange = 0f..1f,
                onValueChange = { driftAmount = it },
            )
            SliderField(
                label = "Follow-through",
                value = followThroughAmount,
                valueRange = 0f..1f,
                onValueChange = { followThroughAmount = it },
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Grid.One),
                horizontalArrangement = Arrangement.spacedBy(Grid.One),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = { noiseSeed += 1 }) {
                    Text("Reset Noise Seed")
                }
                Text("Seed $noiseSeed")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = Grid.One))
            Text(
                text = "Fluff Material",
                style = MaterialTheme.typography.headlineSmall,
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Grid.One),
            ) {
                materialParameterDefinitions.forEach { definition ->
                    val parameter = materialParameters[definition.name] ?: return@forEach
                    ParameterInputField(
                        name = definition.name,
                        type = definition.type,
                        value = parameter.value,
                    ) { updatedValue ->
                        materialParameters = materialParameters + (
                            definition.name to MaterialParameter(definition.name, updatedValue)
                        )
                    }
                }
            }
        },
    )
}

private fun createBodyMaterialInstance(
    engine: FilamentEngine,
    materialHandle: Int,
    color: Float3,
    roughness: Float,
    metallic: Float = 0f,
): Int {
    val instanceHandle = engine.createMaterialInstance(materialHandle)
    engine.setMaterialParameter(
        instanceHandle,
        MaterialParameter("baseColor", color),
    )
    engine.setMaterialParameter(
        instanceHandle,
        MaterialParameter("roughness", roughness),
    )
    engine.setMaterialParameter(
        instanceHandle,
        MaterialParameter("metallic", metallic),
    )
    return instanceHandle
}

@Composable
private fun SliderField(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Grid.Half),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label)
            Text(formatSliderValue(value))
        }
        Slider(
            value = value,
            valueRange = valueRange,
            onValueChange = onValueChange,
        )
    }
}

private fun formatSliderValue(value: Float): String {
    val rounded = (value * 100f).roundToInt() / 100f
    val hundredths = (absFraction(rounded) * 100).roundToInt()
    return if (hundredths == 0) {
        rounded.roundToInt().toString()
    } else if (hundredths % 10 == 0) {
        (rounded * 10f).roundToInt().let { scaled ->
            "${scaled / 10}.${kotlin.math.abs(scaled % 10)}"
        }
    } else {
        (rounded * 100f).roundToInt().let { scaled ->
            "${scaled / 100}.${kotlin.math.abs((scaled / 10) % 10)}${kotlin.math.abs(scaled % 10)}"
        }
    }
}

private fun absFraction(value: Float): Float = kotlin.math.abs(value - value.toInt())

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
