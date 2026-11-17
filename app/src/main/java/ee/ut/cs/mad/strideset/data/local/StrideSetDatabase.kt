package ee.ut.cs.mad.strideset.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutPlanEntity::class,
        PlanExerciseEntity::class,
        PlanSetEntity::class,
    ],
    version = 3,
    autoMigrations = [
        AutoMigration(from = 1, to = 2), // adds workout_plans and plan_exercises
        // 2 -> 3 is MIGRATION_2_3 (manual, in Migrations.kt)
    ],
)
abstract class StrideSetDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutPlanDao(): WorkoutPlanDao

    companion object {
        fun create(context: Context): StrideSetDatabase =
            Room.databaseBuilder(context, StrideSetDatabase::class.java, "strideset.db")
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // Seed exercises first so the sample plans can link to them by name. Can't create smaple plans wihtout existing smaple exercises yo.
                        SeedDefaultExercises.onCreate(db)
                        seedSamplePlans(db)
                    }
                })
                .addMigrations(MIGRATION_2_3)
                .build()
    }
}
