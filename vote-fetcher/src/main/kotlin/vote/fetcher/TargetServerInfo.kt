package vote.fetcher

import okhttp3.HttpUrl
import java.net.URL

data class TargetServerInfo(val protocol: String, val hostname: String, val port: Int = 0, val baseFile: String = "") {
    constructor(url: URL) : this(
        url.protocol,
        url.host,
        url.port,
        url.file
    )

    constructor(url: String) : this(URL(url))

    fun urlBuilder(): HttpUrl.Builder {
        return applyPortIfNeeded(
            HttpUrl.Builder()
                .scheme(protocol)
                .host(hostname)
                .addPathSegments(baseFile)
        )
    }

    fun baseUrl(): String {
        return urlBuilder().build().toString()
    }

    private fun applyPortIfNeeded(builder: HttpUrl.Builder): HttpUrl.Builder {
        if (port != 0)
            return builder.port(port)
        else
            return builder
    }
}