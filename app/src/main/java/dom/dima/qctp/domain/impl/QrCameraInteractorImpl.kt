package dom.dima.qctp.domain.impl

import android.util.Base64
import android.util.Log
import dom.dima.qctp.domain.FileRepository
import dom.dima.qctp.domain.QrCameraInteractor
import dom.dima.qctp.util.SaveResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject
import java.io.File

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

            val bookDir = fileRepository.initBookDir(fileName)
            val pageFile = fileRepository.writePartFile(page, bookDir, bytes)

            if (!fileRepository.isAllPagesPresent(bookDir, fileName, totalPages)) {
                val missingCount = (1..totalPages).count { !File(bookDir, "$fileName.$it.txt").exists() }
                emit(SaveResult.Incomplete(pageFile.name, missingCount, totalPages))
                return@flow
            }

            val merged = fileRepository.mergeFile(bookDir, fileName, totalPages)
            fileRepository.removePartsFragments(bookDir, totalPages, fileName)

            emit(SaveResult.Complete(merged, totalPages))
        } catch (e: Exception) {
            Log.e("SavePayload", "Error", e)
            emit(SaveResult.Error(e.message ?: "Unknown error"))
        }
    }.flowOn(Dispatchers.IO)
}