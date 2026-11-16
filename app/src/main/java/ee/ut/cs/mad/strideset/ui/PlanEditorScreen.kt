package ee.ut.cs.mad.strideset.ui

import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import ee.ut.cs.mad.strideset.R
import ee.ut.cs.mad.strideset.ui.components.ScreenLayout

/** A null plan ID starts a new plan. An existing ID identifies the plan to edit. */
@Composable
internal fun PlanEditorScreen(planId: Long?, onBack: () -> Unit) {
    val title = if (planId == null) R.string.new_plan_title else R.string.edit_plan_title
    ScreenLayout(title = stringResource(title), onBack = onBack) {
        if (planId != null) Text(stringResource(R.string.selected_plan_id, planId))
    }
}
