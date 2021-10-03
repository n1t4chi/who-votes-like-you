package vote.fetcher

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
                .orElse(null ) ?: return arrayListOf()
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

        fun rowsToUrls(rows: List<Element>, rowToUrl: Function<Element, Optional<URL>>): List<URL> {
            return rows.stream()
                .map(rowToUrl)
                .filter { obj: Optional<URL> -> obj.isPresent }
                .map { obj: Optional<URL> -> obj.get() }
                .collect(Collectors.toList())
        }
    }
}