package ee.ut.cs.mad.strideset.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import ee.ut.cs.mad.strideset.R
import ee.ut.cs.mad.strideset.ui.components.RoundIconButton
import ee.ut.cs.mad.strideset.ui.theme.Dimens
import ee.ut.cs.mad.strideset.ui.theme.InactiveColor
import ee.ut.cs.mad.strideset.ui.theme.StrideSetTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Month title with buttons to go to the previous and next month. */
@Composable
fun MonthHeader(
    month: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
        val title = month.format(formatter)

        Text(text = title, style = MaterialTheme.typography.titleMedium)

        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)
        ) {
            RoundIconButton(
                iconRes = R.drawable.ic_chevron_left,
                contentDescription = R.string.calendar_previous_month,
                onClick = onPreviousMonth
            )
            RoundIconButton(
                iconRes = R.drawable.ic_chevron_right,
                contentDescription = R.string.calendar_next_month,
                onClick = onNextMonth
            )
        }
    }
}

@Composable
fun WeekDayLabels(
    modifier: Modifier = Modifier
) {
    val letters = stringArrayResource(R.array.calendar_weekday_letters)

    Row(
        modifier = modifier.fillMaxWidth()
    ) {
        for (letter in letters)
            Text(
                text = letter,
                Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = InactiveColor
            )
    }
}

@Composable
fun DayCell(
    date: LocalDate,
    isInShownMonth: Boolean,
    isSelected: Boolean,
    hasActivity: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val circleColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else if (isInShownMonth) {
        MaterialTheme.colorScheme.onSurface
    } else {
        InactiveColor
    }

    val dotColor =
        if (hasActivity) MaterialTheme.colorScheme.onSurfaceVariant else Color.Transparent

    val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)
    val description = date.format(formatter)

    Column(
        modifier = modifier
            .height(Dimens.minTouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.calendarDayCircleSize)
                .background(circleColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = textColor
            )
        }
        Spacer(Modifier.height(Dimens.spacingXSmall))
        // Activity dot. Transparent when there is no activity, so every cell keeps the same layout.
        Box(
            modifier = Modifier
                .size(Dimens.activityDotSize)
                .background(dotColor, CircleShape)
        )
    }
}

@Composable
fun MonthGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    datesWithActivity: Set<LocalDate>,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val days = calendarGridDays(month)
    val weekCount = days.size / 7

    Column(modifier = modifier.fillMaxWidth()) {
        WeekDayLabels()
        for (week in 0 until weekCount) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (dayOfWeek in 0 until 7) {
                    val date = days[week * 7 + dayOfWeek]
                    val isInShownMonth = YearMonth.from(date) == month
                    val isSelected = date == selectedDate
                    val hasActivity = datesWithActivity.contains(date)
                    DayCell(
                        date = date,
                        isInShownMonth = isInShownMonth,
                        isSelected = isSelected,
                        hasActivity = hasActivity,
                        onClick = { onDateClick(date) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MonthHeaderPreview() {
    StrideSetTheme {
        MonthHeader(
            month = YearMonth.of(2026, 10),
            onPreviousMonth = {},
            onNextMonth = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WeekDayLabelsPreview() {
    StrideSetTheme {
        WeekDayLabels()
    }
}

/** The four day states side by side: normal, other month, selected, with an activity. */
@Preview(showBackground = true)
@Composable
private fun DayCellPreview() {
    StrideSetTheme {
        Row(modifier = Modifier.fillMaxWidth()) {
            DayCell(
                date = LocalDate.of(2026, 10, 1),
                isInShownMonth = true,
                isSelected = false,
                hasActivity = false,
                onClick = {},
                modifier = Modifier.weight(1f)
            )
            DayCell(
                date = LocalDate.of(2026, 9, 28),
                isInShownMonth = false,
                isSelected = false,
                hasActivity = false,
                onClick = {},
                modifier = Modifier.weight(1f)
            )
            DayCell(
                date = LocalDate.of(2026, 10, 18),
                isInShownMonth = true,
                isSelected = true,
                hasActivity = false,
                onClick = {},
                modifier = Modifier.weight(1f)
            )
            DayCell(
                date = LocalDate.of(2026, 10, 8),
                isInShownMonth = true,
                isSelected = false,
                hasActivity = true,
                onClick = {},
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MonthGridPreview() {
    StrideSetTheme {
        MonthGrid(
            month = YearMonth.of(2026, 10),
            selectedDate = LocalDate.of(2026, 10, 18),
            datesWithActivity = setOf(
                LocalDate.of(2026, 10, 8),
                LocalDate.of(2026, 10, 12),
                LocalDate.of(2026, 10, 15),
                LocalDate.of(2026, 10, 22),
                LocalDate.of(2026, 10, 26),
            ),
            onDateClick = {},
        )
    }
}