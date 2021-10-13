package move.storage

import com.graphaware.test.unit.GraphUnit
import model.Party
import org.junit.jupiter.api.*

class DbAccessorTest {

    companion object {
        val connector : TestDbConnector = TestDbConnector()

        @BeforeAll
        @JvmStatic
        fun setUp() {
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            connector.close()
        }
    }

    @Test
    fun canAddParty() {
        //setup
        val dbAccessor = DbAccessor(connector)

        //execute
        dbAccessor.addParty( Party("MyParty") )

        //verify
        GraphUnit.assertSameGraph(
            connector.service(),
            "CREATE (party:Party) SET party.name = 'MyParty'"
        )
    }
}