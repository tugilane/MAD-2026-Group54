package ee.ut.cs.mad.strideset.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ee.ut.cs.mad.strideset.R
import ee.ut.cs.mad.strideset.data.model.Exercise
import ee.ut.cs.mad.strideset.data.model.ExerciseType
import ee.ut.cs.mad.strideset.data.model.Workout
import ee.ut.cs.mad.strideset.data.model.WorkoutExercise
import ee.ut.cs.mad.strideset.data.model.WorkoutSet
import ee.ut.cs.mad.strideset.ui.components.PrimaryButton
import ee.ut.cs.mad.strideset.ui.components.ScreenLayout
import ee.ut.cs.mad.strideset.ui.components.SecondaryButton
import ee.ut.cs.mad.strideset.ui.theme.Dimens
import ee.ut.cs.mad.strideset.ui.theme.StrideSetTheme
import java.time.Instant

@Composable
internal fun WorkoutCompleteScreen(
    workout: Workout,
    onViewCalendar: () -> Unit,
    onDone: () -> Unit,
) {
    val completedExercises = workout.exercises.mapNotNull { exercise ->
        val sets = exercise.sets.mapIndexedNotNull { index, set ->
            if (set.isCompleted) index to set else null
        }
        if (sets.isNotEmpty()) exercise to sets else null
    }

    ScreenLayout(title = stringResource(R.string.workout_complete_title)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
            ) {
                SummaryCard(
                    duration = formatCompletionDuration(workout.duration().seconds.coerceAtLeast(0)),
                    exerciseCount = workout.exercises.size,
                    completedSetCount = workout.completedSetCount,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.completed_exercises_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.completed_set_count,
                            workout.completedSetCount,
                            workout.completedSetCount,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                completedExercises.forEach { (exercise, sets) ->
                    CompletedExerciseCard(exercise = exercise, sets = sets)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium)) {
                PrimaryButton(R.string.view_calendar, onViewCalendar)
                SecondaryButton(R.string.done, onDone)
            }
        }
    }
}

private fun formatCompletionDuration(seconds: Long): String {
    if (seconds >= 3_600) return formatElapsedTime(seconds)
    return "%02d:%02d".format(seconds / 60, seconds % 60)
}

@Composable
private fun SummaryCard(
    duration: String,
    exerciseCount: Int,
    completedSetCount: Int,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(Dimens.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SummaryMetric(R.string.workout_time_label, duration, Modifier.weight(1f))
            SummaryDivider()
            SummaryMetric(
                R.string.workout_exercises_label,
                exerciseCount.toString(),
                Modifier.weight(1f),
            )
            SummaryDivider()
            SummaryMetric(
                R.string.workout_sets_label,
                completedSetCount.toString(),
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SummaryMetric(label: Int, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingXSmall),
    ) {
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun SummaryDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = Dimens.spacingXSmall)
            .width(1.dp)
            .height(36.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

@Composable
private fun CompletedExerciseCard(
    exercise: WorkoutExercise,
    sets: List<Pair<Int, WorkoutSet>>,
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
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(exerciseTypeLabel(exercise.exercise.type)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            sets.forEach { (index, set) ->
                CompletedSetRow(setNumber = index + 1, set = set, bodyweight = exercise.exercise.type == ExerciseType.BODYWEIGHT)
            }
        }
    }
}

@Composable
private fun CompletedSetRow(setNumber: Int, set: WorkoutSet, bodyweight: Boolean) {
    val result = if (bodyweight) {
        stringResource(R.string.completed_set_bodyweight, set.reps)
    } else {
        stringResource(R.string.completed_set_weighted, set.reps, formatWeight(set.weightKg))
    }

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
            Text(
                text = stringResource(R.string.workout_set_label, setNumber),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = result, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Preview(name = "Workout complete", showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun WorkoutCompleteScreenPreview() {
    StrideSetTheme {
        WorkoutCompleteScreen(
            workout = previewCompletedWorkout(),
            onViewCalendar = {},
            onDone = {},
        )
    }
}

private fun previewCompletedWorkout(): Workout {
    val finishedAt = Instant.now()
    return Workout(
        name = "Workout",
        startedAt = finishedAt.minusSeconds(24 * 60 + 18),
        finishedAt = finishedAt,
        exercises = listOf(
            WorkoutExercise(
                id = 1,
                exercise = Exercise(id = 1, name = "Back squat", type = ExerciseType.BARBELL),
                sets = listOf(
                    WorkoutSet(id = 1, reps = 8, weightKg = 80.0, isCompleted = true),
                    WorkoutSet(id = 2, reps = 8, weightKg = 80.0, isCompleted = true),
                    WorkoutSet(id = 3, reps = 6, weightKg = 85.0, isCompleted = true),
                ),
            ),
            WorkoutExercise(
                id = 2,
                exercise = Exercise(id = 2, name = "Pull-up", type = ExerciseType.BODYWEIGHT),
                sets = listOf(
                    WorkoutSet(id = 4, reps = 8, weightKg = 0.0, isCompleted = true),
                    WorkoutSet(id = 5, reps = 7, weightKg = 0.0, isCompleted = true),
                ),
            ),
        ),
    )
}
