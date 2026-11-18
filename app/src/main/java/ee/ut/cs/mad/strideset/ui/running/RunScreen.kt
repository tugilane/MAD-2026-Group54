package ee.ut.cs.mad.strideset.ui.running

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ee.ut.cs.mad.strideset.R
import ee.ut.cs.mad.strideset.data.model.DistanceUnit
import ee.ut.cs.mad.strideset.running.GpsState
import ee.ut.cs.mad.strideset.running.RunCommand
import ee.ut.cs.mad.strideset.ui.components.PrimaryButton
import ee.ut.cs.mad.strideset.ui.components.ScreenLayout
import ee.ut.cs.mad.strideset.ui.components.SecondaryButton
import ee.ut.cs.mad.strideset.ui.theme.Dimens
import java.time.Duration

private enum class RunDialog { LEAVE, FINISH, DISCARD }

@Composable
internal fun RunScreen(
    state: RunUiState,
    unit: DistanceUnit,
    onCommand: (RunCommand) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    accessMessage: String? = null,
    onOpenSettings: (() -> Unit)? = null,
) {
    val run = state.run
    val complete = run?.isFinished == true
    var dialog by rememberSaveable { mutableStateOf<RunDialog?>(null) }
    val back = {
        if (!state.busy) {
            if (run != null && !complete) dialog = RunDialog.LEAVE else onBack()
        }
    }
    BackHandler { back() }
    ScreenLayout(title = stringResource(if (complete) R.string.run_complete else R.string.run_title), onBack = back) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.loading) {
                CircularProgressIndicator()
                Text(stringResource(R.string.run_loading))
            } else {
                Text(
                    stringResource(when {
                        complete -> R.string.run_saved
                        run == null -> R.string.run_ready
                        run.isPaused -> R.string.run_paused
                        state.gpsState == GpsState.TRACKING -> R.string.run_gps_tracking
                        state.gpsState == GpsState.SIGNAL_LOST -> R.string.run_gps_lost
                        else -> R.string.run_gps_searching
                    }),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(stringResource(R.string.run_distance_label), style = MaterialTheme.typography.labelSmall)
                Text(formatRunDistance(run?.distanceMeters ?: 0.0, unit), style = MaterialTheme.typography.displayMedium)
                Text(stringResource(if (unit == DistanceUnit.MILES) R.string.run_miles_short else R.string.run_km_short))
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
                    Row(Modifier.fillMaxWidth().padding(Dimens.cardPadding), horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)) {
                        RunMetric(stringResource(R.string.run_active_time), formatRunDuration(run?.activeDuration ?: Duration.ZERO), Modifier.weight(1f))
                        RunMetric(
                            stringResource(if (unit == DistanceUnit.MILES) R.string.run_pace_miles else R.string.run_pace_km),
                            formatRunPace(state.paceRun?.distanceMeters ?: 0.0, state.paceRun?.activeDuration ?: Duration.ZERO, unit),
                            Modifier.weight(1f),
                        )
                    }
                }
                if (state.recovered) Text(stringResource(R.string.run_recovered), style = MaterialTheme.typography.bodyMedium)
                val error = accessMessage ?: state.errorText ?: state.errorRes?.let { stringResource(it) }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (accessMessage != null && onOpenSettings != null) {
                    TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.run_open_settings)) }
                }
                if (state.loadFailed) SecondaryButton(R.string.run_retry, onRetry)
            }
        }
        Spacer(Modifier.height(Dimens.spacingLarge))
        if (!state.loading && (!state.loadFailed || run != null)) {
            when {
                complete -> PrimaryButton(R.string.run_done, onBack)
                run == null -> PrimaryButton(R.string.start_run, { onCommand(RunCommand.START) }, enabled = !state.busy)
                else -> {
                    PrimaryButton(
                        if (run.isPaused) R.string.run_resume else R.string.run_pause,
                        { onCommand(if (run.isPaused) RunCommand.RESUME else RunCommand.PAUSE) },
                        enabled = !state.busy,
                    )
                    Spacer(Modifier.height(Dimens.spacingSmall))
                    SecondaryButton(R.string.run_finish, { dialog = RunDialog.FINISH }, enabled = !state.busy)
                    TextButton(onClick = { dialog = RunDialog.DISCARD }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.run_discard), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        if (state.busy) Text(stringResource(R.string.run_updating), style = MaterialTheme.typography.bodyMedium)
    }
    if (dialog != null) {
        AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(stringResource(when (dialog) {
                RunDialog.LEAVE -> R.string.run_leave_title
                RunDialog.FINISH -> R.string.run_finish_title
                else -> R.string.run_discard_title
            })) },
            text = {
                if (dialog == RunDialog.LEAVE) {
                    Column {
                        Text(stringResource(if (run?.isPaused == true) R.string.run_leave_paused_message else R.string.run_leave_message))
                        TextButton(onClick = { dialog = null; onBack() }) {
                            Text(stringResource(if (run?.isPaused == true) R.string.run_leave_paused else R.string.run_background))
                        }
                        TextButton(onClick = { dialog = RunDialog.FINISH }) { Text(stringResource(R.string.run_finish)) }
                        TextButton(onClick = { dialog = RunDialog.DISCARD }) { Text(stringResource(R.string.run_discard)) }
                    }
                } else Text(stringResource(if (dialog == RunDialog.FINISH) R.string.run_finish_message else R.string.run_discard_message))
            },
            confirmButton = {
                TextButton(onClick = {
                    val action = dialog
                    dialog = null
                    when (action) {
                        RunDialog.FINISH -> onCommand(RunCommand.FINISH)
                        RunDialog.DISCARD -> onCommand(RunCommand.DISCARD)
                        else -> Unit
                    }
                }) { Text(stringResource(when (dialog) {
                    RunDialog.FINISH -> R.string.run_save
                    RunDialog.DISCARD -> R.string.run_confirm_discard
                    else -> R.string.run_continue
                })) }
            },
            dismissButton = {
                if (dialog != RunDialog.LEAVE) TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.run_cancel)) }
            },
        )
    }
}

@Composable
private fun RunMetric(label: String, value: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
