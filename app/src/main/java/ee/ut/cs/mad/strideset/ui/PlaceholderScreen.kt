package ee.ut.cs.mad.strideset.ui

import androidx.annotation.StringRes

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ee.ut.cs.mad.strideset.R
import ee.ut.cs.mad.strideset.ui.components.ScreenLayout

/** Use this for placeholder screens. */
@Composable
internal fun PlaceholderScreen(@StringRes title: Int, onBack: (() -> Unit)? = null) {
    ScreenLayout(
        title = stringResource(title),
        onBack = onBack
    ) {
        Text(
            text = stringResource(R.string.placeholder_message),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
