package vote.fetcher.polishsejm.pipeline

import vote.fetcher.polishsejm.client.infrastructure.ClientException

sealed interface ApiResult<T> {
    fun orThrow(block: (Exception) -> Exception = { it }): T {
        when (this) {
            is Error -> throw block(this.exception)
            is Ok -> return this.result
        }
    }

    fun onNotFound(block: (ClientException) -> Unit): ApiResult<T> {
        if (this is NotFound) {
            block(this.exception)
        }
        return this
    }

    fun onUnknownError(block: (Exception) -> Unit): ApiResult<T> {
        if (this is UnknownError) {
            block(this.exception)
        }
        return this
    }

    fun onAnyError(block: (Exception) -> Unit): ApiResult<T> {
        if (this is Error) {
            block(this.exception)
        }
        return this
    }

    fun getOrNull(): T? {
        if (this is Ok) {
            return this.result
        } else {
            return null
        }
    }
}

class NotFound<T>(override val exception: ClientException) : Error<T>(exception)

class UnknownError<T>(exception: Exception) : Error<T>(exception)

sealed class Error<T>(open val exception: Exception) : ApiResult<T>

class Ok<T>(val result: T) : ApiResult<T>

fun <T> call(block: () -> T): ApiResult<T> {
    try {
        return Ok(block())
    } catch (e: Exception) {
        if (e is ClientException && e.statusCode == 404) {
            return NotFound(e)
        } else {
            return UnknownError(e)
        }
    }
}
