// Modified from sample https://github.com/google/filament/tree/main/web/examples/sky
package dev.nstv.practicalfilament.screen.sky

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import dev.nstv.practicalfilament.theme.components.SampleNotice
import dev.nstv.practicalfilament.theme.components.SampleScreenLayout
import dev.nstv.practicalfilament.filament.AttributeDataType
import dev.nstv.practicalfilament.filament.BoundingBox
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.FilamentColor
import dev.nstv.practicalfilament.filament.CustomRenderableConfig
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.Float2
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.Float4
import dev.nstv.practicalfilament.filament.PrimitiveType
import dev.nstv.practicalfilament.filament.VertexAttribute
import dev.nstv.practicalfilament.filament.VertexAttributeLayout
import dev.nstv.practicalfilament.filament.material.MaterialParameter
import dev.nstv.practicalfilament.filament.material.MaterialParameterDefinition
import dev.nstv.practicalfilament.filament.toByteArray
import dev.nstv.practicalfilament.components.platformSpecific.SetSkyAsWallpaperButton
import dev.nstv.practicalfilament.theme.Grid
import dev.nstv.practicalfilament.theme.components.CheckBoxLabel
import dev.nstv.practicalfilament.theme.components.ExpandableSection
import practicalfilament.composeapp.generated.resources.Res
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val SkyMaterialPath = "files/materials/simulated_skybox.filamat"
private const val MoonDiskTexturePath = "files/textures/moon_disk.png"
private const val MoonNormalTexturePath = "files/textures/moon_normal.png"
private const val MilkyWayTexturePath = "files/textures/milkyway.png"

private const val DefaultSunAzimuth = 0f
private const val DefaultSunHeight = 0f
private const val DefaultSunIntensity = 100_000f
private const val DefaultFullMoonIlluminance = 0.25f
private val DefaultSunTint = Float3(1f, 0.94f, 0.82f)
private const val DefaultSunRadius = 1.2f
private const val DefaultSunLimbDarkening = 0.5f
private const val DefaultSunDiskIntensityBoost = 1f
private const val DefaultTurbidity = 2f
private const val DefaultRayleigh = 1f
private const val DefaultMieCoefficient = 1f
private const val DefaultOzone = 0.25f
private const val DefaultMieG = 0.8f
private const val DefaultCloudCoverage = 0.4f
private const val DefaultCloudDensity = 0.02f
private const val DefaultCloudHeightMeters = 8000f
private const val DefaultCloudSpeed = 50f
private const val DefaultCloudEvolutionSpeed = 0.02f
private const val DefaultAperture = 16f
private const val DefaultShutterSpeed = 125f
private const val DefaultIso = 100f
private const val DefaultFocalLength = 24f
private const val DefaultMoonAzimuth = 180f
private const val DefaultMoonHeight = 0.70710677f
private const val DefaultMoonIntensity = DefaultFullMoonIlluminance
private const val DefaultMoonRadius = 0.75f
private val DefaultMoonTint = Float3(0.58f, 0.68f, 0.92f)
private const val DefaultMilkyWayIntensity = 1f
private const val DefaultMilkyWaySaturation = 1f
private const val DefaultMilkyWayBlackPoint = 0.07f
private const val DefaultMilkyWayLatitude = 34f
private const val DefaultMilkyWaySiderealTime = 0f
private const val DefaultStarDensity = 0.001f
private const val DefaultStarIntensityExponent = 0f
private const val DefaultMsRayleigh = 0.1f
private const val DefaultMsMie = 0.5f
private const val DefaultHorizonGlow = 0f
private const val DefaultContrast = 1f
private const val DefaultShimmerStrength = 0f
private const val DefaultShimmerFrequency = 20f
private const val DefaultShimmerMaskHeight = 0.1f
private const val DefaultWaterStrength = 50f
private const val DefaultWaterSpeed = 1f
private const val DefaultWaterOctaves = 4f
private const val DefaultManualTimeHours = 12f
private const val DefaultCameraAzimuth = 0f
private const val DefaultCameraElevation = 0f
private const val MaxCameraElevation = 89f

private const val PlanetRadiusKm = 6360f

