package move.storage

import com.graphaware.test.unit.GraphUnit
import model.*
import org.junit.jupiter.api.*

class DbAccessorTest {

    companion object {
        val connector : TestDbConnector = TestDbConnector()
        val dbAccessor = DbAccessor(connector)

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

    @AfterEach
    fun reset() {
        connector.db.defaultDatabaseService().executeTransactionally("MATCH (m) DETACH DELETE (m)")
    }

    @Test
    fun canAddParty_emptyDatabase() {
        //execute
        dbAccessor.addParty( Party("MyParty") )

        //verify
        GraphUnit.assertSameGraph(
            connector.service(),
            "CREATE (party:Party {name: 'MyParty'} )"
        )
    }

    @Test
    fun canAddPerson_emptyDatabase() {
        //execute
        dbAccessor.addPerson( Person("Grzegorz Brzęczyszczykiewicz") )

        //verify
        GraphUnit.assertSameGraph(
            connector.service(),
            "CREATE (person:Person {name: 'Grzegorz Brzęczyszczykiewicz'} )"
        )
    }

    @Test
    fun canAddVoting_emptyDatabase() {
        //execute
        dbAccessor.addVoting(Voting("Głosowanie o wprowadzeniu ustawy nr 666") )

        //verify
        GraphUnit.assertSameGraph(
            connector.service(),
            "CREATE (voting:Voting {name: 'Głosowanie o wprowadzeniu ustawy nr 666'} )"
        )
    }

    @Test
    fun canAddVote_whenPersonVotingAndPartyExists() {
        //setup
        val party = Party("Popis")
        val person = Person("Jan Urwał")
        val voting = Voting("Glosowanie nr 1")
        dbAccessor.addParty(party)
        dbAccessor.addPerson(person)
        dbAccessor.addVoting(voting)

        //verify before execute
        GraphUnit.assertSameGraph(
            connector.service(),
            """CREATE 
                (party:Party {name: 'Popis'} ),
                (person:Person {name: 'Jan Urwał'} ),
                (voting:Voting {name: 'Glosowanie nr 1'} )
            """.trimMargin()
        )

        //execute
        dbAccessor.addVote( Vote(voting, person, VoteResult.yes, party ) )

        //verify
        GraphUnit.assertSameGraph(
            connector.service(),
            """CREATE 
                (party:Party {name: 'Popis'} ),
                (person:Person {name: 'Jan Urwał'} ),
                (voting:Voting {name: 'Glosowanie nr 1'} ),
                (vote:Vote {result: 'yes'} ),
                (vote)-[r1:castFor]->(party),
                (vote)-[r2:castBy]->(person),
                (vote)-[r3:castAt]->(voting)
            """.trimMargin()
        )
    }
}