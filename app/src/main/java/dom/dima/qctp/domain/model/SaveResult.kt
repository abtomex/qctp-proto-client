package dom.dima.qctp.domain.model

sealed interface SaveResult {

    data class Incomplete(val savedName: String, val missing: Int, val total: Int) : SaveResult
    data class Complete(val filePath: String, val fileName: String, val pages: Int) : SaveResult
    data class Error(val message: String) : SaveResult

}