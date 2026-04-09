package dev.nstv.practicalfilament.components.materials

import dev.nstv.practicalfilament.components.materials.textured.blueTileMaterial
import dev.nstv.practicalfilament.components.materials.textured.brownMudLeavesMaterial
import dev.nstv.practicalfilament.components.materials.textured.monkeyMaterial
import dev.nstv.practicalfilament.components.materials.textured.mossMaterial
import dev.nstv.practicalfilament.components.materials.textured.ratMaterial
import dev.nstv.practicalfilament.components.materials.textured.stripedCottonMaterial
import dev.nstv.practicalfilament.screen.marbles.components.MarbleCeramicMaterial
import dev.nstv.practicalfilament.screen.marbles.components.MarbleClayMaterial
import dev.nstv.practicalfilament.screen.marbles.components.MarbleGlassMaterial
import dev.nstv.practicalfilament.screen.marbles.components.MarbleMetalMaterial
import dev.nstv.practicalfilament.screen.marbles.components.MarbleStoneMaterial
import dev.nstv.practicalfilament.screen.marbles.components.MarbleVelvetMaterial

val MaterialOverridesList = listOf(
    MarbleClayMaterial,
    MarbleGlassMaterial,
    MarbleStoneMaterial,
    MarbleMetalMaterial,
    MarbleCeramicMaterial,
    MarbleVelvetMaterial,
    redballMaterial(),
    aiDefaultMatMaterial(),
//    texturedSampleMaterial(),
    sandboxLitFadeMaterial(),
    sandboxClothMaterial(),
    mirrorMaterial(),
    sheepFluffMaterial(),
    sheepBodyMaterial(),
)

val TextureMaterials = listOf(
    brownMudLeavesMaterial(),
    mossMaterial(),
    monkeyMaterial(),
    ratMaterial(),
    stripedCottonMaterial(),
    blueTileMaterial(),
)

val MaterialFilesList = MaterialOverridesList

val AllMaterialsList = MaterialFilesList + TextureMaterials
