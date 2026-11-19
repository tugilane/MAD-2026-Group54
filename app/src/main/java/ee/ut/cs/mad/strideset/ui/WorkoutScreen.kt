package ee.ut.cs.mad.strideset.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ee.ut.cs.mad.strideset.R
import ee.ut.cs.mad.strideset.data.model.ExercisePerformance
import ee.ut.cs.mad.strideset.data.model.Exercise
import ee.ut.cs.mad.strideset.data.model.ExerciseType
import ee.ut.cs.mad.strideset.data.model.Workout
import ee.ut.cs.mad.strideset.data.model.WorkoutExercise
import ee.ut.cs.mad.strideset.data.model.WorkoutSet
import ee.ut.cs.mad.strideset.ui.components.PrimaryButton
import ee.ut.cs.mad.strideset.ui.components.ScreenLayout
import ee.ut.cs.mad.strideset.ui.components.SecondaryButton
import ee.ut.cs.mad.strideset.ui.components.ExercisePickerPopup
import ee.ut.cs.mad.strideset.ui.theme.Dimens
import ee.ut.cs.mad.strideset.ui.theme.StrideSetTheme
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

/**
 * Displays an active workout.
 * Receives workout data and sends user actions through callbacks.
 * This composable does not create or store workouts itself.
 */
@Composable
internal fun WorkoutScreen(
    workout: Workout,
    exercises: List<Exercise> = emptyList(),
    onAddExercise: (Exercise) -> Unit,
    onAddSet: (Long) -> Unit,
    onSetChanged: (WorkoutSet) -> Unit,
    onFinishWorkout: () -> Unit,
    onDiscardWorkout: () -> Unit,
    onMinimize: () -> Unit,
    initialNow: Instant = Instant.now(),
    lastPerformance: Map<Long, ExercisePerformance> = emptyMap(),
) {
    var now by remember(workout.id, workout.startedAt) { mutableStateOf(initialNow) }
    var showDiscardDialog by remember(workout.id) { mutableStateOf(false) }
    var showBackDialog by remember(workout.id) { mutableStateOf(false) }
    var showExercisePicker by remember(workout.id) { mutableStateOf(false) }
    var showFinishValidation by remember(workout.id) { mutableStateOf(false) }

    BackHandler { showBackDialog = true }

    LaunchedEffect(workout.id, workout.startedAt, workout.finishedAt) {
        while (!workout.isFinished) {
            delay(1_000)
            now = Instant.now()
        }
    }
    LaunchedEffect(workout.completedSetCount) {
        if (workout.completedSetCount > 0) showFinishValidation = false
    }

    Box(Modifier.fillMaxSize()) {
        ScreenLayout(
            title = stringResource(R.string.workout_title),
            onBack = { showBackDialog = true },
            actions = {
                Text(
                    text = pluralStringResource(
                        R.plurals.workout_exercise_count,
                        workout.exercises.size,
                        workout.exercises.size,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingXSmall)) {
                    Text(
                        text = stringResource(R.string.elapsed_time),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatElapsedTime(workout.duration(now).seconds.coerceAtLeast(0)),
                        style = MaterialTheme.typography.displaySmall,
                    )
                }

                if (workout.exercises.isEmpty()) {
                    EmptyWorkoutCard()
                } else {
                    workout.exercises.forEach { exercise ->
                        WorkoutExerciseCard(
                            exercise = exercise,
                            lastPerformance = lastPerformance[exercise.exercise.id],
                            onSetChanged = onSetChanged,
                            onAddSet = { onAddSet(exercise.id) },
                        )
                    }
                }

                SecondaryButton(R.string.add_exercise, onClick = { showExercisePicker = true })

                if (showFinishValidation) {
                    Text(
                        text = stringResource(R.string.workout_finish_requires_completed_set),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                PrimaryButton(R.string.finish_workout, onClick = {
                    if (workout.completedSetCount == 0) {
                        showFinishValidation = true
                    } else {
                        onFinishWorkout()
                    }
                })
                SecondaryButton(R.string.discard_workout, onClick = { showDiscardDialog = true })
            }
        }

        if (showDiscardDialog) {
            WorkoutModal {
                Text(stringResource(R.string.discard_workout_title), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.discard_workout_message), style = MaterialTheme.typography.bodyMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showDiscardDialog = false }) { Text(stringResource(R.string.cancel)) }
                    TextButton(onClick = {
                        showDiscardDialog = false
                        onDiscardWorkout()
                    }) { Text(stringResource(R.string.confirm_discard)) }
                }
            }
        }

        if (showBackDialog) {
            WorkoutModal {
                Text(stringResource(R.string.workout_leave_title), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.workout_leave_message), style = MaterialTheme.typography.bodyMedium)
                if (showFinishValidation) {
                    Text(
                        text = stringResource(R.string.workout_finish_requires_completed_set),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TextButton(onClick = {
                    if (workout.completedSetCount == 0) {
                        showFinishValidation = true
                    } else {
                        showBackDialog = false
                        onFinishWorkout()
                    }
                }) { Text(stringResource(R.string.finish_and_save)) }
                TextButton(onClick = {
                    showBackDialog = false
                    onMinimize()
                }) { Text(stringResource(R.string.workout_background)) }
                TextButton(onClick = {
                    showBackDialog = false
                    showDiscardDialog = true
                }) { Text(stringResource(R.string.discard_workout)) }
                TextButton(onClick = { showBackDialog = false }) { Text(stringResource(R.string.continue_workout)) }
            }
        }

        if (showExercisePicker) {
            ExercisePickerPopup(
                exercises = exercises,
                onSelect = { exercise ->
                    showExercisePicker = false
                    onAddExercise(exercise)
                },
                onDismiss = { showExercisePicker = false },
            )
        }
    }
}

