package dev.nstv.practicalfilament.screen.marbles.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.nstv.practicalfilament.theme.components.DropDownWithArrows

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
    selectedMeshIndex: Int = 0,
    onMeshSelectionChanged: (Mesh) -> Unit,
){
    DropDownWithArrows(
        label = "Mesh",
        options = meshList.map { it.name },
        onSelectionChanged = {
            onMeshSelectionChanged(meshList[it])
        },
        modifier = modifier,
        selectedIndex = selectedMeshIndex,
    )
}
