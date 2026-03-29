package dev.nstv.practicalfilament.screen

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color.Companion
import dev.nstv.practicalfilament.theme.Grid
import dev.nstv.practicalfilament.theme.components.DropDownWithArrows
import dev.nstv.practicalfilament.theme.slidesBackground


private enum class Screen {
    MATERIAL_VIEWER,
}

const val musicFileName = "nicmix.wav"
const val musicFilePath = "files/$musicFileName"
const val UseSlidesBackground = false
const val HideOptions = false
const val SheepIt = false

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = if (UseSlidesBackground) slidesBackground else MaterialTheme.colorScheme.background,
    ) {
        var selectedScreen by remember { mutableStateOf(Screen.MATERIAL_VIEWER) }

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
                    Screen.MATERIAL_VIEWER -> MaterialViewerScreen()
                }
            }
        }
    }
}
