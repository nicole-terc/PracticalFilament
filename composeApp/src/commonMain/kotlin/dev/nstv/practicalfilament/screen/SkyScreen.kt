// Migrated sample from https://github.com/google/filament/tree/main/web/examples/sky
package dev.nstv.practicalfilament.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.layout.onSizeChanged
import dev.nstv.practicalfilament.filament.AttributeDataType
import dev.nstv.practicalfilament.filament.BoundingBox
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.Color
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
import dev.nstv.practicalfilament.filament.toByteArray
import dev.nstv.practicalfilament.theme.Grid
import dev.nstv.practicalfilament.theme.components.CheckBoxLabel
import practicalfilament.composeapp.generated.resources.Res
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val SkyMaterialPath = "files/materials/simulated_skybox.filamat"
private const val MoonDiskTexturePath = "files/textures/moon_disk.png"
private const val MoonNormalTexturePath = "files/textures/moon_normal.png"
private const val MilkyWayTexturePath = "files/textures/milkyway.png"

private const val DefaultSunAzimuth = 0f
private const val DefaultSunHeight = 0f
private const val DefaultTurbidity = 2f
private const val DefaultRayleigh = 1f
private const val DefaultCloudCoverage = 0.4f
private const val DefaultCloudDensity = 0.02f
private const val DefaultAperture = 16f
private const val DefaultShutterSpeed = 125f
private const val DefaultIso = 100f
private const val DefaultMoonAzimuth = 180f
private const val DefaultMoonHeight = 0.70710677f
private const val DefaultStarDensity = 0.001f

private const val FixedFocalLengthMm = 24f
private const val PlanetRadiusKm = 6360f
private const val CloudHeightMeters = 8000f
private const val CloudSpeed = 50f
private const val CloudEvolutionSpeed = 0.02f
private const val MieCoefficient = 1f
private const val MieG = 0.8f
private const val OzoneStrength = 0.25f
private const val MultiScatterRayleigh = 0.1f
private const val MultiScatterMie = 0.5f
private const val HorizonGlow = 0f
private const val StarIntensity = 1f
private const val SunAngularRadiusDegrees = 1.2f
private const val SunLimbDarkening = 0.5f
private const val SunDiskIntensityBoost = 1f
private const val MoonAngularRadiusDegrees = 1.2f
private const val MoonIntensity = 6f
private const val MilkyWayIntensity = 1f
private const val MilkyWaySaturation = 1f
private const val MilkyWayBlackPoint = 0.07f
private const val MilkyWayLatitude = 34f
private const val MilkyWaySiderealTime = 0f

private val NightColor = Float3(0f, 3.0e-9f, 7.5e-9f)
private val ShimmerControl = Float3(0f, 20f, 0.1f)

