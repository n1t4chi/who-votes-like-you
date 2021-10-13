package move.storage

import model.*
import org.neo4j.driver.*

class DbAccessor(private val dbConnector: DbConnector) {

    fun addParty(party: Party) {
        val updatedResults = write { tx -> tx.use {updatedResults(it.run(
            """
                CREATE(party:Party)
                SET party.name = '${party.name}'
            """.trimIndent()
        )) } }
        if (updatedResults != 1) {
            throw DbException("Party $party was not added")
        }
    }

    fun addPerson(person: Person) {
        val updatedResults = write { tx -> tx.use {updatedResults(it.run(
            """
                CREATE(person:Person)
                SET person.name = '${person.name}'
            """.trimIndent()
        )) } }
        if (updatedResults != 1) {
            throw DbException("Person $person was not added")
        }
    }

    fun addVoting(voting: Voting) {
        val updatedResults = write { tx -> tx.use {updatedResults(it.run(
            """
                CREATE(voting:Voting)
                SET voting.name = '${voting.name}'
            """.trimIndent()
        )) } }
        if (updatedResults != 1) {
            throw DbException("Voting $voting was not added")
        }
    }

    fun addVote(vote: Vote) {
        val updatedResults = write { tx -> tx.use {updatedResults(it.run(
            """ MATCH (voting:Voting) (person:Person) (party:Party)
                WHERE voting.name = '${vote.voting.name}' AND person.name = '${vote.person.name}' AND party.name = '${vote.party.name}'
                CREATE (vote:Vote),(vote)-[castBy]->(person),(vote)->[castFor]->(party),vote-[castAt]->(voting)
                SET vote.result = '${vote.result}'
            """.trimIndent()
        )) } }
        if (updatedResults != 1) {
            throw DbException("Vote $vote was not added")
        }
    }

    private fun updatedResults(run: Result) = run.consume().counters().indexesAdded()

    private fun <T> write(work: TransactionWork<T>): T {
        return doInSession { session: Session -> session.writeTransaction(work) }
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