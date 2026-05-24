package move.storage.queries

import org.neo4j.driver.*

class ReadQuery<T> (val query: String, val mapper: (Record) -> T) {
    fun read(transaction: Transaction): T? {
        return mapOptionally(transaction.run(query), mapper)
    }
    
    private fun <T> mapOptionally(run: Result, mapper: (Record) -> T): T? {
        return if (run.hasNext())
            mapper.invoke(run.single())
        else
            null
    }
}