@Composable
fun SkyScreen(
    modifier: Modifier = Modifier,
) {
    var engine by remember { mutableStateOf<FilamentEngine?>(null) }
    var materialInstanceHandle by remember { mutableIntStateOf(0) }
    var viewportHeightPx by remember { mutableIntStateOf(1) }
    var notice by remember { mutableStateOf<String?>(null) }

    var sunAzimuth by remember { mutableFloatStateOf(DefaultSunAzimuth) }
    var sunHeight by remember { mutableFloatStateOf(DefaultSunHeight) }
    var turbidity by remember { mutableFloatStateOf(DefaultTurbidity) }
    var rayleigh by remember { mutableFloatStateOf(DefaultRayleigh) }
    var cloudCoverage by remember { mutableFloatStateOf(DefaultCloudCoverage) }
    var cloudDensity by remember { mutableFloatStateOf(DefaultCloudDensity) }
    var aperture by remember { mutableFloatStateOf(DefaultAperture) }
    var shutterSpeed by remember { mutableFloatStateOf(DefaultShutterSpeed) }
    var iso by remember { mutableFloatStateOf(DefaultIso) }
    var moonEnabled by remember { mutableStateOf(true) }
    var moonAzimuth by remember { mutableFloatStateOf(DefaultMoonAzimuth) }
    var moonHeight by remember { mutableFloatStateOf(DefaultMoonHeight) }
    var starsEnabled by remember { mutableStateOf(true) }
    var starDensity by remember { mutableFloatStateOf(DefaultStarDensity) }

    LaunchedEffect(
        engine,
        materialInstanceHandle,
        viewportHeightPx,
        sunAzimuth,
        sunHeight,
        turbidity,
        rayleigh,
        cloudCoverage,
        cloudDensity,
        aperture,
        shutterSpeed,
        iso,
        moonEnabled,
        moonAzimuth,
        moonHeight,
        starsEnabled,
        starDensity,
    ) {
        val currentEngine = engine ?: return@LaunchedEffect
        if (materialInstanceHandle <= 0) return@LaunchedEffect
        currentEngine.setCameraExposure(aperture, 1f / shutterSpeed.coerceAtLeast(0.05f), iso)
        applySkyParameters(
            engine = currentEngine,
            instanceHandle = materialInstanceHandle,
            viewportHeightPx = viewportHeightPx,
            sunAzimuth = sunAzimuth,
            sunHeight = sunHeight,
            turbidity = turbidity,
            rayleigh = rayleigh,
            cloudCoverage = cloudCoverage,
            cloudDensity = cloudDensity,
            aperture = aperture,
            shutterSpeed = shutterSpeed,
            iso = iso,
            moonEnabled = moonEnabled,
            moonAzimuth = moonAzimuth,
            moonHeight = moonHeight,
            starsEnabled = starsEnabled,
            starDensity = starDensity,
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
                    .onSizeChanged { viewportHeightPx = it.height.coerceAtLeast(1) },
                camera = CameraConfig(
                    position = Float3(0f, 0f, 0f),
                    lookAt = Float3(1f, 0f, 0f),
                    fovDegrees = 53.13,
                    near = 0.1,
                    far = 5000.0,
                ),
                lights = emptyList(),
                backgroundColor = Color(0f, 0f, 0f, 1f),
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

                    bindTexture(
                        engine = readyEngine,
                        instanceHandle = instanceHandle,
                        parameterName = "moonTexture",
                        path = MoonDiskTexturePath,
                        onFailure = { notice = it },
                    )
                    bindTexture(
                        engine = readyEngine,
                        instanceHandle = instanceHandle,
                        parameterName = "moonNormal",
                        path = MoonNormalTexturePath,
                        onFailure = { notice = it },
                    )
                    bindTexture(
                        engine = readyEngine,
                        instanceHandle = instanceHandle,
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

            SkySlider(
                label = "Sun Azimuth",
                value = sunAzimuth,
                valueRange = 0f..360f,
                onValueChange = { sunAzimuth = it },
            )
            SkySlider(
                label = "Sun Height",
                value = sunHeight,
                valueRange = -0.2f..1f,
                onValueChange = { sunHeight = it },
            )
            SkySlider(
                label = "Turbidity",
                value = turbidity,
                valueRange = 1f..10f,
                onValueChange = { turbidity = it },
            )
            SkySlider(
                label = "Rayleigh",
                value = rayleigh,
                valueRange = 0f..10f,
                onValueChange = { rayleigh = it },
            )
            SkySlider(
                label = "Cloud Coverage",
                value = cloudCoverage,
                valueRange = 0f..1f,
                onValueChange = { cloudCoverage = it },
            )
            SkySlider(
                label = "Cloud Density",
                value = cloudDensity,
                valueRange = 0f..1f,
                onValueChange = { cloudDensity = it },
            )
            SkySlider(
                label = "Aperture",
                value = aperture,
                valueRange = 1.4f..32f,
                onValueChange = { aperture = it },
            )
            SkySlider(
                label = "Shutter Speed (1/x s)",
                value = shutterSpeed,
                valueRange = 1f..1000f,
                onValueChange = { shutterSpeed = it },
            )
            SkySlider(
                label = "ISO",
                value = iso,
                valueRange = 50f..3200f,
                onValueChange = { iso = it },
            )

            CheckBoxLabel(
                modifier = Modifier.padding(top = Grid.One),
                text = "Moon Enabled",
                checked = moonEnabled,
                onCheckedChange = { moonEnabled = it },
            )
            SkySlider(
                label = "Moon Azimuth",
                value = moonAzimuth,
                valueRange = 0f..360f,
                onValueChange = { moonAzimuth = it },
            )
            SkySlider(
                label = "Moon Height",
                value = moonHeight,
                valueRange = -0.2f..1f,
                onValueChange = { moonHeight = it },
            )

            CheckBoxLabel(
                modifier = Modifier.padding(top = Grid.One),
                text = "Stars Enabled",
                checked = starsEnabled,
                onCheckedChange = { starsEnabled = it },
            )
            SkySlider(
                label = "Star Density",
                value = starDensity,
                valueRange = 0f..0.01f,
                onValueChange = { starDensity = it },
            )
        },
    )
}

@Composable
private fun SkySlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Text(
        text = label,
        modifier = Modifier.padding(top = Grid.One),
    )
    Slider(
        value = value,
        valueRange = valueRange,
        onValueChange = onValueChange,
    )
}

