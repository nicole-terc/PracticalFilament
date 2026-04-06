package dev.nstv.practicalfilament.screen.marbles.comparison

import android.content.Context
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.LightType
import dev.nstv.practicalfilament.screen.marbles.ComparisonMarbleCamera
import dev.nstv.practicalfilament.screen.marbles.MarbleAliveLights
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import android.graphics.Canvas as AndroidCanvas

@Composable
internal fun RenderEffectMarbleStage(
    preset: ComparisonPresetSpec,
    animationTimeSeconds: Float,
    backgroundEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val sweep = sin(animationTimeSeconds * 0.72f)
    val drift = cos(animationTimeSeconds * 0.48f)
    val shimmer = sin(animationTimeSeconds * 1.1f)
    val backgroundFactor = if (backgroundEnabled) 1f else 0f
    val reflectionAlpha =
        (0.12f + preset.reflectionStrength * (0.32f + backgroundFactor * 0.4f)).coerceAtMost(0.92f)
    val rimAlpha =
        (0.14f + preset.translucency * 0.36f + preset.metallic * 0.08f).coerceAtMost(0.74f)
    val secondaryGlowAlpha =
        (0.04f + preset.translucency * 0.16f + preset.reflectionStrength * (0.06f + backgroundFactor * 0.1f))
            .coerceAtMost(0.46f)

    CircularComparisonStage(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension * 0.5f
            val lightCenter = Offset(
                x = size.width * (0.28f + drift * 0.025f),
                y = size.height * (0.22f + sweep * 0.02f),
            )
            val shadowCenter = Offset(
                x = size.width * 0.76f,
                y = size.height * 0.8f,
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        preset.highlightColor.copy(alpha = 0.48f + (1f - preset.roughness) * 0.2f),
                        preset.baseColor,
                        preset.shadowColor,
                    ),
                    center = lightCenter,
                    radius = radius * 1.55f,
                ),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        preset.shadowColor.copy(alpha = 0f),
                        preset.shadowColor.copy(alpha = 0.22f + preset.metallic * 0.08f),
                        preset.shadowColor.copy(alpha = 0.58f + preset.roughness * 0.14f),
                    ),
                    center = shadowCenter,
                    radius = radius * 1.18f,
                ),
            )
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        preset.highlightColor.copy(alpha = 0.08f + preset.translucency * 0.16f),
                        preset.baseColor.copy(alpha = 0.02f),
                        preset.baseColor.copy(alpha = 0f),
                    ),
                    start = Offset(size.width * 0.18f, size.height * 0.08f),
                    end = Offset(size.width * 0.64f, size.height * 0.42f),
                ),
            )

            if (preset.veinStrength > 0.03f) {
                drawPath(
                    path = createVeinPath(
                        width = size.width,
                        height = size.height,
                        seed = 0.9f + sweep * 0.05f,
                        bend = -0.18f,
                        thickness = 0.08f,
                    ),
                    color = preset.shadowColor.copy(alpha = preset.veinStrength * 0.55f),
                    style = Stroke(width = radius * (0.022f + preset.veinStrength * 0.06f)),
                )
                drawPath(
                    path = createVeinPath(
                        width = size.width,
                        height = size.height,
                        seed = 1.75f + drift * 0.04f,
                        bend = 0.1f,
                        thickness = 0.05f,
                    ),
                    color = preset.veinColor.copy(alpha = preset.veinStrength * 0.9f),
                    style = Stroke(width = radius * (0.012f + preset.veinStrength * 0.03f)),
                )
                drawPath(
                    path = createVeinPath(
                        width = size.width,
                        height = size.height,
                        seed = 2.5f + shimmer * 0.03f,
                        bend = 0.24f,
                        thickness = 0.06f,
                    ),
                    color = preset.highlightColor.copy(alpha = preset.veinStrength * 0.42f),
                    style = Stroke(width = radius * (0.009f + preset.veinStrength * 0.02f)),
                )
            }
            drawCircle(
                color = preset.rimColor.copy(alpha = rimAlpha),
                style = Stroke(width = 1.5.dp.toPx()),
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (34.dp + (sweep * 3f).dp), y = (28.dp + (drift * 3f).dp))
                .size(
                    width = (78.dp + (preset.reflectionStrength * 16f).dp),
                    height = (48.dp + (preset.translucency * 10f).dp),
                )
                .rotate(-16f + preset.translucency * 3f)
                .graphicsLayer {
                    renderEffect = BlurEffect(34f, 34f, TileMode.Decal)
                    alpha = reflectionAlpha * 0.82f
                }
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            preset.reflectionColor.copy(alpha = 0.92f),
                            preset.highlightColor.copy(alpha = 0f),
                        ),
                    ),
                    shape = CircleShape,
                ),
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (60.dp + (sweep * 1.5f).dp), y = (72.dp + (drift * 1f).dp))
                .size(12.dp)
                .graphicsLayer {
                    renderEffect = BlurEffect(7f, 7f, TileMode.Decal)
                    alpha = (0.24f + preset.reflectionStrength * 0.24f).coerceAtMost(0.56f)
                }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            preset.highlightColor.copy(alpha = 0.98f),
                            preset.highlightColor.copy(alpha = 0f),
                        ),
                    ),
                    shape = CircleShape,
                ),
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-58).dp + (drift * 8f).dp, y = (-62).dp + (sweep * 10f).dp)
                .size(112.dp)
                .graphicsLayer {
                    renderEffect = BlurEffect(58f, 58f, TileMode.Decal)
                    alpha = secondaryGlowAlpha
                }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            preset.rimColor.copy(alpha = 0.72f),
                            preset.highlightColor.copy(alpha = 0f),
                        ),
                    ),
                    shape = CircleShape,
                ),
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-18).dp, y = (10).dp)
                .size(92.dp, 152.dp)
                .rotate(13f)
                .graphicsLayer {
                    renderEffect = BlurEffect(46f, 46f, TileMode.Decal)
                    alpha = (0.06f + preset.reflectionStrength * 0.12f).coerceAtMost(0.18f)
                }
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            preset.reflectionColor.copy(alpha = 0.78f),
                            preset.rimColor.copy(alpha = 0f),
                        ),
                    ),
                    shape = CircleShape,
                ),
        )

        if (preset.metallic > 0.5f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (-38).dp, y = 8.dp)
                    .size(54.dp, 172.dp)
                    .rotate(8f)
                    .graphicsLayer {
                        renderEffect = BlurEffect(18f, 54f, TileMode.Decal)
                        alpha = 0.18f + preset.reflectionStrength * 0.16f
                    }
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                preset.highlightColor.copy(alpha = 0f),
                                preset.reflectionColor.copy(alpha = 0.96f),
                                preset.highlightColor.copy(alpha = 0f),
                            ),
                        ),
                        shape = CircleShape,
                    ),
            )
        }

        if (backgroundEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                preset.reflectionColor.copy(alpha = 0f),
                                preset.reflectionColor.copy(alpha = 0.06f),
                                preset.reflectionColor.copy(alpha = 0f),
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(520f, 380f),
                        ),
                    ),
            )
        }
    }
}

