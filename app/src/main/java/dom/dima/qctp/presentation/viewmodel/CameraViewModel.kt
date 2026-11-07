package dom.dima.qctp.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dom.dima.qctp.domain.QrCameraInteractor
import dom.dima.qctp.presentation.sound.SoundFeedback
import dom.dima.qctp.util.SaveResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.File

class CameraViewModel(
    private val qrCameraInteractor: QrCameraInteractor
) : ViewModel() {

    private val _lastMergedFile = MutableStateFlow<File?>(null)
    val lastMergedFile: StateFlow<File?> = _lastMergedFile.asStateFlow()

    private val _pendingShare = MutableStateFlow<File?>(null)
    val pendingShare: StateFlow<File?> = _pendingShare.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    private var lastScannedContent: String? = null

    fun shouldHandleQr(raw: String): Boolean {
        if (raw == lastScannedContent) return false
        lastScannedContent = raw
        return true
    }

    fun resetLastScanned() {
        lastScannedContent = null
    }

    fun processQr(raw: String) {
        viewModelScope.launch {
            qrCameraInteractor.saveQrPayload(raw)
                .collect { result ->
                    when (result) {
                        is SaveResult.Incomplete -> {
                            SoundFeedback.click()
                            _messages.send(
                                "Сохранено: ${result.savedName} (осталось ${result.missing} из ${result.total})"
                            )
                        }
                        is SaveResult.Complete -> {
                            SoundFeedback.click()
                            _messages.send("Готово: ${result.mergedFile.name} (${result.pages} стр.)")
                            _lastMergedFile.value = result.mergedFile
                            _pendingShare.value = result.mergedFile // Открываем диалог
                            resetLastScanned()
                        }
                        is SaveResult.Error -> {
                            _messages.send("Ошибка: ${result.message}")
                            resetLastScanned()
                        }
                    }
                }
        }
    }

    fun requestShareLast() {
        _pendingShare.value = _lastMergedFile.value
    }

    fun dismissShareDialog() {
        _pendingShare.value = null
    }

    fun confirmShare() {
        _pendingShare.value = null
    }

    fun onShareError(e: Exception) {
        Log.e("Share", "Failed", e)
        viewModelScope.launch {
            _messages.send("Не удалось поделиться: ${e.message}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        SoundFeedback.release()
    }
}