package ee.ut.cs.mad.strideset.ui.running

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.withResumed
import ee.ut.cs.mad.strideset.R
import ee.ut.cs.mad.strideset.data.AppContainer
import ee.ut.cs.mad.strideset.data.model.DistanceUnit
import ee.ut.cs.mad.strideset.running.RunCommand
import ee.ut.cs.mad.strideset.running.RunTrackingService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private enum class RunAccessIssue { PRECISE_LOCATION, GPS_DISABLED }

@Composable
internal fun RunRoute(
    activity: ComponentActivity,
    container: AppContainer,
    distanceUnit: DistanceUnit,
    onBack: () -> Unit,
) {
    val model: RunViewModel = viewModel(factory = viewModelFactory {
        initializer { RunViewModel(container.runRepository, container.runTracker, createSavedStateHandle()) }
    })
    val state by model.state.collectAsState()
    val scope = rememberCoroutineScope()
    var accessIssue by rememberSaveable { mutableStateOf<RunAccessIssue?>(null) }
    var permissionCommand by rememberSaveable { mutableStateOf<RunCommand?>(null) }
    var notificationAsked by rememberSaveable { mutableStateOf(false) }
    fun permitted(permission: String) = ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
    fun gpsEnabled() = activity.getSystemService(LocationManager::class.java).isProviderEnabled(LocationManager.GPS_PROVIDER)

    fun dispatch(command: RunCommand) {
        accessIssue = null
        if (command == RunCommand.START || command == RunCommand.RESUME) {
            if (!permitted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                accessIssue = RunAccessIssue.PRECISE_LOCATION
                return
            }
            if (!gpsEnabled()) {
                accessIssue = RunAccessIssue.GPS_DISABLED
                return
            }
        }
        val request = model.beginCommand(command) ?: return
        scope.launch {
            try {
                // Permission results can arrive before onResume; foreground location must wait for it.
                activity.lifecycle.withResumed { RunTrackingService.sendCommand(activity, command, request) }
            } catch (cancelled: CancellationException) {
                model.dispatchFailed(request)
                throw cancelled
            } catch (_: RuntimeException) {
                model.dispatchFailed(request)
            }
        }
    }

    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val command = permissionCommand
        permissionCommand = null
        // Notifications are optional. Precise location is the only permission required for tracking.
        command?.let { dispatch(it) }
    }
    DisposableEffect(activity) {
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                if ((accessIssue == RunAccessIssue.PRECISE_LOCATION && permitted(Manifest.permission.ACCESS_FINE_LOCATION)) ||
                    (accessIssue == RunAccessIssue.GPS_DISABLED && gpsEnabled())) accessIssue = null
            }
        }
        activity.lifecycle.addObserver(observer)
        onDispose { activity.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(state.discarded) {
        if (state.discarded) { model.leave(); onBack() }
    }
    RunScreen(
        state = state,
        unit = distanceUnit,
        onCommand = { command ->
            if (command == RunCommand.START || command == RunCommand.RESUME) {
                val needed = buildList {
                    if (!permitted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        add(Manifest.permission.ACCESS_COARSE_LOCATION)
                        add(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                    if (Build.VERSION.SDK_INT >= 33 && !notificationAsked && !permitted(Manifest.permission.POST_NOTIFICATIONS)) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                        notificationAsked = true
                    }
                }
                if (needed.isEmpty()) dispatch(command) else {
                    permissionCommand = command
                    permissions.launch(needed.toTypedArray())
                }
            } else dispatch(command)
        },
        onBack = { model.leave(); onBack() },
        onRetry = model::reload,
        accessMessage = accessIssue?.let {
            stringResource(if (it == RunAccessIssue.GPS_DISABLED) R.string.run_enable_gps else R.string.run_precise_required)
        },
        onOpenSettings = {
            val intent = if (accessIssue == RunAccessIssue.GPS_DISABLED) Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${activity.packageName}".toUri())
            activity.startActivity(intent)
        },
    )
}
