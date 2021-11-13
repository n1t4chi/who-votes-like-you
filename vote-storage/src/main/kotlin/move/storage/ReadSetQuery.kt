package move.storage

import org.neo4j.driver.*
import java.util.stream.Collectors

class ReadSetQuery<T>(val query: String, val mapper: (Record) -> T)  {
    fun read(transaction: Transaction) : Set<T> {
        return transaction.run(query)
            .stream()
            .map(mapper)
            .collect(Collectors.toSet())
    }
}