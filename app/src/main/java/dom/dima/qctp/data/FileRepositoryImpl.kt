package dom.dima.qctp.data

import android.content.Context
import dom.dima.qctp.domain.FileRepository
import java.io.File

class FileRepositoryImpl(
    private val context: Context
) : FileRepository {

    private fun getBookDir(fileName: String): File {
        val appDir = File(context.filesDir, "scanned_data").apply { mkdirs() }
        return File(appDir, fileName).apply { mkdirs() }
    }

    override suspend fun initBookDir(fileName: String) {
        getBookDir(fileName)
    }

    override suspend fun writePartFile(fileName: String, page: Int, bytes: ByteArray): String {
        val bookDir = getBookDir(fileName)
        val pageFile = File(bookDir, "$fileName.$page.txt")
        pageFile.writeBytes(bytes)
        return pageFile.name
    }

    override suspend fun mergeFile(fileName: String, totalPages: Int): Pair<String, String> {
        val bookDir = getBookDir(fileName)
        val merged = File(bookDir, fileName)
        merged.outputStream().buffered().use { out ->
            for (i in 1..totalPages) {
                File(bookDir, "$fileName.$i.txt").inputStream().buffered().use { input ->
                    input.copyTo(out)
                }
            }
        }
        return Pair(merged.absolutePath, merged.name)
    }

    override suspend fun removePartsFragments(fileName: String, totalPages: Int) {
        val bookDir = getBookDir(fileName)
        for (i in 1..totalPages) {
            File(bookDir, "$fileName.$i.txt").delete()
        }
    }

    override suspend fun isAllPagesPresent(fileName: String, totalPages: Int): Boolean {
        val bookDir = getBookDir(fileName)
        return (1..totalPages).all { File(bookDir, "$fileName.$it.txt").exists() }
    }

    override suspend fun calculateMissingPages(fileName: String, totalPages: Int): Int {
        val bookDir = getBookDir(fileName)
        return (1..totalPages).count { !File(bookDir, "$fileName.$it.txt").exists() }
    }
}