@Composable
private fun EmptyWorkoutCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = Dimens.spacingLarge,
                vertical = Dimens.spacingXLarge,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(stringResource(R.string.empty_workout_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.empty_workout_message),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun WorkoutModal(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
            .clickable(onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.widthIn(min = 280.dp, max = 400.dp).padding(Dimens.spacingLarge),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(Dimens.spacingLarge),
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
                content = content,
            )
        }
    }
}

@Composable
private fun WorkoutExerciseCard(
    exercise: WorkoutExercise,
    lastPerformance: ExercisePerformance?,
    onSetChanged: (WorkoutSet) -> Unit,
    onAddSet: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(Dimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = exercise.exercise.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(exerciseTypeLabel(exercise.exercise.type)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            exercise.sets.forEachIndexed { index, set ->
                WorkoutSetRow(
                    setNumber = index + 1,
                    set = set,
                    previousSet = lastPerformance?.sets?.getOrNull(index),
                    bodyweight = exercise.exercise.type == ExerciseType.BODYWEIGHT,
                    onSetChanged = onSetChanged,
                )
            }
            SecondaryButton(R.string.add_set, onAddSet)
        }
    }
}

@Composable
private fun WorkoutSetRow(
    setNumber: Int,
    set: WorkoutSet,
    previousSet: WorkoutSet?,
    bodyweight: Boolean,
    onSetChanged: (WorkoutSet) -> Unit,
) {
    var repsText by remember(set.id, set.reps) { mutableStateOf(set.reps.toString()) }
    var weightText by remember(set.id, set.weightKg) { mutableStateOf(formatWeight(set.weightKg)) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.background,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.spacingMedium, vertical = Dimens.spacingSmall),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingXSmall),
            ) {
                Text(
                    text = stringResource(R.string.workout_set_label, setNumber),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                previousSet?.let {
                    val previousPerformance = if (bodyweight) {
                        stringResource(R.string.last_performance_bodyweight, it.reps)
                    } else {
                        stringResource(
                            R.string.last_performance_weighted,
                            it.reps,
                            formatWeight(it.weightKg),
                        )
                    }
                    Text(
                        text = stringResource(R.string.last_performance_label, previousPerformance),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            CompactNumberField(
                value = repsText,
                description = stringResource(R.string.set_reps_description, setNumber),
                keyboardType = KeyboardType.Number,
                modifier = Modifier.width(48.dp),
                onValueChange = { value ->
                    if (value.all { it in '0'..'9' }) {
                        repsText = value
                        value.toIntOrNull()?.let { onSetChanged(set.copy(reps = it)) }
                    }
                },
            )
            Text("×", style = MaterialTheme.typography.bodyMedium)
            if (bodyweight) {
                Text(
                    text = stringResource(R.string.reps_unit),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                CompactNumberField(
                    value = weightText,
                    description = stringResource(R.string.set_weight_description, setNumber),
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.width(72.dp),
                    onValueChange = { value ->
                        if (value.all { it.isDigit() || it == '.' } && value.count { it == '.' } <= 1) {
                            weightText = value
                            value.toDoubleOrNull()?.takeIf { it.isFinite() }?.let {
                                onSetChanged(set.copy(weightKg = it))
                            }
                        }
                    },
                )
                Text(
                    text = stringResource(R.string.weight_kilograms_abbreviation),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val completionDescription = stringResource(
                if (set.isCompleted) R.string.mark_set_incomplete else R.string.mark_set_complete,
                setNumber,
            )
            Checkbox(
                checked = set.isCompleted,
                onCheckedChange = { onSetChanged(set.copy(isCompleted = it)) },
                modifier = Modifier
                    .size(Dimens.minTouchTarget)
                    .semantics {
                        contentDescription = completionDescription
                    },
            )
        }
    }
}

@Composable
private fun CompactNumberField(
    value: String,
    description: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
) {
    Surface(
        modifier = modifier
            .height(36.dp)
            .semantics { contentDescription = description },
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.background,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.spacingSmall, vertical = Dimens.spacingXSmall),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

@Preview(name = "Empty workout", showBackground = true)
@Composable
private fun WorkoutScreenEmptyPreview() {
    StrideSetTheme {
        WorkoutScreen(
            workout = Workout(name = "Workout", startedAt = Instant.now()),
            onAddExercise = {},
            onAddSet = {},
            onSetChanged = {},
            onFinishWorkout = {},
            onDiscardWorkout = {},
            onMinimize = {},
        )
    }
}

@Preview(name = "Workout with exercises", showBackground = true)
@Composable
private fun WorkoutScreenPopulatedPreview() {
    StrideSetTheme {
        WorkoutScreen(
            workout = previewWorkout(),
            onAddExercise = {},
            onAddSet = {},
            onSetChanged = {},
            onFinishWorkout = {},
            onDiscardWorkout = {},
            onMinimize = {},
            lastPerformance = previewLastPerformance(),
        )
    }
}

private fun previewWorkout() = Workout(
    name = "Leg day",
    startedAt = Instant.now().minus(Duration.ofMinutes(24)),
    exercises = listOf(
        WorkoutExercise(
            id = 1,
            exercise = ee.ut.cs.mad.strideset.data.model.Exercise(
                id = 1,
                name = "Back squat",
                type = ExerciseType.BARBELL,
            ),
            sets = listOf(
                WorkoutSet(id = 1, reps = 8, weightKg = 80.0, isCompleted = true),
                WorkoutSet(id = 2, reps = 8, weightKg = 80.0),
                WorkoutSet(id = 3, reps = 6, weightKg = 87.5),
            ),
        ),
        WorkoutExercise(
            id = 2,
            exercise = ee.ut.cs.mad.strideset.data.model.Exercise(
                id = 2,
                name = "Pull-up",
                type = ExerciseType.BODYWEIGHT,
            ),
            sets = listOf(
                WorkoutSet(id = 4, reps = 8, weightKg = 0.0),
                WorkoutSet(id = 5, reps = 7, weightKg = 0.0),
            ),
        ),
    ),
)

private fun previewLastPerformance() = mapOf(
    1L to ExercisePerformance(
        exerciseId = 1,
        performedAt = Instant.now().minus(Duration.ofDays(7)),
        sets = listOf(
            WorkoutSet(reps = 8, weightKg = 80.0),
            WorkoutSet(reps = 8, weightKg = 80.0),
            WorkoutSet(reps = 6, weightKg = 85.0),
        ),
    ),
    2L to ExercisePerformance(
        exerciseId = 2,
        performedAt = Instant.now().minus(Duration.ofDays(3)),
        sets = listOf(
            WorkoutSet(reps = 8, weightKg = 0.0),
            WorkoutSet(reps = 7, weightKg = 0.0),
        ),
    ),
)
