@file:OptIn(ExperimentalUnsignedTypes::class)

package dev.nstv.practicalfilament

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.nstv.practicalfilament.screen.MainScreen
import dev.nstv.practicalfilament.theme.PracticalFilamentTheme

@Composable
@Preview
fun App() {
    PracticalFilamentTheme {
        MainScreen()
    }
}
