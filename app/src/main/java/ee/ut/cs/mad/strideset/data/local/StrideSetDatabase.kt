package ee.ut.cs.mad.strideset.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.AutoMigration

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutPlanEntity::class,
        PlanExerciseEntity::class,
    ],
    version = 2,
    autoMigrations = [
        AutoMigration(from = 1, to = 2), // adds workout_plans and plan_exercises
    ],
)
abstract class StrideSetDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutPlanDao(): WorkoutPlanDao

    companion object {
        fun create(context: Context): StrideSetDatabase =
            Room.databaseBuilder(context, StrideSetDatabase::class.java, "strideset.db")
                .addCallback(SeedDefaultExercises)
                .build()
    }
}