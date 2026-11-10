package ee.ut.cs.mad.strideset.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import ee.ut.cs.mad.strideset.data.model.Exercise
import ee.ut.cs.mad.strideset.data.model.ExerciseType

/** One row in the exercises table. Names are unique ignoring case ("Squat" and "squat" clash). */
@Entity(
    tableName = "exercises",
    indices = [Index(value = ["name"], unique = true)],
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
    val type: ExerciseType,
    val isCustom: Boolean,
)

fun ExerciseEntity.toModel() = Exercise(id = id, name = name, type = type, isCustom = isCustom)