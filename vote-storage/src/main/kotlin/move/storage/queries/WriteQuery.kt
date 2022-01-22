package move.storage.queries

import move.storage.access.*
import move.storage.exceptions.DbException
import org.neo4j.driver.Transaction

class WriteQuery(val query: String, val params: Map<String, Any>, val verifier: WriteVerifier = WriteVerifier()) {
    fun writeOrThrow(transaction: Transaction) {
        val (success, status) = tryWrite(transaction)
        if (!success)
            throw DbException(status)
    }
    
    fun tryWrite(transaction: Transaction): VerifyResult {
        val result = transaction.run(query, params)
        return verifier.verify(result.consume().counters())
    }
}