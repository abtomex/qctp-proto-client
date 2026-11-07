package dom.dima.qctp.domain.impl

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import dom.dima.qctp.domain.FileSendInteractor
import java.io.File

class FileSendInteractorImpl(
    private val context: Context
) : FileSendInteractor {

    override fun sendFile(filePath: String, fileName: String) {
        val file = File(filePath)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            putExtra(Intent.EXTRA_TEXT, "Файл: $fileName")

            // Обязательно для Gmail!
            clipData = ClipData.newRawUri("scanned_file", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Отправить файл").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Так как Context может быть не Activity
        }
        context.startActivity(chooser)
    }
}