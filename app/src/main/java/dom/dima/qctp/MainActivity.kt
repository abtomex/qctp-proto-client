package dom.dima.qctp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.lifecycleScope
import dom.dima.qctp.di.dataModule
import dom.dima.qctp.di.interactorModule
import dom.dima.qctp.di.repositoryModule
import dom.dima.qctp.di.viewModelModule
import dom.dima.qctp.presentation.ui.screen.QrScannerScreen
import dom.dima.qctp.util.clearScannedData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startKoin {
            androidContext(this@MainActivity)
            modules(dataModule, repositoryModule, interactorModule, viewModelModule)
        }

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