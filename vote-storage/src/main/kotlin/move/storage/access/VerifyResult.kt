package move.storage.access

data class VerifyResult( val success: Boolean, val status: String ) {
}
fun error(errorReason: String) = VerifyResult(false,errorReason)
fun ok(): VerifyResult = VerifyResult(true,"ok")