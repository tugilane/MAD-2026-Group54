package ee.ut.cs.mad.strideset.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    // The exercise library supplied by the screen.
    exercises: List<Exercise>,
    // Give the chosen exercise back to the screen when either Apply button is pressed.
    onSelect: (Exercise) -> Unit,
    onDismiss: () -> Unit,
) {
    // Keep these two inputs separate so their Apply buttons use the correct value.
    var customExerciseName by rememberSaveable { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf("") }
    // Existing exercises Apply uses the selected library exercise.
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    val focusManager = LocalFocusManager.current
    // Find the library exercise that was last tapped. No selection means Apply is disabled.
    val selectedExercise = exercises.firstOrNull { it.id == selectedId }
    // Ignore accidental spaces at the start and end of names and searches.
    val customName = customExerciseName.trim()
    val searchQuery = search.trim()
    // Prevent creating a second library exercise with the same name, ignoring capital letters.
    val duplicateName = exercises.any {
        it.name.equals(customName, ignoreCase = true)
    }
    // An empty search shows all exercises. Otherwise, show names containing the typed text.
    val matchingExercises = exercises.filter { it.name.contains(searchQuery, ignoreCase = true) }
    val customExerciseLabel = stringResource(R.string.custom_exercise)
    val existingExercisesLabel = stringResource(R.string.search_exercises)
    // Android Back or tapping outside the popup closes it without applying either input.
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge) {
            Column(
                modifier = Modifier.fillMaxHeight(0.75f).padding(Dimens.cardPadding), // Use 75% of the available height.
            ) {
                Text(
                    stringResource(R.string.choose_exercise),
                    style = MaterialTheme.typography.headlineSmall,
                )
                // Keep custom exercise entry visible while the library scrolls.
                Text(
                    customExerciseLabel,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = Dimens.spacingSmall),
                )
                ExerciseInputRow(
                    label = customExerciseLabel,
                    value = customExerciseName,
                    onValueChange = { customExerciseName = it },
                    isError = duplicateName,
                    canApply = customName.isNotEmpty() && !duplicateName, // A custom name must be usable.
                    onApply = {
                            // Keep the custom exercise in the draft until the plan is saved.
                            onSelect(Exercise(
                                name = customName,
                                // The picker currently has no equipment-type choice.
                                type = ExerciseType.OTHER,
                                isCustom = true,
                            ))
                    },
                )
                // Explain why the custom Apply button is unavailable for an existing name.
                if (duplicateName) {
                    Text(stringResource(R.string.exercise_name_taken), color = MaterialTheme.colorScheme.error)
                }
                // Separate creating an exercise from choosing one that already exists.
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = Dimens.spacingSmall),
                )
                Text(
                    existingExercisesLabel,
                    style = MaterialTheme.typography.labelMedium,
                )
                ExerciseInputRow(
                    label = existingExercisesLabel,
                    value = search,
                    onValueChange = {
                        search = it
                        // Typing changes the search, so the user must tap a result again.
                        selectedId = null
                    },
                    canApply = selectedExercise != null,
                    onApply = { selectedExercise?.let(onSelect) }, // Apply only the chosen library exercise.
                )
                // Scroll the library while keeping the input and Cancel visible.
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(matchingExercises, key = { it.id }) { exercise ->
                        // Each result is tappable and keeps the normal background color.
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth().selectable(
                                selected = selectedId == exercise.id,
                                onClick = {
                                    // Close the keyboard and show the chosen name in the input.
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
                // Cancel closes the picker without adding or replacing an exercise.
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

// Both inputs share the same layout, with their own Apply action.
@Composable
private fun ExerciseInputRow(
    // The heading is displayed by the caller and also identifies the input to screen readers.
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    canApply: Boolean, // Each caller decides when its own Apply button is available.
    onApply: () -> Unit,
    isError: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            isError = isError,
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            // Let the input use the space left beside Apply. Give spoken feedback its label.
            modifier = Modifier.weight(1f).semantics { contentDescription = label },
        )
        Button(enabled = canApply, onClick = onApply) {
            Text(stringResource(R.string.apply_exercise), style = MaterialTheme.typography.titleMedium)
        }
    }
}
