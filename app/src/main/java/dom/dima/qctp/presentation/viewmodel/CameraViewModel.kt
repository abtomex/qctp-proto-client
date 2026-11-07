package dom.dima.qctp.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dom.dima.qctp.domain.FileSendInteractor
import dom.dima.qctp.domain.QrCameraInteractor
import dom.dima.qctp.domain.model.SaveResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class CameraViewModel(
    private val qrCameraInteractor: QrCameraInteractor,
    private val fileSendInteractor: FileSendInteractor
) : ViewModel() {

    // Состояние для UI
    sealed interface UiEvent {
        data object PlayClickSound : UiEvent
        data class ShowMessage(val message: String) : UiEvent
    }

    private val _lastMergedFile = MutableStateFlow<Pair<String, String>?>(null)
    val lastMergedFile: StateFlow<Pair<String, String>?> = _lastMergedFile.asStateFlow()

    private val _pendingShare = MutableStateFlow<Pair<String, String>?>(null)
    val pendingShare: StateFlow<Pair<String, String>?> = _pendingShare.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

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
                            _events.send(UiEvent.PlayClickSound)
                            _events.send(UiEvent.ShowMessage("Сохранено: ${result.savedName} (осталось ${result.missing} из ${result.total})"))
                        }
                        is SaveResult.Complete -> {
                            _events.send(UiEvent.PlayClickSound)
                            _events.send(UiEvent.ShowMessage("Готово: ${result.fileName} (${result.pages} стр.)"))

                            val fileData = Pair(result.filePath, result.fileName)
                            _lastMergedFile.value = fileData
                            _pendingShare.value = fileData
                            resetLastScanned()
                        }
                        is SaveResult.Error -> {
                            _events.send(UiEvent.ShowMessage("Ошибка: ${result.message}"))
                            resetLastScanned()
                        }
                    }
                }
        }
    }

//    fun processQr1(raw: String) {
//        viewModelScope.launch {
//            when (val result = qrCameraInteractor.saveQrPayload(raw)) {
//                is SaveResult.Incomplete -> {
//                    _events.send(UiEvent.PlayClickSound)
//                    _events.send(UiEvent.ShowMessage("Сохранено: ${result.savedName} (осталось ${result.missing} из ${result.total})"))
//                }
//                is SaveResult.Complete -> {
//                    _events.send(UiEvent.PlayClickSound)
//                    _events.send(UiEvent.ShowMessage("Готово: ${result.fileName} (${result.pages} стр.)"))
//
//                    val fileData = Pair(result.filePath, result.fileName)
//                    _lastMergedFile.value = fileData
//                    _pendingShare.value = fileData
//                    resetLastScanned()
//                }
//                is SaveResult.Error -> {
//                    _events.send(UiEvent.ShowMessage("Ошибка: ${result.message}"))
//                    resetLastScanned()
//                }
//            }
//        }
//    }

    fun requestShareLast() {
        _pendingShare.value = _lastMergedFile.value
    }

    fun dismissShareDialog() {
        _pendingShare.value = null
    }

    fun confirmShare() {
        val file = _pendingShare.value ?: return
        _pendingShare.value = null
        try {
            fileSendInteractor.sendFile(file.first, file.second)
        } catch (e: Exception) {
            Log.e("Share", "Failed", e)
            viewModelScope.launch {
                _events.send(UiEvent.ShowMessage("Не удалось поделиться: ${e.message}"))
            }
        }
    }
}