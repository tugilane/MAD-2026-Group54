package ee.ut.cs.mad.strideset.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ee.ut.cs.mad.strideset.R
import ee.ut.cs.mad.strideset.data.model.DistanceUnit
import ee.ut.cs.mad.strideset.data.model.UnitSettings
import ee.ut.cs.mad.strideset.data.model.WeightUnit
import ee.ut.cs.mad.strideset.ui.components.ScreenLayout
import ee.ut.cs.mad.strideset.ui.components.UnitSelector
import ee.ut.cs.mad.strideset.ui.theme.Dimens

@Composable
fun SettingsScreen(
    settings: UnitSettings,
    onDistanceUnitSelected: (DistanceUnit) -> Unit,
    onWeightUnitSelected: (WeightUnit) -> Unit
) {
    ScreenLayout(
        title = stringResource(R.string.nav_settings)
    ) {
        Text(stringResource(R.string.distance_heading), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Dimens.spacingXSmall))
        Text(stringResource(R.string.distance_description), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(Dimens.spacingMedium))
        UnitSelector(
            options = DistanceUnit.entries,
            selectedOption = settings.distanceUnit,
            onOptionSelected = onDistanceUnitSelected,
            optionLabel = { unit ->
                stringResource(
                    when (unit) {
                        DistanceUnit.KILOMETERS -> R.string.distance_kilometers
                        DistanceUnit.MILES -> R.string.distance_miles
                    }
                )
            }
        )

        Spacer(Modifier.height(Dimens.spacingXLarge))

        Text(stringResource(R.string.weight_heading), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Dimens.spacingXSmall))
        Text(stringResource(R.string.weight_description), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(Dimens.spacingMedium))
        UnitSelector(
            options = WeightUnit.entries,
            selectedOption = settings.weightUnit,
            onOptionSelected = onWeightUnitSelected,
            optionLabel = { unit ->
                stringResource(
                    when (unit) {
                        WeightUnit.KILOGRAMS -> R.string.weight_kilograms
                        WeightUnit.POUNDS -> R.string.weight_pounds
                    }
                )
            }
        )
    }
}
