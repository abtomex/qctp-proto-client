package dom.dima.qctp.util

import android.content.Context
import android.content.Intent
import android.util.Base64
import android.util.Log
import androidx.core.content.FileProvider
import dom.dima.qctp.domain.model.SaveResult
import org.json.JSONObject
import java.io.File

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