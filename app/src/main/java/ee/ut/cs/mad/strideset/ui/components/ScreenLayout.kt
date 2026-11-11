package ee.ut.cs.mad.strideset.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ee.ut.cs.mad.strideset.R
import ee.ut.cs.mad.strideset.ui.theme.Dimens
import ee.ut.cs.mad.strideset.ui.theme.StrideSetTheme

/** Template for creating different screen layouts. */
@Composable
fun ScreenLayout(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimens.screenMargin)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (onBack != null) {
                OutlinedIconButton(
                    onClick = onBack,
                    modifier = Modifier.size(Dimens.backButtonSize),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = IconButtonDefaults.outlinedIconButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_back),
                        contentDescription = stringResource(R.string.navigate_back),
                        modifier = Modifier.size(Dimens.smallIconSize)
                    )
                }
                Spacer(Modifier.width(Dimens.spacingMedium))
            }
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
                style = MaterialTheme.typography.titleLarge
            )
            actions()
        }
        Spacer(Modifier.height(Dimens.spacingXLarge))
        content() // Content of the page goes here
    }
}

@Preview(showBackground = true)
@Composable
private fun ScreenLayoutPreview() {
    StrideSetTheme {
        ScreenLayout(
            title = "New plan",
            onBack = {},
            actions = { TextButton(onClick = {}) { Text("Save") } }
        ) {
            Text("Something")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScreenLayoutTitleOnlyPreview() {
    StrideSetTheme {
        ScreenLayout(title = "Calendar") {
            Text("Something")
        }
    }
}