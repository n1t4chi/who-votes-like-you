package move.storage

data class VerifyResult( val success: Boolean, val status: String ) {
}
fun error(errorReason: String) = VerifyResult(false,errorReason)
fun ok(): VerifyResult = VerifyResult(true,"ok")