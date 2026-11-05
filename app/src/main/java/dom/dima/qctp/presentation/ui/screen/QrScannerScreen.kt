package dom.dima.qctp.presentation.ui.screen

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState

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
private fun PermissionStatus.isGrantedCompat(): Boolean =
    this is PermissionStatus.Granted