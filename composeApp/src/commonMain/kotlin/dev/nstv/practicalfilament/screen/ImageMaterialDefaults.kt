package dev.nstv.practicalfilament.screen

import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.material.MaterialParameter
import dev.nstv.practicalfilament.filament.material.MaterialParameterDefinition
import dev.nstv.practicalfilament.filament.material.identityMat3

internal fun FilamentEngine.applyImageMaterialDefaults(
    instanceHandle: Int,
    definitions: List<MaterialParameterDefinition>,
    backgroundColor: Float3 = Float3(0f, 0f, 0f),
) {
    if (definitions.any { it.name == "transform" }) {
        setMaterialParameter(instanceHandle, MaterialParameter("transform", identityMat3()))
    }
    if (definitions.any { it.name == "backgroundColor" }) {
        setMaterialParameter(instanceHandle, MaterialParameter("backgroundColor", backgroundColor))
    }
    if (definitions.any { it.name == "showImage" }) {
        setMaterialParameter(instanceHandle, MaterialParameter("showImage", 1))
    }
}
