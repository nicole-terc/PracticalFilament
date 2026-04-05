package dev.nstv.practicalfilament.screen

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.nstv.practicalfilament.theme.Grid
import dev.nstv.practicalfilament.theme.components.DropDownWithArrows
import dev.nstv.practicalfilament.theme.slidesBackground


private enum class Screen {
    SAMPLE_IBL,
    SAMPLE_LIT_CUBE,
    SAMPLE_STRESS_TEST,
    SAMPLE_HELLO_TRIANGLE,
    SAMPLE_TRANSPARENT_VIEW,
    SAMPLE_TEXTURED_OBJECT,
    SAMPLE_MATERIAL_BUILDER,
    SAMPLE_PAGE_CURL,
    SAMPLE_MULTI_VIEW,
    SAMPLE_LIVE_WALLPAPER,
    SAMPLE_SKY,
    SAMPLE_GLTF_VIEWER,
    MATERIAL_VIEWER,
    MARBLE,
    MARBLE_UI,
    GRAPHICS_EFFECTS_COMPARISON,
    REDBALL,
    SHEEP,
    MORPHING,
    PARTICLE_WORD,
}

const val UseSlidesBackground = false
const val HideOptions = false
const val SheepIt = false

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = if (UseSlidesBackground) slidesBackground else MaterialTheme.colorScheme.background,
    ) {
        var selectedScreen by remember { mutableStateOf(Screen.SAMPLE_SKY) }

        Column(
            modifier = Modifier
                .fillMaxSize().safeDrawingPadding()
        ) {
            DropDownWithArrows(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentSize(Alignment.TopStart)
                    .padding(Grid.One),
                options = Screen.entries.map { it.name }.toList(),
                selectedIndex = Screen.entries.indexOf(selectedScreen),
                onSelectionChanged = { selectedScreen = Screen.entries[it] },
                textStyle = MaterialTheme.typography.headlineSmall,
                loopSelection = true,
            )
            HorizontalDivider()
            Crossfade(
                targetState = selectedScreen,
                animationSpec = tween(durationMillis = 500)
            ) { screen ->
                when (screen) {
                    Screen.SAMPLE_IBL -> IBLScreen()
                    Screen.SAMPLE_LIT_CUBE -> LitCubeScreen()
                    Screen.SAMPLE_STRESS_TEST -> StressTestScreen()
                    Screen.SAMPLE_HELLO_TRIANGLE -> HelloTriangleScreen()
                    Screen.SAMPLE_TRANSPARENT_VIEW -> TransparentViewScreen()
                    Screen.SAMPLE_TEXTURED_OBJECT -> TexturedObjectScreen()
                    Screen.SAMPLE_MATERIAL_BUILDER -> MaterialBuilderScreen()
                    Screen.SAMPLE_PAGE_CURL -> PageCurlScreen()
                    Screen.SAMPLE_MULTI_VIEW -> MultiViewScreen()
                    Screen.SAMPLE_LIVE_WALLPAPER -> LiveWallpaperScreen()
                    Screen.SAMPLE_SKY -> SkyScreen()
                    Screen.SAMPLE_GLTF_VIEWER -> GltfViewerScreen()
                    Screen.MATERIAL_VIEWER -> MaterialViewerScreen()
                    Screen.MARBLE -> MarbleScreen()
                    Screen.MARBLE_UI -> MarbleUIScreen()
                    Screen.GRAPHICS_EFFECTS_COMPARISON -> GraphicsEffectsComparisonScreen()
                    Screen.REDBALL -> RedballScreen()
                    Screen.SHEEP -> SheepScreen()
                    Screen.MORPHING -> MorphingScreen()
                    Screen.PARTICLE_WORD -> ParticleWordScreen()
                }
            }
        }
    }
}
