package dom.dima.qctp.domain

interface FileRepository {
    suspend fun initBookDir(fileName: String)
    suspend fun writePartFile(fileName: String, page: Int, bytes: ByteArray): String
    suspend fun mergeFile(fileName: String, totalPages: Int): Pair<String, String>
    suspend fun removePartsFragments(fileName: String, totalPages: Int)
    suspend fun isAllPagesPresent(fileName: String, totalPages: Int): Boolean
    suspend fun calculateMissingPages(fileName: String, totalPages: Int): Int
}