@Composable
fun SkyScreen(
    modifier: Modifier = Modifier,
) {
    var engine by remember { mutableStateOf<FilamentEngine?>(null) }
    var materialInstanceHandle by remember { mutableIntStateOf(0) }
    var viewportHeightPx by remember { mutableIntStateOf(1) }
    var supportedParameters by remember { mutableStateOf<Set<String>>(emptySet()) }
    var notice by remember { mutableStateOf<String?>(null) }

    var sunAzimuth by remember { mutableFloatStateOf(DefaultSunAzimuth) }
    var sunHeight by remember { mutableFloatStateOf(DefaultSunHeight) }
    var sunIntensity by remember { mutableFloatStateOf(DefaultSunIntensity) }
    var sunRadius by remember { mutableFloatStateOf(DefaultSunRadius) }
    var sunLimbDarkening by remember { mutableFloatStateOf(DefaultSunLimbDarkening) }
    var sunDiskIntensityBoost by remember { mutableFloatStateOf(DefaultSunDiskIntensityBoost) }
    var sunTintRed by remember { mutableFloatStateOf(DefaultSunTint.x) }
    var sunTintGreen by remember { mutableFloatStateOf(DefaultSunTint.y) }
    var sunTintBlue by remember { mutableFloatStateOf(DefaultSunTint.z) }
    var turbidity by remember { mutableFloatStateOf(DefaultTurbidity) }
    var rayleigh by remember { mutableFloatStateOf(DefaultRayleigh) }
    var mieCoefficient by remember { mutableFloatStateOf(DefaultMieCoefficient) }
    var ozone by remember { mutableFloatStateOf(DefaultOzone) }
    var mieG by remember { mutableFloatStateOf(DefaultMieG) }
    var cloudCoverage by remember { mutableFloatStateOf(DefaultCloudCoverage) }
    var cloudDensity by remember { mutableFloatStateOf(DefaultCloudDensity) }
    var cloudVolumetrics by remember { mutableStateOf(false) }
    var cloudHeightMeters by remember { mutableFloatStateOf(DefaultCloudHeightMeters) }
    var cloudSpeed by remember { mutableFloatStateOf(DefaultCloudSpeed) }
    var cloudEvolutionSpeed by remember { mutableFloatStateOf(DefaultCloudEvolutionSpeed) }
    var waterDerivativeTrick by remember { mutableStateOf(true) }
    var waterStrength by remember { mutableFloatStateOf(DefaultWaterStrength) }
    var waterSpeed by remember { mutableFloatStateOf(DefaultWaterSpeed) }
    var waterOctaves by remember { mutableFloatStateOf(DefaultWaterOctaves) }
    var aperture by remember { mutableFloatStateOf(DefaultAperture) }
    var shutterSpeed by remember { mutableFloatStateOf(DefaultShutterSpeed) }
    var iso by remember { mutableFloatStateOf(DefaultIso) }
    var focalLength by remember { mutableFloatStateOf(DefaultFocalLength) }
    var moonEnabled by remember { mutableStateOf(true) }
    var moonAzimuth by remember { mutableFloatStateOf(DefaultMoonAzimuth) }
    var moonHeight by remember { mutableFloatStateOf(DefaultMoonHeight) }
    var moonIntensity by remember { mutableFloatStateOf(DefaultMoonIntensity) }
    var moonRadius by remember { mutableFloatStateOf(DefaultMoonRadius) }
    var moonTintRed by remember { mutableFloatStateOf(DefaultMoonTint.x) }
    var moonTintGreen by remember { mutableFloatStateOf(DefaultMoonTint.y) }
    var moonTintBlue by remember { mutableFloatStateOf(DefaultMoonTint.z) }
    var milkyWayEnabled by remember { mutableStateOf(true) }
    var milkyWayIntensity by remember { mutableFloatStateOf(DefaultMilkyWayIntensity) }
    var milkyWaySaturation by remember { mutableFloatStateOf(DefaultMilkyWaySaturation) }
    var milkyWayBlackPoint by remember { mutableFloatStateOf(DefaultMilkyWayBlackPoint) }
    var milkyWaySiderealTime by remember { mutableFloatStateOf(DefaultMilkyWaySiderealTime) }
    var milkyWayLatitude by remember { mutableFloatStateOf(DefaultMilkyWayLatitude) }
    var starsEnabled by remember { mutableStateOf(true) }
    var starDensity by remember { mutableFloatStateOf(DefaultStarDensity) }
    var starIntensityExponent by remember { mutableFloatStateOf(DefaultStarIntensityExponent) }
    var msRayleigh by remember { mutableFloatStateOf(DefaultMsRayleigh) }
    var msMie by remember { mutableFloatStateOf(DefaultMsMie) }
    var horizonGlow by remember { mutableFloatStateOf(DefaultHorizonGlow) }
    var contrast by remember { mutableFloatStateOf(DefaultContrast) }
    var shimmerStrength by remember { mutableFloatStateOf(DefaultShimmerStrength) }
    var shimmerFrequency by remember { mutableFloatStateOf(DefaultShimmerFrequency) }
    var shimmerMaskHeight by remember { mutableFloatStateOf(DefaultShimmerMaskHeight) }
    var syncEnabled by remember { mutableStateOf(false) }
    var syncManualOverride by remember { mutableStateOf(false) }
    var manualTimeHours by remember { mutableFloatStateOf(DefaultManualTimeHours) }
    var syncDeviceLocation by remember { mutableStateOf(false) }
    var syncLatitude by remember { mutableFloatStateOf(DefaultMilkyWayLatitude) }
    var syncLongitude by remember { mutableFloatStateOf(0f) }
    var syncNotice by remember { mutableStateOf<String?>(null) }
    var cameraAzimuth by remember { mutableFloatStateOf(DefaultCameraAzimuth) }
    var cameraElevation by remember { mutableFloatStateOf(DefaultCameraElevation) }

    val editableConfig = SkyWallpaperConfig(
        sunAzimuth = sunAzimuth,
        sunHeight = sunHeight,
        sunIntensity = sunIntensity,
        sunRadius = sunRadius,
        sunLimbDarkening = sunLimbDarkening,
        sunDiskIntensityBoost = sunDiskIntensityBoost,
        sunTintRed = sunTintRed,
        sunTintGreen = sunTintGreen,
        sunTintBlue = sunTintBlue,
        turbidity = turbidity,
        rayleigh = rayleigh,
        mieCoefficient = mieCoefficient,
        ozone = ozone,
        mieG = mieG,
        cloudCoverage = cloudCoverage,
        cloudDensity = cloudDensity,
        cloudVolumetrics = cloudVolumetrics,
        cloudHeightMeters = cloudHeightMeters,
        cloudSpeed = cloudSpeed,
        cloudEvolutionSpeed = cloudEvolutionSpeed,
        waterDerivativeTrick = waterDerivativeTrick,
        waterStrength = waterStrength,
        waterSpeed = waterSpeed,
        waterOctaves = waterOctaves,
        aperture = aperture,
        shutterSpeed = shutterSpeed,
        iso = iso,
        focalLength = focalLength,
        moonEnabled = moonEnabled,
        moonAzimuth = moonAzimuth,
        moonHeight = moonHeight,
        moonIntensity = moonIntensity,
        moonRadius = moonRadius,
        moonTintRed = moonTintRed,
        moonTintGreen = moonTintGreen,
        moonTintBlue = moonTintBlue,
        milkyWayEnabled = milkyWayEnabled,
        milkyWayIntensity = milkyWayIntensity,
        milkyWaySaturation = milkyWaySaturation,
        milkyWayBlackPoint = milkyWayBlackPoint,
        milkyWaySiderealTime = milkyWaySiderealTime,
        milkyWayLatitude = milkyWayLatitude,
        starsEnabled = starsEnabled,
        starDensity = starDensity,
        starIntensityExponent = starIntensityExponent,
        msRayleigh = msRayleigh,
        msMie = msMie,
        horizonGlow = horizonGlow,
        contrast = contrast,
        shimmerStrength = shimmerStrength,
        shimmerFrequency = shimmerFrequency,
        shimmerMaskHeight = shimmerMaskHeight,
        syncEnabled = syncEnabled,
        syncManualOverride = syncManualOverride,
        manualTimeHours = manualTimeHours,
        syncDeviceLocation = syncDeviceLocation,
        syncLatitude = syncLatitude,
        syncLongitude = syncLongitude,
    )
    val realtimeModeEnabled = syncEnabled || syncManualOverride
    val cameraModified = cameraAzimuth != DefaultCameraAzimuth || cameraElevation != DefaultCameraElevation

    fun clearRealtimeSync() {
        syncEnabled = false
        syncNotice = null
    }

    fun applyManualEdit(change: () -> Unit) {
        clearRealtimeSync()
        change()
    }

    fun applyCoordinateEdit(change: () -> Unit) {
        syncNotice = null
        change()
    }

    fun applyMoonEdit(change: () -> Unit) {
        syncNotice = null
        change()
    }

    fun applyRealtimeValues(realtimeValues: SkyRealtimeValues) {
        sunAzimuth = realtimeValues.sunAzimuth
        sunHeight = realtimeValues.sunHeight
        moonAzimuth = realtimeValues.moonAzimuth
        moonHeight = realtimeValues.moonHeight
        milkyWaySiderealTime = realtimeValues.siderealTimeHours
        milkyWayLatitude = syncLatitude
    }

    LaunchedEffect(syncEnabled, syncManualOverride, manualTimeHours, syncLatitude, syncLongitude) {
        if (!realtimeModeEnabled) return@LaunchedEffect
        while (true) {
            val currentTimeMillis = platformCurrentTimeMillis()
            if (syncEnabled && !syncManualOverride) {
                manualTimeHours = platformCurrentLocalTimeHours(currentTimeMillis)
            }
            val realtimeValues = if (syncManualOverride) {
                computeManualSkyValues(
                    currentTimeMillis = currentTimeMillis,
                    localTimeHours = manualTimeHours,
                    latitudeDegrees = syncLatitude,
                    longitudeDegrees = syncLongitude,
                )
            } else {
                computeRealtimeSkyValues(
                    currentTimeMillis = currentTimeMillis,
                    latitudeDegrees = syncLatitude,
                    longitudeDegrees = syncLongitude,
                )
            }
            applyRealtimeValues(realtimeValues)
            delay(1000)
        }
    }

    LaunchedEffect(
        engine,
        materialInstanceHandle,
        viewportHeightPx,
        editableConfig,
        cameraAzimuth,
        cameraElevation,
    ) {
        val currentEngine = engine ?: return@LaunchedEffect
        if (materialInstanceHandle <= 0) return@LaunchedEffect
        currentEngine.updateCamera(
            CameraConfig(
                position = Float3(0f, 0f, 0f),
                lookAt = skyCameraLookDirection(cameraAzimuth, cameraElevation),
                fovDegrees = computeVerticalFovDegrees(focalLength).toDouble(),
                near = 0.1,
                far = 5000.0,
            ),
        )
        currentEngine.setCameraExposure(
            aperture,
            1f / shutterSpeed.coerceAtLeast(0.05f),
            iso,
        )
        applySkyParameters(
            engine = currentEngine,
            instanceHandle = materialInstanceHandle,
            viewportHeightPx = viewportHeightPx,
            sunAzimuth = sunAzimuth,
            sunHeight = sunHeight,
            sunIntensity = sunIntensity,
            sunRadius = sunRadius,
            sunLimbDarkening = sunLimbDarkening,
            sunDiskIntensityBoost = sunDiskIntensityBoost,
            sunTint = Float3(sunTintRed, sunTintGreen, sunTintBlue),
            turbidity = turbidity,
            rayleigh = rayleigh,
            mieCoefficient = mieCoefficient,
            ozone = ozone,
            mieG = mieG,
            cloudCoverage = cloudCoverage,
            cloudDensity = cloudDensity,
            cloudVolumetrics = cloudVolumetrics,
            cloudHeightMeters = cloudHeightMeters,
            cloudSpeed = cloudSpeed,
            cloudEvolutionSpeed = cloudEvolutionSpeed,
            waterDerivativeTrick = waterDerivativeTrick,
            waterStrength = waterStrength,
            waterSpeed = waterSpeed,
            waterOctaves = waterOctaves,
            aperture = aperture,
            shutterSpeed = shutterSpeed,
            iso = iso,
            focalLength = focalLength,
            supportedParameters = supportedParameters,
            moonEnabled = moonEnabled,
            moonAzimuth = moonAzimuth,
            moonHeight = moonHeight,
            moonIntensity = moonIntensity,
            moonRadius = moonRadius,
            moonTint = Float3(moonTintRed, moonTintGreen, moonTintBlue),
            milkyWayEnabled = milkyWayEnabled,
            milkyWayIntensity = milkyWayIntensity,
            milkyWaySaturation = milkyWaySaturation,
            milkyWayBlackPoint = milkyWayBlackPoint,
            milkyWaySiderealTime = milkyWaySiderealTime,
            milkyWayLatitude = milkyWayLatitude,
            starsEnabled = starsEnabled,
            starDensity = starDensity,
            starIntensityExponent = starIntensityExponent,
            msRayleigh = msRayleigh,
            msMie = msMie,
            horizonGlow = horizonGlow,
            contrast = contrast,
            shimmerStrength = shimmerStrength,
            shimmerFrequency = shimmerFrequency,
            shimmerMaskHeight = shimmerMaskHeight,
            onError = { notice = it },
        )
        currentEngine.requestFrame()
    }

    LaunchedEffect(engine, materialInstanceHandle) {
        val currentEngine = engine ?: return@LaunchedEffect
        if (materialInstanceHandle <= 0) return@LaunchedEffect
        while (true) {
            withFrameNanos { }
            currentEngine.requestFrame()
        }
    }

    SampleScreenLayout(
        modifier = modifier,
        title = "Sky",
        view = {
            FilamentView(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { viewportHeightPx = it.height.coerceAtLeast(1) }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            val width = size.width.toFloat().coerceAtLeast(1f)
                            val height = size.height.toFloat().coerceAtLeast(1f)
                            cameraAzimuth = normalizeAngleDegrees(
                                cameraAzimuth - dragAmount.x * 180f / width,
                            )
                            cameraElevation = (cameraElevation + dragAmount.y * 120f / height)
                                .coerceIn(-MaxCameraElevation, MaxCameraElevation)
                            change.consume()
                        }
                    },
                camera = CameraConfig(
                    position = Float3(0f, 0f, 0f),
                    lookAt = skyCameraLookDirection(cameraAzimuth, cameraElevation),
                    fovDegrees = 53.13,
                    near = 0.1,
                    far = 5000.0,
                ),
                lights = emptyList(),
                backgroundColor = FilamentColor(0f, 0f, 0f, 1f),
                onEngineReady = { readyEngine ->
                    engine = readyEngine
                    notice = null

                    val materialHandle = readyEngine.loadMaterial(Res.getUri(SkyMaterialPath))
                    if (materialHandle <= 0) {
                        notice = "The simulated sky material could not be loaded."
                        return@FilamentView
                    }

                    val instanceHandle = readyEngine.createMaterialInstance(materialHandle)
                    if (instanceHandle <= 0) {
                        notice = "The sky material instance could not be created."
                        return@FilamentView
                    }

                    val parameterNames = readyEngine.getMaterialParameters(materialHandle)
                        .mapTo(linkedSetOf(), MaterialParameterDefinition::name)
                    supportedParameters = parameterNames

                    bindTexture(
                        engine = readyEngine,
                        instanceHandle = instanceHandle,
                        supportedParameters = parameterNames,
                        parameterName = "moonTexture",
                        path = MoonDiskTexturePath,
                        onFailure = { notice = it },
                    )
                    bindTexture(
                        engine = readyEngine,
                        instanceHandle = instanceHandle,
                        supportedParameters = parameterNames,
                        parameterName = "moonNormal",
                        path = MoonNormalTexturePath,
                        onFailure = { notice = it },
                    )
                    bindTexture(
                        engine = readyEngine,
                        instanceHandle = instanceHandle,
                        supportedParameters = parameterNames,
                        parameterName = "milkyWayTexture",
                        path = MilkyWayTexturePath,
                        onFailure = { notice = it },
                    )

                    val renderableHandle = readyEngine.createCustomRenderable(
                        CustomRenderableConfig(
                            vertexData = floatArrayOf(
                                -1f, -1f,
                                3f, -1f,
                                -1f, 3f,
                            ).toByteArray(),
                            vertexCount = 3,
                            strideBytes = 8,
                            attributes = listOf(
                                VertexAttributeLayout(
                                    attribute = VertexAttribute.POSITION,
                                    type = AttributeDataType.FLOAT2,
                                    offsetBytes = 0,
                                ),
                            ),
                            indices = shortArrayOf(0, 1, 2),
                            materialInstanceHandle = instanceHandle,
                            boundingBox = BoundingBox(
                                center = Float3(0f, 0f, 0f),
                                halfExtent = Float3(10_000f, 10_000f, 10_000f),
                            ),
                            primitiveType = PrimitiveType.TRIANGLES,
                        ),
                    )
                    if (renderableHandle <= 0) {
                        notice = "The platform engine could not create the fullscreen sky triangle."
                        return@FilamentView
                    }

                    readyEngine.setCameraExposure(aperture, 1f / shutterSpeed, iso)
                    materialInstanceHandle = instanceHandle
                },
            )
        },
        controls = {
            notice?.let { SampleNotice(it) }
            ExpandableSection(
                title = "Sun",
            ) {
                if (syncManualOverride) {
                    SampleNotice("Manual latitude, longitude, and time are driving sun azimuth and height.")
                } else if (syncEnabled && syncDeviceLocation) {
                    SampleNotice("Device location and current time are driving sun azimuth and height.")
                } else if (syncEnabled) {
                    SampleNotice("Current time and your manual coordinates are driving sun azimuth and height.")
                }
                SkySlider(
                    "Azimuth",
                    sunAzimuth,
                    0f..360f,
                    enabled = !realtimeModeEnabled,
                ) { applyManualEdit { sunAzimuth = it } }
                SkySlider(
                    "Height",
                    sunHeight,
                    -0.2f..1f,
                    enabled = !realtimeModeEnabled,
                ) { applyManualEdit { sunHeight = it } }
                SkySlider(
                    "Intensity",
                    sunIntensity,
                    0f..500_000f
                ) { applyManualEdit { sunIntensity = it } }
                SkySlider("Radius", sunRadius, 0f..5f) { applyManualEdit { sunRadius = it } }
                SkySlider(
                    "Limb Darkening",
                    sunLimbDarkening,
                    0f..2f
                ) { applyManualEdit { sunLimbDarkening = it } }
                SkySlider(
                    "Disk Intensity Boost",
                    sunDiskIntensityBoost,
                    0f..100f
                ) { applyManualEdit { sunDiskIntensityBoost = it } }
                SkySlider("Tint Red", sunTintRed, 0f..1f) { applyManualEdit { sunTintRed = it } }
                SkySlider("Tint Green", sunTintGreen, 0f..1f) {
                    applyManualEdit {
                        sunTintGreen = it
                    }
                }
                SkySlider("Tint Blue", sunTintBlue, 0f..1f) { applyManualEdit { sunTintBlue = it } }
            }

            ExpandableSection(
                title = "Moon",
            ) {
                CheckBoxLabel("Enabled", moonEnabled, { applyMoonEdit { moonEnabled = it } })
                SkySlider(
                    "Azimuth",
                    moonAzimuth,
                    0f..360f,
                    enabled = !realtimeModeEnabled,
                ) { applyManualEdit { moonAzimuth = it } }
                SkySlider(
                    "Height",
                    moonHeight,
                    -0.2f..1f,
                    enabled = !realtimeModeEnabled,
                ) { applyManualEdit { moonHeight = it } }
                SkySlider("Intensity", moonIntensity, 0f..1000f) {
                    applyMoonEdit {
                        moonIntensity = it
                    }
                }
                SkySlider("Radius", moonRadius, 0.1f..5f) { applyMoonEdit { moonRadius = it } }
                SkySlider("Tint Red", moonTintRed, 0f..1f) { applyMoonEdit { moonTintRed = it } }
                SkySlider("Tint Green", moonTintGreen, 0f..1f) {
                    applyMoonEdit {
                        moonTintGreen = it
                    }
                }
                SkySlider("Tint Blue", moonTintBlue, 0f..1f) { applyMoonEdit { moonTintBlue = it } }
            }

            ExpandableSection(
                title = "Milky Way",
            ) {
                CheckBoxLabel(
                    "Enabled",
                    milkyWayEnabled,
                    { applyManualEdit { milkyWayEnabled = it } })
                SkySlider(
                    "Intensity",
                    milkyWayIntensity,
                    0f..100f
                ) { applyManualEdit { milkyWayIntensity = it } }
                SkySlider(
                    "Saturation",
                    milkyWaySaturation,
                    0f..2f
                ) { applyManualEdit { milkyWaySaturation = it } }
                SkySlider(
                    "Black Point",
                    milkyWayBlackPoint,
                    0f..0.5f
                ) { applyManualEdit { milkyWayBlackPoint = it } }
                SkySlider(
                    "Sidereal Time",
                    milkyWaySiderealTime,
                    0f..24f,
                    enabled = !realtimeModeEnabled,
                ) {
                    applyManualEdit { milkyWaySiderealTime = it }
                }
                SkySlider(
                    "Latitude",
                    milkyWayLatitude,
                    -90f..90f,
                    enabled = !realtimeModeEnabled,
                ) { applyManualEdit { milkyWayLatitude = it } }
            }

            ExpandableSection(
                title = "Atmosphere",
            ) {
                SkySlider("Turbidity", turbidity, 1f..10f) { applyManualEdit { turbidity = it } }
                SkySlider("Rayleigh", rayleigh, 0f..10f) { applyManualEdit { rayleigh = it } }
                SkySlider(
                    "Mie Coefficient",
                    mieCoefficient,
                    0f..10f
                ) { applyManualEdit { mieCoefficient = it } }
                SkySlider("Ozone", ozone, 0f..1f) { applyManualEdit { ozone = it } }
                SkySlider("Mie G", mieG, 0f..0.999f) { applyManualEdit { mieG = it } }
            }

            ExpandableSection(
                title = "Clouds",
            ) {
                CheckBoxLabel(
                    "Volumetrics",
                    cloudVolumetrics,
                    { applyManualEdit { cloudVolumetrics = it } })
                SkySlider("Coverage", cloudCoverage, 0f..1f) {
                    applyManualEdit {
                        cloudCoverage = it
                    }
                }
                SkySlider("Density", cloudDensity, 0f..1f) { applyManualEdit { cloudDensity = it } }
                SkySlider(
                    "Height",
                    cloudHeightMeters,
                    2000f..20_000f
                ) { applyManualEdit { cloudHeightMeters = it } }
                SkySlider("Speed", cloudSpeed, 0f..200f) { applyManualEdit { cloudSpeed = it } }
                SkySlider(
                    "Evolution",
                    cloudEvolutionSpeed,
                    0f..2f
                ) { applyManualEdit { cloudEvolutionSpeed = it } }
            }

            ExpandableSection(
                title = "Water",
            ) {
                CheckBoxLabel(
                    "Derivative Trick",
                    waterDerivativeTrick,
                    { applyManualEdit { waterDerivativeTrick = it } })
                SkySlider("Strength", waterStrength, 10f..100f) {
                    applyManualEdit {
                        waterStrength = it
                    }
                }
                SkySlider("Speed", waterSpeed, 0f..5f) { applyManualEdit { waterSpeed = it } }
                SkySlider("Octaves", waterOctaves, 1f..8f) { applyManualEdit { waterOctaves = it } }
            }

            ExpandableSection(
                title = "Stars",
            ) {
                CheckBoxLabel("Enabled", starsEnabled, { applyManualEdit { starsEnabled = it } })
                SkySlider("Density", starDensity, 0f..0.01f) {
                    applyManualEdit {
                        starDensity = it
                    }
                }
                SkySlider(
                    "Intensity (Exp)",
                    starIntensityExponent,
                    0f..24f
                ) { applyManualEdit { starIntensityExponent = it } }
            }

            ExpandableSection(
                title = "Artistic",
            ) {
                SkySlider("MS Rayleigh", msRayleigh, 0f..2f) { applyManualEdit { msRayleigh = it } }
                SkySlider("MS Mie", msMie, 0f..2f) { applyManualEdit { msMie = it } }
                SkySlider("Horizon Glow", horizonGlow, 0f..1f) {
                    applyManualEdit {
                        horizonGlow = it
                    }
                }
                SkySlider("Contrast", contrast, 0.1f..2f) { applyManualEdit { contrast = it } }
                SkySlider(
                    "Shimmer Strength",
                    shimmerStrength,
                    0f..0.1f
                ) { applyManualEdit { shimmerStrength = it } }
                SkySlider(
                    "Shimmer Frequency",
                    shimmerFrequency,
                    1f..100f
                ) { applyManualEdit { shimmerFrequency = it } }
                SkySlider(
                    "Shimmer Mask Height",
                    shimmerMaskHeight,
                    0.01f..0.5f
                ) { applyManualEdit { shimmerMaskHeight = it } }
            }

            ExpandableSection(
                title = "Camera",
            ) {
                SkySlider("Focal Length", focalLength, 8f..300f) {
                    applyManualEdit {
                        focalLength = it
                    }
                }
                SkySlider("Aperture", aperture, 1.4f..32f) { applyManualEdit { aperture = it } }
                SkySlider(
                    "Shutter Speed (1/x s)",
                    shutterSpeed,
                    0.05f..1000f
                ) { applyManualEdit { shutterSpeed = it } }
                SkySlider("ISO", iso, 50f..3200f) { applyManualEdit { iso = it } }
                Button(
                    onClick = {
                        cameraAzimuth = DefaultCameraAzimuth
                        cameraElevation = DefaultCameraElevation
                    },
                    enabled = cameraModified,
                    modifier = Modifier.fillMaxWidth().padding(top = Grid.One),
                ) {
                    Text("Reset Camera")
                }
            }

            ExpandableSection(
                title = "Location and Time",
            ) {
                CheckBoxLabel("Enable Sync", syncEnabled, {
                    syncEnabled = it
                    if (it) syncManualOverride = false
                    if (!it) syncNotice = null
                })
                CheckBoxLabel("Use Device Location", syncDeviceLocation, {
                    syncDeviceLocation = it
                    if (it) syncManualOverride = false
                    if (!it) syncNotice = null
                })
                CheckBoxLabel("Manual Override", syncManualOverride, {
                    syncManualOverride = it
                    if (it) syncNotice = null
                }, enabled = !syncEnabled && !syncDeviceLocation)
                SkySlider(
                    "Latitude",
                    syncLatitude,
                    -90f..90f,
                    enabled = syncManualOverride && !syncEnabled && !syncDeviceLocation,
                ) { applyCoordinateEdit { syncLatitude = it } }
                SkySlider(
                    "Longitude",
                    syncLongitude,
                    -180f..180f,
                    enabled = syncManualOverride && !syncEnabled && !syncDeviceLocation,
                ) { applyCoordinateEdit { syncLongitude = it } }
                SkySlider(
                    "Time",
                    manualTimeHours,
                    0f..24f,
                    enabled = syncManualOverride && !syncEnabled && !syncDeviceLocation,
                    valueFormatter = ::formatTimeOfDay,
                ) {
                    syncNotice = null
                    manualTimeHours = it
                }
                SkyLocationSyncEffect(
                    enabled = syncDeviceLocation && !syncManualOverride,
                    onLocationUpdated = { latitude, longitude ->
                        syncLatitude = latitude
                        syncLongitude = longitude
                    },
                    onStatusChanged = { syncNotice = it },
                )
                syncNotice?.let {
                    SampleNotice(
                        it
                    )
                }
                if (realtimeModeEnabled) {
                    SampleNotice(
                        "Synced sun ${formatSliderValue(sunAzimuth)} deg, " +
                                "height ${formatSliderValue(sunHeight)}, " +
                                "sidereal ${formatSliderValue(milkyWaySiderealTime)} h."
                    )
                } else {
                    SampleNotice("Enable Sync to drive the sky. Use Manual Override to test latitude, longitude, and time explicitly.")
                }
            }
            SetSkyAsWallpaperButton(
                config = editableConfig,
                modifier = Modifier.fillMaxWidth().padding(top = Grid.One),
            )
        },
    )
}

