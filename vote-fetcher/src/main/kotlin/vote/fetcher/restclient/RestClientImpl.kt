package vote.fetcher.restclient

import okhttp3.*
import vote.fetcher.data.ResponseData
import vote.fetcher.exceptions.CannotGetResponseException
import java.io.IOException

object RestClientImpl: RestClient {
    private val client: OkHttpClient = OkHttpClient()
    private val synchronizer = Object()
    private var waitBeforeRequest: Long = 0
    
    override fun get(url: HttpUrl): ResponseData {
        synchronized (synchronizer) {
            var lastException: IOException? = null
            for (i in 1..20) {
                if( waitBeforeRequest > 0)
                    Thread.sleep(waitBeforeRequest)
                try {
                    val get = tryToGet(url)
                    waitBeforeRequest /= 2
                    return get
                } catch ( e: IOException ) {
                    if( waitBeforeRequest == 0L )
                        waitBeforeRequest = 1000
                    waitBeforeRequest += 500
                    lastException = e
                }
            }
            throw CannotGetResponseException("Could not retrieve page for $url", lastException!!)
        }
    }

    private fun tryToGet(url: HttpUrl): ResponseData {
        val request: Request = Request.Builder()
            .url(url)
            .build()
        return client.newCall(request).execute().use { response ->
            val body = response.body ?: throw CannotGetResponseException("Empty body for $url")
            val string = toBody(body)
            ResponseData(response.code, response.isSuccessful, string)
        }
    }

    private fun toBody(responseBody: ResponseBody): String {
        return try {
            responseBody.string()
        } catch (e: IOException) {
            throw CannotGetResponseException(e)
        }
    }
}