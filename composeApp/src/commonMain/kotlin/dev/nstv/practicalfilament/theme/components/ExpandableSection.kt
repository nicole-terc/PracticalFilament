package dev.nstv.practicalfilament.theme.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
import dev.nstv.practicalfilament.theme.Grid
import org.jetbrains.compose.resources.vectorResource
import practicalfilament.composeapp.generated.resources.Res
import practicalfilament.composeapp.generated.resources.ic_arrow_right

@Composable
fun ExpandableSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = Grid.One),
        ) {
            Icon(
                vectorResource(Res.drawable.ic_arrow_right),
                contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                modifier = Modifier
                    .graphicsLayer {
                        rotationZ = if (expanded) 90f else 0f
                    },
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 18.sp,
            )
        }
        AnimatedVisibility(expanded) {
            Column(Modifier.padding(start = Grid.Two)) {
                content()
            }
        }
    }
}
