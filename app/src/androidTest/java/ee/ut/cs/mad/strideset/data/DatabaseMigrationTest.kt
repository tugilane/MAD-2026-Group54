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
}