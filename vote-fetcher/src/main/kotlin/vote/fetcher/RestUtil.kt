package vote.fetcher

import okhttp3.*
import java.io.IOException
import java.lang.IllegalArgumentException
import java.net.MalformedURLException
import java.net.URL

class RestUtil {
    companion object{
        fun get(client: OkHttpClient, url: URL): Response {
            val response: Response
            response = try {
                val request: Request = Request.Builder()
                    .url(url)
                    .build()
                client.newCall(request).execute()
            } catch (e: IOException) {
                throw CannotGetResponseException("Could not retrieve page for $url", e)
            }
            return response
        }

        fun get(client: OkHttpClient, url: HttpUrl): Response {
            val response: Response
            response = try {
                val request: Request = Request.Builder()
                    .url(url)
                    .build()
                client.newCall(request).execute()
            } catch (e: IOException) {
                throw CannotGetResponseException("Could not retrieve page for $url", e)
            }
            return response
        }

        fun toUrl(url: String): URL {
            return try {
                URL(url)
            } catch (e: MalformedURLException) {
                throw IllegalArgumentException("Could not parse url $url", e)
            }
        }

        fun getStringContentForUrl(client: OkHttpClient, url: URL): String {
            val response = get(client, url)
            if (!response.isSuccessful) {
                throw CannotGetResponseException(
                    "Get for $url received status code ${response.code}"
                )
            }
            val body = response.body ?: throw CannotGetResponseException(
                "Empty body for $url"
            )
            return toBody(body)
        }

        fun getStringContentForUrl(client: OkHttpClient, url: HttpUrl): String {
            val response = get(client, url)
            if (!response.isSuccessful) {
                throw CannotGetResponseException(
                    "Get for $url received status code ${response.code}"
                )
            }
            val body = response.body ?: throw CannotGetResponseException(
                "Empty body for $url"
            )
            return toBody(body)
        }

        private fun toBody(responseBody: ResponseBody): String {
            return try {
                responseBody.string()
            } catch (e: IOException) {
                throw CannotGetResponseException(e)
            }
        }
    }
}