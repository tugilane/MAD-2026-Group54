package ee.ut.cs.mad.strideset.ui.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ee.ut.cs.mad.strideset.R
import ee.ut.cs.mad.strideset.ui.StrideSetScaffold
import ee.ut.cs.mad.strideset.ui.components.PrimaryButton
import ee.ut.cs.mad.strideset.ui.components.SecondaryButton
import ee.ut.cs.mad.strideset.ui.theme.Dimens
import ee.ut.cs.mad.strideset.ui.theme.InactiveColor
import ee.ut.cs.mad.strideset.ui.theme.StrideSetTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

enum class HomeTab(@param:StringRes val label: Int, @param:DrawableRes val icon: Int) {
    Start(R.string.nav_start, R.drawable.ic_home),
    Calendar(R.string.nav_calendar, R.drawable.ic_calendar),
    Settings(R.string.nav_settings, R.drawable.ic_settings)
}

@Composable
fun HomeScreen(
    onStartWorkout: () -> Unit,
    onStartRun: () -> Unit,
    modifier: Modifier = Modifier,
    date: LocalDate = LocalDate.now()
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(horizontal = Dimens.spacingLarge, vertical = Dimens.spacingMedium)
        ) {
            Text(
                text = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(Dimens.spacingXLarge))
            PrimaryButton(R.string.start_workout, onStartWorkout)
            Spacer(Modifier.height(48.dp))
            PrimaryButton(R.string.start_run, onStartRun)
            Spacer(Modifier.height(Dimens.spacingXLarge))
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
fun HomeBottomBar(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Dimens.spacingMedium, vertical = Dimens.spacingMedium)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium)
        ) {
            HomeTab.entries.forEach { tab ->
                val selected = tab == selectedTab
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.surfaceContainer
                    } else MaterialTheme.colorScheme.background,
                    border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null
                ) {
                    Column(
                        modifier = Modifier
                            .selectable(selected = selected, role = Role.Tab, onClick = { onTabSelected(tab) })
                            .heightIn(min = 56.dp)
                            .padding(horizontal = Dimens.spacingXSmall, vertical = Dimens.spacingSmall),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Dimens.spacingXSmall, Alignment.CenterVertically)
                    ) {
                        Icon(
                            painter = painterResource(tab.icon),
                            contentDescription = null,
                            modifier = Modifier.size(Dimens.bottomBarIconSize),
                            tint = if (selected) MaterialTheme.colorScheme.primary else InactiveColor
                        )
                        Text(
                            text = stringResource(tab.label),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.primary else InactiveColor
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Home small", showBackground = true, widthDp = 360, heightDp = 800)
@Preview(name = "Home medium", showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun HomeScreenPreview() {
    StrideSetTheme {
        StrideSetScaffold(selectedTab = HomeTab.Start, onTabSelected = {}) { padding ->
            HomeScreen(
                onStartWorkout = {},
                onStartRun = {},
                modifier = Modifier.padding(padding),
                date = LocalDate.of(2026, 11, 5)
            )
        }
    }
}
