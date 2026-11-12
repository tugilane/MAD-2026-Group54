package ee.ut.cs.mad.strideset.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ee.ut.cs.mad.strideset.R
import ee.ut.cs.mad.strideset.ui.components.ScreenLayout
import ee.ut.cs.mad.strideset.ui.theme.Dimens

/** Draws the Choose workout screen. The caller decides where the Back button leads. */
@Composable
internal fun ChooseWorkoutScreen(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    // ScreenLayout supplies the shared title and spacing. Its actions slot sits to the right.
    ScreenLayout(
        title = stringResource(R.string.choose_workout_title),
        actions = {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_back), stringResource(R.string.navigate_back))
            }
        },
    ) {
        // Stack the cards vertically and allow scrolling on shorter screens.
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
        ) {
            // This Surface only draws the card. When the workout flow is ready, give it
            // a click handler and let the caller decide what to open.
            Surface(
                shape = shape,
                border = BorderStroke(1.dp, colors.outline),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(Dimens.spacingLarge),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Let the text take the available space so the plus stays on the right.
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.start_empty_workout), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.empty_workout_hint), color = colors.onSurfaceVariant)
                    }
                    Text("+", style = MaterialTheme.typography.titleMedium)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacingMedium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.saved_plans), style = MaterialTheme.typography.labelSmall)
                // Static label styled like the future Create plan button.
                Surface(shape = shape, color = colors.primary) {
                    Text(
                        stringResource(R.string.create_plan),
                        modifier = Modifier.padding(Dimens.spacingSmall),
                        color = colors.onPrimary,
                    )
                }
            }
            // Sample rows for the layout. Saved plans will come from storage in a later step. Remove this when implementing actual solution
            SamplePlanRow(stringResource(R.string.sample_full_body), 3)
            SamplePlanRow(stringResource(R.string.sample_upper_body), 2)
        }
    }
}

/** Draws one sample plan. It receives display values and doesn't read from storage. */
@Composable
private fun SamplePlanRow(name: String, exerciseCount: Int) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = MaterialTheme.shapes.medium, color = colors.surfaceVariant) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimens.spacingLarge),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                // Android picks “exercise” or “exercises” based on the count.
                Text(
                    pluralStringResource(R.plurals.plan_exercise_count, exerciseCount, exerciseCount),
                    color = colors.onSurfaceVariant,
                )
            }
            // The Edit label shows where the action will go once plan editing exists.
            Surface(shape = MaterialTheme.shapes.medium, border = BorderStroke(1.dp, colors.outline), color = colors.surfaceVariant) {
                Text(stringResource(R.string.edit_plan), Modifier.padding(Dimens.spacingSmall))
            }
        }
    }
}
