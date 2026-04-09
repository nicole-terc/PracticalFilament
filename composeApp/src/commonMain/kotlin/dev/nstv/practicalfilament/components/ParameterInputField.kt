package dev.nstv.practicalfilament.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.nstv.practicalfilament.filament.Bool2
import dev.nstv.practicalfilament.filament.Bool3
import dev.nstv.practicalfilament.filament.Bool4
import dev.nstv.practicalfilament.filament.Float2
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.Float4
import dev.nstv.practicalfilament.filament.Int2
import dev.nstv.practicalfilament.filament.Int3
import dev.nstv.practicalfilament.filament.Int4
import dev.nstv.practicalfilament.filament.UInt2
import dev.nstv.practicalfilament.filament.UInt3
import dev.nstv.practicalfilament.filament.UInt4
import dev.nstv.practicalfilament.filament.material.BuiltInTexture
import dev.nstv.practicalfilament.filament.material.MaterialParameterType
import dev.nstv.practicalfilament.filament.material.identityMat3
import dev.nstv.practicalfilament.filament.material.identityMat4
import dev.nstv.practicalfilament.theme.components.DropDownWithArrows
import kotlin.math.roundToInt


@Composable
fun ParameterInputField(
    name: String,
    type: MaterialParameterType,
    value: Any,
    overrideSamplers: Boolean = false,
    onValueChange: (Any) -> Unit,
) {
    when (type) {
        is MaterialParameterType.Bool -> {
            if (type.arraySize == 1) {
                LabeledToggle(name, value as? Boolean ?: false, onValueChange)
            } else {
                BooleanArrayField(
                    name,
                    value as? BooleanArray ?: BooleanArray(type.arraySize),
                    type.arraySize,
                    onValueChange
                )
            }
        }

        is MaterialParameterType.Bool2 -> {
            if (type.arraySize == 1) {
                val current = value as? Bool2 ?: Bool2(false, false)
                BooleanVectorField(
                    name = name,
                    labels = listOf("1", "2"),
                    values = listOf(current.x, current.y),
                ) { values -> onValueChange(Bool2(values[0], values[1])) }
            } else {
                BooleanArrayField(
                    name,
                    value as? BooleanArray ?: BooleanArray(type.arraySize * 2),
                    type.arraySize * 2,
                    onValueChange
                )
            }
        }

        is MaterialParameterType.Bool3 -> {
            if (type.arraySize == 1) {
                val current = value as? Bool3 ?: Bool3(false, false, false)
                BooleanVectorField(
                    name = name,
                    labels = listOf("1", "2", "3"),
                    values = listOf(current.x, current.y, current.z),
                ) { values -> onValueChange(Bool3(values[0], values[1], values[2])) }
            } else {
                BooleanArrayField(
                    name,
                    value as? BooleanArray ?: BooleanArray(type.arraySize * 3),
                    type.arraySize * 3,
                    onValueChange
                )
            }
        }

        is MaterialParameterType.Bool4 -> {
            if (type.arraySize == 1) {
                val current = value as? Bool4 ?: Bool4(false, false, false, false)
                BooleanVectorField(
                    name = name,
                    labels = listOf("1", "2", "3", "4"),
                    values = listOf(current.x, current.y, current.z, current.w),
                ) { values -> onValueChange(Bool4(values[0], values[1], values[2], values[3])) }
            } else {
                BooleanArrayField(
                    name,
                    value as? BooleanArray ?: BooleanArray(type.arraySize * 4),
                    type.arraySize * 4,
                    onValueChange
                )
            }
        }

        is MaterialParameterType.Float -> {
            if (type.arraySize == 1) {
                LabeledFloatSlider(name, value as? Float ?: 0.5f, 0f..1f) { onValueChange(it) }
            } else {
                FloatArrayField(
                    name,
                    value as? FloatArray ?: FloatArray(type.arraySize) { 0.5f },
                    0f..1f,
                    onValueChange
                )
            }
        }

        is MaterialParameterType.Float2 -> {
            if (type.arraySize == 1) {
                val current = value as? Float2 ?: Float2(0.5f, 0.5f)
                FloatVectorField(
                    name = name,
                    labels = listOf("x", "y"),
                    values = listOf(current.x, current.y),
                    range = 0f..1f,
                ) { values -> onValueChange(Float2(values[0], values[1])) }
            } else {
                FloatArrayField(
                    name,
                    value as? FloatArray ?: FloatArray(type.arraySize * 2) { 0.5f },
                    0f..1f,
                    onValueChange
                )
            }
        }

        is MaterialParameterType.Float3 -> {
            if (type.arraySize == 1) {
                val current = value as? Float3 ?: Float3(0.5f, 0.5f, 0.5f)
                FloatVectorField(
                    name = name,
                    labels = listOf("x", "y", "z"),
                    values = listOf(current.x, current.y, current.z),
                    range = 0f..1f,
                ) { values -> onValueChange(Float3(values[0], values[1], values[2])) }
            } else {
                FloatArrayField(
                    name,
                    value as? FloatArray ?: FloatArray(type.arraySize * 3) { 0.5f },
                    0f..1f,
                    onValueChange
                )
            }
        }

        is MaterialParameterType.Float4 -> {
            if (type.arraySize == 1) {
                val current = value as? Float4 ?: Float4(0.5f, 0.5f, 0.5f, 0.5f)
                FloatVectorField(
                    name = name,
                    labels = listOf("x", "y", "z", "w"),
                    values = listOf(current.x, current.y, current.z, current.w),
                    range = 0f..1f,
                ) { values -> onValueChange(Float4(values[0], values[1], values[2], values[3])) }
            } else {
                FloatArrayField(
                    name,
                    value as? FloatArray ?: FloatArray(type.arraySize * 4) { 0.5f },
                    0f..1f,
                    onValueChange
                )
            }
        }

        is MaterialParameterType.Int -> {
            if (type.arraySize == 1) {
                LabeledIntSlider(name, value as? Int ?: 0, -10..10) { onValueChange(it) }
            } else {
                IntArrayField(
                    name,
                    value as? IntArray ?: IntArray(type.arraySize),
                    -10..10,
                    onValueChange
                )
            }
        }

        is MaterialParameterType.Int2 -> {
            if (type.arraySize == 1) {
                val current = value as? Int2 ?: Int2(0, 0)
                IntVectorField(
                    name,
                    listOf("x", "y"),
                    listOf(current.x, current.y),
                    -10..10
                ) { values ->
                    onValueChange(Int2(values[0], values[1]))
                }
            } else {
                IntArrayField(
                    name,
                    value as? IntArray ?: IntArray(type.arraySize * 2),
                    -10..10,
                    onValueChange
                )
            }
        }

        is MaterialParameterType.Int3 -> {
            if (type.arraySize == 1) {
                val current = value as? Int3 ?: Int3(0, 0, 0)
                IntVectorField(
                    name,
                    listOf("x", "y", "z"),
                    listOf(current.x, current.y, current.z),
                    -10..10
                ) { values ->
                    onValueChange(Int3(values[0], values[1], values[2]))
                }
            } else {
                IntArrayField(
                    name,
                    value as? IntArray ?: IntArray(type.arraySize * 3),
                    -10..10,
                    onValueChange
                )
            }
        }

        is MaterialParameterType.Int4 -> {
            if (type.arraySize == 1) {
                val current = value as? Int4 ?: Int4(0, 0, 0, 0)
                IntVectorField(
                    name,
                    listOf("x", "y", "z", "w"),
                    listOf(current.x, current.y, current.z, current.w),
                    -10..10
                ) { values ->
                    onValueChange(Int4(values[0], values[1], values[2], values[3]))
                }
            } else {
                IntArrayField(
                    name,
                    value as? IntArray ?: IntArray(type.arraySize * 4),
                    -10..10,
                    onValueChange
                )
            }
        }

        is MaterialParameterType.UInt -> {
            if (type.arraySize == 1) {
                LabeledIntSlider(
                    name,
                    (value as? UInt ?: 0u).toInt(),
                    0..10
                ) { onValueChange(it.toUInt()) }
            } else {
                UIntArrayField(
                    name,
                    value as? UIntArray ?: UIntArray(type.arraySize),
                    0..10,
                    onValueChange
                )
            }
        }

        is MaterialParameterType.UInt2 -> {
            if (type.arraySize == 1) {
                val current = value as? UInt2 ?: UInt2(0u, 0u)
                IntVectorField(
                    name,
                    listOf("x", "y"),
                    listOf(current.x.toInt(), current.y.toInt()),
                    0..10
                ) { values ->
                    onValueChange(UInt2(values[0].toUInt(), values[1].toUInt()))
                }
            } else {
                UIntArrayField(
                    name,
                    value as? UIntArray ?: UIntArray(type.arraySize * 2),
                    0..10,
                    onValueChange
                )
            }
        }

        is MaterialParameterType.UInt3 -> {
            if (type.arraySize == 1) {
                val current = value as? UInt3 ?: UInt3(0u, 0u, 0u)
                IntVectorField(
                    name,
                    listOf("x", "y", "z"),
                    listOf(current.x.toInt(), current.y.toInt(), current.z.toInt()),
                    0..10
                ) { values ->
                    onValueChange(UInt3(values[0].toUInt(), values[1].toUInt(), values[2].toUInt()))
                }
            } else {
                UIntArrayField(
                    name,
                    value as? UIntArray ?: UIntArray(type.arraySize * 3),
                    0..10,
                    onValueChange
                )
            }
        }

        is MaterialParameterType.UInt4 -> {
            if (type.arraySize == 1) {
                val current = value as? UInt4 ?: UInt4(0u, 0u, 0u, 0u)
                IntVectorField(
                    name,
                    listOf("x", "y", "z", "w"),
                    listOf(
                        current.x.toInt(),
                        current.y.toInt(),
                        current.z.toInt(),
                        current.w.toInt()
                    ),
                    0..10
                ) { values ->
                    onValueChange(
                        UInt4(
                            values[0].toUInt(),
                            values[1].toUInt(),
                            values[2].toUInt(),
                            values[3].toUInt()
                        )
                    )
                }
            } else {
                UIntArrayField(
                    name,
                    value as? UIntArray ?: UIntArray(type.arraySize * 4),
                    0..10,
                    onValueChange
                )
            }
        }

        is MaterialParameterType.Float3x3 -> {
            FloatArrayField(name, value as? FloatArray ?: identityMat3(), -1f..1f, onValueChange)
        }

        is MaterialParameterType.Float4x4 -> {
            FloatArrayField(name, value as? FloatArray ?: identityMat4(), -1f..1f, onValueChange)
        }

        is MaterialParameterType.Sampler2d,
        is MaterialParameterType.Sampler2dArray,
        is MaterialParameterType.SamplerExternal,
        is MaterialParameterType.SamplerCubemap -> {
            if (overrideSamplers) {
                val textures = BuiltInTexture.entries
                val current = value as? BuiltInTexture ?: BuiltInTexture.NONE
                DropDownWithArrows(
                    options = textures.map(BuiltInTexture::displayName),
                    onSelectionChanged = { index ->
                        onValueChange(textures[index])
                    },
                    selectedIndex = textures.indexOf(current).coerceAtLeast(0),
                    label = name,
                    loopSelection = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(name)
                    Text("$value")
                }

            }
        }
    }
}

