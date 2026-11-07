package dom.dima.qctp.domain.impl

import android.util.Base64
import android.util.Log
import dom.dima.qctp.domain.FileRepository
import dom.dima.qctp.domain.QrCameraInteractor
import dom.dima.qctp.util.SaveResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject
import java.io.File

class QrCameraInteractorImpl(
    val fileRepository: FileRepository
) : QrCameraInteractor {

    override fun saveQrPayload(jsonString: String) = flow {
        try {
            val json = JSONObject(jsonString)
            val fileName = json.getString("fileName")
            val page = json.getInt("page")
            val totalPages = json.getInt("totalPages")
            val contentBase64 = json.getString("content")

            if (fileName.contains("/") || fileName.contains("..") || fileName.isBlank()) {
                emit(SaveResult.Error("Invalid fileName"))
            }
            if (page < 1 || totalPages < 1 || page > totalPages) {
                emit(SaveResult.Error("Invalid page/totalPages"))
            }

            // Декодируем base64 в байты
            val bytes = try {
                Base64.decode(contentBase64, Base64.NO_WRAP)
            } catch (_: IllegalArgumentException) {
                emit(SaveResult.Error("Invalid base64 in content"))
            }

            // 0. Создаем пространство под будущий файл
            val bookDir = fileRepository.initBookDir(fileName)

            // 1. Пишем страницу как бинарный файл
            val pageFile = fileRepository.writePartFile(page, bookDir, bytes as ByteArray)

            // 2. Проверяем комплект
            val missing = (1..totalPages).filter {
                !File(bookDir, "$fileName.$it.txt").exists()
            }
            if (missing.isNotEmpty()) {
                emit(SaveResult.Incomplete(pageFile.name, missing.size, totalPages))
            }

            // 3. Собираем итоговый файл встык — побайтово
            val merged = fileRepository.mergeFile(bookDir, fileName, totalPages)

            // 4. Удаляем промежуточные .page файлы
            fileRepository.removePartsFragments(bookDir, totalPages, fileName)

            SaveResult.Complete(merged, totalPages)
        } catch (e: Exception) {
            Log.e("SavePayload", "Error", e)
            SaveResult.Error(e.message ?: "Unknown error")
        }
    }
        .flowOn(Dispatchers.IO)
}