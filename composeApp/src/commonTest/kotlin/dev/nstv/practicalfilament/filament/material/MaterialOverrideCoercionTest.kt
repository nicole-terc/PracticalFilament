package dev.nstv.practicalfilament.filament.material

import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.Float4
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MaterialOverrideCoercionTest {
    @Test
    fun `coerces float4 override into float3 parameter`() {
        val definition = MaterialParameterDefinition(
            name = "baseColor",
            type = MaterialParameterType.Float3(),
        )

        val result = coerceOverrideValue(definition, Float4(0.72f, 0.74f, 0.70f, 1f))

        assertEquals(Float3(0.72f, 0.74f, 0.70f), result)
    }

    @Test
    fun `coerces float3 override into float4 parameter`() {
        val definition = MaterialParameterDefinition(
            name = "emissive",
            type = MaterialParameterType.Float4(),
        )

        val result = coerceOverrideValue(definition, Float3(0.1f, 0.2f, 0.3f))

        assertEquals(Float4(0.1f, 0.2f, 0.3f, 1f), result)
    }

    @Test
    fun `rejects unsupported override value`() {
        val definition = MaterialParameterDefinition(
            name = "baseColor",
            type = MaterialParameterType.Float3(),
        )

        val result = coerceOverrideValue(definition, "not-a-color")

        assertNull(result)
    }
}
