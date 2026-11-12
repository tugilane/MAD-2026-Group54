package ee.ut.cs.mad.strideset.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ee.ut.cs.mad.strideset.R
import ee.ut.cs.mad.strideset.ui.home.HomeBottomBar
import ee.ut.cs.mad.strideset.ui.home.HomeScreen
import ee.ut.cs.mad.strideset.ui.home.HomeTab
import ee.ut.cs.mad.strideset.ui.theme.StrideSetTheme

private enum class AppDestination(
    @param:StringRes val title: Int,
    val tab: HomeTab? = null
) {
    Home(R.string.app_name, HomeTab.Start),
    ChooseWorkout(R.string.choose_workout_title),
    Workout(R.string.workout_title),
    PlanEditor(R.string.new_plan_title),
    Run(R.string.run_title),
    Calendar(R.string.nav_calendar, HomeTab.Calendar),
    Settings(R.string.nav_settings, HomeTab.Settings)
}

@Composable
fun StrideSetApp() {
    var destination by rememberSaveable { mutableStateOf(AppDestination.Home) }

    var selectedPlanId by rememberSaveable { mutableStateOf<Long?>(null) }
    // Temporary choices until the plan repository can supply saved plans.
    val plans = listOf(
        WorkoutPlanChoice(1L, stringResource(R.string.sample_full_body), 3),
        WorkoutPlanChoice(2L, stringResource(R.string.sample_upper_body), 2),
    )
    val goBack = {
        destination = when (destination) {
            AppDestination.Workout, AppDestination.PlanEditor -> AppDestination.ChooseWorkout
            else -> AppDestination.Home
        }
        selectedPlanId = null
    }
    BackHandler(enabled = destination != AppDestination.Home) {
        goBack()
    }

    StrideSetScaffold(
        selectedTab = destination.tab,
        onTabSelected = { tab ->
            destination = AppDestination.entries.first { it.tab == tab }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (destination == AppDestination.Home) {
                HomeScreen(
                    onStartWorkout = { destination = AppDestination.ChooseWorkout },
                    onStartRun = { destination = AppDestination.Run }
                )
            } else if (destination == AppDestination.ChooseWorkout) {
                ChooseWorkoutScreen(
                    plans = plans,
                    onBack = goBack,
                    onStartWorkout = { planId ->
                        selectedPlanId = planId
                        destination = AppDestination.Workout
                    },
                    onEditPlan = { planId ->
                        selectedPlanId = planId
                        destination = AppDestination.PlanEditor
                    },
                )
            } else if (destination == AppDestination.Workout) {
                WorkoutScreen(planId = selectedPlanId, onBack = goBack)
            } else if (destination == AppDestination.PlanEditor) {
                PlanEditorScreen(planId = selectedPlanId, onBack = goBack)
            } else {
                PlaceholderScreen(
                    title = destination.title,
                    onBack = if (destination.tab == null) goBack else null,
                )
            }
        }
    }
}

@Composable
internal fun StrideSetScaffold(
    selectedTab: HomeTab?,
    onTabSelected: (HomeTab) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        bottomBar = {
            if (selectedTab != null) {
                HomeBottomBar(selectedTab = selectedTab, onTabSelected = onTabSelected)
            }
        },
        content = content
    )
}

@Preview(name = "App navigation", showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun StrideSetAppPreview() {
    StrideSetTheme {
        StrideSetApp()
    }
}
