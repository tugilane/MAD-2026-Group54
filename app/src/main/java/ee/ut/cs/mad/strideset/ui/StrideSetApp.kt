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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.Text
import androidx.compose.ui.tooling.preview.Preview
import ee.ut.cs.mad.strideset.R
import ee.ut.cs.mad.strideset.data.model.Exercise
import ee.ut.cs.mad.strideset.data.model.DistanceUnit
import ee.ut.cs.mad.strideset.data.model.UnitSettings
import ee.ut.cs.mad.strideset.data.model.WeightUnit
import ee.ut.cs.mad.strideset.data.model.WorkoutPlan
import ee.ut.cs.mad.strideset.ui.calendar.CalendarScreen
import ee.ut.cs.mad.strideset.ui.components.ScreenLayout
import ee.ut.cs.mad.strideset.ui.home.HomeBottomBar
import ee.ut.cs.mad.strideset.ui.home.HomeScreen
import ee.ut.cs.mad.strideset.ui.home.HomeTab
import ee.ut.cs.mad.strideset.ui.theme.StrideSetTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.time.YearMonth

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
fun StrideSetApp(
    settings: UnitSettings,
    savedPlans: Flow<List<WorkoutPlan>>,
    availableExercises: Flow<List<Exercise>> = emptyFlow(),
    onSavePlan: (suspend (WorkoutPlan) -> Unit)? = null,
    onDistanceUnitSelected: (DistanceUnit) -> Unit,
    onWeightUnitSelected: (WeightUnit) -> Unit
) {
    var destination by rememberSaveable { mutableStateOf(AppDestination.Home) }

    var selectedPlanId by rememberSaveable { mutableStateOf<Long?>(null) }
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
            } else if (destination == AppDestination.Settings) {
                SettingsScreen(
                    settings = settings,
                    onDistanceUnitSelected = onDistanceUnitSelected,
                    onWeightUnitSelected = onWeightUnitSelected
                )
            } else if (destination == AppDestination.ChooseWorkout) {
                val plans by savedPlans.collectAsState(initial = emptyList())
                ChooseWorkoutScreen(
                    plans = plans.map { WorkoutPlanChoice(it.id, it.name, it.exercises.size) },
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
                val exercises by availableExercises.collectAsState(initial = emptyList())
                val planId = selectedPlanId
                if (planId == null) {
                    PlanEditorScreen(plan = null, weightUnit = settings.weightUnit, exercises = exercises, onBack = goBack, onSave = onSavePlan)
                } else {
                    val plans: List<WorkoutPlan>? by savedPlans.collectAsState(initial = null)
                    val plan = plans?.firstOrNull { it.id == planId }
                    if (plan != null) {
                        PlanEditorScreen(plan = plan, weightUnit = settings.weightUnit, exercises = exercises, onBack = goBack, onSave = onSavePlan)
                    } else {
                        ScreenLayout(
                            title = stringResource(R.string.edit_plan_title),
                            onBack = goBack,
                        ) {
                            Text(stringResource(if (plans == null) R.string.plan_loading else R.string.plan_unavailable))
                        }
                    }
                }
            } else if (destination == AppDestination.Calendar) {
                var calendarMonth by rememberSaveable { mutableStateOf(YearMonth.now()) }
                CalendarScreen(
                    shownMonth = calendarMonth,
                    onMonthChange = { newMonth -> calendarMonth = newMonth },
                    // Empty until finished workouts and runs are stored.
                    activities = emptyList(),
                )
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
        StrideSetApp(
            settings = UnitSettings(),
            savedPlans = emptyFlow(),
            onDistanceUnitSelected = {},
            onWeightUnitSelected = {}
        )
    }
}
