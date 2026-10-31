package dom.dima.qctp.ui.screen

import android.util.Log
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dom.dima.qctp.sound.SoundFeedback
import dom.dima.qctp.util.SaveResult
import dom.dima.qctp.util.saveQrPayload
import dom.dima.qctp.util.shareMergedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreviewWithScanner() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Защита от повторного сканирования того же QR подряд
    val lastScanned = remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Последний собранный итоговый файл — для кнопки "Отправить файл"
    var lastMergedFile by remember { mutableStateOf<File?>(null) }
    // Файл, для которого сейчас показан диалог отправки
    var pendingShare by remember { mutableStateOf<File?>(null) }

    // --- Зум ---
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    // Линейный зум для ползунка (0f..1f)
    var linearZoom by remember { mutableFloatStateOf(0f) }
    // Текущий зум в "разах" — для жеста щипка
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var minZoomRatio by remember { mutableFloatStateOf(1f) }
    var maxZoomRatio by remember { mutableFloatStateOf(1f) }

    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            SoundFeedback.release()
        }
    }

    Box(Modifier.fillMaxSize()) {
        // --- Превью камеры + анализатор ---
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = surfaceProvider
                        }

                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        val scanner = BarcodeScanning.getClient(
                            BarcodeScannerOptions.Builder()
                                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                                .build()
                        )

                        analysis.setAnalyzer(executor) { imageProxy: ImageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage == null) {
                                imageProxy.close()
                                return@setAnalyzer
                            }
                            val image = InputImage.fromMediaImage(
                                mediaImage, imageProxy.imageInfo.rotationDegrees
                            )
                            scanner.process(image)
                                .addOnSuccessListener { codes ->
                                    for (b in codes) {
                                        val raw = b.rawValue ?: continue
                                        if (raw == lastScanned.value) continue
                                        lastScanned.value = raw

                                        scope.launch {
                                            val result = withContext(Dispatchers.IO) {
                                                saveQrPayload(ctx, raw)
                                            }
                                            when (result) {
                                                is SaveResult.Incomplete -> {
                                                    SoundFeedback.click()
                                                    snackbarHostState.showSnackbar(
                                                        "Сохранено: ${result.savedName} " +
                                                                "(осталось ${result.missing} из ${result.total})"
                                                    )
                                                }
                                                is SaveResult.Complete -> {
                                                    SoundFeedback.click()
                                                    snackbarHostState.showSnackbar(
                                                        "Готово: ${result.mergedFile.name} " +
                                                                "(${result.pages} стр.)"
                                                    )
                                                    lastMergedFile = result.mergedFile
                                                    pendingShare = result.mergedFile
                                                    lastScanned.value = null
                                                }
                                                is SaveResult.Error -> {
                                                    snackbarHostState.showSnackbar(
                                                        "Ошибка: ${result.message}"
                                                    )
                                                    lastScanned.value = null
                                                }
                                            }
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("MLKit", "Scan failed", e)
                                }
                                .addOnCompleteListener { imageProxy.close() }
                        }

                        try {
                            cameraProvider.unbindAll()
                            val camera: Camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                analysis
                            )

                            // --- Инициализируем данные зума из CameraInfo ---
                            cameraControl = camera.cameraControl
                            camera.cameraInfo.zoomState.value?.let { state ->
                                minZoomRatio = state.minZoomRatio
                                maxZoomRatio = state.maxZoomRatio
                                zoomRatio = state.zoomRatio
                                // setLinearZoom оперирует 0..1, а zoomRatio — "разами".
                                // Для начального положения ползунка переведём:
                                linearZoom = if (maxZoomRatio > minZoomRatio) {
                                    (zoomRatio - minZoomRatio) / (maxZoomRatio - minZoomRatio)
                                } else 0f
                            }

                            // --- Жест "щипок" ---
                            val scaleDetector = ScaleGestureDetector(
                                ctx,
                                object : SimpleOnScaleGestureListener() {
                                    override fun onScale(detector: ScaleGestureDetector): Boolean {
                                        val control = cameraControl ?: return false
                                        val current = camera.cameraInfo.zoomState.value
                                            ?.zoomRatio ?: zoomRatio
                                        val newZoom = (current * detector.scaleFactor)
                                            .coerceIn(minZoomRatio, maxZoomRatio)

                                        control.setZoomRatio(newZoom)
                                        zoomRatio = newZoom
                                        // Синхронизируем ползунок
                                        linearZoom = if (maxZoomRatio > minZoomRatio) {
                                            (newZoom - minZoomRatio) / (maxZoomRatio - minZoomRatio)
                                        } else 0f
                                        return true
                                    }
                                }
                            )

                            setOnTouchListener { _, event ->
                                scaleDetector.onTouchEvent(event)
                                true
                            }
                        } catch (e: Exception) {
                            Log.e("CameraX", "Bind failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            }
        )

        // --- Snackbar внизу ---
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // --- Ползунок зума внизу ---
        cameraControl?.let { control ->
            Slider(
                value = linearZoom,
                onValueChange = { value ->
                    linearZoom = value
                    control.setLinearZoom(value)
                    // Синхронизируем zoomRatio (для жеста)
                    zoomRatio = minZoomRatio + value * (maxZoomRatio - minZoomRatio)
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 80.dp)
            )
        }

        // --- Кнопка "Отправить файл" сверху справа ---
        if (lastMergedFile != null) {
            Button(
                onClick = { pendingShare = lastMergedFile },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Text("Отправить файл")
            }
        }

        // --- Диалог отправки ---
        pendingShare?.let { file ->
            AlertDialog(
                onDismissRequest = { pendingShare = null },
                title = { Text("Комплект собран") },
                text = { Text("Файл ${file.name} готов. Отправить?") },
                confirmButton = {
                    TextButton(onClick = {
                        pendingShare = null
                        try {
                            shareMergedFile(context, file)
                        } catch (e: Exception) {
                            Log.e("Share", "Failed", e)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "Не удалось поделиться: ${e.message}"
                                )
                            }
                        }
                    }) { Text("Отправить") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingShare = null }) { Text("Позже") }
                }
            )
        }
    }
}