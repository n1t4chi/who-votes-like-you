package vote.fetcher

import okhttp3.HttpUrl
import java.io.File
import java.nio.file.Files

class FileCachedRestClient(
    private val client: RestClient,
    private val directory: File
) : RestClient {
    companion object {
        val fileFormat  = "code:(\\d+)\\nisSuccess:(\\w+)\\nbody:\\n(.+)".toRegex(RegexOption.DOT_MATCHES_ALL)
    }
    
    override fun get(url: HttpUrl): ResponseData {
        val cached = getCache(url)
        if( cached != null ) {
            return cached
        }
        val data = client.get(url)
        saveToCache(url, data)
        return data
    }
    
    private fun saveToCache(url: HttpUrl, data: ResponseData) {
        val file = directory.resolve(encode(url))
        writeToFile(file, data)
    }
    
    private fun getCache(url: HttpUrl): ResponseData? {
        val file = directory.resolve(encode(url))
        if( !file.exists() )
            return null
        return readFile(file)
    }
    
    fun writeToFile(file: File, data: ResponseData) {
        file.parentFile.mkdirs()
        Files.writeString(
            file.toPath(),
            "code:${data.code}\nisSuccess:${data.isSuccess}\nbody:\n${data.body}"
        )
    }
    
    fun readFile(file: File): ResponseData {
        val content = Files.readString(file.toPath())
        val result = fileFormat.matchEntire(content) ?: throw RuntimeException("invalid format of file $file, content:\n$content")
        val groups = result.groups
        val (code, _) = groups.get(1) ?: throw RuntimeException("invalid format of file $file")
        val (isSuccess, _) = groups.get(2) ?: throw RuntimeException("invalid format of file $file")
        val (body, _) = groups.get(3) ?: throw RuntimeException("invalid format of file $file")
        return ResponseData(
            code.toInt(),
            isSuccess.toBoolean(),
            body
        )
    }
    
    fun encode(url: HttpUrl): String {
        val fileName = url.toString()
            .replace("%20", "")
            .replace("[^a-zA-Z0-9]".toRegex(), "")
        return "$fileName.cache"
    }
}