@Composable
private fun SkySlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean = true,
    valueFormatter: (Float) -> String = ::formatSliderValue,
    onValueChange: (Float) -> Unit,
) {
    Text(
        text = "$label: ${valueFormatter(value)}",
        modifier = Modifier.padding(top = Grid.One),
        style = MaterialTheme.typography.bodyMedium,
    )
    Slider(
        value = value,
        valueRange = valueRange,
        enabled = enabled,
        onValueChange = onValueChange,
    )
}



private fun bindTexture(
    engine: FilamentEngine,
    instanceHandle: Int,
    supportedParameters: Set<String>,
    parameterName: String,
    path: String,
    onFailure: (String) -> Unit,
) {
    if (parameterName !in supportedParameters) return
    val textureHandle = engine.loadTexture(Res.getUri(path))
    if (textureHandle <= 0) {
        onFailure("The texture $path could not be loaded.")
        return
    }
    engine.setTextureParameter(instanceHandle, parameterName, textureHandle)
}

private fun applySkyParameters(
    engine: FilamentEngine,
    instanceHandle: Int,
    viewportHeightPx: Int,
    sunAzimuth: Float,
    sunHeight: Float,
    sunIntensity: Float,
    sunRadius: Float,
    sunLimbDarkening: Float,
    sunDiskIntensityBoost: Float,
    sunTint: Float3,
    turbidity: Float,
    rayleigh: Float,
    mieCoefficient: Float,
    ozone: Float,
    mieG: Float,
    cloudCoverage: Float,
    cloudDensity: Float,
    cloudVolumetrics: Boolean,
    cloudHeightMeters: Float,
    cloudSpeed: Float,
    cloudEvolutionSpeed: Float,
    waterDerivativeTrick: Boolean,
    waterStrength: Float,
    waterSpeed: Float,
    waterOctaves: Float,
    aperture: Float,
    shutterSpeed: Float,
    iso: Float,
    focalLength: Float,
    supportedParameters: Set<String>,
    moonEnabled: Boolean,
    moonAzimuth: Float,
    moonHeight: Float,
    moonIntensity: Float,
    moonRadius: Float,
    moonTint: Float3,
    milkyWayEnabled: Boolean,
    milkyWayIntensity: Float,
    milkyWaySaturation: Float,
    milkyWayBlackPoint: Float,
    milkyWaySiderealTime: Float,
    milkyWayLatitude: Float,
    starsEnabled: Boolean,
    starDensity: Float,
    starIntensityExponent: Float,
    msRayleigh: Float,
    msMie: Float,
    horizonGlow: Float,
    contrast: Float,
    shimmerStrength: Float,
    shimmerFrequency: Float,
    shimmerMaskHeight: Float,
    onError: (String) -> Unit,
) {
    val sunDirection = directionFromAzimuthHeight(sunAzimuth, sunHeight)
    val moonDirection = directionFromAzimuthHeight(moonAzimuth, moonHeight)
    val exposure = computeExposure(aperture, shutterSpeed, iso)
    val preExposedSunIntensity = sunIntensity * exposure
    val shaderExposure = 1f

    val lambda = doubleArrayOf(680e-9, 550e-9, 440e-9)
    val n = 1.0003
    val moleculeDensity = 2.545e25
    val rayleighTerm = (8.0 * PI.pow(3.0) * (n * n - 1.0).pow(2.0)) / (3.0 * moleculeDensity)
    val depthR = FloatArray(3) { index ->
        ((rayleighTerm / lambda[index].pow(4.0)) * 8000.0 * rayleigh).toFloat()
    }

    val mieAlpha = 1.3
    val mieBase = 2.0e-5 * turbidity
    val depthM = FloatArray(3) { index ->
        (
                mieBase *
                        (550e-9 / lambda[index]).pow(mieAlpha) *
                        1200.0 *
                        mieCoefficient
                ).toFloat()
    }

    val cutoffAngle = degreesToRadians(96.0)
    val steepness = 1.5
    val zenithFade = 1.0 - exp(-(cutoffAngle / steepness))
    val zenithAngle = acos(sunDirection.y.coerceIn(-1f, 1f).toDouble())
    val sunFade = (
            max(0.0, 1.0 - exp(-((cutoffAngle - zenithAngle) / steepness))) /
                    zenithFade
            ).toFloat()
    val physicalSunIntensity = preExposedSunIntensity * sunFade

    val sunHalo = buildHaloUniform(
        angularRadiusDegrees = sunRadius,
        limbDarkening = sunLimbDarkening,
        intensity = sunDiskIntensityBoost,
        enabled = true,
    )
    val moonHalo = buildMoonHaloUniform(
        angularRadiusDegrees = moonRadius,
        intensity = 1f,
        enabled = moonEnabled,
    )

    val cloudHeightKm = cloudHeightMeters * 0.001f
    val cloudIntersectC = PlanetRadiusKm * PlanetRadiusKm -
            (PlanetRadiusKm + cloudHeightKm) * (PlanetRadiusKm + cloudHeightKm)
    val cloudUniform = Float4(
        cloudCoverage.coerceIn(0f, 1f),
        cloudDensity.coerceAtLeast(0f),
        cloudIntersectC,
        cloudSpeed * (0.05f / 72f),
    )
    val shimmerUniform = Float4(
        shimmerStrength,
        shimmerFrequency,
        shimmerMaskHeight,
        PlanetRadiusKm,
    )
    val multiScatter = Float4(
        (depthR[0] * msRayleigh + depthM[0] * msMie) * 0.25f,
        (depthR[1] * msRayleigh + depthM[1] * msMie) * 0.25f,
        (depthR[2] * msRayleigh + depthM[2] * msMie) * 0.25f,
        horizonGlow,
    )

    val g2 = mieG * mieG
    val miePhaseParams = Float2(1f + g2, -2f * mieG)

    val starControl = buildStarControl(
        density = starDensity,
        enabled = starsEnabled,
        viewportHeightPx = viewportHeightPx,
        focalLengthMm = focalLength,
    )
    val starIntensity = 2f.pow(starIntensityExponent)
    val milkyWayControl = Float3(
        if (milkyWayEnabled) milkyWayIntensity * 0.003f else 0f,
        milkyWaySaturation,
        milkyWayBlackPoint,
    )

    setMaterialParameterSafely(
        engine,
        instanceHandle,
        supportedParameters,
        MaterialParameter("sunDirection", sunDirection),
        onError
    )
    setMaterialParameterSafely(
        engine,
        instanceHandle,
        supportedParameters,
        MaterialParameter("sunDirection2", moonDirection),
        onError
    )
    setMaterialParameterSafely(
        engine,
        instanceHandle,
        supportedParameters,
        MaterialParameter("depthR", Float3(depthR[0], depthR[1], depthR[2])),
        onError,
    )
    setMaterialParameterSafely(
        engine,
        instanceHandle,
        supportedParameters,
        MaterialParameter("depthM", Float3(depthM[0], depthM[1], depthM[2])),
        onError,
    )
    setMaterialParameterSafely(
        engine,
        instanceHandle,
        supportedParameters,
        MaterialParameter("miePhaseParams", miePhaseParams),
        onError
    )
    setMaterialParameterSafely(
        engine,
        instanceHandle,
        supportedParameters,
        MaterialParameter("sunIntensity", physicalSunIntensity),
        onError
    )
    setMaterialParameterSafely(
        engine,
        instanceHandle,
        supportedParameters,
        MaterialParameter("sunTint", sunTint),
        onError,
    )
    setMaterialParameterSafely(
        engine,
        instanceHandle,
        supportedParameters,
        MaterialParameter("contrast", contrast),
        onError
    )
    setMaterialParameterSafely(
        engine,
        instanceHandle,
        supportedParameters,
        MaterialParameter(
            "nightColor",
            Float3(
                0.0035f,
                0.006f,
                0.012f,
            ),
        ),
        onError,
    )
    setMaterialParameterSafely(
        engine,
        instanceHandle,
        supportedParameters,
        MaterialParameter("ozone", Float3(0f, ozone * 0.1f, 0f)),
        onError,
    )
    setMaterialParameterSafely(
        engine,
        instanceHandle,
        supportedParameters,
        MaterialParameter(
            "eclipseFactor",
            computeEclipseFactor(
                sunDirection = sunDirection,
                moonDirection = moonDirection,
                sunRadiusDegrees = sunRadius,
                moonRadiusDegrees = moonRadius,
                moonEnabled = moonEnabled,
            ),
        ),
        onError,
    )
    setMaterialParameterSafely(
        engine,
        instanceHandle,
        supportedParameters,
        MaterialParameter("multiScatParams", multiScatter),
        onError
    )
    setMaterialParameterSafely(
        engine,
        instanceHandle,
        supportedParameters,
        MaterialParameter("sunHalo", sunHalo),
        onError
    )
    setMaterialParameterSafely(
        engine,
        instanceHandle,
        supportedParameters,
        MaterialParameter("shimmerControl", shimmerUniform),
        onError
    )
    setMaterialParameterSafely(
        engine,
        instanceHandle,
        supportedParameters,
        MaterialParameter("cloudControl", cloudUniform),
        onError
    )
    setMaterialParameterSafely(
        engine,
        instanceHandle,
        supportedParameters,
        MaterialParameter(
            "cloudControl2",
            Float4(cloudEvolutionSpeed, if (cloudVolumetrics) 1f else 0f, 0f, 0f)
        ),
        onError,
    )
    setMaterialParameterSafely(
        engine,
        instanceHandle,
        supportedParameters,
        MaterialParameter("sunIntensity2", moonIntensity),
        onError
    )
    setMaterialParameterSafely(
        engine,
        instanceHandle,
        supportedParameters,
        MaterialParameter("moonTint", moonTint),
        onError,
    )
    setMaterialParameterSafely(
        engine,
        instanceHandle,
        supportedParameters,
        MaterialParameter("sunHalo2", moonHalo),
        onError
    )
    setMaterialParameterSafely(
        engine,
        instanceHandle,
        supportedParameters,
        MaterialParameter(
            "waterControl",
            Float4(
                waterStrength,
                waterSpeed,
                if (waterDerivativeTrick) 1f else 0f,
                waterOctaves,
            ),
        ),
        onError,
    )
    setMaterialParameterSafely(
        engine,
        instanceHandle,
        supportedParameters,
        MaterialParameter("starControl", starControl),
        onError
    )
    setMaterialParameterSafely(
        engine,
        instanceHandle,
        supportedParameters,
        MaterialParameter("starIntensity", starIntensity),
        onError
    )
    setMaterialParameterSafely(
        engine,
        instanceHandle,
        supportedParameters,
        MaterialParameter("exposure", shaderExposure),
        onError
    )
    setMaterialParameterSafely(
        engine,
        instanceHandle,
        supportedParameters,
        MaterialParameter("milkyWayControl", milkyWayControl),
        onError
    )
    setMaterialParameterSafely(
        engine,
        instanceHandle,
        supportedParameters,
        MaterialParameter(
            "milkyWayRotation",
            buildMilkyWayRotation(
                siderealTimeHours = milkyWaySiderealTime,
                latitudeDegrees = milkyWayLatitude,
            ),
        ),
        onError,
    )
}

