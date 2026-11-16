package ee.ut.cs.mad.strideset.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import ee.ut.cs.mad.strideset.R
import ee.ut.cs.mad.strideset.data.model.WeightUnit
import ee.ut.cs.mad.strideset.ui.theme.Dimens

// Keep numbers as text while editing so fields can be temporarily empty.
internal data class DraftSet(val reps: String, val weight: String) : java.io.Serializable

@Composable
internal fun PlanExercisePopup(
    exerciseName: String,
    initialSets: List<DraftSet>,
    defaultReps: Int,
    weightUnit: WeightUnit,
    onDismiss: () -> Unit,
    onDone: (List<DraftSet>) -> Unit,
) {
    // Copy the editor draft so Cancel and Android Back can discard popup edits.
    val sets = remember { initialSets.toMutableStateList() }

    val listState = rememberLazyListState()
    val repsLabel = stringResource(R.string.target_reps)
    val weightLabel = stringResource(R.string.target_weight)
    // The unit comes from Settings and is displayed beside the editable number.
    val weightUnitLabel = stringResource(
        if (weightUnit == WeightUnit.POUNDS) R.string.pound_symbol else R.string.kilogram_symbol
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(horizontal = 12.dp).widthIn(max = 480.dp).fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(exerciseName, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)) {
                // Only the set list scrolls, so Add set and the dialog buttons stay accessible.
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f, fill = false).heightIn(max = 416.dp),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
                ) {
                    items(sets.size) { index ->
                        val set = sets[index]
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(stringResource(R.string.set_number, index + 1))
                            SetNumberField(
                                label = repsLabel,
                                value = set.reps,
                                onValueChange = { sets[index] = set.copy(reps = it) },
                                keyboardType = KeyboardType.Number,
                                modifier = Modifier.weight(0.5f),
                            )
                            Row(
                                modifier = Modifier.weight(1.2f).padding(end = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                SetNumberField(
                                    label = weightLabel,
                                    value = set.weight,
                                    onValueChange = { sets[index] = set.copy(weight = it) },
                                    keyboardType = KeyboardType.Decimal,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(weightUnitLabel, style = MaterialTheme.typography.bodyLarge)
                            }
                            TextButton(
                                onClick = { sets.removeAt(index) },
                                contentPadding = PaddingValues(4.dp),
                            ) {
                                Text(stringResource(R.string.remove_plan_exercise))
                            }
                        }
                    }
                }
                if (listState.canScrollForward || listState.canScrollBackward) {
                    Text(
                        stringResource(R.string.scroll_to_view_sets),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                SecondaryButton(
                    label = R.string.add_set,
                    onClick = { sets.add(DraftSet(reps = defaultReps.toString(), weight = "")) },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
        confirmButton = {
            Button(onClick = { onDone(sets.toList()) }, shape = MaterialTheme.shapes.medium) {
                Text(stringResource(R.string.done))
            }
        },
    )
}

// Put the label above the field to give it room on narrow screens.
@Composable
private fun SetNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    modifier: Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            // The label is outside the field, so attach it for screen readers.
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
        )
    }
}
