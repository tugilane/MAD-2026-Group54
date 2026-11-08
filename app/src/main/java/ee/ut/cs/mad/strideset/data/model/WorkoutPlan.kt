package ee.ut.cs.mad.strideset.data.model

data class WorkoutPlan(
    val id: Long = 0,
    val name: String,
    /** In the order the user arranged them. */
    val exercises: List<PlanExercise>,
)

data class PlanExercise(
    val exercise: Exercise,
    val targetSets: Int,
    val targetReps: Int,
)