private fun setMaterialParameterSafely(
    engine: FilamentEngine,
    instanceHandle: Int,
    supportedParameters: Set<String>,
    parameter: MaterialParameter,
    onError: (String) -> Unit,
) {
    if (parameter.name !in supportedParameters) return
    runCatching {
        engine.setMaterialParameter(instanceHandle, parameter)
    }.onFailure { error ->
        onError("Failed to update ${parameter.name}: ${error.message ?: "unknown error"}")
    }
}

private fun computeExposure(
    aperture: Float,
    shutterSpeed: Float,
    iso: Float,
): Float {
    val shutterSeconds = 1f / shutterSpeed.coerceAtLeast(0.05f)
    val ev100Linear = (aperture * aperture) / shutterSeconds * (100f / iso.coerceAtLeast(1f))
    return 1f / (1.2f * ev100Linear)
}

private fun directionFromAzimuthHeight(
    azimuthDegrees: Float,
    heightCos: Float,
): Float3 {
    val theta = acos(heightCos.coerceIn(-1f, 1f).toDouble())
    val phi = degreesToRadians(azimuthDegrees.toDouble())
    return Float3(
        (sin(theta) * cos(phi)).toFloat(),
        cos(theta).toFloat(),
        (sin(theta) * sin(phi)).toFloat(),
    )
}

