package ee.ut.cs.mad.strideset.ui.calendar

import java.time.LocalDate
import java.time.YearMonth

/**
 * Returns the dates shown in the calendar grid for [month]: only the weeks the month needs
 * (4 to 6 rows of 7 days), from the Monday on or before the 1st to the Sunday on or after
 * the last day. Days of the neighboring months fill the first and last rows.
 */
fun calendarGridDays(month: YearMonth): List<LocalDate> {
    val days = mutableListOf<LocalDate>()

    val firstOfMonth = month.atDay(1)
    val start = firstOfMonth.minusDays((firstOfMonth.dayOfWeek.value - 1).toLong())

    val lastOfMonth = month.atEndOfMonth()
    val end = lastOfMonth.plusDays((7 - lastOfMonth.dayOfWeek.value).toLong())

    var day = start

    while (!day.isAfter(end)) {
        days.add(day)
        day = day.plusDays(1)
    }

    return days
}
