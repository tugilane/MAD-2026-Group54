package ee.ut.cs.mad.strideset.ui

import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import ee.ut.cs.mad.strideset.R
import ee.ut.cs.mad.strideset.ui.components.ScreenLayout

/** A null plan ID starts an empty workout. An existing ID identifies the starting plan. */
@Composable
internal fun WorkoutScreen(planId: Long?, onBack: () -> Unit) {
    ScreenLayout(title = stringResource(R.string.workout_title), onBack = onBack) {
        if (planId == null) {
            Text(stringResource(R.string.empty_workout_title))
        } else {
            Text(stringResource(R.string.selected_plan_id, planId))
        }
    }
}
