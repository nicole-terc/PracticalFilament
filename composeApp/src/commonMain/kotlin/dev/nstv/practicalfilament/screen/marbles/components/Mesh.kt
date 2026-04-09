package dev.nstv.practicalfilament.screen.marbles.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.nstv.practicalfilament.theme.Grid
import dev.nstv.practicalfilament.theme.components.DropDownWithArrows
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding

data class Mesh(
    val name: String,
    val path: String,
    val scale: Float = 1f,
)

val SphereMesh = Mesh(
    name = "Sphere",
    path = "files/models/sphere.filamesh",
    scale = 2f,
)

val MonkeyMesh = Mesh(
    name = "Monkey",
    path = "files/models/monkey.filamesh",
)


val StreetRatMesh = Mesh(
    name = "Street Rat",
    path = "files/models/street_rat.filamesh",
    scale = 50f,
)

val MeshList = listOf(
    SphereMesh,
    MonkeyMesh,
    StreetRatMesh,
)


@Composable
fun MeshSelectionField(
    modifier: Modifier = Modifier,
    meshList: List<Mesh> = MeshList,
    selectedMesh: Mesh = meshList.first(),
    onMeshSelectionChanged: (Mesh) -> Unit,
){
    var selectedMeshIndex by remember(selectedMesh, meshList) {
        mutableIntStateOf(meshList.indexOf(selectedMesh).coerceAtLeast(0))
    }

    DropDownWithArrows(
        label = "Mesh",
        options = meshList.map { it.name },
        onSelectionChanged = {
            selectedMeshIndex = it
            onMeshSelectionChanged(meshList[it])
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = Grid.One),
        selectedIndex = selectedMeshIndex,
    )
}
