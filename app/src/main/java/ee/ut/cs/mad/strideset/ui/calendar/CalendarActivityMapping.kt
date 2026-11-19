package ee.ut.cs.mad.strideset.ui.calendar

import android.content.res.Resources
import ee.ut.cs.mad.strideset.R
import ee.ut.cs.mad.strideset.data.model.DistanceUnit
import ee.ut.cs.mad.strideset.data.model.Run
import ee.ut.cs.mad.strideset.data.model.Workout
import ee.ut.cs.mad.strideset.ui.running.formatRunDistance
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/**
 * Turns finished workouts and runs from the database into the calendar's own rows.
 */
fun calendarActivitiesFrom(
    workouts: List<Workout>,
    runs: List<Run>,
    distanceUnit: DistanceUnit,
    resources: Resources
): List<CalendarActivity> {
    val activities = mutableListOf<CalendarActivity>()
    for (workout in workouts) {
        activities.add(workoutToCalendarActivity(workout, resources))
    }
    for (run in runs) {
        activities.add(runToCalendarActivity(run, distanceUnit, resources))
    }
    return activities
}

/** A workout row */
private fun workoutToCalendarActivity(workout: Workout, resources: Resources): CalendarActivity {
    val minutes = workout.duration().toMinutes()
    val exerciseCount = workout.exercises.size
    val exercisesText = resources.getQuantityString(
        R.plurals.workout_exercise_count,
        exerciseCount,
        exerciseCount
    )
    val detail = resources.getString(R.string.calendar_workout_detail, minutes, exercisesText)

    return CalendarActivity(
        date = dayOf(workout.startedAt),
        title = workout.name,
        detail = detail,
        isRun = false
    )
}

/** A run row */
private fun runToCalendarActivity(run: Run, distanceUnit: DistanceUnit, resources: Resources): CalendarActivity {
    val distance = formatRunDistance(run.distanceMeters, distanceUnit)
    val unitLabel = if (distanceUnit == DistanceUnit.MILES) {
        resources.getString(R.string.run_miles_short)
    } else {
        resources.getString(R.string.run_km_short)
    }
    val time = formatRunTime(run.activeDuration)
    val detail = resources.getString(R.string.calendar_run_detail, distance, unitLabel, time)

    return CalendarActivity(
        date = dayOf(run.startedAt),
        title = resources.getString(R.string.run_title),
        detail = detail,
        isRun = true
    )
}

private fun formatRunTime(duration: Duration): String {
    val totalSeconds = duration.seconds
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    if (hours > 0) {
        return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    }
    return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
}

/**
 * The calendar day something started on. The database stores an [Instant] (a moment without a
 * time zone), so it is converted with the phone's time zone, the same one the queries use.
 */
private fun dayOf(instant: Instant): LocalDate {
    return instant.atZone(ZoneId.systemDefault()).toLocalDate()
}
