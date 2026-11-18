package ee.ut.cs.mad.strideset.running

import ee.ut.cs.mad.strideset.data.model.Run
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RunTrackingState(
    val run: Run? = null,
    val gpsState: GpsState = GpsState.SEARCHING,
    val serviceRunning: Boolean = false,
    val error: String? = null,
)

data class RunCommandResult(val requestId: String, val succeeded: Boolean)

/** The screen observes this state; only the service's session writes it. */
class RunTracker {
    private val mutableState = MutableStateFlow(RunTrackingState())
    val state = mutableState.asStateFlow()
    private val mutableResult = MutableStateFlow<RunCommandResult?>(null)
    val commandResult = mutableResult.asStateFlow()

    internal fun publish(state: RunTrackingState) { mutableState.value = state }
    internal fun completeCommand(requestId: String, succeeded: Boolean) {
        mutableResult.value = RunCommandResult(requestId, succeeded)
    }
}
