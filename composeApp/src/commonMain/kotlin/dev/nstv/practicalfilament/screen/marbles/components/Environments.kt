package dev.nstv.practicalfilament.screen.marbles.components


internal data class EnvironmentOption(
    val label: String,
    val iblPath: String? = null,
    val skyboxPath: String? = null,
)

internal data class LoadedEnvironment(
    val indirectLightHandle: Int,
    val skyboxHandle: Int,
)

internal val MarbleTextureBackgrounds = listOf(
    EnvironmentOption(label = "None"),
    environmentOption("flower_road_2k"),
    environmentOption("flower_road_no_sun_2k"),
    environmentOption("graffiti_shelter_2k"),
    environmentOption("lightroom_14b"),
    environmentOption("noon_grass_2k"),
    environmentOption("parking_garage_2k"),
    environmentOption("pillars_2k"),
    environmentOption("studio_small_02_2k"),
    environmentOption("syferfontein_18d_clear_2k"),
    environmentOption("the_sky_is_on_fire_2k"),
    environmentOption("venetian_crossroads_2k"),
)

internal fun environmentOption(name: String): EnvironmentOption {
    return EnvironmentOption(
        label = name,
        iblPath = "files/envs/$name/${name}_ibl.ktx",
        skyboxPath = "files/envs/$name/${name}_skybox.ktx",
    )
}
