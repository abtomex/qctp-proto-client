package dom.dima.qctp.ui.screen

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QrScannerScreen() {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGrantedCompat()) {
            cameraPermission.launchPermissionRequest()
        }
    }

    if (cameraPermission.status.isGrantedCompat()) {
        CameraPreviewWithScanner()
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Нужно разрешение камеры")
                Spacer(Modifier.height(12.dp))
                Button(onClick = { cameraPermission.launchPermissionRequest() }) {
                    Text("Разрешить")
                }
            }
        }
    }
}

// маленький хелпер, чтобы не тянуть isGranted extension
@OptIn(ExperimentalPermissionsApi::class)
private fun com.google.accompanist.permissions.PermissionStatus.isGrantedCompat(): Boolean =
    this is com.google.accompanist.permissions.PermissionStatus.Granted