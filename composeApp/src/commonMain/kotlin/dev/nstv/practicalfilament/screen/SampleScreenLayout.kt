package dev.nstv.practicalfilament.screen

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
import dev.nstv.practicalfilament.theme.Grid

@Composable
internal fun SampleScreenLayout(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
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
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(Grid.Two),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                modifier = Modifier.padding(top = Grid.Half, bottom = Grid.One),
                text = description,
                style = MaterialTheme.typography.bodyMedium,
            )
            controls()
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
