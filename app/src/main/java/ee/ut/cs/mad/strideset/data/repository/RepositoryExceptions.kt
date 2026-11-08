package ee.ut.cs.mad.strideset.data.repository

class ExerciseNameTakenException(name: String) :
    Exception("An exercise named \"$name\" already exists")

class EmptyWorkoutException :
    Exception("A workout needs at least one completed set before it can be finished")