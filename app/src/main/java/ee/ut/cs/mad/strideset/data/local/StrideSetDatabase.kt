package ee.ut.cs.mad.strideset.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ExerciseEntity::class], version = 1)
abstract class StrideSetDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao

    companion object {
        fun create(context: Context): StrideSetDatabase =
            Room.databaseBuilder(context, StrideSetDatabase::class.java, "strideset.db")
                .addCallback(SeedDefaultExercises)
                .build()
    }
}