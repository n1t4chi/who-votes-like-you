package move.storage

import model.*
import org.neo4j.driver.*

class DbAccessor(private val dbConnector: DbConnector, private val config: SessionConfig) {
    constructor(dbConnector: DbConnector) : this(dbConnector, SessionConfig.defaultConfig())
    
    fun addParty(party: Party) = write(addPartyQuery(party))
    
    fun addPerson(person: Person) = write(addPersonQuery(person))
    
    fun addVoting(voting: Voting) = write(addVotingQuery(voting))
    
    fun addVote(vote: Vote) = write(addVoteQuery(vote))
    
    fun getPeople(): Set<Person> = readSet(getPeopleQuery())
    
    fun getVotings(): Set<Voting> = readSet(getVotingsQuery())
    
    fun getVotesFor(voting: Voting): Set<Vote> = readSet(getVotesForQuery(voting))
    
    fun getParties(): Set<Party> = readSet(getPartiesQuery())
    
    fun getParty(name: String): Party? = readSingle(getPartyQuery(name))
    
    fun getPerson(name: String): Person? = readSingle(getPersonQuery(name))
    
    fun getVoting(name: String): Voting? = readSingle(getVotingQuery(name))
    
    fun getVotes(): Set<Vote> = readSet(getVotesQuery())
    
    
    private fun <T> readSet(query: ReadSetQuery<T>): Set<T> {
        return doInTransactionInternal { tx -> query.read(tx) }
    }
    
    private fun <T> readSingle(query: ReadQuery<T>): T? {
        return doInTransactionInternal { tx -> query.read(tx) }
    }
    
    private fun write(query: WriteQuery): VerifyResult {
        return doInTransactionInternal { tx -> query.tryWrite(tx) }
    }
    
    fun doInTransaction(consumer: TransactionConsumer) {
        doInTransactionInternal { transaction -> consumer.accept(DbTransaction(transaction)) }
    }
    
    private fun <T> doInTransactionInternal(consumer: TransactionWork<T>): T {
        return doInSession { session: Session ->
            session.beginTransaction().use { transaction -> tryToDoInTransaction(transaction, consumer) }
        }
    }
    
    private fun <T> tryToDoInTransaction(transaction: Transaction, consumer: TransactionWork<T>): T {
        try {
            val returnedValue = consumer.execute(transaction)
            transaction.commit()
            return returnedValue
        } catch (ex: Exception) {
            transaction.rollback()
            throw ex
        }
    }
    
    private fun <T> doInSession(onSession: SessionWork<T>): T {
        return dbConnector.openConnection().use { db -> db.session(config).use(onSession::apply) }
    }
    
    
    fun interface SessionWork<T> {
        fun apply(session: Session): T
    }
}