@Composable
internal fun AgslMarbleStage(
    preset: ComparisonPresetSpec,
    animationTimeSeconds: Float,
    backgroundEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val agslView = remember(context) { AgslMarbleView(context) }

    CircularComparisonStage(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { agslView },
            update = { view ->
                view.update(
                    preset = preset,
                    timeSeconds = animationTimeSeconds,
                    backgroundEnabled = backgroundEnabled,
                )
            },
        )
    }
}

private class AgslMarbleView(
    context: Context,
) : View(context) {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val shader = RuntimeShader(MarbleShaderSource)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = this@AgslMarbleView.shader
        isDither = true
    }

    private var preset: ComparisonPresetSpec = ComparisonMaterialPreset.Ceramic.toSpec()
    private var timeSeconds: Float = 0f
    private var backgroundEnabled: Boolean = false

    fun update(
        preset: ComparisonPresetSpec,
        timeSeconds: Float,
        backgroundEnabled: Boolean,
    ) {
        this.preset = preset
        this.timeSeconds = timeSeconds
        this.backgroundEnabled = backgroundEnabled
        invalidate()
    }

    override fun onDraw(canvas: AndroidCanvas) {
        super.onDraw(canvas)
        val width = width.toFloat().coerceAtLeast(1f)
        val height = height.toFloat().coerceAtLeast(1f)
        val rig = CalculatedComparisonRigProjection
        shader.setFloatUniform("resolution", width, height)
        shader.setFloatUniform("time", timeSeconds)
        shader.setFloatUniform(
            "baseColor",
            preset.baseColor.red,
            preset.baseColor.green,
            preset.baseColor.blue,
            preset.baseColor.alpha,
        )
        shader.setFloatUniform(
            "shadowColor",
            preset.shadowColor.red,
            preset.shadowColor.green,
            preset.shadowColor.blue,
            preset.shadowColor.alpha,
        )
        shader.setFloatUniform(
            "highlightColor",
            preset.highlightColor.red,
            preset.highlightColor.green,
            preset.highlightColor.blue,
            preset.highlightColor.alpha,
        )
        shader.setFloatUniform(
            "rimColor",
            preset.rimColor.red,
            preset.rimColor.green,
            preset.rimColor.blue,
            preset.rimColor.alpha,
        )
        shader.setFloatUniform(
            "veinColor",
            preset.veinColor.red,
            preset.veinColor.green,
            preset.veinColor.blue,
            preset.veinColor.alpha,
        )
        shader.setFloatUniform(
            "reflectionColor",
            preset.reflectionColor.red,
            preset.reflectionColor.green,
            preset.reflectionColor.blue,
            preset.reflectionColor.alpha,
        )
        shader.setFloatUniform("roughness", preset.roughness)
        shader.setFloatUniform("metallic", preset.metallic)
        shader.setFloatUniform("translucency", preset.translucency)
        shader.setFloatUniform("veinStrength", preset.veinStrength)
        shader.setFloatUniform("reflectionStrength", preset.reflectionStrength)
        shader.setFloatUniform("backgroundEnabled", if (backgroundEnabled) 1f else 0f)
        shader.setFloatUniform(
            "primaryHighlightCenter",
            rig.primaryHighlightCenter.x,
            rig.primaryHighlightCenter.y
        )
        shader.setFloatUniform(
            "secondaryHighlightCenter",
            rig.secondaryHighlightCenter.x,
            rig.secondaryHighlightCenter.y
        )
        shader.setFloatUniform(
            "keyLightDirection",
            rig.keyLightDirection.x,
            rig.keyLightDirection.y,
            rig.keyLightDirection.z
        )
        canvas.drawRect(0f, 0f, width, height, paint)
    }
}

