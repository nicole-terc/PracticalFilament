package dev.nstv.practicalfilament.screen.comparison

import androidx.compose.ui.graphics.Color as ComposeColor
import dev.nstv.practicalfilament.filament.material.Material
import dev.nstv.practicalfilament.screen.marbles.MarbleCeramicMaterial
import dev.nstv.practicalfilament.screen.marbles.MarbleGlassMaterial
import dev.nstv.practicalfilament.screen.marbles.MarbleMetalMaterial

internal enum class ComparisonPresentationMode(
    val label: String,
) {
    Split("Split"),
    Reveal("Reveal"),
}

internal enum class Android2DEffectMode(
    val label: String,
    val caption: String,
) {
    AGSL(
        label = "AGSL",
        caption = "Animated shader on flat pixels. The highlight is authored manually.",
    ),
    RenderEffect(
        label = "RenderEffect",
        caption = "Blurred and layered 2D content. Good polish, but no real surface response.",
    ),
}

internal enum class ComparisonMaterialPreset(
    val label: String,
) {
    Ceramic("Ceramic"),
    Glass("Glass"),
    Metal("Metal"),
}

internal data class ComparisonPresetSpec(
    val preset: ComparisonMaterialPreset,
    val filamentMaterial: Material,
    val baseColor: ComposeColor,
    val shadowColor: ComposeColor,
    val highlightColor: ComposeColor,
    val rimColor: ComposeColor,
    val veinColor: ComposeColor,
    val reflectionColor: ComposeColor,
    val roughness: Float,
    val metallic: Float,
    val translucency: Float,
    val veinStrength: Float,
    val reflectionStrength: Float,
)

internal val ComparisonPresetSpecs = listOf(
    ComparisonPresetSpec(
        preset = ComparisonMaterialPreset.Ceramic,
        filamentMaterial = MarbleCeramicMaterial,
        baseColor = ComposeColor(red = 0.22f, green = 0.45f, blue = 0.72f, alpha = 1f),
        shadowColor = ComposeColor(red = 0.09f, green = 0.19f, blue = 0.33f, alpha = 1f),
        highlightColor = ComposeColor(red = 0.94f, green = 0.97f, blue = 1f, alpha = 1f),
        rimColor = ComposeColor(red = 0.78f, green = 0.86f, blue = 0.98f, alpha = 1f),
        veinColor = ComposeColor(red = 0.42f, green = 0.58f, blue = 0.8f, alpha = 1f),
        reflectionColor = ComposeColor(red = 0.84f, green = 0.9f, blue = 1f, alpha = 1f),
        roughness = 0.42f,
        metallic = 0f,
        translucency = 0.1f,
        veinStrength = 0.22f,
        reflectionStrength = 0.36f,
    ),
    ComparisonPresetSpec(
        preset = ComparisonMaterialPreset.Glass,
        filamentMaterial = MarbleGlassMaterial,
        baseColor = ComposeColor(red = 0.92f, green = 0.96f, blue = 1f, alpha = 1f),
        shadowColor = ComposeColor(red = 0.51f, green = 0.58f, blue = 0.67f, alpha = 1f),
        highlightColor = ComposeColor(red = 1f, green = 1f, blue = 1f, alpha = 1f),
        rimColor = ComposeColor(red = 0.9f, green = 0.96f, blue = 1f, alpha = 1f),
        veinColor = ComposeColor(red = 0.96f, green = 0.98f, blue = 1f, alpha = 1f),
        reflectionColor = ComposeColor(red = 0.86f, green = 0.92f, blue = 1f, alpha = 1f),
        roughness = 0.06f,
        metallic = 0f,
        translucency = 0.68f,
        veinStrength = 0.08f,
        reflectionStrength = 0.84f,
    ),
    ComparisonPresetSpec(
        preset = ComparisonMaterialPreset.Metal,
        filamentMaterial = MarbleMetalMaterial,
        baseColor = ComposeColor(red = 0.04f, green = 0.04f, blue = 0.045f, alpha = 1f),
        shadowColor = ComposeColor(red = 0.005f, green = 0.005f, blue = 0.008f, alpha = 1f),
        highlightColor = ComposeColor(red = 0.97f, green = 0.98f, blue = 1f, alpha = 1f),
        rimColor = ComposeColor(red = 0.36f, green = 0.38f, blue = 0.42f, alpha = 1f),
        veinColor = ComposeColor(red = 0.18f, green = 0.18f, blue = 0.2f, alpha = 1f),
        reflectionColor = ComposeColor(red = 0.95f, green = 0.96f, blue = 0.99f, alpha = 1f),
        roughness = 0.11f,
        metallic = 1f,
        translucency = 0f,
        veinStrength = 0.02f,
        reflectionStrength = 0.92f,
    ),
)

internal fun ComparisonMaterialPreset.toSpec(): ComparisonPresetSpec {
    return ComparisonPresetSpecs.first { it.preset == this }
}
