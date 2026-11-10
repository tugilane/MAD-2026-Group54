package ee.ut.cs.mad.strideset.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ee.ut.cs.mad.strideset.R

@Composable
internal fun PlaceholderScreen(@StringRes title: Int, onBack: (() -> Unit)? = null) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        if (onBack != null) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.navigate_back))
            }
        }
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.placeholder_message), style = MaterialTheme.typography.bodyLarge)
    }
}
