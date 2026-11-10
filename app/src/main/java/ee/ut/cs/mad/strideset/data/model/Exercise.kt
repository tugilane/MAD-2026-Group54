package ee.ut.cs.mad.strideset.data.model

enum class ExerciseType { BARBELL, DUMBBELL, MACHINE, CABLE, BODYWEIGHT, OTHER }

data class Exercise(
    val id: Long = 0,
    val name: String,
    val type: ExerciseType,
    /** False for the small built-in list, true for exercises the user created. */
    val isCustom: Boolean = false,
)