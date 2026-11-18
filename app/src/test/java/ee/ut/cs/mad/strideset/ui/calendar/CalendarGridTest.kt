package ee.ut.cs.mad.strideset.ui.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * Checks that [calendarGridDays] builds the month grid shown on the Calendar screen:
 * only the weeks the month needs (4 to 6 rows of 7 days), Monday first, like the Figma design.
 */
class CalendarGridTest {

    /** Every month must be shown as whole weeks, Monday to Sunday, with its first and last day visible. */
    @Test
    fun everyMonthIsShownAsWholeWeeks() {
        // Check every month of two years, so all possible starting weekdays are covered.
        for (year in 2026..2027) {
            for (month in 1..12) {
                val yearMonth = YearMonth.of(year, month)
                val days = calendarGridDays(yearMonth)
                val name = "$year-$month"

                assertEquals("$name: not whole weeks", 0, days.size % 7)
                assertTrue("$name: fewer than 4 rows", days.size >= 28)
                assertTrue("$name: more than 6 rows", days.size <= 42)
                assertEquals("$name: first day", DayOfWeek.MONDAY, days[0].dayOfWeek)
                assertEquals("$name: last day", DayOfWeek.SUNDAY, days[days.size - 1].dayOfWeek)
                assertTrue("$name: missing the 1st", days.contains(yearMonth.atDay(1)))
                assertTrue("$name: missing the last day", days.contains(yearMonth.atEndOfMonth()))
            }
        }
    }

    /** October 2026 is the month in the Figma design: 5 rows, Mon 28 Sep to Sun 1 Nov. */
    @Test
    fun october2026MatchesTheFigmaDesign() {
        val days = calendarGridDays(YearMonth.of(2026, 10))

        assertEquals(35, days.size)
        assertEquals(LocalDate.of(2026, 9, 28), days[0])
        assertEquals(LocalDate.of(2026, 11, 1), days[days.size - 1])
    }

    /** June 2026 starts on a Monday, so no days from May should be shown before it. */
    @Test
    fun monthStartingOnMondayStartsOnItsOwnFirst() {
        val days = calendarGridDays(YearMonth.of(2026, 6))

        assertEquals(LocalDate.of(2026, 6, 1), days[0])
        assertEquals(LocalDate.of(2026, 7, 5), days[days.size - 1])
    }

    /** March 2026 starts on a Sunday and has 31 days, so it needs all 6 rows. */
    @Test
    fun march2026NeedsSixRows() {
        val days = calendarGridDays(YearMonth.of(2026, 3))

        assertEquals(42, days.size)
        assertEquals(LocalDate.of(2026, 2, 23), days[0])
        assertEquals(LocalDate.of(2026, 4, 5), days[days.size - 1])
    }

    /** February 2027 starts on a Monday and ends on a Sunday, so it fits in exactly 4 rows. */
    @Test
    fun february2027NeedsOnlyFourRows() {
        val days = calendarGridDays(YearMonth.of(2027, 2))

        assertEquals(28, days.size)
        assertEquals(LocalDate.of(2027, 2, 1), days[0])
        assertEquals(LocalDate.of(2027, 2, 28), days[days.size - 1])
    }

    /** January 2027 starts on a Friday, so the grid begins in December of the previous year. */
    @Test
    fun gridCanStartInThePreviousYear() {
        val days = calendarGridDays(YearMonth.of(2027, 1))

        assertEquals(LocalDate.of(2026, 12, 28), days[0])
        // 31 January 2027 is a Sunday, so no February days are added after it.
        assertEquals(LocalDate.of(2027, 1, 31), days[days.size - 1])
    }

    /** The days must follow each other with no gaps or repeats. */
    @Test
    fun daysAreConsecutive() {
        val days = calendarGridDays(YearMonth.of(2026, 10))

        for (index in 1 until days.size) {
            val expected = days[index - 1].plusDays(1)
            assertEquals("Gap after index ${index - 1}", expected, days[index])
        }
    }
}