private fun bindTexture(
    engine: FilamentEngine,
    instanceHandle: Int,
    parameterName: String,
    path: String,
    onFailure: (String) -> Unit,
) {
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
    turbidity: Float,
    rayleigh: Float,
    cloudCoverage: Float,
    cloudDensity: Float,
    aperture: Float,
    shutterSpeed: Float,
    iso: Float,
    moonEnabled: Boolean,
    moonAzimuth: Float,
    moonHeight: Float,
    starsEnabled: Boolean,
    starDensity: Float,
) {
    val sunDirection = directionFromAzimuthHeight(sunAzimuth, sunHeight)
    val moonDirection = directionFromAzimuthHeight(moonAzimuth, moonHeight)
    val exposure = computeExposure(aperture, shutterSpeed, iso)
    val preExposedSunIntensity = 100_000f * exposure
    val preExposedMoonIntensity = MoonIntensity * exposure

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
                MieCoefficient
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
        angularRadiusDegrees = SunAngularRadiusDegrees,
        limbDarkening = SunLimbDarkening,
        intensity = SunDiskIntensityBoost,
        enabled = true,
    )
    val moonHalo = buildMoonHaloUniform(
        angularRadiusDegrees = MoonAngularRadiusDegrees,
        intensity = 1f,
        enabled = moonEnabled,
    )

    val cloudHeightKm = CloudHeightMeters * 0.001f
    val cloudIntersectC = PlanetRadiusKm * PlanetRadiusKm -
        (PlanetRadiusKm + cloudHeightKm) * (PlanetRadiusKm + cloudHeightKm)
    val cloudUniform = Float4(
        cloudCoverage.coerceIn(0f, 1f),
        cloudDensity.coerceAtLeast(0f),
        cloudIntersectC,
        CloudSpeed * (0.05f / 72f),
    )
    val shimmerUniform = Float4(
        ShimmerControl.x,
        ShimmerControl.y,
        ShimmerControl.z,
        PlanetRadiusKm,
    )
    val multiScatter = Float4(
        (depthR[0] * MultiScatterRayleigh + depthM[0] * MultiScatterMie) * 0.25f,
        (depthR[1] * MultiScatterRayleigh + depthM[1] * MultiScatterMie) * 0.25f,
        (depthR[2] * MultiScatterRayleigh + depthM[2] * MultiScatterMie) * 0.25f,
        HorizonGlow,
    )

    val g2 = MieG * MieG
    val miePhaseParams = Float2(1f + g2, -2f * MieG)

    val starControl = buildStarControl(
        density = starDensity,
        enabled = starsEnabled,
        viewportHeightPx = viewportHeightPx,
    )
    val milkyWayControl = Float3(
        MilkyWayIntensity * preExposedSunIntensity * 1.5e-8f,
        MilkyWaySaturation,
        MilkyWayBlackPoint,
    )

    engine.setMaterialParameter(instanceHandle, MaterialParameter("sunDirection", sunDirection))
    engine.setMaterialParameter(instanceHandle, MaterialParameter("sunDirection2", moonDirection))
    engine.setMaterialParameter(
        instanceHandle,
        MaterialParameter("depthR", Float3(depthR[0], depthR[1], depthR[2])),
    )
    engine.setMaterialParameter(
        instanceHandle,
        MaterialParameter("depthM", Float3(depthM[0], depthM[1], depthM[2])),
    )
    engine.setMaterialParameter(instanceHandle, MaterialParameter("miePhaseParams", miePhaseParams))
    engine.setMaterialParameter(instanceHandle, MaterialParameter("sunIntensity", physicalSunIntensity))
    engine.setMaterialParameter(instanceHandle, MaterialParameter("contrast", 1f))
    engine.setMaterialParameter(
        instanceHandle,
        MaterialParameter(
            "nightColor",
            Float3(
                NightColor.x * preExposedSunIntensity,
                NightColor.y * preExposedSunIntensity,
                NightColor.z * preExposedSunIntensity,
            ),
        ),
    )
    engine.setMaterialParameter(instanceHandle, MaterialParameter("ozone", Float3(0f, OzoneStrength * 0.1f, 0f)))
    engine.setMaterialParameter(
        instanceHandle,
        MaterialParameter(
            "eclipseFactor",
            computeEclipseFactor(sunDirection = sunDirection, moonDirection = moonDirection, moonEnabled = moonEnabled),
        ),
    )
    engine.setMaterialParameter(instanceHandle, MaterialParameter("multiScatParams", multiScatter))
    engine.setMaterialParameter(instanceHandle, MaterialParameter("sunHalo", sunHalo))
    engine.setMaterialParameter(instanceHandle, MaterialParameter("shimmerControl", shimmerUniform))
    engine.setMaterialParameter(instanceHandle, MaterialParameter("cloudControl", cloudUniform))
    engine.setMaterialParameter(
        instanceHandle,
        MaterialParameter("cloudControl2", Float4(CloudEvolutionSpeed, 0f, 0f, 0f)),
    )
    engine.setMaterialParameter(instanceHandle, MaterialParameter("sunIntensity2", preExposedMoonIntensity))
    engine.setMaterialParameter(instanceHandle, MaterialParameter("sunHalo2", moonHalo))
    engine.setMaterialParameter(instanceHandle, MaterialParameter("waterControl", Float4(50f, 1f, 1f, 4f)))
    engine.setMaterialParameter(instanceHandle, MaterialParameter("starControl", starControl))
    engine.setMaterialParameter(instanceHandle, MaterialParameter("starIntensity", StarIntensity))
    engine.setMaterialParameter(instanceHandle, MaterialParameter("exposure", exposure))
    engine.setMaterialParameter(instanceHandle, MaterialParameter("milkyWayControl", milkyWayControl))
    engine.setMaterialParameter(
        instanceHandle,
        MaterialParameter(
            "milkyWayRotation",
            buildMilkyWayRotation(
                siderealTimeHours = MilkyWaySiderealTime,
                latitudeDegrees = MilkyWayLatitude,
            ),
        ),
    )
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
): Float4 {
    val compensatedDensity = (density * 12f).coerceIn(0f, 1f)
    val pixelScale = (1f / viewportHeightPx.coerceAtLeast(1)) * (24f / FixedFocalLengthMm)
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
    moonEnabled: Boolean,
): Float {
    if (!moonEnabled) return 1f

    val sunRadius = degreesToRadians(SunAngularRadiusDegrees.toDouble())
    val moonRadius = degreesToRadians(MoonAngularRadiusDegrees.toDouble())
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

private fun degreesToRadians(degrees: Float): Float = (degrees.toDouble() * PI / 180.0).toFloat()

private fun degreesToRadians(degrees: Double): Double = degrees * PI / 180.0