private fun buildHaloUniform(
    angularRadiusDegrees: Float,
    limbDarkening: Float,
    intensity: Float,
    enabled: Boolean,
): Float4 {
    val cosRadius = cos(degreesToRadians(angularRadiusDegrees.toDouble())).toFloat()
    val solidAngle = (2.0 * PI * (1.0 - cosRadius)).toFloat()
    val radianceConversion = 1f / max(1e-9f, solidAngle)
    return Float4(
        cosRadius,
        limbDarkening,
        intensity * radianceConversion,
        if (enabled) 1f else 0f,
    )
}

private fun buildMoonHaloUniform(
    angularRadiusDegrees: Float,
    intensity: Float,
    enabled: Boolean,
): Float4 {
    val radians = degreesToRadians(angularRadiusDegrees.toDouble())
    val cosRadius = cos(radians).toFloat()
    val sinRadius = sin(radians).toFloat()
    val solidAngle = (2.0 * PI * (1.0 - cosRadius)).toFloat()
    val radianceConversion = 1f / max(1e-9f, solidAngle)
    return Float4(
        cosRadius,
        sinRadius,
        intensity * radianceConversion,
        if (enabled) 1f else 0f,
    )
}

private fun buildStarControl(
    density: Float,
    enabled: Boolean,
    viewportHeightPx: Int,
    focalLengthMm: Float,
): Float4 {
    val compensatedDensity = (density * 12f).coerceIn(0f, 1f)
    val pixelScale =
        (1f / viewportHeightPx.coerceAtLeast(1)) * (24f / focalLengthMm.coerceAtLeast(1f))
    return Float4(
        compensatedDensity,
        if (enabled) 1f else 0f,
        100f,
        pixelScale * 1.3f,
    )
}

