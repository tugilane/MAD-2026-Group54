package ee.ut.cs.mad.strideset.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ee.ut.cs.mad.strideset.data.local.DefaultExercises
import ee.ut.cs.mad.strideset.data.local.SeedDefaultExercises
import ee.ut.cs.mad.strideset.data.local.StrideSetDatabase
import ee.ut.cs.mad.strideset.data.model.ExerciseType
import ee.ut.cs.mad.strideset.data.repository.ExerciseNameTakenException
import ee.ut.cs.mad.strideset.data.repository.RoomExerciseRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomExerciseRepositoryTest {
    private lateinit var db: StrideSetDatabase
    private lateinit var repository: RoomExerciseRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // In-memory: a fresh, empty database for every test, deleted afterwards.
        db = Room.inMemoryDatabaseBuilder(context, StrideSetDatabase::class.java)
            .addCallback(SeedDefaultExercises)
            .build()
        repository = RoomExerciseRepository(db.exerciseDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun newDatabaseContainsDefaultExercises() = runBlocking {
        val exercises = repository.observeExercises().first()

        assertEquals(DefaultExercises.all.size, exercises.size)
        assertTrue(exercises.none { it.isCustom })
    }

    @Test
    fun addedExerciseIsSavedAndListed() = runBlocking {
        val added = repository.addCustomExercise("  Cable fly ", ExerciseType.CABLE)

        assertEquals("Cable fly", added.name)
        assertTrue(added.isCustom)
        val exercises = repository.observeExercises().first()
        assertTrue(added in exercises)
        assertEquals(exercises.sortedBy { it.name.lowercase() }, exercises)
    }

    @Test
    fun duplicateNameInAnyCaseIsRejected() = runBlocking {
        assertThrows(ExerciseNameTakenException::class.java) {
            runBlocking { repository.addCustomExercise("bench PRESS", ExerciseType.BARBELL) }
        }
        assertFalse(repository.observeExercises().first().any { it.isCustom })
    }

    @Test
    fun blankNameIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.addCustomExercise("   ", ExerciseType.OTHER) }
        }
    }
}