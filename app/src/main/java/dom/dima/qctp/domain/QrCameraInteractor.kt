package dom.dima.qctp.domain

import dom.dima.qctp.util.SaveResult
import kotlinx.coroutines.flow.Flow

interface QrCameraInteractor {
    fun saveQrPayload(jsonString: String) : Flow<SaveResult>
}