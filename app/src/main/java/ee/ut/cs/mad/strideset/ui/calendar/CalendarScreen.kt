package ee.ut.cs.mad.strideset.ui.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ee.ut.cs.mad.strideset.R
import ee.ut.cs.mad.strideset.ui.components.ScreenLayout
import ee.ut.cs.mad.strideset.ui.theme.Dimens
import ee.ut.cs.mad.strideset.ui.theme.StrideSetTheme
import java.time.LocalDate
import java.time.YearMonth

/**
 * The Calendar tab: a month grid with a dot on every day that has an activity, and the list of
 * activities for the selected day below it.
 *
 * The shown month is owned by the caller ([shownMonth] / [onMonthChange]), because the caller will
 * later load that month's workouts and runs from the database. The selected day is kept here.
 *
 * @param activities all activities the caller has for the shown month (may include other days).
 * @param today the day selected when the screen opens.
 */
@Composable
fun CalendarScreen(
    shownMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    activities: List<CalendarActivity>,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now()
) {
    // rememberSaveable keeps the selected day when the phone is rotated or the app is
    // recreated.
    var selectedDate by rememberSaveable { mutableStateOf(today) }

    // Days that get a dot. A Set holds every date only once, so two workouts on the same day
    // still give one dot.
    val datesWithActivity = mutableSetOf<LocalDate>()
    for (activity in activities) {
        datesWithActivity.add(activity.date)
    }

    val selectedDayActivities = mutableListOf<CalendarActivity>()
    for (activity in activities) {
        if (activity.date == selectedDate) {
            selectedDayActivities.add(activity)
        }
    }

    ScreenLayout(
        title = stringResource(R.string.nav_calendar),
        modifier = modifier
    ) {
        // Scrolls when a 6-row month and a long day list don't fit on a small phone.
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            MonthHeader(
                month = shownMonth,
                onPreviousMonth = {
                    val newMonth = shownMonth.minusMonths(1)
                    onMonthChange(newMonth)
                    selectedDate = dayToSelectIn(newMonth, today)
                },
                onNextMonth = {
                    val newMonth = shownMonth.plusMonths(1)
                    onMonthChange(newMonth)
                    selectedDate = dayToSelectIn(newMonth, today)
                }
            )
            Spacer(Modifier.height(Dimens.spacingLarge))
            MonthGrid(
                month = shownMonth,
                selectedDate = selectedDate,
                datesWithActivity = datesWithActivity,
                onDateClick = { date ->
                    selectedDate = date
                    // Tapping a grey day from the previous or next month jumps to that month.
                    val tappedMonth = YearMonth.from(date)
                    if (tappedMonth != shownMonth) {
                        onMonthChange(tappedMonth)
                    }
                }
            )
            Spacer(Modifier.height(Dimens.spacingLarge))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(Dimens.spacingLarge))
            DayActivities(date = selectedDate, activities = selectedDayActivities)
        }
    }
}

private fun dayToSelectIn(month: YearMonth, today: LocalDate): LocalDate {
    if (YearMonth.from(today) == month) {
        return today
    }
    return month.atDay(1)
}

/** Fake activities */
private val calendarPreviewActivities = listOf(
    CalendarActivity(LocalDate.of(2026, 10, 8), "Upper body", "38 min · 5 exercises", isRun = false),
    CalendarActivity(LocalDate.of(2026, 10, 12), "Easy run", "4.10 km · 25:12", isRun = true),
    CalendarActivity(LocalDate.of(2026, 10, 15), "Legs", "45 min · 4 exercises", isRun = false),
    CalendarActivity(LocalDate.of(2026, 10, 18), "Full body workout", "42 min · 4 exercises", isRun = false),
    CalendarActivity(LocalDate.of(2026, 10, 18), "Easy run", "5.24 km · 32:46", isRun = true),
    CalendarActivity(LocalDate.of(2026, 10, 22), "Upper body", "40 min · 5 exercises", isRun = false),
    CalendarActivity(LocalDate.of(2026, 10, 26), "Long run", "10.02 km · 58:40", isRun = true),
)

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun CalendarScreenPreview() {
    StrideSetTheme {
        CalendarScreen(
            shownMonth = YearMonth.of(2026, 10),
            onMonthChange = {},
            activities = calendarPreviewActivities,
            today = LocalDate.of(2026, 10, 18)
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun CalendarScreenEmptyPreview() {
    StrideSetTheme {
        CalendarScreen(
            shownMonth = YearMonth.of(2026, 10),
            onMonthChange = {},
            activities = emptyList(),
            today = LocalDate.of(2026, 10, 18)
        )
    }
}