@Composable
private fun LabeledFloatSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            text = "$label: ${((value * 100).toInt() / 100f)}",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.weight(2f),
        )
    }
}

@Composable
private fun LabeledIntSlider(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            text = "$label: $value",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first - 1).coerceAtLeast(0),
            modifier = Modifier.weight(2f),
        )
    }
}

@Composable
private fun LabeledToggle(
    label: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
        )
        Switch(checked = value, onCheckedChange = onValueChange)
    }
}

@Composable
private fun FloatVectorField(
    name: String,
    labels: List<String>,
    values: List<Float>,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (List<Float>) -> Unit,
) {
    Text(text = "$name:", style = MaterialTheme.typography.bodyMedium)
    Column(modifier = Modifier.padding(start = 16.dp)) {
        labels.forEachIndexed { index, label ->
            LabeledFloatSlider(label, values[index], range) { updated ->
                onValueChange(values.toMutableList().also { it[index] = updated })
            }
        }
    }
}

@Composable
private fun IntVectorField(
    name: String,
    labels: List<String>,
    values: List<Int>,
    range: IntRange,
    onValueChange: (List<Int>) -> Unit,
) {
    Text(text = "$name:", style = MaterialTheme.typography.bodyMedium)
    Column(modifier = Modifier.padding(start = 16.dp)) {
        labels.forEachIndexed { index, label ->
            LabeledIntSlider(label, values[index], range) { updated ->
                onValueChange(values.toMutableList().also { it[index] = updated })
            }
        }
    }
}

