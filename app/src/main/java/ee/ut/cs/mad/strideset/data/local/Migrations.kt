package ee.ut.cs.mad.strideset.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Version 2 -> 3: plans store reps and weight per set (new plan_sets table)
 * instead of one targetSets/targetReps pair per exercise.
 * Old plans keep their targets as sets without a weight: 3 x 8 becomes three sets of 8 reps.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Move the old table aside. Drop its index, because the new table needs the same index name.
        db.execSQL("ALTER TABLE plan_exercises RENAME TO plan_exercises_old")
        db.execSQL("DROP INDEX IF EXISTS index_plan_exercises_exerciseId")

        // 2. New plan_exercises without the target columns (SQL must match schemas/.../3.json).
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `plan_exercises` (`planId` INTEGER NOT NULL, `position` INTEGER NOT NULL, " +
                    "`exerciseId` INTEGER NOT NULL, PRIMARY KEY(`planId`, `position`), " +
                    "FOREIGN KEY(`planId`) REFERENCES `workout_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                    "FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_exercises_exerciseId` ON `plan_exercises` (`exerciseId`)")
        db.execSQL(
            "INSERT INTO plan_exercises (planId, position, exerciseId) " +
                    "SELECT planId, position, exerciseId FROM plan_exercises_old"
        )

        // 3. The new plan_sets table.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `plan_sets` (`planId` INTEGER NOT NULL, `exercisePosition` INTEGER NOT NULL, " +
                    "`setIndex` INTEGER NOT NULL, `reps` INTEGER NOT NULL, `weightKg` REAL, " +
                    "PRIMARY KEY(`planId`, `exercisePosition`, `setIndex`), " +
                    "FOREIGN KEY(`planId`, `exercisePosition`) REFERENCES `plan_exercises`(`planId`, `position`) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )

        // 4. Turn each old row into targetSets rows: setIndex counts 0, 1, 2, ... up to targetSets - 1.
        db.execSQL(
            """
            WITH RECURSIVE expanded(planId, position, setIndex, reps, targetSets) AS (
                SELECT planId, position, 0, targetReps, targetSets FROM plan_exercises_old WHERE targetSets > 0
                UNION ALL
                SELECT planId, position, setIndex + 1, reps, targetSets FROM expanded WHERE setIndex + 1 < targetSets
            )
            INSERT INTO plan_sets (planId, exercisePosition, setIndex, reps, weightKg)
            SELECT planId, position, setIndex, reps, NULL FROM expanded
            """.trimIndent()
        )

        // 5. The old table is no longer needed.
        db.execSQL("DROP TABLE plan_exercises_old")
    }
}