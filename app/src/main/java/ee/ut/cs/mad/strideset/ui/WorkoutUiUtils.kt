package ee.ut.cs.mad.strideset.ui

import ee.ut.cs.mad.strideset.R
import ee.ut.cs.mad.strideset.data.model.ExerciseType

internal fun exerciseTypeLabel(type: ExerciseType): Int = when (type) {
    ExerciseType.BARBELL -> R.string.exercise_type_barbell
    ExerciseType.DUMBBELL -> R.string.exercise_type_dumbbell
    ExerciseType.MACHINE -> R.string.exercise_type_machine
    ExerciseType.CABLE -> R.string.exercise_type_cable
    ExerciseType.BODYWEIGHT -> R.string.exercise_type_bodyweight
    ExerciseType.OTHER -> R.string.exercise_type_other
}

internal fun formatWeight(weightKg: Double): String =
    if (weightKg % 1.0 == 0.0) weightKg.toLong().toString() else weightKg.toString()

internal fun formatElapsedTime(seconds: Long): String {
    val hours = seconds / 3_600
    val minutes = (seconds % 3_600) / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, remainingSeconds)
}
