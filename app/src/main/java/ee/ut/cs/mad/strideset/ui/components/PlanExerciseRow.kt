package ee.ut.cs.mad.strideset.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ee.ut.cs.mad.strideset.R
import ee.ut.cs.mad.strideset.data.model.PlanExercise
import ee.ut.cs.mad.strideset.data.model.WeightUnit
import ee.ut.cs.mad.strideset.data.model.toLb
import ee.ut.cs.mad.strideset.ui.theme.Dimens

/** Shows an exercise and keeps the edits confirmed in its popup. */
@Composable
internal fun PlanExerciseRow(
    exercise: PlanExercise,
    weightUnit: WeightUnit,
    onChooseExercise: () -> Unit,
    onRemove: () -> Unit,
    // The screen tells this card when it should allow dragging instead of editing.
    reordering: Boolean,
    // The screen uses this to move the card's drawing while it is being dragged.
    modifier: Modifier = Modifier,
) {
    // Opening details shows this row's set popup.
    var showPopup by rememberSaveable { mutableStateOf(false) }
    // Start with this occurrence's saved sets in the selected weight unit.
    var draftSets by rememberSaveable(weightUnit) {
        // Keep text as text while editing, including fields the user has temporarily cleared.
        mutableStateOf(ArrayList(exercise.sets.map { set ->
            DraftSet(
                reps = set.reps.toString(),
                // Stored weights are kilograms. Show the unit chosen in Settings.
                weight = set.weightKg?.let { weightUnit.toLb(it).toString() }.orEmpty(),
            )
        }))
    }
    if (showPopup) {
        // The popup makes its own copy, so Cancel leaves these draft sets alone.
        PlanExercisePopup(
            exerciseName = exercise.exercise.name,
            initialSets = draftSets,
            defaultReps = exercise.sets.firstOrNull()?.reps ?: 8, // Used when Add set is pressed.
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
    // A colored background and border show that the card can currently be dragged.
    Surface(
        modifier = modifier.fillMaxWidth(), shape = shape,
        color = if (reordering) colors.secondaryContainer else colors.surfaceVariant,
        border = if (reordering) BorderStroke(2.dp, colors.primary) else null,
    ) {
        Column(
            modifier = Modifier.padding(Dimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
        ) {
            // Tapping the name lets the user choose another exercise for this row.
            // The name button is disabled while the user arranges the cards.
            Surface(onClick = onChooseExercise, enabled = !reordering, shape = shape, border = BorderStroke(1.dp, colors.onSurfaceVariant), modifier = Modifier.fillMaxWidth()) {
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
                    // Avoid opening details when the user is trying to drag the card.
                    enabled = !reordering,
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
                // Remove only this occurrence of the exercise from the draft.
                TextButton(
                    // Removal is available again when reorder mode is turned off.
                    enabled = !reordering,
                    onClick = onRemove,
                    modifier = Modifier.heightIn(min = Dimens.buttonHeight),
                    colors = ButtonDefaults.textButtonColors(disabledContentColor = colors.primary),
                ) {
                    Text(stringResource(R.string.remove_plan_exercise), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
