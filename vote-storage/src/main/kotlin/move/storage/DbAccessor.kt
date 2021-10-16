package move.storage

import model.*
import org.neo4j.driver.*
import org.neo4j.driver.summary.SummaryCounters

class DbAccessor(private val dbConnector: DbConnector) {

    fun addParty(party: Party) {
        write(
            "CREATE(party:Party { name: '${party.name}' } )",
            WriteVerifier()
                .verify( SummaryCounters::nodesCreated,
                    1,
                    "Party $party could not be added"
                )
        )
    }

    fun getParty(name: String) : List<Record> {
        return read{ tx -> tx.run("""
            match (party:Party) 
            where party.name = '$name' 
            return party
        """.trimIndent()).list()
        }
    }

    fun addPerson(person: Person) {
        write(
            "CREATE(person:Person { name: '${person.name}' } )",
            WriteVerifier()
                .verify( SummaryCounters::nodesCreated,
                    1,
                    "Party $person could not be added"
                )
        )
    }

    fun addVoting(voting: Voting) {
        write(
            "CREATE(voting:Voting { name: '${voting.name}' } )",
            WriteVerifier()
                .verify( SummaryCounters::nodesCreated,
                    1,
                    "Party $voting could not be added"
                )
        )
    }

    fun addVote(vote: Vote) {
        write(""" 
            MATCH (voting:Voting), (person:Person), (party:Party)
            WHERE voting.name = '${vote.voting.name}' AND person.name = '${vote.person.name}' AND party.name = '${vote.party.name}'
            CREATE (vote:Vote {result:'${vote.result}'} ), 
                (vote)-[r1:castBy]->(person), 
                (vote)-[r2:castFor]->(party), 
                (vote)-[r3:castAt]->(voting)
            """.trimIndent(),
            WriteVerifier()
                .verify( SummaryCounters::nodesCreated,
                    1,
                    "Party $vote could not be added"
                )
                .verify( SummaryCounters::relationshipsCreated,
                    3,
                    "Vote $vote could not been linked properly"
                )
        )
    }

    private fun write( query: String, verifier: WriteVerifier ) {
        return doInSession { session: Session ->
            session.writeTransaction{ tx: Transaction ->
                val result = tx.run(query)
                val verifyStatus = verifier.verify(result.consume().counters())
                if( verifyStatus.success ) {
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