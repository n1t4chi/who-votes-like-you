package vote.fetcher.polishsejm.client.infrastructure

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class LocalDateAdapter {
    @ToJson
    fun toJson(value: LocalDate): String = DateTimeFormatter.ISO_LOCAL_DATE.format(value)

    @FromJson
    fun fromJson(value: String): LocalDate = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
}
