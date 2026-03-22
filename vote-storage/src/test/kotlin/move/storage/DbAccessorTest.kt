package move.storage

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import model.*
import model.Voting
import model.VotingDay
import move.storage.access.DbAccessor
import move.storage.exceptions.DbException
import org.junit.jupiter.api.*
import java.time.LocalDate

class DbAccessorTest {

    companion object {
        val cadence1 = Cadence(
            number = 1,
            status = CadenceStatus.old,
            daysWithVotes = 11
        )
        val cadence2 = Cadence(
            number = 2,
            status = CadenceStatus.active,
            daysWithVotes = 22
        )
        val votingDay1 = VotingDay(
            cadence = cadence1,
            date = LocalDate.of(1995, 4, 8),
            votingsInDay = 11
        )
        val votingDay2 = VotingDay(
            cadence = cadence2,
            date = LocalDate.of(2001, 9, 11),
            votingsInDay = 22
        )
        val voting1 = Voting(
            name = "Głosowanie o wprowadzeniu ustawy nr 666",
            number = 1,
            votingDay = votingDay1,
            votesCast = 11
        )
        val voting2 = Voting(
            name = "Głosowanie o wprowadzeniu ustawy nr 1337",
            number = 2,
            votingDay = votingDay2,
            votesCast = 22
        )

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
    fun canAddCadence_emptyDatabase() {
        //setup
        val cadence = cadence1

        //execute
        dbAccessor.addCadence(cadence)

        //verify
        assertDatabaseState(
            cadences = listOf(Cadence(number = 1, status = CadenceStatus.old, daysWithVotes = 11)),
        )
    }

    @Test
    fun canAddParty_emptyDatabase() {
        //execute
        dbAccessor.addParty(Party(name = "MyParty"))

        //verify
        assertDatabaseState(
            parties = listOf(Party(name = "MyParty"))
        )
    }

    @Test
    fun canAddPerson_emptyDatabase() {
        //execute
        dbAccessor.addPerson(Person(name = "Grzegorz Brzęczyszczykiewicz"))

        //verify
        assertDatabaseState(
            people = listOf(Person(name = "Grzegorz Brzęczyszczykiewicz")),
        )
    }

    @Test
    fun canAddVotingDay_whenCadenceExists() {
        //setup
        val cadence = cadence1
        val votingDay = votingDay1
        dbAccessor.addCadence(cadence)

        //verify before execute
        assertDatabaseState(
            cadences = listOf(Cadence(number = 1, status = CadenceStatus.old, daysWithVotes = 11)),
        )

        //execute
        dbAccessor.addVotingDay(votingDay)

        //verify
        assertDatabaseState(
            cadences = listOf(Cadence(number = 1, status = CadenceStatus.old, daysWithVotes = 11)),
            votingDays = listOf(VotingDay(cadence = cadence1, date = LocalDate.of(1995, 4, 8), votingsInDay = 11)),
        )
    }

    @Test
    fun addVotingDay_whenNoCadencePresent_throwsException() {
        //setup
        val votingDay = votingDay1

        //execute
        Assertions.assertThrows(DbException::class.java) { dbAccessor.addVotingDay(votingDay) }
    }

    @Test
    fun canAddVoting_whenVotingDayAndCadenceExists() {
        //setup
        val cadence = cadence1
        val votingDay = votingDay1
        val voting = voting1
        dbAccessor.addCadence(cadence)
        dbAccessor.addVotingDay(votingDay)

        //verify before execute
        assertDatabaseState(
            cadences = listOf(Cadence(number = 1, status = CadenceStatus.old, daysWithVotes = 11)),
            votingDays = listOf(VotingDay(cadence = cadence, date = LocalDate.of(1995, 4, 8), votingsInDay = 11)),
        )

        //execute
        dbAccessor.addVoting(voting)

        //verify
        assertDatabaseState(
            cadences = listOf(Cadence(number = 1, status = CadenceStatus.old, daysWithVotes = 11)),
            votingDays = listOf(VotingDay(cadence = cadence, date = LocalDate.of(1995, 4, 8), votingsInDay = 11)),
            votings = listOf(Voting(name = "Głosowanie o wprowadzeniu ustawy nr 666", number = 1, votingDay = votingDay, votesCast = 11)),
        )
    }

    @Test
    fun addVoting_whenNoVotingPresent_throwsException() {
        //setup
        val cadence = cadence1
        val voting = voting1
        dbAccessor.addCadence(cadence)

        //execute
        Assertions.assertThrows(DbException::class.java) { dbAccessor.addVoting(voting) }
    }

    @Test
    fun canAddVote_whenPersonVotingAndPartyExists() {
        //setup
        val cadence = cadence1
        val party = Party(name = "Popis")
        val person = Person(name = "Jan Urwał")
        val votingDay = votingDay1
        val voting = voting1
        dbAccessor.addCadence(cadence)
        dbAccessor.addParty(party)
        dbAccessor.addPerson(person)
        dbAccessor.addVotingDay(votingDay)
        dbAccessor.addVoting(voting)

        //verify before execute
        assertDatabaseState(
            cadences = listOf(Cadence(number = 1, status = CadenceStatus.old, daysWithVotes = 11)),
            parties = listOf(Party("Popis")),
            people = listOf(Person("Jan Urwał")),
            votingDays = listOf(VotingDay(cadence = cadence, date = LocalDate.of(1995, 4, 8), votingsInDay = 11)),
            votings = listOf(Voting(name = "Głosowanie o wprowadzeniu ustawy nr 666", number = 1, votingDay = votingDay, votesCast = 11)),
        )

        //execute
        dbAccessor.addVote(Vote(voting = voting, party = party, person = person, result = VoteResult.yes))

        //verify
        assertDatabaseState(
            cadences = listOf(Cadence(number = 1, status = CadenceStatus.old, daysWithVotes = 11)),
            parties = listOf(Party("Popis")),
            people = listOf(Person("Jan Urwał")),
            votingDays = listOf(VotingDay(cadence = cadence, date = LocalDate.of(1995, 4, 8), votingsInDay = 11)),
            votings = listOf(Voting(name = "Głosowanie o wprowadzeniu ustawy nr 666", number = 1, votingDay = votingDay, votesCast = 11)),
            votes = listOf(Vote(voting = voting, party = party, person = person, result = VoteResult.yes))
        )
    }

    @Test
    fun addVote_whenNoPartyPresent_throwsException() {
        //setup
        val cadence = cadence1
        val person = Person("Jan Urwał")
        val votingDay = votingDay1
        val voting = voting1
        dbAccessor.addCadence(cadence)
        dbAccessor.addPerson(person)
        dbAccessor.addVotingDay(votingDay)
        dbAccessor.addVoting(voting)

        //execute
        Assertions.assertThrows(DbException::class.java) {
            dbAccessor.addVote(
                Vote(
                    voting = voting,
                    party = Party("Popis"),
                    person = person,
                    result = VoteResult.yes
                )
            )
        }
    }

    @Test
    fun addVote_whenNoPersonPresent_throwsException() {
        //setup
        val cadence = cadence1
        val party = Party("Popis")
        val votingDay = votingDay1
        val voting = voting1
        dbAccessor.addCadence(cadence)
        dbAccessor.addParty(party)
        dbAccessor.addVotingDay(votingDay)
        dbAccessor.addVoting(voting)

        //execute
        Assertions.assertThrows(DbException::class.java) {
            dbAccessor.addVote(
                Vote(
                    voting = voting,
                    party = party,
                    person = Person("Jan Urwał"),
                    result = VoteResult.yes
                )
            )
        }
    }

    @Test
    fun addVote_whenNoVotingPresent_throwsException() {
        //setup
        val cadence = cadence1
        val party = Party("Popis")
        val person = Person("Jan Urwał")
        val votingDay = votingDay1
        dbAccessor.addCadence(cadence)
        dbAccessor.addParty(party)
        dbAccessor.addPerson(person)
        dbAccessor.addVotingDay(votingDay)

        //execute
        Assertions.assertThrows(DbException::class.java) {
            dbAccessor.addVote(
                Vote(
                    voting = voting1,
                    party = party,
                    person = person,
                    result = VoteResult.yes
                )
            )
        }
    }

    @Test
    fun canGetAllCadences() {
        //setup
        dbAccessor.addCadence(cadence1)
        dbAccessor.addCadence(cadence2)

        //execute
        val returnedCadences: Set<Cadence> = dbAccessor.getCadences()

        //verify
        Assertions.assertEquals(
            setOf(cadence1, cadence2),
            returnedCadences
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
        Assertions.assertEquals(
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
        Assertions.assertEquals(
            setOf(party1, party2),
            returnedParties
        )
    }

    @Test
    fun canGetAllVotingDays() {
        //setup
        dbAccessor.addCadence(cadence1)
        dbAccessor.addCadence(cadence2)
        dbAccessor.addVotingDay(votingDay1)
        dbAccessor.addVotingDay(votingDay2)

        //execute
        val returnedVotingDays: Set<VotingDay> = dbAccessor.getVotingDays()

        //verify
        Assertions.assertEquals(
            setOf(votingDay1, votingDay2),
            returnedVotingDays
        )
    }

    @Test
    fun canGetAllVotings() {
        //setup
        dbAccessor.addCadence(cadence1)
        dbAccessor.addCadence(cadence2)
        dbAccessor.addVotingDay(votingDay1)
        dbAccessor.addVotingDay(votingDay2)
        dbAccessor.addVoting(voting1)
        dbAccessor.addVoting(voting2)

        //execute
        val returnedVotings: Set<Voting> = dbAccessor.getVotings()

        //verify
        Assertions.assertEquals(
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
        Assertions.assertEquals(
            person,
            returnedPerson
        )
    }

    @Test
    fun getPerson_givenUnknownName_returnsEmptyValue() {
        //setup
        val person = Person("Jan Zwisł")
        dbAccessor.addPerson(person)

        //execute
        val returnedPerson: Person? = dbAccessor.getPerson("Jan Urwał")

        //verify
        Assertions.assertNull(returnedPerson)
    }

    @Test
    fun canGetSpecificVoting() {
        //setup
        val voting = voting1
        dbAccessor.addCadence(voting1.votingDay.cadence)
        dbAccessor.addVotingDay(voting1.votingDay)
        dbAccessor.addVoting(voting)

        //execute
        val returnedVoting: Voting? = dbAccessor.getVoting(voting1.name)

        //verify
        Assertions.assertEquals(voting, returnedVoting)
    }

    @Test
    fun getVoting_givenUnknownName_returnsEmptyValue() {
        //setup
        val voting = voting1
        dbAccessor.addCadence(voting.votingDay.cadence)
        dbAccessor.addVotingDay(voting.votingDay)
        dbAccessor.addVoting(voting)

        //execute
        val returnedVoting: Voting? = dbAccessor.getVoting("Super inne glosowanie nr 1")

        //verify
        Assertions.assertNull(returnedVoting)
    }

    @Test
    fun canGetSpecificParty() {
        //setup
        val party = Party("Left")
        dbAccessor.addParty(party)

        //execute
        val returnedParty: Party? = dbAccessor.getParty("Left")

        //verify
        Assertions.assertEquals(
            party,
            returnedParty
        )
    }

    @Test
    fun getParty_givenUnknownName_returnsEmptyValue() {
        //setup
        val party = Party("Right")
        dbAccessor.addParty(party)

        //execute
        val returnedParty: Party? = dbAccessor.getParty("Left")

        //verify
        Assertions.assertNull(returnedParty)
    }

    @Test
    fun canGetSpecificCadence() {
        //setup
        val cadence = Cadence(2)
        dbAccessor.addCadence(cadence)

        //execute
        val returnedCadence: Cadence? = dbAccessor.getCadence(2)

        //verify
        Assertions.assertEquals(cadence, returnedCadence)
    }

    @Test
    fun getCadence_givenUnknownNumber_returnsEmptyValue() {
        //setup
        val cadence = Cadence(2)
        dbAccessor.addCadence(cadence)

        //execute
        val returnedCadence: Cadence? = dbAccessor.getCadence(1)

        //verify
        Assertions.assertNull(returnedCadence)
    }

    @Test
    fun canGetSpecificVotingDay() {
        //setup
        val date = LocalDate.of(2000, 10, 10)
        val votingDay = VotingDay(cadence1, date)
        dbAccessor.addCadence(cadence1)
        dbAccessor.addVotingDay(votingDay)

        //execute
        val returnedVotingDay: VotingDay? = dbAccessor.getVotingDay(date)

        //verify
        Assertions.assertEquals(
            votingDay,
            returnedVotingDay
        )
    }

    @Test
    fun getVotingDay_givenUnknownDate_returnsEmptyValue() {
        //setup
        val votingDay = VotingDay(cadence1, LocalDate.of(2000, 10, 10))
        dbAccessor.addCadence(cadence1)
        dbAccessor.addVotingDay(votingDay)

        //execute
        val returnedVotingDay: VotingDay? = dbAccessor.getVotingDay(LocalDate.of(2012, 12, 12))

        //verify
        Assertions.assertNull(returnedVotingDay)
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
        dbAccessor.addCadence(voting1.votingDay.cadence)
        dbAccessor.addCadence(voting2.votingDay.cadence)
        dbAccessor.addVotingDay(voting1.votingDay)
        dbAccessor.addVotingDay(voting2.votingDay)
        dbAccessor.addParty(party1)
        dbAccessor.addParty(party2)
        dbAccessor.addPerson(person1)
        dbAccessor.addPerson(person2)
        dbAccessor.addVoting(voting1)
        dbAccessor.addVoting(voting2)
        val vote1 = Vote(voting1, party1, person1, VoteResult.yes)
        val vote2 = Vote(voting1, party2, person2, VoteResult.no)
        val vote3 = Vote(voting2, party2, person1, VoteResult.absent)
        val vote4 = Vote(voting2, party1, person2, VoteResult.abstain)
        dbAccessor.addVote(vote1)
        dbAccessor.addVote(vote2)
        dbAccessor.addVote(vote3)
        dbAccessor.addVote(vote4)

        //execute
        val returnedVotes: Set<Vote> = dbAccessor.getVotes()

        //verify
        Assertions.assertEquals(
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
        dbAccessor.addCadence(voting1.votingDay.cadence)
        dbAccessor.addCadence(voting2.votingDay.cadence)
        dbAccessor.addVotingDay(voting1.votingDay)
        dbAccessor.addVotingDay(voting2.votingDay)
        dbAccessor.addParty(party1)
        dbAccessor.addParty(party2)
        dbAccessor.addPerson(person1)
        dbAccessor.addPerson(person2)
        dbAccessor.addVoting(voting1)
        dbAccessor.addVoting(voting2)
        val vote1 = Vote(voting1, party1, person1, VoteResult.yes)
        val vote2 = Vote(voting1, party2, person2, VoteResult.no)
        val vote3 = Vote(voting2, party2, person1, VoteResult.absent)
        val vote4 = Vote(voting2, party1, person2, VoteResult.abstain)
        dbAccessor.addVote(vote1)
        dbAccessor.addVote(vote2)
        dbAccessor.addVote(vote3)
        dbAccessor.addVote(vote4)

        //execute
        val returnedVotes: Set<Vote> = dbAccessor.getVotesFor(voting1)

        //verify
        Assertions.assertEquals(
            setOf(vote1, vote2),
            returnedVotes
        )
    }

    fun assertDatabaseState(
        cadences: List<Cadence> = emptyList(),
        parties: List<Party> = emptyList(),
        people: List<Person> = emptyList(),
        votingDays: List<VotingDay> = emptyList(),
        votings: List<Voting> = emptyList(),
        votes: List<Vote> = emptyList(),
    ) {
        dbAccessor.getCadences() shouldContainExactlyInAnyOrder cadences
        dbAccessor.getParties() shouldContainExactlyInAnyOrder parties
        dbAccessor.getPeople() shouldContainExactlyInAnyOrder people
        dbAccessor.getVotingDays() shouldContainExactlyInAnyOrder votingDays
        dbAccessor.getVotings() shouldContainExactlyInAnyOrder votings
        dbAccessor.getVotes() shouldContainExactlyInAnyOrder votes
    }
}