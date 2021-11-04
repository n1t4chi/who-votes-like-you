package vote.fetcher

import okhttp3.HttpUrl
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URL
import java.util.*
import java.util.function.Function
import java.util.stream.Collectors

interface ParseUtil {
    companion object {
        fun findTable(document: Document): Optional<Element> {
            return Optional.ofNullable(document.getElementsByTag("table").first())
        }

        fun toRows(table: Element): List<Element> {
            return Optional.of(table.getElementsByTag("tbody"))
                .map { obj -> obj.first() }
                .map { element -> element!!.getElementsByTag("tr") }
                .orElse(null) ?: return arrayListOf()
        }

        fun toXml(content: String): Document {
            return Jsoup.parse(content)
        }

        fun getRows(content: String): List<Element> {
            val document = toXml(content)
            val table = findTable(document)
            if (table.isEmpty) {
                throw CannotParseDocumentException("Table not found")
            }
            return toRows(table.get())
        }

        fun <T> rowsToUrls(rows: List<Element>, rowToUrl: Function<Element, Optional<T>>): List<T> {
            return rows.stream()
                .map(rowToUrl)
                .filter(Optional<T>::isPresent)
                .map(Optional<T>::get)
                .collect(Collectors.toList())
        }

        fun joinBaseWithLink(baseUrl: HttpUrl, link: String): HttpUrl {
            val resolve = baseUrl.resolve(link)
            if (resolve == null) {
                throw CannotParseDocumentException("Malformed URL link: '$link' for base '${baseUrl.toString()}")
            }
            return resolve
        }
    }
}