package ee.ut.cs.mad.strideset.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ee.ut.cs.mad.strideset.data.local.StrideSetDatabase
import org.junit.Assert.assertEquals
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
}