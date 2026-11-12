package ee.ut.cs.mad.strideset.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import ee.ut.cs.mad.strideset.ui.theme.Dimens

/** Displays unit options; the caller owns the selection and supplies their labels. */
@Composable
fun <T> UnitSelector(
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    optionLabel: @Composable (T) -> String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(Modifier.fillMaxWidth().padding(Dimens.spacingXSmall).selectableGroup()) {
            options.forEach { option ->
                val selected = selectedOption == option

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { onOptionSelected(option) }
                        ),
                    shape = MaterialTheme.shapes.medium,
                    color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Box(
                        modifier = Modifier
                            .heightIn(min = Dimens.minTouchTarget)
                            .padding(Dimens.spacingSmall),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(optionLabel(option))
                    }
                }
            }
        }
    }
}
