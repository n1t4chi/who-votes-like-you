package move.storage

import org.neo4j.driver.*

interface DbConnector {
    fun openConnection() : Driver
}