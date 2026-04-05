package dev.nstv.practicalfilament.screen

import android.content.Context
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.os.Build
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal actual fun AndroidEffectsMarblePanel(
    preset: ComparisonPresetSpec,
    effectMode: Android2DEffectMode,
    animationTimeSeconds: Float,
    modifier: Modifier,
) {
    val agslSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    ComparisonPanelCard(
        modifier = modifier,
        title = "Android Effects",
        presetLabel = preset.preset.label,
        supportText = if (effectMode == Android2DEffectMode.AGSL && !agslSupported) {
            "AGSL requires Android 13+"
        } else {
            null
        },
    ) {
        when {
            effectMode == Android2DEffectMode.AGSL && agslSupported -> {
                AgslMarbleStage(
                    preset = preset,
                    animationTimeSeconds = animationTimeSeconds,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            else -> {
                RenderEffectMarbleStage(
                    preset = preset,
                    animationTimeSeconds = animationTimeSeconds,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun RenderEffectMarbleStage(
    preset: ComparisonPresetSpec,
    animationTimeSeconds: Float,
    modifier: Modifier = Modifier,
) {
    val sweep = sin(animationTimeSeconds * 0.72f)
    val drift = cos(animationTimeSeconds * 0.48f)
    val shimmer = sin(animationTimeSeconds * 1.1f)
    val reflectionAlpha = (0.18f + preset.reflectionStrength * 0.72f).coerceAtMost(0.92f)
    val rimAlpha = (0.14f + preset.translucency * 0.36f + preset.metallic * 0.08f).coerceAtMost(0.74f)
    val secondaryGlowAlpha = (0.08f + preset.translucency * 0.22f + preset.reflectionStrength * 0.16f)
        .coerceAtMost(0.46f)

    CircularComparisonStage(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension * 0.5f
            val lightCenter = Offset(
                x = size.width * (0.33f + drift * 0.045f),
                y = size.height * (0.29f + sweep * 0.03f),
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
                .offset(x = (40.dp + (sweep * 12f).dp), y = (34.dp + (drift * 8f).dp))
                .size(
                    width = (132.dp + (preset.reflectionStrength * 24f).dp),
                    height = (74.dp + (preset.translucency * 18f).dp),
                )
                .rotate(-18f + preset.translucency * 4f)
                .graphicsLayer {
                    renderEffect = BlurEffect(62f, 62f, TileMode.Decal)
                    alpha = reflectionAlpha
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
                .offset(x = (-12).dp, y = (18).dp)
                .size(124.dp, 164.dp)
                .rotate(14f)
                .graphicsLayer {
                    renderEffect = BlurEffect(70f, 70f, TileMode.Decal)
                    alpha = (0.1f + preset.reflectionStrength * 0.18f).coerceAtMost(0.28f)
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
    }
}

@Composable
private fun AgslMarbleStage(
    preset: ComparisonPresetSpec,
    animationTimeSeconds: Float,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val agslView = remember(context) { AgslMarbleView(context) }

    CircularComparisonStage(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { agslView },
            update = { view ->
                view.update(preset = preset, timeSeconds = animationTimeSeconds)
            },
        )
    }
}

private class AgslMarbleView(
    context: Context,
) : View(context) {
    private val shader = RuntimeShader(MarbleShaderSource)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = this@AgslMarbleView.shader
        isDither = true
    }

    private var preset: ComparisonPresetSpec = ComparisonMaterialPreset.Ceramic.toSpec()
    private var timeSeconds: Float = 0f

    fun update(
        preset: ComparisonPresetSpec,
        timeSeconds: Float,
    ) {
        this.preset = preset
        this.timeSeconds = timeSeconds
        invalidate()
    }

    override fun onDraw(canvas: AndroidCanvas) {
        super.onDraw(canvas)
        val width = width.toFloat().coerceAtLeast(1f)
        val height = height.toFloat().coerceAtLeast(1f)
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
        canvas.drawRect(0f, 0f, width, height, paint)
    }
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

float hash21(float2 p) {
    p = fract(p * float2(234.34, 435.345));
    p += dot(p, p + 34.23);
    return fract(p.x * p.y);
}

float noise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float a = hash21(i);
    float b = hash21(i + float2(1.0, 0.0));
    float c = hash21(i + float2(0.0, 1.0));
    float d = hash21(i + float2(1.0, 1.0));
    float2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

float fbm(float2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 4; ++i) {
        value += noise(p) * amplitude;
        p = p * 2.03 + float2(18.4, 12.7);
        amplitude *= 0.5;
    }
    return value;
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / resolution;
    float2 p = uv * 2.0 - 1.0;
    p.x *= resolution.x / resolution.y;
    float r = length(p);
    if (r > 1.0) {
        return half4(0.0);
    }

    float z = sqrt(max(0.0, 1.0 - r * r));
    float3 n = normalize(float3(p.x, p.y, z));
    float3 l = normalize(float3(-0.55 + sin(time * 0.45) * 0.1, -0.6, 0.8));
    float3 v = float3(0.0, 0.0, 1.0);
    float3 h = normalize(l + v);

    float diffuse = max(dot(n, l), 0.0);
    float specPower = mix(12.0, 68.0, 1.0 - roughness);
    float specular = pow(max(dot(n, h), 0.0), specPower) * mix(0.55, 1.15, reflectionStrength);
    float fresnel = pow(1.0 - max(dot(n, v), 0.0), mix(4.2, 2.0, reflectionStrength));

    float swirl = fbm(float2(atan(p.y, p.x) * 1.7 + time * 0.08, z * 3.8 - time * 0.04));
    float veins = smoothstep(0.54, 0.76, fbm(p * 5.2 + float2(swirl * 2.2, -time * 0.03)));
    veins *= smoothstep(0.1, 0.95, z) * veinStrength;

    float reflectionBand = smoothstep(0.22, 0.0, abs(dot(p, normalize(float2(0.96, 0.28))) - 0.2 + sin(time * 0.95) * 0.06));
    reflectionBand += smoothstep(0.28, 0.0, abs(dot(p, normalize(float2(0.85, -0.52))) + 0.58));
    reflectionBand *= reflectionStrength;

    float backGlow = smoothstep(-0.25, 0.85, z + translucency * 0.55 - p.x * 0.18 - p.y * 0.12) * translucency;
    float ambient = 0.22 + z * (0.16 + translucency * 0.2);

    half3 diffuseColor = mix(shadowColor.rgb, baseColor.rgb, half(clamp(ambient + diffuse * (0.66 + translucency * 0.08), 0.0, 1.0)));
    diffuseColor = mix(diffuseColor, veinColor.rgb, half(veins));
    diffuseColor = mix(diffuseColor, reflectionColor.rgb, half(reflectionBand * 0.36));
    diffuseColor = mix(diffuseColor, highlightColor.rgb, half(backGlow * 0.18));

    half3 finalColor = diffuseColor;
    finalColor = mix(finalColor, reflectionColor.rgb, half(specular * mix(0.35, 0.82, metallic)));
    finalColor = mix(finalColor, highlightColor.rgb, half(specular * (0.55 + translucency * 0.18) + reflectionBand * 0.18));
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
