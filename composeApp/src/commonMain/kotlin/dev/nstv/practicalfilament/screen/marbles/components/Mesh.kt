package dev.nstv.practicalfilament.screen.marbles.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.nstv.practicalfilament.theme.components.DropDownWithArrows


val MeshList = mapOf(
    "sphere" to "files/models/sphere.filamesh",
    "monkey" to "files/models/monkey.filamesh",
)


@Composable
fun MeshSelectionField(
    modifier: Modifier = Modifier,
    meshList: Map<String, String> = MeshList,
    selectedMeshIndex: Int = 0,
    onMeshSelectionChanged: (String) -> Unit,
){
    DropDownWithArrows(
        label = "Mesh",
        options = meshList.keys.toList(),
        onSelectionChanged = {
            onMeshSelectionChanged(meshList.values.toList()[it])
        },
        modifier = modifier,
        selectedIndex = selectedMeshIndex,
    )
}
