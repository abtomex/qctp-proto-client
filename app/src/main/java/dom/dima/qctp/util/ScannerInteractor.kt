package dom.dima.qctp.util

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File
import android.content.Intent
import androidx.core.content.FileProvider

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
        val content = json.getString("content")

        if (fileName.contains("/") || fileName.contains("..") || fileName.isBlank()) {
            return SaveResult.Error("Invalid fileName")
        }
        if (page < 1 || totalPages < 1 || page > totalPages) {
            return SaveResult.Error("Invalid page/totalPages")
        }

        val appDir = File(context.filesDir, "scanned_data").apply { mkdirs() }
        val bookDir = File(appDir, fileName).apply { mkdirs() }

        // 1. Пишем страницу
        val pageFile = File(bookDir, "$fileName.$page.txt")
        pageFile.writeText(content)

        // 2. Проверяем комплект
        val missing = (1..totalPages).filter {
            !File(bookDir, "$fileName.$it.txt").exists()
        }
        if (missing.isNotEmpty()) {
            return SaveResult.Incomplete(pageFile.name, missing.size, totalPages)
        }

        // 3. Собираем итоговый файл встык
        val merged = File(bookDir, "$fileName.txt")
        merged.bufferedWriter().use { out ->
            for (i in 1..totalPages) {
                out.write(File(bookDir, "$fileName.$i.txt").readText())
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
        type = "text/plain"           // поменяйте на "application/octet-stream" при необходимости
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Отправить файл"))
}