@Composable
private fun BooleanVectorField(
    name: String,
    labels: List<String>,
    values: List<Boolean>,
    onValueChange: (List<Boolean>) -> Unit,
) {
    Text(text = "$name:", style = MaterialTheme.typography.bodyMedium)
    Column(modifier = Modifier.padding(start = 16.dp)) {
        labels.forEachIndexed { index, label ->
            LabeledToggle(label, values[index]) { updated ->
                onValueChange(values.toMutableList().also { it[index] = updated })
            }
        }
    }
}

@Composable
private fun FloatArrayField(
    name: String,
    values: FloatArray,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Any) -> Unit,
) {
    Text(text = "$name:", style = MaterialTheme.typography.bodyMedium)
    Column(modifier = Modifier.padding(start = 16.dp)) {
        values.forEachIndexed { index, currentValue ->
            LabeledFloatSlider("${index + 1}", currentValue, range) { updated ->
                onValueChange(values.copyOf().also { it[index] = updated })
            }
        }
    }
}

@Composable
private fun IntArrayField(
    name: String,
    values: IntArray,
    range: IntRange,
    onValueChange: (Any) -> Unit,
) {
    Text(text = "$name:", style = MaterialTheme.typography.bodyMedium)
    Column(modifier = Modifier.padding(start = 16.dp)) {
        values.forEachIndexed { index, currentValue ->
            LabeledIntSlider("${index + 1}", currentValue, range) { updated ->
                onValueChange(values.copyOf().also { it[index] = updated })
            }
        }
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
@Composable
private fun UIntArrayField(
    name: String,
    values: UIntArray,
    range: IntRange,
    onValueChange: (Any) -> Unit,
) {
    Text(text = "$name:", style = MaterialTheme.typography.bodyMedium)
    Column(modifier = Modifier.padding(start = 16.dp)) {
        values.forEachIndexed { index, currentValue ->
            LabeledIntSlider("${index + 1}", currentValue.toInt(), range) { updated ->
                onValueChange(values.copyOf().also { it[index] = updated.toUInt() })
            }
        }
    }
}

@Composable
private fun BooleanArrayField(
    name: String,
    values: BooleanArray,
    size: Int,
    onValueChange: (Any) -> Unit,
) {
    Text(text = "$name:", style = MaterialTheme.typography.bodyMedium)
    Column(modifier = Modifier.padding(start = 16.dp)) {
        repeat(size) { index ->
            LabeledToggle("${index + 1}", values[index]) { updated ->
                onValueChange(values.copyOf().also { it[index] = updated })
            }
        }
    }
}
