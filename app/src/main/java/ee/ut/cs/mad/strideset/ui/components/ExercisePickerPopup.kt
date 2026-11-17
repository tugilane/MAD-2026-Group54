package ee.ut.cs.mad.strideset.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import ee.ut.cs.mad.strideset.R
import ee.ut.cs.mad.strideset.data.model.Exercise
import ee.ut.cs.mad.strideset.data.model.ExerciseType
import ee.ut.cs.mad.strideset.ui.theme.Dimens

@Composable
internal fun ExercisePickerPopup(
    exercises: List<Exercise>,
    onSelect: (Exercise) -> Unit,
    onDismiss: () -> Unit,
) {
    var customExerciseName by rememberSaveable { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf("") }
    // The bottom Apply button uses the selected library exercise.
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    val focusManager = LocalFocusManager.current
    val selectedExercise = exercises.firstOrNull { it.id == selectedId }
    val duplicateName = exercises.any {
        it.name.equals(customExerciseName.trim(), ignoreCase = true)
    }
    val matchingExercises = exercises.filter { it.name.contains(search.trim(), ignoreCase = true) }
    val customExerciseLabel = stringResource(R.string.custom_exercise)
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge) {
            Column(
                modifier = Modifier.fillMaxHeight(0.75f).padding(Dimens.cardPadding),
            ) {
                Text(
                    stringResource(R.string.choose_exercise),
                    style = MaterialTheme.typography.headlineSmall,
                )
                // Keep custom exercise entry visible while the library scrolls.
                Text(
                    stringResource(R.string.custom_exercise),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = Dimens.spacingSmall),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
                ) {
                    OutlinedTextField(
                        value = customExerciseName,
                        onValueChange = { customExerciseName = it },
                        isError = duplicateName,
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.weight(1f).semantics {
                            contentDescription = customExerciseLabel
                        },
                    )
                    Button(
                        enabled = customExerciseName.isNotBlank() && !duplicateName,
                        onClick = {
                            // Keep the custom exercise in the draft until the plan is saved.
                            onSelect(Exercise(
                                name = customExerciseName.trim(),
                                type = ExerciseType.OTHER,
                                isCustom = true,
                            ))
                        },
                    ) {
                        Text(stringResource(R.string.apply_exercise), style = MaterialTheme.typography.titleMedium)
                    }
                }
                if (duplicateName) {
                    Text(stringResource(R.string.exercise_name_taken), color = MaterialTheme.colorScheme.error)
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = Dimens.spacingSmall),
                )
                Text(
                    stringResource(R.string.search_exercises),
                    style = MaterialTheme.typography.labelMedium,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
                ) {
                    val searchLabel = stringResource(R.string.search_exercises)
                    OutlinedTextField(
                        value = search,
                        onValueChange = {
                            search = it
                            selectedId = null
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.weight(1f).semantics {
                            contentDescription = searchLabel
                        },
                    )
                    Button(
                        enabled = selectedExercise != null,
                        onClick = { selectedExercise?.let(onSelect) },
                    ) {
                        Text(stringResource(R.string.apply_exercise), style = MaterialTheme.typography.titleMedium)
                    }
                }
                // Scroll the library while keeping the input and Cancel visible.
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(matchingExercises.size) { index ->
                        val exercise = matchingExercises[index]
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth().selectable(
                                selected = selectedId == exercise.id,
                                onClick = {
                                    focusManager.clearFocus()
                                    selectedId = exercise.id
                                    search = exercise.name
                                },
                            ),
                        ) {
                            Text(exercise.name, modifier = Modifier.padding(Dimens.spacingSmall))
                        }
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(top = Dimens.spacingSmall),
                ) {
                    Text(stringResource(R.string.cancel), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
