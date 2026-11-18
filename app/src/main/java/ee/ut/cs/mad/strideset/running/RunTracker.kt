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

/** The screen observes this state; only the service's session writes it. */
class RunTracker {
    private val mutableState = MutableStateFlow(RunTrackingState())
    val state = mutableState.asStateFlow()

    internal fun publish(state: RunTrackingState) { mutableState.value = state }
}
