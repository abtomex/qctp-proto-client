package dom.dima.qctp.domain

import java.io.File

interface FileRepository {

    suspend fun initBookDir(fileName: String): File
    suspend fun writePartFile(page: Int, bookDir: File, bytes: ByteArray): File
    suspend fun mergeFile(bookDir: File, fileName: String, totalPages: Int) : File
    suspend fun removePartsFragments(bookDir: File, totalPages: Int, fileName: String)
}