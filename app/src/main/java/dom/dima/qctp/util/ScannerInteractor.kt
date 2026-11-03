package dom.dima.qctp.util

import android.content.Context
import android.content.Intent
import android.util.Base64
import android.util.Log
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File

sealed interface SaveResult {
    data class Incomplete(val savedName: String, val missing: Int, val total: Int) : SaveResult
    data class Complete(val mergedFile: File, val pages: Int) : SaveResult
    data class Error(val message: String) : SaveResult
}

fun saveQrPayload(context: Context, jsonString: String): SaveResult {
    return try {
        val json = JSONObject(jsonString)
        val fileName = json.getString("fileName")
        val page = json.getInt("page")
        val totalPages = json.getInt("totalPages")
        val contentBase64 = json.getString("content")

        if (fileName.contains("/") || fileName.contains("..") || fileName.isBlank()) {
            return SaveResult.Error("Invalid fileName")
        }
        if (page < 1 || totalPages < 1 || page > totalPages) {
            return SaveResult.Error("Invalid page/totalPages")
        }

        // Декодируем base64 в байты
        val bytes = try {
            Base64.decode(contentBase64, Base64.NO_WRAP)
        } catch (e: IllegalArgumentException) {
            return SaveResult.Error("Invalid base64 in content")
        }

        val appDir = File(context.filesDir, "scanned_data").apply { mkdirs() }
        val bookDir = File(appDir, fileName).apply { mkdirs() }

        // 1. Пишем страницу как бинарный файл
        val pageFile = File(bookDir, "$fileName.$page.txt")
        pageFile.writeBytes(bytes)

        // 2. Проверяем комплект
        val missing = (1..totalPages).filter {
            !File(bookDir, "$fileName.$it.txt").exists()
        }
        if (missing.isNotEmpty()) {
            return SaveResult.Incomplete(pageFile.name, missing.size, totalPages)
        }

        // 3. Собираем итоговый файл встык — побайтово
        val merged = File(bookDir, "$fileName")
        merged.outputStream().buffered().use { out ->
            for (i in 1..totalPages) {
                File(bookDir, "$fileName.$i.txt").inputStream().buffered().use { input ->
                    input.copyTo(out)
                }
            }
        }

        // 4. Удаляем промежуточные .page файлы
        for (i in 1..totalPages) {
            File(bookDir, "$fileName.$i.txt").delete()
        }

        SaveResult.Complete(merged, totalPages)
    } catch (e: Exception) {
        Log.e("SavePayload", "Error", e)
        SaveResult.Error(e.message ?: "Unknown error")
    }
}
fun shareMergedFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {

        type = "application/octet-stream"
//        type = "*/*"

        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
        putExtra(Intent.EXTRA_TEXT, "Файл: ${file.name}")

//        clipData = ClipData.newRawUri("scanned_file", uri)

        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(Intent.createChooser(intent, "Отправить файл"))
}

/**
 * Рекурсивно удаляет всё содержимое папки scanned_data, но оставляет саму папку.
 * Возвращает true, если очистка прошла успешно.
 */
fun clearScannedData(context: Context): Boolean {
    val appDir = File(context.filesDir, "scanned_data")
    if (!appDir.exists()) return true

    return try {
        appDir.listFiles()?.forEach { file ->
            file.deleteRecursively()
        }
        true
    } catch (e: Exception) {
        Log.e("ClearData", "Failed to clear scanned_data", e)
        false
    }
}