package dom.dima.qctp.domain.impl

import android.util.Base64
import android.util.Log
import dom.dima.qctp.domain.FileRepository
import dom.dima.qctp.domain.QrCameraInteractor
import dom.dima.qctp.domain.model.SaveResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class QrCameraInteractorImpl(
    private val fileRepository: FileRepository
) : QrCameraInteractor {

    override suspend fun saveQrPayload(jsonString: String): SaveResult = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject(jsonString)
            val fileName = json.getString("fileName")
            val page = json.getInt("page")
            val totalPages = json.getInt("totalPages")
            val contentBase64 = json.getString("content")

            if (fileName.contains("/") || fileName.contains("..") || fileName.isBlank()) {
                return@withContext SaveResult.Error("Invalid fileName")
            }
            if (page < 1 || totalPages < 1 || page > totalPages) {
                return@withContext SaveResult.Error("Invalid page/totalPages")
            }

            val bytes = try {
                Base64.decode(contentBase64, Base64.NO_WRAP)
            } catch (_: IllegalArgumentException) {
                return@withContext SaveResult.Error("Invalid base64 in content")
            }

            fileRepository.initBookDir(fileName)
            val pageFileName = fileRepository.writePartFile(fileName, page, bytes)

            if (!fileRepository.isAllPagesPresent(fileName, totalPages)) {
                val missingCount = fileRepository.calculateMissingPages(fileName, totalPages)
                return@withContext SaveResult.Incomplete(pageFileName, missingCount, totalPages)
            }
            val (filePath, mergedName) = fileRepository.mergeFile(fileName, totalPages)
            fileRepository.removePartsFragments(fileName, totalPages)

            SaveResult.Complete(filePath, mergedName, totalPages)
        } catch (e: Exception) {
            Log.e("SavePayload", "Error", e)
            SaveResult.Error(e.message ?: "Unknown error")
        }
    }
}