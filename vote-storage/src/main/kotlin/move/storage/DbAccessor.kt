package move.storage

import model.*
import org.neo4j.driver.*
import org.neo4j.driver.summary.SummaryCounters
import java.util.stream.Collectors

class DbAccessor(private val dbConnector: DbConnector) {

    fun addParty(party: Party) {
        write(
            "CREATE(party:Party { name: '${party.name}' } )",
            WriteVerifier()
                .verify(
                    SummaryCounters::nodesCreated,
                    1,
                    "Party $party could not be added"
                )
        )
    }

    fun getParties(): Set<Party> {
        return readSet("MATCH (party:Party) RETURN party", ObjectFactory::parseParty)
    }

    fun getParty(name: String): Party? {
        return readSingle(
            """MATCH (party:Party) 
                WHERE party.name = '$name' 
                RETURN party
            """.trimMargin(),
            ObjectFactory::parseParty
        )
    }

    fun addPerson(person: Person) {
        write(
            "CREATE(person:Person { name: '${person.name}' } )",
            WriteVerifier()
                .verify(
                    SummaryCounters::nodesCreated,
                    1,
                    "Party $person could not be added"
                )
        )
    }

    fun getPeople(): Set<Person> {
        return readSet("MATCH (person:Person) RETURN person", ObjectFactory::parsePerson)
    }

    fun getPerson(name: String): Person? {
        return readSingle(
            """MATCH (person:Person) 
                WHERE person.name = '$name' 
                RETURN person
            """.trimMargin(),
            ObjectFactory::parsePerson
        )
    }

    fun addVoting(voting: Voting) {
        write(
            "CREATE(voting:Voting { name: '${voting.name}' } )",
            WriteVerifier()
                .verify(
                    SummaryCounters::nodesCreated,
                    1,
                    "Party $voting could not be added"
                )
        )
    }

    fun getVotings(): Set<Voting> {
        return readSet("MATCH (voting:Voting) RETURN voting", ObjectFactory::parseVoting)
    }

    fun getVoting(name: String): Voting? {
        return readSingle(
            """MATCH (voting:Voting) 
                WHERE voting.name = '$name' 
                RETURN voting
            """.trimMargin(),
            ObjectFactory::parseVoting
        )
    }

    fun addVote(vote: Vote) {
        write(
            """ 
            MATCH (voting:Voting), (person:Person), (party:Party)
            WHERE voting.name = '${vote.voting.name}' AND person.name = '${vote.person.name}' AND party.name = '${vote.party.name}'
            CREATE (vote:Vote {result:'${vote.result}'} ), 
                (vote)-[r1:castBy]->(person), 
                (vote)-[r2:castFor]->(party), 
                (vote)-[r3:castAt]->(voting)
            """.trimIndent(),
            WriteVerifier()
                .verify(
                    SummaryCounters::nodesCreated,
                    1,
                    "Party $vote could not be added"
                )
                .verify(
                    SummaryCounters::relationshipsCreated,
                    3,
                    "Vote $vote could not been linked properly"
                )
        )
    }

    private fun <T> readSet(query: String, mapper: (Record) -> T): Set<T> {
        return read { tx ->
            tx.run(query)
                .stream()
                .map(mapper)
                .collect(Collectors.toSet())
        }
    }

    private fun <T> readSingle(query: String, mapper: (Record) -> T): T? {
        return read { tx -> mapOptionally( tx.run(query), mapper ) }
    }

    private fun <T> mapOptionally(run: Result, mapper: (Record) -> T): T? {
        return if( run.hasNext() )
            mapper.invoke( run.single() )
        else
            null
    }

    private fun write(query: String, verifier: WriteVerifier) {
        return doInSession { session: Session ->
            session.writeTransaction { tx: Transaction ->
                val result = tx.run(query)
                val verifyStatus = verifier.verify(result.consume().counters())
                if (verifyStatus.success) {
                    tx.commit()
                } else {
                    tx.rollback()
                    throw DbException("Write query '$query' failed. Reason:\n${verifyStatus.status}")
                }
            }
        }
    }

    private fun <T> read(work: TransactionWork<T>): T {
        return doInSession { session: Session -> session.readTransaction(work) }
    }

    private fun <T> doInSession(onSession: SessionWork<T>): T {
        return dbConnector.openConnection().use { db -> db.session().use(onSession::apply) }
    }


    fun interface SessionWork<T> {
        fun apply(session: Session): T
    }
}