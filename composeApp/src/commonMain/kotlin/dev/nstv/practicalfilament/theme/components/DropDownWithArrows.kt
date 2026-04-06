package dev.nstv.practicalfilament.theme.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize.Max
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import dev.nstv.practicalfilament.screen.HideOptions
import org.jetbrains.compose.resources.vectorResource
import practicalfilament.composeapp.generated.resources.Res
import practicalfilament.composeapp.generated.resources.ic_arrow_left
import practicalfilament.composeapp.generated.resources.ic_arrow_right
import kotlin.math.max
import kotlin.math.min


private enum class Direction {
    TO_LEFT, TO_RIGHT, FROM_DROPDOWN
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DropDownWithArrows(
    options: List<String>,
    onSelectionChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    selectedIndex: Int = 0,
    label: String? = null,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    loopSelection: Boolean = true,
) {
    if (!HideOptions) {
        var expanded by remember { mutableStateOf(false) }
        var selectedItemIndex by remember { mutableStateOf(selectedIndex) }
        var direction by remember { mutableStateOf(Direction.FROM_DROPDOWN) }

        LaunchedEffect(selectedIndex, options) {
            if (options.isNotEmpty()) {
                selectedItemIndex = selectedIndex.coerceIn(0, options.lastIndex)
            }
        }

        Box(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentSize(Alignment.TopStart)
        ) {
            Row(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.height(Max)) {
                if (label != null) {
                    Text(
                        modifier = Modifier.weight(2f),
                        style = textStyle,
                        text = label
                    )
                }
                Icon(
                    vectorResource(Res.drawable.ic_arrow_left),
                    contentDescription = "Previous Option",
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = {
                            direction = Direction.TO_LEFT
                            selectedItemIndex = if (loopSelection && selectedItemIndex == 0) {
                                options.size - 1
                            } else {
                                max(selectedItemIndex - 1, 0)
                            }
                            onSelectionChanged(selectedItemIndex)
                        })
                )
                AnimatedContent(
                    modifier = Modifier.weight(4f),
                    targetState = selectedItemIndex,
                    transitionSpec = {
                        when (direction) {
                            Direction.FROM_DROPDOWN -> slideInVertically { height -> height } + fadeIn() togetherWith
                                    slideOutVertically { height -> -height } + fadeOut()

                            Direction.TO_LEFT -> slideInHorizontally { width -> -width / 2 } + fadeIn() togetherWith
                                    slideOutHorizontally { width -> width / 2 } + fadeOut()

                            Direction.TO_RIGHT -> slideInHorizontally { width -> width / 2 } + fadeIn() togetherWith
                                    slideOutHorizontally { width -> -width / 2 } + fadeOut()
                        }.using(
                            SizeTransform(clip = false)
                        )
                    }
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = options[selectedItemIndex],
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                }
                Icon(
                    vectorResource(Res.drawable.ic_arrow_right),
                    contentDescription = "Next Option",
                    Modifier
                        .weight(1f)
                        .clickable(onClick = {
                            direction = Direction.TO_RIGHT

                            selectedItemIndex =
                                if (loopSelection && selectedItemIndex == options.size - 1) {
                                    0
                                } else {
                                    min(selectedItemIndex + 1, options.size - 1)
                                }

                            onSelectionChanged(selectedItemIndex)
                        })
                )
            }
            DropdownMenu(
                modifier = Modifier.fillMaxWidth(),
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        onClick = {
                            expanded = false
                            direction = Direction.FROM_DROPDOWN
                            selectedItemIndex = options.indexOf(option)
                            onSelectionChanged(options.indexOf(option))
                        },
                        text = {
                            Text(
                                text = option,
                                textAlign = TextAlign.Center,
                            )
                        }
                    )
                }
            }
        }
    }
}
