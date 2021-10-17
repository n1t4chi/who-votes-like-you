package move.storage

import com.graphaware.test.unit.GraphUnit
import model.*
import org.junit.Assert
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

    @Test
    fun canGetAllPeople() {
        //setup
        val person1 = Person("Jan Urwał")
        val person2 = Person("Piotr Walił")
        dbAccessor.addPerson(person1)
        dbAccessor.addPerson(person2)

        //execute
        val returnedPeople: Set<Person> = dbAccessor.getPeople()

        //verify
        Assert.assertEquals(
            setOf( person1, person2 ),
            returnedPeople
        )
    }

    @Test
    fun canGetAllParties() {
        //setup
        val party1 = Party("Left")
        val party2 = Party("Right")
        dbAccessor.addParty(party1)
        dbAccessor.addParty(party2)

        //execute
        val returnedParties: Set<Party> = dbAccessor.getParties()

        //verify
        Assert.assertEquals(
            setOf( party1, party2 ),
            returnedParties
        )
    }

    @Test
    fun canGetAllVotings() {
        //setup
        val voting1 = Voting("Glosowanie nr 1")
        val voting2 = Voting("Glosowanie nr 2")
        dbAccessor.addVoting(voting1)
        dbAccessor.addVoting(voting2)

        //execute
        val returnedVotings: Set<Voting> = dbAccessor.getVotings()

        //verify
        Assert.assertEquals(
            setOf( voting1, voting2 ),
            returnedVotings
        )
    }
}