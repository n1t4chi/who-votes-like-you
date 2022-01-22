package vote.fetcher.restclient

import okhttp3.HttpUrl
import vote.fetcher.data.ResponseData
import vote.fetcher.exceptions.CannotGetResponseException

interface RestClient {
    fun get(url: HttpUrl): ResponseData
    
    fun getStringContentForUrl(url: HttpUrl): String {
        val (code,isSuccess,body) = get(url)
        if (!isSuccess) {
            throw CannotGetResponseException(
                "Get for $url received status code ${code}"
            )
        }
        return body
    }
    
    fun tryToGetStringContentForUrl(url: HttpUrl): String {
        val (_,_,body) = get(url)
        return body
    }
}
