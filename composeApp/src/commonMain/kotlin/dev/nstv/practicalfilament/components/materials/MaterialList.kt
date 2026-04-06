package dev.nstv.practicalfilament.components.materials

import dev.nstv.practicalfilament.screen.marbles.MarbleCeramicMaterial
import dev.nstv.practicalfilament.screen.marbles.MarbleClayMaterial
import dev.nstv.practicalfilament.screen.marbles.MarbleGlassMaterial
import dev.nstv.practicalfilament.screen.marbles.MarbleMetalMaterial
import dev.nstv.practicalfilament.screen.marbles.MarbleStoneMaterial
import dev.nstv.practicalfilament.screen.marbles.MarbleVelvetMaterial

val MaterialOverridesList = listOf(
    MarbleClayMaterial,
    MarbleGlassMaterial,
    MarbleStoneMaterial,
    MarbleMetalMaterial,
    MarbleCeramicMaterial,
    MarbleVelvetMaterial,
    redballMaterial(),
    aiDefaultMatMaterial(),
    texturedSampleMaterial(),
    sandboxLitFadeMaterial(),
    sandboxClothMaterial(),
    mirrorMaterial(),
    sheepFluffMaterial(),
    sheepBodyMaterial(),
    brownMudLeavesMaterial(),
)

val MaterialFilesList = MaterialOverridesList
