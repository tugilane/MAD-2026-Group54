package ee.ut.cs.mad.strideset.data.repository

import ee.ut.cs.mad.strideset.data.local.RunDao
import ee.ut.cs.mad.strideset.data.local.toModel
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import kotlinx.coroutines.flow.map

class RoomRunRepository(
    private val dao: RunDao,
    private val clock: Clock = Clock.systemDefaultZone(),
) : RunRepository {
    override fun observeActiveRun() = dao.observeActiveRun().map { it?.toModel() }
    override fun observeRun(id: Long) = dao.observeRun(id).map { it?.toModel() }
    override suspend fun startRun() = dao.startRun(clock.millis())

    override suspend fun updateProgress(runId: Long, distanceMeters: Double, activeDuration: Duration) {
        require(distanceMeters.isFinite() && distanceMeters >= 0) { "Distance must be finite and nonnegative" }
        require(!activeDuration.isNegative) { "Duration must be nonnegative" }
        check(dao.updateProgress(runId, distanceMeters, activeDuration.seconds) == 1) {
            "Run is missing, finished, or progress would decrease"
        }
    }

    override suspend fun pauseRun(runId: Long) { check(dao.setPaused(runId, true) == 1) }
    override suspend fun resumeRun(runId: Long) { check(dao.setPaused(runId, false) == 1) }
    override suspend fun finishRun(runId: Long) = dao.finishAndGet(runId, clock.millis()).toModel()
    override suspend fun discardRun(runId: Long) { check(dao.discard(runId) == 1) }

    override fun observeFinishedRuns(start: LocalDate, endInclusive: LocalDate) =
        dao.observeFinishedRuns(
            start.atStartOfDay(clock.zone).toInstant().toEpochMilli(),
            endInclusive.plusDays(1).atStartOfDay(clock.zone).toInstant().toEpochMilli(),
        ).map { rows -> rows.map { it.toModel() } }
}