private data class ComparisonRigProjection(
    val primaryHighlightCenter: Offset,
    val secondaryHighlightCenter: Offset,
    val keyLightDirection: Float3,
)

private val CalculatedComparisonRigProjection: ComparisonRigProjection by lazy {
    val cameraDirection = normalize3(ComparisonMarbleCamera.position)
    val sunLight =
        MarbleAliveLights.first { it.type == LightType.SUN }
    val pointLight =
        MarbleAliveLights.first { it.type == LightType.POINT }
    val sunDirection = normalize3(
        Float3(
            x = -sunLight.direction.x,
            y = -sunLight.direction.y,
            z = -sunLight.direction.z,
        ),
    )
    val pointDirection = normalize3(pointLight.position)
    val pointDistance = length3(pointLight.position)
    val pointAttenuation = (1f - pointDistance / pointLight.falloffRadius).coerceIn(0f, 1f)
    val keyLightDirection = normalize3(
        Float3(
            x = sunDirection.x * 1f + pointDirection.x * pointAttenuation,
            y = sunDirection.y * 1f + pointDirection.y * pointAttenuation,
            z = sunDirection.z * 1f + pointDirection.z * pointAttenuation,
        ),
    )

    ComparisonRigProjection(
        primaryHighlightCenter = projectHighlightCenter(sunDirection, cameraDirection),
        secondaryHighlightCenter = projectHighlightCenter(pointDirection, cameraDirection),
        keyLightDirection = keyLightDirection,
    )
}

