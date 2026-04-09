package dev.nstv.practicalfilament.theme.components

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.nstv.practicalfilament.screen.HideOptions
import dev.nstv.practicalfilament.theme.Grid

@Composable
internal fun SampleScreenLayout(
    title: String,
    modifier: Modifier = Modifier,
    showControls: Boolean = !HideOptions,
    showControlsTitle: Boolean = true,
    view: @Composable () -> Unit,
    controls: @Composable () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            view()
        }
        if (showControls) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(Grid.Two),
                verticalArrangement = spacedBy(Grid.One),
            ) {
                if (showControlsTitle) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
                controls()
            }
        }
    }
}

@Composable
internal fun SampleNotice(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier.fillMaxWidth().padding(top = Grid.One),
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}
