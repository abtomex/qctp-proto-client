package dom.dima.qctp.data

import android.content.Context
import dom.dima.qctp.domain.FileRepository
import java.io.File

class FileRepositoryImpl(
    val context: Context
) : FileRepository {
    override suspend fun initBookDir(fileName: String): File {
        val appDir = File(context.filesDir, "scanned_data").apply { mkdirs() }
        return File(appDir, fileName).apply { mkdirs() }
    }

    override suspend fun writePartFile(page: Int, bookDir: File, bytes: ByteArray): File {

        val pageFile = File(bookDir, "${bookDir.name}.$page.txt")
        pageFile.writeBytes(bytes)

        return bookDir

    }

    override suspend fun mergeFile(bookDir: File, fileName: String, totalPages: Int): File {
        val merged = File(bookDir, fileName)
        merged.outputStream().buffered().use { out ->
            for (i in 1..totalPages) {
                File(bookDir, "$fileName.$i.txt").inputStream().buffered().use { input ->
                    input.copyTo(out)
                }
            }
        }
        return merged
    }

    override suspend fun removePartsFragments(bookDir: File, totalPages: Int, fileName: String) {
        for (i in 1..totalPages) {
            File(bookDir, "$fileName.$i.txt").delete()
        }

    }

}