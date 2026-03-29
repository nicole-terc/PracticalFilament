package dev.nstv.practicalfilament.filament.material

import dev.nstv.practicalfilament.filament.FilamentEngine
import practicalfilament.composeapp.generated.resources.Res

data class Material(
    val fileName: String,
    val label: String,
    val description: String,
    val overrides: Map<String, Any>,
)

fun loadMaterialOnEngine(
    engine: FilamentEngine,
    fileName: String,
): Triple<Int, List<MaterialParameterDefinition>, Map<String, MaterialParameter>> {
    val materialHandle = engine.loadMaterial(Res.getUri("files/$fileName"))
    val definitions = engine.getMaterialParameters(materialHandle)
    val instanceHandle = engine.createMaterialInstance(materialHandle)
    val parameters = definitions.associate { definition ->
        definition.name to defaultMaterialParameter(definition)
    }
    return Triple(instanceHandle, definitions, parameters)
}

fun loadMaterialOnEngine(
    engine: FilamentEngine,
    material: Material,
): Triple<Int, List<MaterialParameterDefinition>, Map<String, MaterialParameter>> {
    val materialHandle = engine.loadMaterial(Res.getUri("files/${material.fileName}"))
    val definitions = engine.getMaterialParameters(materialHandle)
    val instanceHandle = engine.createMaterialInstance(materialHandle)
    return Triple(instanceHandle, definitions, buildMaterialParameters(definitions, material))
}

private fun buildMaterialParameters(
    definitions: List<MaterialParameterDefinition>,
    material: Material,
): Map<String, MaterialParameter> {
    return definitions.associate { definition ->
        val defaultValue = when {
            definition.name in material.overrides -> material.overrides.getValue(definition.name)
            definition.type is MaterialParameterType.Sampler2d ||
                    definition.type is MaterialParameterType.Sampler2dArray ||
                    definition.type is MaterialParameterType.SamplerCubemap ||
                    definition.type is MaterialParameterType.SamplerExternal -> BuiltInTexture.CHECKERBOARD

            else -> defaultMaterialParameter(definition).value
        }
        definition.name to MaterialParameter(definition.name, defaultValue)
    }
}
