package dev.nstv.practicalfilament.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.nstv.practicalfilament.components.ParameterInputField
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.Color
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.LightConfig
import dev.nstv.practicalfilament.filament.LightType
import dev.nstv.practicalfilament.filament.MaterialParameter
import dev.nstv.practicalfilament.filament.MaterialParameterDefinition
import dev.nstv.practicalfilament.filament.defaultMaterialParameter
import practicalfilament.composeapp.generated.resources.Res

@Composable
fun MaterialViewerScreen(
    modifier: Modifier = Modifier,
) {
    var filamentEngine by remember {
        mutableStateOf<dev.nstv.practicalfilament.filament.FilamentEngine?>(
            null
        )
    }
    var materialInstanceHandle by remember { mutableIntStateOf(0) }
    var materialParameterDefinitions by remember {
        mutableStateOf<List<MaterialParameterDefinition>>(
            emptyList()
        )
    }
    var materialParameters by remember { mutableStateOf<Map<String, MaterialParameter>>(emptyMap()) }

    LaunchedEffect(
        filamentEngine,
        materialInstanceHandle,
        materialParameters,
    ) {
        val currentEngine = filamentEngine ?: return@LaunchedEffect
        if (materialInstanceHandle == 0) return@LaunchedEffect

        materialParameters.values.forEach { parameter ->
            currentEngine.setMaterialParameter(materialInstanceHandle, parameter)
        }
        currentEngine.requestFrame()
    }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            FilamentView(
                modifier = Modifier.fillMaxSize(),
                camera = CameraConfig(
                    position = Float3(0f, 0f, 4f),
                    lookAt = Float3(0f, 0f, 0f),
                ),
                lights = listOf(
                    LightConfig(
                        type = LightType.DIRECTIONAL,
                        color = Color(1f, 1f, 1f),
                        intensity = 110_000f,
                        direction = Float3(0f, -1f, -1f),
                    ),
                    LightConfig(
                        type = LightType.POINT,
                        color = Color(1f, 0.9f, 0.8f),
                        intensity = 50_000f,
                        position = Float3(2f, 2f, 2f),
                    ),
                ),
                onEngineReady = { engine ->
                    val materialHandle =
                        engine.loadMaterial(Res.getUri("files/sandboxLitFade.filamat"))
                    val definitions = engine.getMaterialParameters(materialHandle)
                    val instanceHandle = engine.createMaterialInstance(materialHandle)
                    filamentEngine = engine
                    materialParameterDefinitions = definitions
                    materialParameters = definitions.associate { definition ->
                        definition.name to defaultMaterialParameter(definition)
                    }
                    materialInstanceHandle = instanceHandle
                    engine.createPlaneRenderable(instanceHandle)
                },
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            materialParameterDefinitions.forEach { definition ->
                val parameter = materialParameters[definition.name] ?: return@forEach
                ParameterInputField(
                    name = definition.name,
                    type = definition.type,
                    value = parameter.value,
                ) { updatedValue ->
                    materialParameters = materialParameters + (
                            definition.name to MaterialParameter(definition.name, updatedValue)
                            )
                }
            }
        }
    }
}
