package ee.ut.cs.mad.strideset.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ee.ut.cs.mad.strideset.R
import ee.ut.cs.mad.strideset.ui.theme.Dimens
import ee.ut.cs.mad.strideset.ui.theme.PrimaryActionTextStyle
import ee.ut.cs.mad.strideset.ui.theme.StrideSetTheme

/**
 * For buttons such as "Start workout", "Start run", "Finish Workout" etc.
 * */
@Composable
fun PrimaryButton(@StringRes label: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.buttonHeight),
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(stringResource(label), style = PrimaryActionTextStyle)
    }
}

@Preview
@Composable
private fun PrimaryButtonPreview() {
    StrideSetTheme {
        PrimaryButton(label = R.string.start_workout, onClick = {})
    }
}