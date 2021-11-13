package vote.synchronizer

import move.storage.DbConnector
import org.neo4j.driver.*
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.harness.*
import java.net.URI

class TestDbConnector : DbConnector {
    val db : Neo4j = Neo4jBuilders.newInProcessBuilder()
        .build()

    private fun getAddress(): URI {
        return db.boltURI()
    }

    fun close() {
        db.close()
    }

    override fun openConnection(): Driver {
        try {
            return tryToOpen()
        } catch ( ex : IllegalStateException ) {
            return tryToOpen()
        }
    }

    private fun tryToOpen() = GraphDatabase.driver(getAddress())

    fun service(): GraphDatabaseService = db.defaultDatabaseService()
}