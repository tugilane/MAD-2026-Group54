package ee.ut.cs.mad.strideset

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import ee.ut.cs.mad.strideset.data.model.UnitSettings
import ee.ut.cs.mad.strideset.ui.StrideSetApp
import ee.ut.cs.mad.strideset.ui.theme.StrideSetTheme
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsRepository = (application as StrideSetApplication).container.settingsRepository
        val settingsFlow = settingsRepository.settings.catch { error ->
            if (error is IOException) {
                Toast.makeText(this@MainActivity, R.string.settings_load_error, Toast.LENGTH_LONG).show()
            } else {
                throw error
            }
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContent {
            val settings by settingsFlow.collectAsState(initial = UnitSettings())
            StrideSetTheme {
                StrideSetApp(
                    settings = settings,
                    onDistanceUnitSelected = { unit ->
                        saveSetting { settingsRepository.setDistanceUnit(unit) }
                    },
                    onWeightUnitSelected = { unit ->
                        saveSetting { settingsRepository.setWeightUnit(unit) }
                    }
                )
            }
        }
    }

    private fun saveSetting(update: suspend () -> Unit) {
        lifecycleScope.launch {
            try {
                update()
            } catch (error: IOException) {
                Toast.makeText(this@MainActivity, R.string.settings_save_error, Toast.LENGTH_LONG).show()
            }
        }
    }
}
