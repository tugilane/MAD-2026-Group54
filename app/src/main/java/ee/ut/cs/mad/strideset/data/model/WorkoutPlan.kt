package ee.ut.cs.mad.strideset.data.model

data class WorkoutPlan(
    val id: Long = 0,
    val name: String,
    /** In the order the user arranged them. */
    val exercises: List<PlanExercise>,
)

data class PlanExercise(
    val exercise: Exercise,
    /** In order: the first item is SET 1. The number of sets is sets.size. */
    val sets: List<PlanSet>,
)

/** One planned set. */
data class PlanSet(
    val reps: Int,
    /** Always kilograms (convert with WeightUnit for display). Null = no weight set, e.g. bodyweight. */
    val weightKg: Double? = null,
)