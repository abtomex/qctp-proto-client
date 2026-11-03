package dom.dima.qctp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.lifecycleScope
import dom.dima.qctp.ui.screen.QrScannerScreen
import dom.dima.qctp.util.clearScannedData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Очистка папки при запуске — в фоновом потоке
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                clearScannedData(applicationContext)
            }
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    QrScannerScreen()
                }
            }
        }
    }
}