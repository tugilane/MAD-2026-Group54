package ee.ut.cs.mad.strideset.data.local

import androidx.sqlite.db.SupportSQLiteDatabase

/** Called when Room creates the database, where plan IDs 1 and 2 are available. */
internal fun seedSamplePlans(db: SupportSQLiteDatabase) {
    db.execSQL(
        """
        INSERT INTO workout_plans (id, name)
        VALUES (1, 'Full body'), (2, 'Upper body')
        """.trimIndent()
    )

    // Each sample row gives the plan ID, the exercise order (starting at 0), and the name.
    // The JOIN finds the real exercise ID in Room. All five entries use 3 sets of 8 reps.
    db.execSQL(
        """
        WITH samples(planId, position, exerciseName) AS (
            VALUES
                (1, 0, 'Squat'),
                (1, 1, 'Bench press'),
                (1, 2, 'Pull-up'),
                (2, 0, 'Overhead press'),
                (2, 1, 'Barbell row')
        )
        INSERT INTO plan_exercises
            (planId, position, exerciseId, targetSets, targetReps)
        SELECT samples.planId, samples.position, exercises.id, 3, 8
        FROM samples
        JOIN exercises ON exercises.name = samples.exerciseName
        """.trimIndent()
    )
}
