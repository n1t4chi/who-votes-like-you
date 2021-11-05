package move.storage

import com.graphaware.test.unit.GraphUnit
import model.*
import org.junit.Assert
import org.junit.jupiter.api.*
import java.time.LocalDate

class DbAccessorTest {

    companion object {
        val voting1 = Voting("Głosowanie o wprowadzeniu ustawy nr 666", 1, Cadence(1), LocalDate.of(1995,4,8))
        val voting2 = Voting("Głosowanie o wprowadzeniu ustawy nr 1337", 2, Cadence(2), LocalDate.of(2001,9,11) )
        
        val connector: TestDbConnector = TestDbConnector()
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

    @BeforeEach
    fun reset() {
        connector.db.defaultDatabaseService().executeTransactionally("MATCH (m) DETACH DELETE (m)")
    }

    @Test
    fun canAddParty_emptyDatabase() {
        //execute
        dbAccessor.addParty(Party("MyParty"))

        //verify
        GraphUnit.assertSameGraph(
            connector.service(),
            "CREATE (party:Party {name: 'MyParty'} )"
        )
    }

    @Test
    fun canAddPerson_emptyDatabase() {
        //execute
        dbAccessor.addPerson(Person("Grzegorz Brzęczyszczykiewicz"))

        //verify
        GraphUnit.assertSameGraph(
            connector.service(),
            "CREATE (person:Person {name: 'Grzegorz Brzęczyszczykiewicz'} )"
        )
    }

    @Test
    fun canAddVoting_emptyDatabase() {
        //execute
        dbAccessor.addVoting(voting1)
        
        //verify
        GraphUnit.assertSameGraph(
            connector.service(),
            "CREATE (voting:Voting { name: '${voting1.name}', number: ${voting1.number}, cadence: ${voting1.cadence.number}, date: '${voting1.date}' } )"
        )
    }

    @Test
    fun canAddVote_whenPersonVotingAndPartyExists() {
        //setup
        val party = Party("Popis")
        val person = Person("Jan Urwał")
        val voting = voting1
        dbAccessor.addParty(party)
        dbAccessor.addPerson(person)
        dbAccessor.addVoting(voting)

        //verify before execute
        GraphUnit.assertSameGraph(
            connector.service(),
            """CREATE 
                (party:Party {name: 'Popis'} ),
                (person:Person {name: 'Jan Urwał'} ),
                (voting:Voting { name: '${voting.name}', number: ${voting.number}, cadence: ${voting.cadence.number}, date: '${voting.date}' } )
            """.trimMargin()
        )

        //execute
        dbAccessor.addVote(Vote(voting, person, VoteResult.yes, party))

        //verify
        GraphUnit.assertSameGraph(
            connector.service(),
            """CREATE 
                (party:Party {name: 'Popis'} ),
                (person:Person {name: 'Jan Urwał'} ),
                (voting:Voting { name: '${voting.name}', number: ${voting.number}, cadence: ${voting.cadence.number}, date: '${voting.date}' } ),
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
            setOf(person1, person2),
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
            setOf(party1, party2),
            returnedParties
        )
    }

    @Test
    fun canGetAllVotings() {
        //setup
        val voting1 = voting1
        val voting2 = voting2
        dbAccessor.addVoting(voting1)
        dbAccessor.addVoting(voting2)

        //execute
        val returnedVotings: Set<Voting> = dbAccessor.getVotings()

        //verify
        Assert.assertEquals(
            setOf(voting1, voting2),
            returnedVotings
        )
    }

    @Test
    fun canGetSpecificPerson() {
        //setup
        val person = Person("Jan Urwał")
        dbAccessor.addPerson(person)

        //execute
        val returnedPerson: Person? = dbAccessor.getPerson("Jan Urwał")

        //verify
        Assert.assertEquals(
            person,
            returnedPerson
        )
    }

    @Test
    fun givenUnknownName_getsEmptyValue() {
        //setup
        val person = Person("Jan Zwisł")
        dbAccessor.addPerson(person)

        //execute
        val returnedPerson: Person? = dbAccessor.getPerson("Jan Urwał")

        //verify
        Assert.assertNull(returnedPerson)
    }

    @Test
    fun canGetSpecificVoting() {
        //setup
        val voting = voting1
        dbAccessor.addVoting(voting)

        //execute
        val returnedVoting: Voting? = dbAccessor.getVoting(voting1.name)

        //verify
        Assert.assertEquals(
            voting,
            returnedVoting
        )
    }

    @Test
    fun givenUnknownVoting_getsEmptyValue() {
        //setup
        val voting = voting1
        dbAccessor.addVoting(voting)

        //execute
        val returnedVoting: Voting? = dbAccessor.getVoting("Super inne glosowanie nr 1")

        //verify
        Assert.assertNull(returnedVoting)
    }

    @Test
    fun canGetSpecificParty() {
        //setup
        val party = Party("Left")
        dbAccessor.addParty(party)

        //execute
        val returnedParty: Party? = dbAccessor.getParty("Left")

        //verify
        Assert.assertEquals(
            party,
            returnedParty
        )
    }

    @Test
    fun givenUnknownParty_getsEmptyValue() {
        //setup
        val party = Party("Right")
        dbAccessor.addParty(party)

        //execute
        val returnedParty: Party? = dbAccessor.getParty("Left")

        //verify
        Assert.assertNull(returnedParty)
    }

    @Test
    fun canQueryAllVotes() {
        //setup
        val party1 = Party("Left")
        val party2 = Party("Right")
        val person1 = Person("Jan Urwał")
        val person2 = Person("Piotr Walił")
        val voting1 = voting1
        val voting2 = voting2
        dbAccessor.addParty(party1)
        dbAccessor.addParty(party2)
        dbAccessor.addPerson(person1)
        dbAccessor.addPerson(person2)
        dbAccessor.addVoting(voting1)
        dbAccessor.addVoting(voting2)
        val vote1 = Vote(voting1, person1, VoteResult.yes, party1)
        val vote2 = Vote(voting1, person2, VoteResult.no, party2)
        val vote3 = Vote(voting2, person1, VoteResult.absent, party2)
        val vote4 = Vote(voting2, person2, VoteResult.abstain, party1)
        dbAccessor.addVote(vote1)
        dbAccessor.addVote(vote2)
        dbAccessor.addVote(vote3)
        dbAccessor.addVote(vote4)

        //execute
        val returnedVotes: Set<Vote> = dbAccessor.getVotes()

        //verify
        Assert.assertEquals(
            setOf(vote1, vote2, vote3, vote4),
            returnedVotes
        )
    }

    @Test
    fun canQueryVotesForSingleVoting() {
        //setup
        val party1 = Party("Left")
        val party2 = Party("Right")
        val person1 = Person("Jan Urwał")
        val person2 = Person("Piotr Walił")
        val voting1 = voting1
        val voting2 = voting2
        dbAccessor.addParty(party1)
        dbAccessor.addParty(party2)
        dbAccessor.addPerson(person1)
        dbAccessor.addPerson(person2)
        dbAccessor.addVoting(voting1)
        dbAccessor.addVoting(voting2)
        val vote1 = Vote(voting1, person1, VoteResult.yes, party1)
        val vote2 = Vote(voting1, person2, VoteResult.no, party2)
        val vote3 = Vote(voting2, person1, VoteResult.absent, party2)
        val vote4 = Vote(voting2, person2, VoteResult.abstain, party1)
        dbAccessor.addVote(vote1)
        dbAccessor.addVote(vote2)
        dbAccessor.addVote(vote3)
        dbAccessor.addVote(vote4)

        //execute
        val returnedVotes: Set<Vote> = dbAccessor.getVotesFor(voting1)

        //verify
        Assert.assertEquals(
            setOf(vote1, vote2),
            returnedVotes
        )
    }
}