private fun computeEclipseFactor(
    sunDirection: Float3,
    moonDirection: Float3,
    sunRadiusDegrees: Float,
    moonRadiusDegrees: Float,
    moonEnabled: Boolean,
): Float {
    if (!moonEnabled) return 1f

    val sunRadius = degreesToRadians(sunRadiusDegrees.toDouble())
    val moonRadius = degreesToRadians(moonRadiusDegrees.toDouble())
    val dot = (
            sunDirection.x * moonDirection.x +
                    sunDirection.y * moonDirection.y +
                    sunDirection.z * moonDirection.z
            ).coerceIn(-1f, 1f)
    val separation = acos(dot.toDouble())
    val overlap = areaIntersection(sunRadius, moonRadius, separation)
    val sunArea = PI * sunRadius * sunRadius
    val ratio = overlap / max(1e-9, sunArea)
    return (1.0 - min(1.0, max(0.0, ratio))).toFloat()
}

private fun areaIntersection(
    r1: Double,
    r2: Double,
    d: Double,
): Double {
    if (d >= r1 + r2) return 0.0
    if (d <= kotlin.math.abs(r1 - r2)) {
        val radius = min(r1, r2)
        return PI * radius * radius
    }

    val r1Sq = r1 * r1
    val r2Sq = r2 * r2
    val c1 = ((d * d + r1Sq - r2Sq) / (2.0 * d * r1)).coerceIn(-1.0, 1.0)
    val c2 = ((d * d + r2Sq - r1Sq) / (2.0 * d * r2)).coerceIn(-1.0, 1.0)
    val part1 = r1Sq * acos(c1)
    val part2 = r2Sq * acos(c2)
    val triangleTerm = (-d + r1 + r2) * (d + r1 - r2) * (d - r1 + r2) * (d + r1 + r2)
    val part3 = 0.5 * sqrt(max(0.0, triangleTerm))
    return part1 + part2 - part3
}

