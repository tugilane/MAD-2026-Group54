package ee.ut.cs.mad.strideset.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ee.ut.cs.mad.strideset.R
import ee.ut.cs.mad.strideset.data.model.PlanExercise
import ee.ut.cs.mad.strideset.data.model.WeightUnit
import ee.ut.cs.mad.strideset.data.model.toLb
import ee.ut.cs.mad.strideset.data.model.WorkoutPlan
import ee.ut.cs.mad.strideset.ui.components.DraftSet
import ee.ut.cs.mad.strideset.ui.components.PlanExercisePopup
import ee.ut.cs.mad.strideset.ui.components.ScreenLayout
import ee.ut.cs.mad.strideset.ui.theme.Dimens

/**
 * A null plan opens the New plan form. An existing plan opens Edit plan
 * and supplies the saved name and exercise list.
 */
@Composable
internal fun PlanEditorScreen(plan: WorkoutPlan?, weightUnit: WeightUnit, onBack: () -> Unit) {
    val title = if (plan == null) R.string.new_plan_title else R.string.edit_plan_title
    // The typed name survives if you rorate your phone. Leaving the screen discards it.
    var name by rememberSaveable(plan?.id) { mutableStateOf(plan?.name.orEmpty()) }
    ScreenLayout(
        title = stringResource(title),
        modifier = Modifier.imePadding(),
        onBack = onBack,
        actions = {
            // Saving comes later.
            TextButton(
                onClick = {},
                enabled = false,
                modifier = Modifier.heightIn(min = Dimens.buttonHeight),
                colors = ButtonDefaults.textButtonColors(
                    disabledContentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(stringResource(R.string.save_plan), style = MaterialTheme.typography.titleMedium)
            }
        },
    ) {
        // Scroll the form while the header stays visible.
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
        ) {
            Text(stringResource(R.string.plan_name), style = MaterialTheme.typography.labelSmall)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text(stringResource(R.string.plan_name_hint)) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(R.string.plan_exercises),
                modifier = Modifier.padding(top = Dimens.spacingMedium),
                style = MaterialTheme.typography.labelSmall,
            )
            // Give each selected plan its own exercise draft state.
            key(plan?.id) {
                plan?.exercises.orEmpty().forEach { PlanExerciseRow(it, weightUnit) }
            }
            // Disabled until we can open the exercise picker and add a row.
            OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.add_exercise))
            }
        }
    }
}

/** Shows an exercise and keeps the edits confirmed in its popup. */
@Composable
private fun PlanExerciseRow(exercise: PlanExercise, weightUnit: WeightUnit) {
    var showPopup by rememberSaveable { mutableStateOf(false) }
    // Saved plans provide a set count and one rep target. Weight starts blank for now (no database table for it yet).
    var draftSets by rememberSaveable(exercise, weightUnit) {
        mutableStateOf(ArrayList(exercise.sets.map { set ->
            DraftSet(
                reps = set.reps.toString(),
                weight = set.weightKg?.let { weightUnit.toLb(it).toString() }.orEmpty(),
            )
        }))
    }
    if (showPopup) {
        PlanExercisePopup(
            exerciseName = exercise.exercise.name,
            initialSets = draftSets,
            defaultReps = exercise.sets.firstOrNull()?.reps ?: 8,
            weightUnit = weightUnit,
            onDismiss = { showPopup = false },
            onDone = {
                // Done updates the editor draft. No database write happens here.
                draftSets = ArrayList(it)
                showPopup = false
            },
        )
    }
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    Surface(modifier = Modifier.fillMaxWidth(), shape = shape, color = colors.surfaceVariant) {
        Column(
            modifier = Modifier.padding(Dimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
        ) {
            Surface(shape = shape, border = BorderStroke(1.dp, colors.onSurfaceVariant), modifier = Modifier.fillMaxWidth()) {
                Text(
                    exercise.exercise.name,
                    modifier = Modifier.padding(Dimens.spacingSmall),
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Give the details area the remaining width so Remove stays on the right.
                Surface(
                    onClick = { showPopup = true },
                    shape = shape,
                    border = BorderStroke(1.dp, colors.onSurfaceVariant),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        stringResource(R.string.click_for_details),
                        modifier = Modifier.padding(Dimens.spacingSmall),
                        color = colors.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                // Removal will edit the draft once the form supports changing exercises.
                TextButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.heightIn(min = Dimens.buttonHeight),
                    colors = ButtonDefaults.textButtonColors(disabledContentColor = colors.primary),
                ) {
                    Text(stringResource(R.string.remove_plan_exercise), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
