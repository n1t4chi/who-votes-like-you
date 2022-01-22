package move.storage.access

import org.neo4j.driver.Driver

interface DbConnector {
    fun openConnection() : Driver
}