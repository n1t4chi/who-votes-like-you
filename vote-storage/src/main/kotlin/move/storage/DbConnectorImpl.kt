package move.storage

import org.neo4j.driver.*

class DbConnectorImpl : DbConnector {
    override fun openConnection(): Driver = GraphDatabase.driver(
        "bolt://localhost:7687",
        AuthTokens.basic("neo4j", "zxcvbnm")
    )
}