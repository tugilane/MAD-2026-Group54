package ee.ut.cs.mad.strideset.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ee.ut.cs.mad.strideset.R
import ee.ut.cs.mad.strideset.ui.theme.Dimens
import ee.ut.cs.mad.strideset.ui.theme.StrideSetTheme

/** Small round outlined button with an icon, used for Back and month navigation. */
@Composable
fun RoundIconButton(
    @DrawableRes iconRes: Int,
    @StringRes contentDescription: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedIconButton(
        onClick = onClick,
        modifier = modifier.size(Dimens.backButtonSize),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = IconButtonDefaults.outlinedIconButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = stringResource(contentDescription),
            modifier = Modifier.size(Dimens.smallIconSize)
        )
    }
}

@Preview
@Composable
private fun RoundIconButtonPreview() {
    StrideSetTheme {
        RoundIconButton(
            R.drawable.ic_chevron_left,
            R.string.calendar_previous_month,
            onClick = {}
        )
    }
}