package dev.nstv.practicalfilament.screen

data class SkyWallpaperConfig(
    val sunAzimuth: Float = 0f,
    val sunHeight: Float = 0f,
    val sunIntensity: Float = 100_000f,
    val sunRadius: Float = 1.2f,
    val sunLimbDarkening: Float = 0.5f,
    val sunDiskIntensityBoost: Float = 1f,
    val turbidity: Float = 2f,
    val rayleigh: Float = 1f,
    val mieCoefficient: Float = 1f,
    val ozone: Float = 0.25f,
    val mieG: Float = 0.8f,
    val cloudCoverage: Float = 0.4f,
    val cloudDensity: Float = 0.02f,
    val cloudVolumetrics: Boolean = false,
    val cloudHeightMeters: Float = 8000f,
    val cloudSpeed: Float = 50f,
    val cloudEvolutionSpeed: Float = 0.02f,
    val waterDerivativeTrick: Boolean = true,
    val waterStrength: Float = 50f,
    val waterSpeed: Float = 1f,
    val waterOctaves: Float = 4f,
    val aperture: Float = 16f,
    val shutterSpeed: Float = 125f,
    val iso: Float = 100f,
    val focalLength: Float = 24f,
    val moonEnabled: Boolean = true,
    val moonAzimuth: Float = 180f,
    val moonHeight: Float = 0.70710677f,
    val moonIntensity: Float = 6f,
    val moonRadius: Float = 1.2f,
    val milkyWayEnabled: Boolean = true,
    val milkyWayIntensity: Float = 1f,
    val milkyWaySaturation: Float = 1f,
    val milkyWayBlackPoint: Float = 0.07f,
    val milkyWaySiderealTime: Float = 0f,
    val milkyWayLatitude: Float = 34f,
    val starsEnabled: Boolean = true,
    val starDensity: Float = 0.001f,
    val starIntensityExponent: Float = 0f,
    val msRayleigh: Float = 0.1f,
    val msMie: Float = 0.5f,
    val horizonGlow: Float = 0f,
    val contrast: Float = 1f,
    val shimmerStrength: Float = 0f,
    val shimmerFrequency: Float = 20f,
    val shimmerMaskHeight: Float = 0.1f,
    val syncEnabled: Boolean = false,
    val syncManualOverride: Boolean = false,
    val manualTimeHours: Float = 12f,
    val syncMoonPosition: Boolean = true,
    val syncDeviceLocation: Boolean = false,
    val syncLatitude: Float = 34f,
    val syncLongitude: Float = 0f,
) {
    fun serialize(): String = buildList {
        add(SerializationVersion.toString())
        add(sunAzimuth.toString())
        add(sunHeight.toString())
        add(sunIntensity.toString())
        add(sunRadius.toString())
        add(sunLimbDarkening.toString())
        add(sunDiskIntensityBoost.toString())
        add(turbidity.toString())
        add(rayleigh.toString())
        add(mieCoefficient.toString())
        add(ozone.toString())
        add(mieG.toString())
        add(cloudCoverage.toString())
        add(cloudDensity.toString())
        add(if (cloudVolumetrics) "1" else "0")
        add(cloudHeightMeters.toString())
        add(cloudSpeed.toString())
        add(cloudEvolutionSpeed.toString())
        add(if (waterDerivativeTrick) "1" else "0")
        add(waterStrength.toString())
        add(waterSpeed.toString())
        add(waterOctaves.toString())
        add(aperture.toString())
        add(shutterSpeed.toString())
        add(iso.toString())
        add(focalLength.toString())
        add(if (moonEnabled) "1" else "0")
        add(moonAzimuth.toString())
        add(moonHeight.toString())
        add(moonIntensity.toString())
        add(moonRadius.toString())
        add(if (milkyWayEnabled) "1" else "0")
        add(milkyWayIntensity.toString())
        add(milkyWaySaturation.toString())
        add(milkyWayBlackPoint.toString())
        add(milkyWaySiderealTime.toString())
        add(milkyWayLatitude.toString())
        add(if (starsEnabled) "1" else "0")
        add(starDensity.toString())
        add(starIntensityExponent.toString())
        add(msRayleigh.toString())
        add(msMie.toString())
        add(horizonGlow.toString())
        add(contrast.toString())
        add(shimmerStrength.toString())
        add(shimmerFrequency.toString())
        add(shimmerMaskHeight.toString())
        add(if (syncEnabled) "1" else "0")
        add(if (syncManualOverride) "1" else "0")
        add(manualTimeHours.toString())
        add(if (syncMoonPosition) "1" else "0")
        add(if (syncDeviceLocation) "1" else "0")
        add(syncLatitude.toString())
        add(syncLongitude.toString())
    }.joinToString("|")

    companion object {
        private const val SerializationVersion = 6

        val default = SkyWallpaperConfig()

        fun deserialize(raw: String?): SkyWallpaperConfig {
            if (raw.isNullOrBlank()) return default
            val parts = raw.split('|')
            val version = parts.firstOrNull()?.toIntOrNull() ?: return default
            if (version !in 1..SerializationVersion) return default
            var index = 1
            fun nextFloat(defaultValue: Float): Float = parts.getOrNull(index++)?.toFloatOrNull() ?: defaultValue
            fun nextBoolean(defaultValue: Boolean): Boolean = when (parts.getOrNull(index++)) {
                "1" -> true
                "0" -> false
                else -> defaultValue
            }
            val common = default.copy(
                sunAzimuth = nextFloat(default.sunAzimuth),
                sunHeight = nextFloat(default.sunHeight),
                sunIntensity = nextFloat(default.sunIntensity),
                sunRadius = nextFloat(default.sunRadius),
                sunLimbDarkening = nextFloat(default.sunLimbDarkening),
                sunDiskIntensityBoost = nextFloat(default.sunDiskIntensityBoost),
                turbidity = nextFloat(default.turbidity),
                rayleigh = nextFloat(default.rayleigh),
                mieCoefficient = nextFloat(default.mieCoefficient),
                ozone = nextFloat(default.ozone),
                mieG = nextFloat(default.mieG),
                cloudCoverage = nextFloat(default.cloudCoverage),
                cloudDensity = nextFloat(default.cloudDensity),
                cloudVolumetrics = nextBoolean(default.cloudVolumetrics),
                cloudHeightMeters = nextFloat(default.cloudHeightMeters),
                cloudSpeed = nextFloat(default.cloudSpeed),
                cloudEvolutionSpeed = nextFloat(default.cloudEvolutionSpeed),
                waterDerivativeTrick = nextBoolean(default.waterDerivativeTrick),
                waterStrength = nextFloat(default.waterStrength),
                waterSpeed = nextFloat(default.waterSpeed),
                waterOctaves = nextFloat(default.waterOctaves),
                aperture = nextFloat(default.aperture),
                shutterSpeed = nextFloat(default.shutterSpeed),
                iso = nextFloat(default.iso),
                focalLength = nextFloat(default.focalLength),
                moonEnabled = nextBoolean(default.moonEnabled),
                moonAzimuth = nextFloat(default.moonAzimuth),
                moonHeight = nextFloat(default.moonHeight),
                moonIntensity = nextFloat(default.moonIntensity),
                moonRadius = nextFloat(default.moonRadius),
                milkyWayEnabled = nextBoolean(default.milkyWayEnabled),
                milkyWayIntensity = nextFloat(default.milkyWayIntensity),
                milkyWaySaturation = nextFloat(default.milkyWaySaturation),
                milkyWayBlackPoint = nextFloat(default.milkyWayBlackPoint),
                milkyWaySiderealTime = nextFloat(default.milkyWaySiderealTime),
                milkyWayLatitude = nextFloat(default.milkyWayLatitude),
                starsEnabled = nextBoolean(default.starsEnabled),
                starDensity = nextFloat(default.starDensity),
                starIntensityExponent = nextFloat(default.starIntensityExponent),
                msRayleigh = nextFloat(default.msRayleigh),
                msMie = nextFloat(default.msMie),
                horizonGlow = nextFloat(default.horizonGlow),
                contrast = nextFloat(default.contrast),
                shimmerStrength = nextFloat(default.shimmerStrength),
                shimmerFrequency = nextFloat(default.shimmerFrequency),
                shimmerMaskHeight = nextFloat(default.shimmerMaskHeight),
            )

            return when (version) {
                1 -> common
                2 -> {
                    val legacySyncTimeOfDay = nextBoolean(false)
                    val legacySyncDeviceLocation = nextBoolean(default.syncDeviceLocation)
                    val latitude = nextFloat(default.syncLatitude)
                    val longitude = nextFloat(default.syncLongitude)
                    common.copy(
                        syncEnabled = legacySyncTimeOfDay || legacySyncDeviceLocation,
                        syncManualOverride = legacySyncTimeOfDay && !legacySyncDeviceLocation,
                        manualTimeHours = default.manualTimeHours,
                        syncDeviceLocation = legacySyncDeviceLocation,
                        syncLatitude = latitude,
                        syncLongitude = longitude,
                    )
                }
                3 -> {
                    val legacyManualOverride = nextBoolean(false)
                    val legacyManualTime = nextFloat(default.manualTimeHours)
                    val legacySyncDeviceLocation = nextBoolean(default.syncDeviceLocation)
                    val latitude = nextFloat(default.syncLatitude)
                    val longitude = nextFloat(default.syncLongitude)
                    common.copy(
                        syncEnabled = legacyManualOverride || legacySyncDeviceLocation,
                        syncManualOverride = legacyManualOverride,
                        manualTimeHours = legacyManualTime,
                        syncMoonPosition = true,
                        syncDeviceLocation = legacySyncDeviceLocation,
                        syncLatitude = latitude,
                        syncLongitude = longitude,
                    )
                }
                4 -> common.copy(
                    syncEnabled = nextBoolean(default.syncEnabled),
                    syncManualOverride = false,
                    manualTimeHours = default.manualTimeHours,
                    syncMoonPosition = true,
                    syncDeviceLocation = nextBoolean(default.syncDeviceLocation),
                    syncLatitude = nextFloat(default.syncLatitude),
                    syncLongitude = nextFloat(default.syncLongitude),
                )
                5 -> common.copy(
                    syncEnabled = nextBoolean(default.syncEnabled),
                    syncManualOverride = false,
                    manualTimeHours = default.manualTimeHours,
                    syncMoonPosition = nextBoolean(default.syncMoonPosition),
                    syncDeviceLocation = nextBoolean(default.syncDeviceLocation),
                    syncLatitude = nextFloat(default.syncLatitude),
                    syncLongitude = nextFloat(default.syncLongitude),
                )
                else -> common.copy(
                    syncEnabled = nextBoolean(default.syncEnabled),
                    syncManualOverride = nextBoolean(default.syncManualOverride),
                    manualTimeHours = nextFloat(default.manualTimeHours),
                    syncMoonPosition = nextBoolean(default.syncMoonPosition),
                    syncDeviceLocation = nextBoolean(default.syncDeviceLocation),
                    syncLatitude = nextFloat(default.syncLatitude),
                    syncLongitude = nextFloat(default.syncLongitude),
                )
            }
        }
    }
}
