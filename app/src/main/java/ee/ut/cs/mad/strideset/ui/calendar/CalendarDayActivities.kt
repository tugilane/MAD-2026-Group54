package ee.ut.cs.mad.strideset.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ee.ut.cs.mad.strideset.R
import ee.ut.cs.mad.strideset.ui.theme.Dimens
import ee.ut.cs.mad.strideset.ui.theme.StrideSetTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** One row in the calendar's day list. The UI's own model, so the screen doesn't depend on Workout/Run. */
data class CalendarActivity(
    val date: LocalDate,
    val title: String,      // "Full body workout"
    val detail: String,     // "42 min · 4 exercises"
    val isRun: Boolean,     // picks the icon
)

/** The selected day with the number of activities on the right. */
@Composable
fun SelectedDayHeader(
    date: LocalDate,
    activityCount: Int,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = date.format(formatter), style = MaterialTheme.typography.titleMedium)

        // Only show the count when there is something to count; the empty text says the rest.
        if (activityCount > 0) {
            Text(
                text = pluralStringResource(
                    R.plurals.calendar_activity_count,
                    activityCount,
                    activityCount
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** One workout or run: icon tile, title and a short summary. Not clickable */
@Composable
fun ActivityRow(
    activity: CalendarActivity,
    modifier: Modifier = Modifier
) {
    val iconRes = if (activity.isRun) R.drawable.ic_run else R.drawable.ic_workout

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.activityIconTileSize)
                .background(
                    MaterialTheme.colorScheme.surfaceContainer,
                    RoundedCornerShape(Dimens.cornerRadius)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(Dimens.smallIconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = activity.title, style = MaterialTheme.typography.labelLarge)
            Text(
                text = activity.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DayActivities(
    date: LocalDate,
    activities: List<CalendarActivity>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium)
    ) {
        SelectedDayHeader(date = date, activityCount = activities.size)

        if (activities.isEmpty()) {
            Text(
                text = stringResource(R.string.calendar_no_activities),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            for (activity in activities) {
                ActivityRow(activity = activity)
            }
        }
    }
}

private val previewDate = LocalDate.of(2026, 10, 18)
private val previewActivities = listOf(
    CalendarActivity(previewDate, "Full body workout", "42 min · 4 exercises", isRun = false),
    CalendarActivity(previewDate, "Easy run", "5.24 km · 32:46", isRun = true),
)

@Preview(showBackground = true)
@Composable
private fun DayActivitiesPreview() {
    StrideSetTheme {
        DayActivities(date = previewDate, activities = previewActivities)
    }
}

@Preview(showBackground = true)
@Composable
private fun DayActivitiesEmptyPreview() {
    StrideSetTheme {
        DayActivities(date = previewDate, activities = emptyList())
    }
}
