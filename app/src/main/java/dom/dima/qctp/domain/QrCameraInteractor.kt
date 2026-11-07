package dom.dima.qctp.domain

import dom.dima.qctp.domain.model.SaveResult

interface QrCameraInteractor {
    suspend fun saveQrPayload(jsonString: String): SaveResult
}