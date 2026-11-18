package ee.ut.cs.mad.strideset.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ee.ut.cs.mad.strideset.data.local.MIGRATION_2_3
import ee.ut.cs.mad.strideset.data.local.StrideSetDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Checks that a phone with the old (version 1) database keeps its data after updating the app. */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    private val dbName = "migration-test.db"

    @Test
    fun migrate4To5KeepsPlansWorkoutsAndAddsRuns() {
        helper.createDatabase(dbName, 4).use { db ->
            db.execSQL("INSERT INTO exercises (id, name, type, isCustom) VALUES (1, 'Squat', 'BARBELL', 0)")
            db.execSQL("INSERT INTO workout_plans (id, name) VALUES (1, 'Legs')")
            db.execSQL("INSERT INTO plan_exercises (planId, position, exerciseId) VALUES (1, 0, 1)")
            db.execSQL("INSERT INTO plan_sets (planId, exercisePosition, setIndex, reps, weightKg) VALUES (1, 0, 0, 8, 60.0)")
            db.execSQL("INSERT INTO workouts (id, name, planId, startedAt, finishedAt) VALUES (1, 'Legs', 1, 1000, 2000)")
            db.execSQL("INSERT INTO workout_exercises (id, workoutId, position, exerciseId) VALUES (1, 1, 0, 1)")
            db.execSQL("INSERT INTO workout_sets (id, workoutExerciseId, position, reps, weightKg, isCompleted) VALUES (1, 1, 0, 8, 60.0, 1)")
        }

        // Upgrade the existing database, then check the data as well as the new table.
        helper.runMigrationsAndValidate(dbName, 5, true).use { db ->
            db.query("SELECT p.name, e.name, s.reps, s.weightKg FROM workout_plans p " +
                "JOIN plan_exercises pe ON pe.planId = p.id " +
                "JOIN exercises e ON e.id = pe.exerciseId " +
                "JOIN plan_sets s ON s.planId = pe.planId AND s.exercisePosition = pe.position").use {
                assertTrue(it.moveToFirst())
                assertEquals("Legs", it.getString(0))
                assertEquals("Squat", it.getString(1))
                assertEquals(8, it.getInt(2))
                assertEquals(60.0, it.getDouble(3), 0.0)
            }
            db.query("SELECT w.planId, w.startedAt, w.finishedAt, s.reps, s.weightKg, s.isCompleted " +
                "FROM workouts w JOIN workout_exercises e ON e.workoutId = w.id " +
                "JOIN workout_sets s ON s.workoutExerciseId = e.id").use {
                assertTrue(it.moveToFirst())
                assertEquals(1L, it.getLong(0))
                assertEquals(1000L, it.getLong(1))
                assertEquals(2000L, it.getLong(2))
                assertEquals(8, it.getInt(3))
                assertEquals(60.0, it.getDouble(4), 0.0)
                assertEquals(1, it.getInt(5))
            }
            db.query("SELECT COUNT(*) FROM runs").use {
                it.moveToFirst()
                assertEquals(0, it.getInt(0))
            }
            // The added table also accepts a run with the expected columns.
            db.execSQL("INSERT INTO runs (startedAtMillis, activeDurationSeconds, distanceMeters, isPaused) " +
                "VALUES (3000, 60, 150.0, 0)")
            db.query("SELECT distanceMeters FROM runs").use {
                assertTrue(it.moveToFirst())
                assertEquals(150.0, it.getDouble(0), 0.0)
            }
        }
    }

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        StrideSetDatabase::class.java,
    )

    @Test
    fun migrate1To2KeepsExercises() {
        // Build a version 1 database from app/schemas/.../1.json, with one custom exercise in it.
        helper.createDatabase(dbName, 1).use { db ->
            db.execSQL("INSERT INTO exercises (name, type, isCustom) VALUES ('Cable fly', 'CABLE', 1)")
        }

        // Run the 1 -> 2 migration and check the result matches 2.json.
        helper.runMigrationsAndValidate(dbName, 2, true).use { db ->
            db.query("SELECT name FROM exercises").use { cursor ->
                cursor.moveToFirst()
                assertEquals("Cable fly", cursor.getString(0))
            }
            db.query("SELECT COUNT(*) FROM workout_plans").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrate2To3TurnsTargetsIntoSetRows() {
        // A version 2 plan: Squat, 3 sets x 8 reps.
        helper.createDatabase(dbName, 2).use { db ->
            db.execSQL("INSERT INTO exercises (id, name, type, isCustom) VALUES (1, 'Squat', 'BARBELL', 0)")
            db.execSQL("INSERT INTO workout_plans (id, name) VALUES (1, 'Legs')")
            db.execSQL(
                "INSERT INTO plan_exercises (planId, position, exerciseId, targetSets, targetReps) " +
                        "VALUES (1, 0, 1, 3, 8)"
            )
        }

        // Run the manual 2 -> 3 migration and check the result matches 3.json.
        helper.runMigrationsAndValidate(dbName, 3, true, MIGRATION_2_3).use { db ->
            db.query("SELECT exerciseId FROM plan_exercises WHERE planId = 1").use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals(1L, cursor.getLong(0))
            }
            db.query(
                "SELECT setIndex, reps, weightKg FROM plan_sets " +
                        "WHERE planId = 1 AND exercisePosition = 0 ORDER BY setIndex"
            ).use { cursor ->
                assertEquals(3, cursor.count)
                while (cursor.moveToNext()) {
                    assertEquals(cursor.position, cursor.getInt(0)) // setIndex 0, 1, 2
                    assertEquals(8, cursor.getInt(1))
                    assertTrue(cursor.isNull(2)) // no weight yet
                }
            }
        }
    }

    @Test
    fun migrate3To4KeepsPlansAndAddsWorkoutTables() {
        // A version 3 database with one plan: Squat, 1 set of 8 x 60 kg.
        helper.createDatabase(dbName, 3).use { db ->
            db.execSQL("INSERT INTO exercises (id, name, type, isCustom) VALUES (1, 'Squat', 'BARBELL', 0)")
            db.execSQL("INSERT INTO workout_plans (id, name) VALUES (1, 'Legs')")
            db.execSQL("INSERT INTO plan_exercises (planId, position, exerciseId) VALUES (1, 0, 1)")
            db.execSQL(
                "INSERT INTO plan_sets (planId, exercisePosition, setIndex, reps, weightKg) VALUES (1, 0, 0, 8, 60.0)"
            )
        }

        // Run the 3 -> 4 AutoMigration and check the result matches 4.json.
        helper.runMigrationsAndValidate(dbName, 4, true).use { db ->
            db.query("SELECT reps, weightKg FROM plan_sets").use { cursor ->
                cursor.moveToFirst()
                assertEquals(8, cursor.getInt(0))
                assertEquals(60.0, cursor.getDouble(1), 0.0)
            }
            for (table in listOf("workouts", "workout_exercises", "workout_sets")) {
                db.query("SELECT COUNT(*) FROM $table").use { cursor ->
                    cursor.moveToFirst()
                    assertEquals(0, cursor.getInt(0))
                }
            }
        }
    }
}