private fun projectHighlightCenter(
    lightDirection: Float3,
    cameraDirection: Float3,
): Offset {
    val halfVector = normalize3(
        Float3(
            x = lightDirection.x + cameraDirection.x,
            y = lightDirection.y + cameraDirection.y,
            z = lightDirection.z + cameraDirection.z,
        ),
    )
    return Offset(halfVector.x, halfVector.y)
}

private fun normalize3(vector: Float3): Float3 {
    val length = length3(vector).coerceAtLeast(1e-5f)
    return Float3(
        x = vector.x / length,
        y = vector.y / length,
        z = vector.z / length,
    )
}

private fun length3(vector: Float3): Float {
    return sqrt(vector.x * vector.x + vector.y * vector.y + vector.z * vector.z)
}

private const val MarbleShaderSource = """
uniform float2 resolution;
uniform float time;
uniform half4 baseColor;
uniform half4 shadowColor;
uniform half4 highlightColor;
uniform half4 rimColor;
uniform half4 veinColor;
uniform half4 reflectionColor;
uniform float roughness;
uniform float metallic;
uniform float translucency;
uniform float veinStrength;
uniform float reflectionStrength;
uniform float backgroundEnabled;
uniform float2 primaryHighlightCenter;
uniform float2 secondaryHighlightCenter;
uniform float3 keyLightDirection;

float2 rot2(float angle, float2 p) {
    float c = cos(angle);
    float s = sin(angle);
    return float2(c * p.x - s * p.y, s * p.x + c * p.y);
}

float hash31(float3 p) {
    return fract(sin(dot(p, float3(127.1, 311.7, 74.7))) * 43758.5453123);
}

float noise3(float3 p) {
    float3 i = floor(p);
    float3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    float n000 = hash31(i + float3(0.0, 0.0, 0.0));
    float n100 = hash31(i + float3(1.0, 0.0, 0.0));
    float n010 = hash31(i + float3(0.0, 1.0, 0.0));
    float n110 = hash31(i + float3(1.0, 1.0, 0.0));
    float n001 = hash31(i + float3(0.0, 0.0, 1.0));
    float n101 = hash31(i + float3(1.0, 0.0, 1.0));
    float n011 = hash31(i + float3(0.0, 1.0, 1.0));
    float n111 = hash31(i + float3(1.0, 1.0, 1.0));

    float nx00 = mix(n000, n100, f.x);
    float nx10 = mix(n010, n110, f.x);
    float nx01 = mix(n001, n101, f.x);
    float nx11 = mix(n011, n111, f.x);
    float nxy0 = mix(nx00, nx10, f.y);
    float nxy1 = mix(nx01, nx11, f.y);
    return mix(nxy0, nxy1, f.z);
}

float fbm3(float3 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 4; ++i) {
        value += noise3(p) * amplitude;
        p = p * 2.02 + float3(17.3, 11.2, 13.7);
        amplitude *= 0.5;
    }
    return value;
}

float marbleField(float3 p) {
    float2 xy = rot2(p.z * 0.65 + time * 0.03, p.xy);
    float2 xz = rot2(p.y * 0.35 - time * 0.02, float2(xy.x, p.z));
    float3 q = float3(xz.x, xy.y, xz.y);
    float swirl = fbm3(q * 1.6 + float3(0.0, time * 0.03, 0.0));
    float bands = sin(q.y * 8.0 + swirl * 4.2 + q.x * 2.1);
    float bands2 = sin(q.z * 5.4 - swirl * 3.1 + q.y * 1.4);
    float pattern = mix(bands, bands2, 0.3);
    return smoothstep(-0.28, 0.36, pattern);
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / resolution;
    float2 p = uv * 2.0 - 1.0;
    p.y = -p.y;
    p.x *= resolution.x / resolution.y;
    float r = length(p);
    if (r > 1.0) {
        return half4(0.0);
    }

    float z = sqrt(max(0.0, 1.0 - r * r));
    float3 position = float3(p.x, p.y, z);
    float3 n = normalize(float3(p.x, p.y, z));
    float3 cameraPos = float3(0.0, 0.08, 4.08);
    float3 viewDir = normalize(cameraPos - position);
    float3 sunToLight = normalize(float3(-0.45, 1.0, 0.72));
    float3 pointVector = float3(-1.6, 1.2, 2.6) - position;
    float pointDistance = length(pointVector);
    float3 pointToLight = pointVector / max(pointDistance, 1e-5);
    float pointAttenuation = pow(clamp(1.0 - pointDistance / 8.0, 0.0, 1.0), 1.8);

    float sunDiffuse = max(dot(n, sunToLight), 0.0);
    float pointDiffuse = max(dot(n, pointToLight), 0.0) * pointAttenuation;
    float specPower = mix(12.0, 68.0, 1.0 - roughness);
    float sunSpec = pow(max(dot(n, normalize(sunToLight + viewDir)), 0.0), specPower) * (0.75 + reflectionStrength * 0.4);
    float pointSpec = pow(max(dot(n, normalize(pointToLight + viewDir)), 0.0), specPower * 1.05) * pointAttenuation * 0.42;
    float specular = sunSpec + pointSpec;
    float fresnel = pow(1.0 - max(dot(n, viewDir), 0.0), mix(4.2, 2.0, reflectionStrength));
    float innerStrength = smoothstep(0.42, 0.78, translucency);

    float3 innerCoord = n;
    innerCoord.xy = rot2(0.45 + innerCoord.z * 0.7 + time * 0.04, innerCoord.xy);
    float2 innerXZ = rot2(innerCoord.y * 0.55 - time * 0.03, float2(innerCoord.x, innerCoord.z));
    innerCoord = float3(innerXZ.x, innerCoord.y, innerXZ.y);
    float depth = mix(1.8, 1.1, metallic) + translucency * 1.4;
    float3 refracted = innerCoord * depth + float3(time * 0.02, -time * 0.015, 0.0);
    float veins = marbleField(refracted * mix(2.8, 1.55, translucency));
    veins = mix(veins, marbleField(refracted.zxy * 0.72 + 4.2), 0.35);
    veins *= smoothstep(0.12, 0.98, z) * veinStrength;
    float surfacePattern = fbm3(float3(n.xy * mix(6.0, 13.0, metallic), n.z * 4.0) + float3(0.0, 0.0, time * 0.02));
    surfacePattern = smoothstep(0.56, 0.8, surfacePattern) * (veinStrength * 0.22 + metallic * 0.18);
    float brushed = sin((p.y * 92.0) + p.x * 18.0 + fbm3(float3(p * 8.0, time * 0.03)) * 4.0);
    brushed = smoothstep(0.72, 0.96, brushed) * metallic * 0.12;

    float reflectionBand = smoothstep(0.16, 0.0, abs(dot(p, normalize(float2(0.95, 0.31))) - 0.42));
    reflectionBand += smoothstep(0.18, 0.0, abs(dot(p, normalize(float2(0.9, -0.43))) + 0.8));
    reflectionBand *= reflectionStrength * backgroundEnabled * 0.35;
    float backgroundReflection = smoothstep(0.14, 0.0, abs(dot(p, normalize(float2(0.92, 0.38))) - 0.12));
    backgroundReflection *= backgroundEnabled * reflectionStrength * 0.26;

    float backGlow = smoothstep(-0.25, 0.85, z + translucency * 0.55 - p.x * 0.18 - p.y * 0.12) * innerStrength;
    float shellMask = smoothstep(0.0, 0.18 + innerStrength * 0.22, z);
    float absorption = exp(-(1.7 - translucency * 0.9) * (1.0 - z));
    float keyDiffuse = dot(n, normalize(keyLightDirection));
    float lightMix = clamp(0.03 + sunDiffuse * 0.82 + pointDiffuse * 0.28, 0.0, 1.0);
    float shadowTerm = smoothstep(0.24, -0.18, keyDiffuse);
    float ambient = 0.02 + z * 0.025;

    float2 primaryDelta = p - primaryHighlightCenter;
    float2 secondaryDelta = p - secondaryHighlightCenter;
    float primaryGlint = exp(-dot(primaryDelta, primaryDelta) / mix(0.011, 0.0048, 1.0 - roughness));
    float secondaryGlint = exp(-dot(secondaryDelta, secondaryDelta) / mix(0.0032, 0.0016, 1.0 - roughness));
    float glintMask = primaryGlint * (0.72 + reflectionStrength * 0.34) + secondaryGlint * 0.9;

    half3 shellColor = mix(shadowColor.rgb, baseColor.rgb, half(lightMix));
    shellColor = mix(shellColor, shadowColor.rgb, half(shadowTerm * 0.84));
    half3 coreColor = mix(baseColor.rgb, veinColor.rgb, half(veins));
    coreColor = mix(coreColor, highlightColor.rgb, half((1.0 - absorption) * innerStrength * 0.22));
    coreColor = mix(coreColor, reflectionColor.rgb, half(reflectionBand * 0.12));
    half3 diffuseColor = mix(shellColor, coreColor, half((innerStrength * 0.42) * shellMask));
    diffuseColor = mix(diffuseColor, veinColor.rgb, half(surfacePattern + brushed));
    diffuseColor = mix(diffuseColor, highlightColor.rgb, half(backGlow * 0.12));
    diffuseColor = mix(shadowColor.rgb, diffuseColor, half(clamp(ambient + lightMix, 0.0, 1.0)));

    half3 finalColor = diffuseColor;
    finalColor = mix(finalColor, reflectionColor.rgb, half(specular * mix(0.35, 0.82, metallic)));
    finalColor = mix(finalColor, highlightColor.rgb, half(specular * 0.28 + glintMask + reflectionBand * 0.12 + backgroundReflection));
    finalColor = mix(finalColor, rimColor.rgb, half(fresnel * (0.24 + translucency * 0.34 + metallic * 0.18)));

    return half4(finalColor, 1.0);
}
"""

private fun createVeinPath(
    width: Float,
    height: Float,
    seed: Float,
    bend: Float,
    thickness: Float,
): Path {
    val path = Path()
    val startX = width * (0.08f + seed * 0.04f)
    val startY = height * (0.18f + thickness * 0.2f)
    val midX = width * (0.46f + bend)
    val midY = height * (0.42f + seed * 0.03f)
    val endX = width * (0.82f - seed * 0.03f)
    val endY = height * (0.74f - thickness * 0.4f)
    path.moveTo(startX, startY)
    path.cubicTo(
        width * (0.18f + bend * 0.25f),
        height * (0.28f + seed * 0.04f),
        width * (0.28f + bend * 0.9f),
        height * (0.56f - seed * 0.05f),
        midX,
        midY,
    )
    path.cubicTo(
        width * (0.58f - bend * 0.4f),
        height * (0.32f + seed * 0.04f),
        width * (0.68f + bend * 0.3f),
        height * (0.66f + thickness * 0.3f),
        endX,
        endY,
    )
    return path
}
