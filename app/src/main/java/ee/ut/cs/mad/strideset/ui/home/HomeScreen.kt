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
import ee.ut.cs.mad.strideset.ui.theme.InactiveOrPlaceholderColor
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(32.dp))
            HomeActionButton(R.string.start_workout, onStartWorkout)
            Spacer(Modifier.height(48.dp))
            HomeActionButton(R.string.start_run, onStartRun)
            Spacer(Modifier.height(32.dp))
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun HomeActionButton(@StringRes label: Int, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(stringResource(label), style = MaterialTheme.typography.titleMedium)
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
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
                    ) {
                        Icon(
                            painter = painterResource(tab.icon),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = if (selected) MaterialTheme.colorScheme.primary else InactiveOrPlaceholderColor
                        )
                        Text(
                            text = stringResource(tab.label),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.primary else InactiveOrPlaceholderColor
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
