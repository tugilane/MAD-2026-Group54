package ee.ut.cs.mad.strideset.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ee.ut.cs.mad.strideset.R
import ee.ut.cs.mad.strideset.data.model.Exercise
import ee.ut.cs.mad.strideset.data.model.PlanSet
import ee.ut.cs.mad.strideset.data.model.PlanExercise
import ee.ut.cs.mad.strideset.data.model.WeightUnit
import ee.ut.cs.mad.strideset.data.model.toLb
import ee.ut.cs.mad.strideset.data.model.WorkoutPlan
import ee.ut.cs.mad.strideset.ui.components.ExercisePickerPopup
import ee.ut.cs.mad.strideset.ui.components.DraftSet
import ee.ut.cs.mad.strideset.ui.components.PlanExerciseRow
import ee.ut.cs.mad.strideset.ui.components.ScreenLayout
import ee.ut.cs.mad.strideset.ui.theme.Dimens

/**
 * A null plan opens the New plan form. An existing plan opens Edit plan
 * and supplies the saved name and exercise list.
 */
@Composable
internal fun PlanEditorScreen(
    // An existing plan supplies the starting values. Null means a new plan.
    plan: WorkoutPlan?,
    weightUnit: WeightUnit,
    exercises: List<Exercise>,
    onBack: () -> Unit,
) {
    // Both New plan and Edit plan use this screen.
    val title = if (plan == null) R.string.new_plan_title else R.string.edit_plan_title
    // The typed name survives if you rotate your phone. Leaving the screen discards it.
    var name by rememberSaveable(plan?.id) { mutableStateOf(plan?.name.orEmpty()) }
    // Each occurrence has its own key, even when the exercise is selected twice.
    var entries by rememberSaveable(plan?.id) {
        mutableStateOf(ArrayList(plan?.exercises.orEmpty().mapIndexed { index, item ->
            PlanExerciseEntry(exerciseId = item.exercise.id, savedPosition = index)
        }))
    }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    // No row chosen means Add exercise opened the picker.
    var replacingEntryKey by rememberSaveable { mutableStateOf<String?>(null) }
    // This switch changes cards from editing to dragging.
    var reordering by rememberSaveable { mutableStateOf(false) }
    // Remember how far down the form the user has scrolled.
    val scrollState = rememberScrollState()
    // Store each card's visible position so we know which card the finger is over.
    val rowBounds = remember { mutableStateMapOf<String, Rect>() }
    // The part of the form that is currently visible on screen.
    var viewport by remember { mutableStateOf(Rect.Zero) }
    // Null means no card is being dragged.
    var draggedKey by remember { mutableStateOf<String?>(null) }
    // The finger's current vertical position on screen.
    var dragY by remember { mutableStateOf(0f) }
    // Keep the card held at the point where the user first grabbed it.
    var grabOffset by remember { mutableStateOf(0f) }
    // Allow for different screen densities when measuring the edge scrolling area.
    val scrollEdge = with(LocalDensity.current) { 64.dp.toPx() }

    // Change the list order when the finger passes the middle of another card.
    fun moveDraggedRow() {
        val from = entries.indexOfFirst { it.key == draggedKey }
        val target = entries.indices.firstOrNull { index ->
            val bounds = rowBounds[entries[index].key] ?: return@firstOrNull false
            dragY in bounds.top..bounds.bottom &&
                ((index > from && dragY > bounds.center.y) ||
                    (index < from && dragY < bounds.center.y))
        } ?: -1
        if (from >= 0 && target >= 0 && from != target) {
            // Moving the whole entry also keeps its name and saved set information together.
            entries = ArrayList(entries).apply { add(target, removeAt(from)) }
        }
    }

    // Keep scrolling while a card is held near the visible list's top or bottom.
    LaunchedEffect(draggedKey) {
        while (draggedKey != null) {
            // Wait for the next screen update before taking another small scroll step.
            withFrameMillis { }
            val scroll = when {
                dragY < viewport.top + scrollEdge -> -12f
                dragY > viewport.bottom - scrollEdge -> 12f
                else -> 0f
            }
            if (scroll != 0f) {
                scrollState.scrollBy(scroll)
                // Scrolling can bring another card underneath the held finger.
                moveDraggedRow()
            }
        }
    }
    if (showPicker) {
        // The same picker handles adding a row and choosing a different exercise for a row.
        ExercisePickerPopup(
            exercises = exercises,
            onDismiss = { showPicker = false },
            onSelect = { exercise ->
                // New custom exercises have no database ID yet, so keep their typed name.
                val customName = exercise.name.takeIf { exercise.id == 0L }
                // Adding makes a new row. Replacing keeps the existing row's sets.
                entries = ArrayList(
                    if (replacingEntryKey == null) {
                        entries + PlanExerciseEntry(exerciseId = exercise.id, customName = customName)
                    } else {
                        entries.map { entry ->
                            if (entry.key == replacingEntryKey) entry.copy(
                                exerciseId = exercise.id,
                                customName = customName,
                            ) else entry
                        }
                    }
                )
                showPicker = false
            },
        )
    }
    ScreenLayout(
        title = stringResource(title),
        modifier = Modifier.imePadding(), // Leave room for the keyboard when it opens.
        onBack = onBack,
        actions = {
            // Press once to start reordering, then press again to return to editing.
            TextButton(
                onClick = { reordering = !reordering },
                modifier = Modifier.semantics { selected = reordering },
                colors = ButtonDefaults.textButtonColors(
                    // Highlight the toggle while reordering is active.
                    containerColor = if (reordering) MaterialTheme.colorScheme.secondaryContainer
                        else androidx.compose.ui.graphics.Color.Transparent,
                ),
            ) {
                Text(stringResource(R.string.reorder_exercises))
            }
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
        // Scroll the form while the header and Add exercise stay visible.
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f)
                // Update the visible area when the window or keyboard changes size.
                .onGloballyPositioned { viewport = it.boundsInRoot() }
                .pointerInput(reordering) {
                    if (reordering) detectDragGestures(
                        onDragStart = { position ->
                            // Find the card where the drag began.
                            dragY = viewport.top + position.y
                            draggedKey = entries.firstOrNull {
                                rowBounds[it.key]?.let { bounds -> dragY in bounds.top..bounds.bottom } == true
                            }?.key
                            grabOffset = dragY - (rowBounds[draggedKey]?.top ?: dragY)
                        },
                        onDrag = { change, amount ->
                            if (draggedKey != null) {
                                change.consume()
                                // Move with the finger and check whether the order should change.
                                dragY += amount.y
                                moveDraggedRow()
                            }
                        },
                        onDragEnd = { draggedKey = null }, // Let the card settle into its new place.
                        onDragCancel = { draggedKey = null },
                    )
                }
                // During reordering, dragging moves cards and the edges handle scrolling.
                .verticalScroll(scrollState, enabled = !reordering),
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
                entries.forEach { entry ->
                    // Original position finds the right saved sets even after rows move.
                    val savedExercise = entry.savedPosition?.let { plan?.exercises?.getOrNull(it) }
                    // A row can contain a draft custom exercise or an exercise from the library.
                    val selectedExercise = entry.customName?.let {
                        Exercise(name = it, type = ee.ut.cs.mad.strideset.data.model.ExerciseType.OTHER, isCustom = true)
                    } ?: exercises.firstOrNull { it.id == entry.exerciseId }
                        ?: savedExercise?.exercise
                    val exercise = selectedExercise?.let {
                        // Newly added rows start with one set of eight reps.
                        PlanExercise(it, savedExercise?.sets ?: listOf(PlanSet(reps = 8)))
                    }
                    if (exercise != null) {
                        key(entry.key) {
                            // This key keeps edits with the same row when its position changes.
                            // Measure the slot separately so the dragged card can follow the finger.
                            Box(Modifier.onGloballyPositioned { rowBounds[entry.key] = it.boundsInRoot() }
                                .zIndex(if (draggedKey == entry.key) 1f else 0f)) {
                            PlanExerciseRow(
                                exercise = exercise,
                                weightUnit = weightUnit,
                                reordering = reordering,
                                modifier = Modifier.graphicsLayer {
                                    // Draw the held card under the finger while its place in the list changes.
                                    translationY = if (draggedKey == entry.key)
                                        dragY - grabOffset - (rowBounds[entry.key]?.top ?: 0f) else 0f
                                },
                                onChooseExercise = {
                                    // Remember which row the picker should replace.
                                    replacingEntryKey = entry.key
                                    showPicker = true
                                },
                                onRemove = {
                                    // Only this row is removed, even if another row uses the same exercise.
                                    entries = ArrayList(entries.filter { it.key != entry.key })
                                },
                            )
                            }
                        }
                    }
                }
            }
        }
        // This button sits below the scrolling form so it stays within reach.
        OutlinedButton(
            enabled = !reordering,
            onClick = {
                replacingEntryKey = null
                showPicker = true
            },
            modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacingSmall),
        ) {
            Text(stringResource(R.string.add_exercise))
        }
    }
}

// Save the original position so repeated saved exercises keep their own sets.
private data class PlanExerciseEntry(
    // The exercise library's ID. Draft custom exercises use zero.
    val exerciseId: Long,
    // Keep a custom name here until saving gives that exercise a database ID.
    val customName: String? = null,
    // Remember where to find this row's original sets in the saved plan.
    val savedPosition: Int? = null,
    // A separate row identifier allows the same exercise to appear more than once.
    val key: String = java.util.UUID.randomUUID().toString(),
) : java.io.Serializable
