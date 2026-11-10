package ee.ut.cs.mad.strideset.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import ee.ut.cs.mad.strideset.data.model.ExerciseType

/** Built-in exercises inserted once, when the database file is first created. */
object DefaultExercises {
    val all: List<Pair<String, ExerciseType>> = listOf(
        "Bench press" to ExerciseType.BARBELL,
        "Squat" to ExerciseType.BARBELL,
        "Deadlift" to ExerciseType.BARBELL,
        "Overhead press" to ExerciseType.BARBELL,
        "Barbell row" to ExerciseType.BARBELL,
        "Romanian deadlift" to ExerciseType.BARBELL,
        "Dumbbell bench press" to ExerciseType.DUMBBELL,
        "Dumbbell shoulder press" to ExerciseType.DUMBBELL,
        "Dumbbell curl" to ExerciseType.DUMBBELL,
        "Lateral raise" to ExerciseType.DUMBBELL,
        "Leg press" to ExerciseType.MACHINE,
        "Leg extension" to ExerciseType.MACHINE,
        "Leg curl" to ExerciseType.MACHINE,
        "Calf raise" to ExerciseType.MACHINE,
        "Lat pulldown" to ExerciseType.CABLE,
        "Seated cable row" to ExerciseType.CABLE,
        "Triceps pushdown" to ExerciseType.CABLE,
        "Face pull" to ExerciseType.CABLE,
        "Pull-up" to ExerciseType.BODYWEIGHT,
        "Push-up" to ExerciseType.BODYWEIGHT,
        "Dip" to ExerciseType.BODYWEIGHT,
        "Lunge" to ExerciseType.BODYWEIGHT,
    )
}

/** Room calls onCreate only the first time the database file is created, so the defaults are added once. */
internal object SeedDefaultExercises : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        DefaultExercises.all.forEach { (name, type) ->
            db.execSQL(
                "INSERT INTO exercises (name, type, isCustom) VALUES (?, ?, 0)",
                arrayOf(name, type.name),
            )
        }
    }
}