private fun buildMilkyWayRotation(
    siderealTimeHours: Float,
    latitudeDegrees: Float,
): FloatArray {
    val eqToGal = floatArrayOf(
        -0.054876f, 0.494109f, -0.867666f,
        -0.873437f, -0.444830f, -0.198076f,
        -0.483835f, 0.746982f, 0.455984f,
    )
    val worldToEq = identityMat3()
    rotateX(worldToEq, degreesToRadians(latitudeDegrees) - (PI / 2.0).toFloat())
    rotateY(worldToEq, siderealTimeHours * (PI / 12.0).toFloat())
    return multiplyMat3(eqToGal, worldToEq)
}

private fun identityMat3(): FloatArray = floatArrayOf(
    1f, 0f, 0f,
    0f, 1f, 0f,
    0f, 0f, 1f,
)

private fun rotateX(matrix: FloatArray, angleRadians: Float) {
    val c = cos(angleRadians.toDouble()).toFloat()
    val s = sin(angleRadians.toDouble()).toFloat()
    val m1 = matrix[1]
    val m2 = matrix[2]
    val m4 = matrix[4]
    val m5 = matrix[5]
    val m7 = matrix[7]
    val m8 = matrix[8]
    matrix[1] = m1 * c - m2 * s
    matrix[2] = m1 * s + m2 * c
    matrix[4] = m4 * c - m5 * s
    matrix[5] = m4 * s + m5 * c
    matrix[7] = m7 * c - m8 * s
    matrix[8] = m7 * s + m8 * c
}

