package dom.dima.qctp.domain.impl

import android.util.Base64
import android.util.Log
import dom.dima.qctp.domain.FileRepository
import dom.dima.qctp.domain.QrCameraInteractor
import dom.dima.qctp.domain.model.SaveResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject

class QrCameraInteractorImpl(
    private val fileRepository: FileRepository
) : QrCameraInteractor {

    override fun saveQrPayload(jsonString: String): Flow<SaveResult> = flow {
        try {
            val json = JSONObject(jsonString)
            val fileName = json.getString("fileName")
            val page = json.getInt("page")
            val totalPages = json.getInt("totalPages")
            val contentBase64 = json.getString("content")

            if (fileName.contains("/") || fileName.contains("..") || fileName.isBlank()) {
                emit(SaveResult.Error("Invalid fileName"))
                return@flow
            }
            if (page < 1 || totalPages < 1 || page > totalPages) {
                emit(SaveResult.Error("Invalid page/totalPages"))
                return@flow
            }

            val bytes = try {
                Base64.decode(contentBase64, Base64.NO_WRAP)
            } catch (_: IllegalArgumentException) {
                emit(SaveResult.Error("Invalid base64 in content"))
                return@flow
            }

            fileRepository.initBookDir(fileName)
            val pageFileName = fileRepository.writePartFile(fileName, page, bytes)

            if (!fileRepository.isAllPagesPresent(fileName, totalPages)) {
                val missingCount = fileRepository.calculateMissingPages(fileName, totalPages)
                emit(SaveResult.Incomplete(pageFileName, missingCount, totalPages))
                return@flow
            }

            val (filePath, mergedName) = fileRepository.mergeFile(fileName, totalPages)
            fileRepository.removePartsFragments(fileName, totalPages)

            // Вот здесь мы отправляем успешный результат
            emit(SaveResult.Complete(filePath, mergedName, totalPages))

        } catch (e: Exception) {
            Log.e("SavePayload", "Error", e)
            emit(SaveResult.Error(e.message ?: "Unknown error"))
        }
    }.flowOn(Dispatchers.IO)
}