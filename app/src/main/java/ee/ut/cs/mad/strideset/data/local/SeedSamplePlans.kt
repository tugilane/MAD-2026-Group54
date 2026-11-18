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
    // The JOIN finds the real exercise ID in Room.
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
        INSERT INTO plan_exercises (planId, position, exerciseId)
        SELECT samples.planId, samples.position, exercises.id
        FROM samples
        JOIN exercises ON exercises.name = samples.exerciseName
        """.trimIndent()
    )

    // Every sample exercise gets 3 sets of 8 reps; Pull-up is bodyweight (no weight).
    db.execSQL(
        """
        INSERT INTO plan_sets (planId, exercisePosition, setIndex, reps, weightKg)
        SELECT plan_exercises.planId, plan_exercises.position, setNumbers.setIndex, 8,
            CASE exercises.name
                WHEN 'Squat' THEN 60.0
                WHEN 'Bench press' THEN 50.0
                WHEN 'Overhead press' THEN 30.0
                WHEN 'Barbell row' THEN 40.0
                ELSE NULL
            END
        FROM plan_exercises
        JOIN exercises ON exercises.id = plan_exercises.exerciseId
        CROSS JOIN (SELECT 0 AS setIndex UNION ALL SELECT 1 UNION ALL SELECT 2) AS setNumbers
        """.trimIndent()
    )
}