private fun rotateY(matrix: FloatArray, angleRadians: Float) {
    val c = cos(angleRadians.toDouble()).toFloat()
    val s = sin(angleRadians.toDouble()).toFloat()
    val m0 = matrix[0]
    val m2 = matrix[2]
    val m3 = matrix[3]
    val m5 = matrix[5]
    val m6 = matrix[6]
    val m8 = matrix[8]
    matrix[0] = m0 * c + m2 * s
    matrix[2] = -m0 * s + m2 * c
    matrix[3] = m3 * c + m5 * s
    matrix[5] = -m3 * s + m5 * c
    matrix[6] = m6 * c + m8 * s
    matrix[8] = -m6 * s + m8 * c
}

private fun multiplyMat3(
    a: FloatArray,
    b: FloatArray,
): FloatArray {
    val out = FloatArray(9)
    val a00 = a[0]
    val a10 = a[1]
    val a20 = a[2]
    val a01 = a[3]
    val a11 = a[4]
    val a21 = a[5]
    val a02 = a[6]
    val a12 = a[7]
    val a22 = a[8]

    val b00 = b[0]
    val b10 = b[1]
    val b20 = b[2]
    val b01 = b[3]
    val b11 = b[4]
    val b21 = b[5]
    val b02 = b[6]
    val b12 = b[7]
    val b22 = b[8]

    out[0] = a00 * b00 + a01 * b10 + a02 * b20
    out[1] = a10 * b00 + a11 * b10 + a12 * b20
    out[2] = a20 * b00 + a21 * b10 + a22 * b20
    out[3] = a00 * b01 + a01 * b11 + a02 * b21
    out[4] = a10 * b01 + a11 * b11 + a12 * b21
    out[5] = a20 * b01 + a21 * b11 + a22 * b21
    out[6] = a00 * b02 + a01 * b12 + a02 * b22
    out[7] = a10 * b02 + a11 * b12 + a12 * b22
    out[8] = a20 * b02 + a21 * b12 + a22 * b22
    return out
}

private fun computeVerticalFovDegrees(
    focalLengthMm: Float,
    sensorHeightMm: Float = 24f,
): Float {
    val focal = focalLengthMm.coerceAtLeast(1f).toDouble()
    val sensor = sensorHeightMm.toDouble()
    return ((2.0 * kotlin.math.atan(sensor / (2.0 * focal))) * 180.0 / PI).toFloat()
}

private fun skyCameraLookDirection(
    azimuthDegrees: Float,
    elevationDegrees: Float,
): Float3 {
    val azimuthRadians = degreesToRadians(azimuthDegrees)
    val elevationRadians = degreesToRadians(elevationDegrees)
    val horizontal = cos(elevationRadians)
    return Float3(
        x = horizontal * cos(azimuthRadians),
        y = sin(elevationRadians),
        z = horizontal * sin(azimuthRadians),
    )
}

private fun normalizeAngleDegrees(value: Float): Float {
    val normalized = value % 360f
    return if (normalized < 0f) normalized + 360f else normalized
}

private fun degreesToRadians(degrees: Float): Float = (degrees.toDouble() * PI / 180.0).toFloat()

private fun degreesToRadians(degrees: Double): Double = degrees * PI / 180.0

private fun formatSliderValue(value: Float): String = when {
    abs(value) >= 100f -> value.toInt().toString()
    abs(value) >= 1f -> formatWithDecimals(value, decimals = 2)
    else -> formatWithDecimals(value, decimals = 3)
}

private fun formatTimeOfDay(value: Float): String {
    val totalMinutes = (((value % 24f) + 24f) % 24f * 60f).roundToInt() % (24 * 60)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
}

private fun formatWithDecimals(
    value: Float,
    decimals: Int,
): String {
    val factor = 10.0.pow(decimals).toFloat()
    val roundedValue = round(value * factor) / factor
    val sign = if (roundedValue < 0f) "-" else ""
    val absoluteValue = abs(roundedValue)
    val whole = absoluteValue.toInt()
    val fraction = ((absoluteValue - whole) * factor).toInt()
    val fractionText = fraction.toString().padStart(decimals, '0')
    return if (decimals == 0) {
        "$sign$whole"
    } else {
        "$sign$whole.$fractionText"
    }
}
