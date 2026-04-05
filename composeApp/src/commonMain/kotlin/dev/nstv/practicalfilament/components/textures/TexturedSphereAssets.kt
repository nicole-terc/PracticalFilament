package dev.nstv.practicalfilament.components.textures

import dev.nstv.practicalfilament.filament.FilamentEngine
import practicalfilament.composeapp.generated.resources.Res

internal data class TextureBinding(
    val parameterName: String,
    val texturePath: String,
    val label: String,
)

internal data class TexturedSphereMaterial(
    val label: String,
    val materialPath: String,
    val textureBindings: List<TextureBinding>,
)

internal data class LoadedTexturedSphereMaterial(
    val instanceHandle: Int,
    val textureHandles: Map<String, Int>,
    val notice: String? = null,
)

internal data class CreatedTexturedSphere(
    val renderableHandle: Int,
    val materialInstanceHandle: Int,
    val textureHandles: Map<String, Int>,
    val notice: String? = null,
)

internal fun createTexturedSphere(
    engine: FilamentEngine,
    material: TexturedSphereMaterial,
    existingTextureHandles: Map<String, Int>,
    radius: Float = 1f,
): CreatedTexturedSphere {
    val loadedMaterial = loadTexturedSphereMaterial(
        engine = engine,
        material = material,
        existingTextureHandles = existingTextureHandles,
    )
    if (loadedMaterial.instanceHandle <= 0) {
        return CreatedTexturedSphere(
            renderableHandle = -1,
            materialInstanceHandle = -1,
            textureHandles = loadedMaterial.textureHandles,
            notice = loadedMaterial.notice,
        )
    }

    val renderableHandle = engine.createSphereRenderable(
        materialInstanceHandle = loadedMaterial.instanceHandle,
        radius = radius,
    )
    return CreatedTexturedSphere(
        renderableHandle = renderableHandle,
        materialInstanceHandle = loadedMaterial.instanceHandle,
        textureHandles = loadedMaterial.textureHandles,
        notice = if (renderableHandle > 0) {
            loadedMaterial.notice
        } else {
            "The textured sphere could not be created on this platform."
        },
    )
}

internal fun loadTexturedSphereMaterial(
    engine: FilamentEngine,
    material: TexturedSphereMaterial,
    existingTextureHandles: Map<String, Int>,
): LoadedTexturedSphereMaterial {
    val materialHandle = engine.loadMaterial(Res.getUri(material.materialPath))
    if (materialHandle <= 0) {
        return LoadedTexturedSphereMaterial(
            instanceHandle = -1,
            textureHandles = existingTextureHandles,
            notice = "The textured material could not be loaded.",
        )
    }

    val instanceHandle = engine.createMaterialInstance(materialHandle)
    if (instanceHandle <= 0) {
        return LoadedTexturedSphereMaterial(
            instanceHandle = -1,
            textureHandles = existingTextureHandles,
            notice = "The textured material instance could not be created.",
        )
    }

    var updatedTextureHandles = existingTextureHandles
    for (binding in material.textureBindings) {
        val textureHandle = updatedTextureHandles[binding.texturePath] ?: engine
            .loadTexture(Res.getUri(binding.texturePath))
            .takeIf { it > 0 }
            ?.also { loadedHandle ->
                updatedTextureHandles = updatedTextureHandles + (binding.texturePath to loadedHandle)
            }
            ?: return LoadedTexturedSphereMaterial(
                instanceHandle = -1,
                textureHandles = updatedTextureHandles,
                notice = "Texture loading failed for ${binding.label}.",
            )
        engine.setTextureParameter(instanceHandle, binding.parameterName, textureHandle)
    }

    return LoadedTexturedSphereMaterial(
        instanceHandle = instanceHandle,
        textureHandles = updatedTextureHandles,
    )
}
