package vote.storage

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.neo4j.harness.Neo4j
import org.neo4j.harness.Neo4jBuilders

class DbTest {

    companion object {
        var db : Neo4j = Neo4jBuilders.newInProcessBuilder()
            .withDisabledServer()
            .withFixture(""
                    + "CREATE (TheMatrix:Movie {title:'The Matrix', released:1999, tagline:'Welcome to the Real World'})"
            )
            .build()

        @BeforeAll
        @JvmStatic
        fun setUp() {
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            db.close()
        }
    }

    @Test
    internal fun setupDb() {
        val beginTx = db.databaseManagementService().database("neo4j").beginTx().use { tx ->
            val result = tx.execute("MATCH (m:Movie) WHERE m.title = 'The Matrix' RETURN m.released")
            Assertions.assertEquals(1999L, result.next()["m.released"])
        }
    }
}