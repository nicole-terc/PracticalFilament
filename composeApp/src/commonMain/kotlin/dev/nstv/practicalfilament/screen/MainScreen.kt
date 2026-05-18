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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.nstv.practicalfilament.screen.marbles.MarbleFilameshScreen
import dev.nstv.practicalfilament.screen.marbles.MarbleLightScreen
import dev.nstv.practicalfilament.screen.marbles.MarbleScreen
import dev.nstv.practicalfilament.screen.marbles.comparison.GraphicsEffectsComparisonScreen
import dev.nstv.practicalfilament.screen.filament.steps.FilamentStepsScreen
import dev.nstv.practicalfilament.screen.marbles.steps.MarbleStepsScreen
import dev.nstv.practicalfilament.screen.otherViewers.GltfViewerScreen
import dev.nstv.practicalfilament.screen.otherViewers.FilamentUiCardScreen
import dev.nstv.practicalfilament.screen.otherViewers.MaterialViewerScreen
import dev.nstv.practicalfilament.screen.otherViewers.MorphingScreen
import dev.nstv.practicalfilament.screen.otherViewers.RedballScreen
import dev.nstv.practicalfilament.screen.otherViewers.SheepScreen
import dev.nstv.practicalfilament.screen.otherViewers.sheep2.SheepHerdRunScreen
import dev.nstv.practicalfilament.screen.otherViewers.sheep2.SheepScreen2
import dev.nstv.practicalfilament.screen.particles.ParticleWordScreen
import dev.nstv.practicalfilament.screen.samples.HelloTriangleScreen
import dev.nstv.practicalfilament.screen.samples.IBLScreen
import dev.nstv.practicalfilament.screen.samples.LitCubeScreen
import dev.nstv.practicalfilament.screen.samples.MaterialBuilderScreen
import dev.nstv.practicalfilament.screen.samples.MultiViewScreen
import dev.nstv.practicalfilament.screen.samples.PageCurlScreen
import dev.nstv.practicalfilament.screen.samples.StressTestScreen
import dev.nstv.practicalfilament.screen.samples.TexturedObjectScreen
import dev.nstv.practicalfilament.screen.samples.TransparentViewScreen
import dev.nstv.practicalfilament.screen.scenes.sky.SkyScreen
import dev.nstv.practicalfilament.screen.scenes.water.WaterScreen
import dev.nstv.practicalfilament.screen.wallpaper.LiveWallpaperScreen
import dev.nstv.practicalfilament.theme.Grid
import dev.nstv.practicalfilament.theme.components.DropDownWithArrows
import dev.nstv.practicalfilament.theme.slidesBackground


private enum class Screen {
    MATERIAL_VIEWER,
    MARBLE_VIEWER,
    MARBLE_FILAMESH,
    MARBLE_LIGHT,
    MARBLE_STEPS,
    FILAMENT_STEPS,
    MARBLE_COMPARISON,
    WATER,
    REDBALL,
    SHEEP,
    SHEEP_2,
    SHEEP_HERD_RUN,
    MORPHING,
    PARTICLE_WORD,
    SKY,
    GLTF_VIEWER,
    FILAMENT_UI_CARD,
    SAMPLE_LIVE_WALLPAPER,
    SAMPLE_PAGE_CURL,
    SAMPLE_STRESS_TEST,
    SAMPLE_HELLO_TRIANGLE,
    SAMPLE_TRANSPARENT_VIEW,
    SAMPLE_TEXTURED_OBJECT,
    SAMPLE_MATERIAL_BUILDER,
    SAMPLE_MULTI_VIEW,
    SAMPLE_IBL,
    SAMPLE_LIT_CUBE,
}

const val UseSlidesBackground = false
const val HideOptions = false
const val HideTopDropDown = false
const val HideSubDropDowns = false
const val SheepIt = false

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = if (UseSlidesBackground) slidesBackground else MaterialTheme.colorScheme.background,
    ) {
        var selectedScreen by rememberSaveable { mutableStateOf(Screen.SHEEP_HERD_RUN) }

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
                hidden = HideTopDropDown,
            )
            HorizontalDivider()
            Crossfade(
                targetState = selectedScreen,
                animationSpec = tween(durationMillis = 500)
            ) { screen ->
                when (screen) {
                    Screen.WATER -> WaterScreen()
                    Screen.MATERIAL_VIEWER -> MaterialViewerScreen()
                    Screen.MARBLE_VIEWER -> MarbleScreen()
                    Screen.MARBLE_STEPS -> MarbleStepsScreen()
                    Screen.FILAMENT_STEPS -> FilamentStepsScreen()
                    Screen.MARBLE_FILAMESH -> MarbleFilameshScreen()
                    Screen.MARBLE_LIGHT -> MarbleLightScreen()
                    Screen.MARBLE_COMPARISON -> GraphicsEffectsComparisonScreen()
                    Screen.REDBALL -> RedballScreen()
                    Screen.SHEEP -> SheepScreen()
                    Screen.SHEEP_2 -> SheepScreen2()
                    Screen.SHEEP_HERD_RUN -> SheepHerdRunScreen()
                    Screen.MORPHING -> MorphingScreen()
                    Screen.PARTICLE_WORD -> ParticleWordScreen()
                    Screen.GLTF_VIEWER -> GltfViewerScreen()
                    Screen.FILAMENT_UI_CARD -> FilamentUiCardScreen()
                    Screen.SKY -> SkyScreen()
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
                }
            }
        }
    }
}
