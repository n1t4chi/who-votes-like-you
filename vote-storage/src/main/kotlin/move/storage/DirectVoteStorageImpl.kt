package move.storage

import model.*
import move.storage.access.DbAccessor
import move.storage.access.DbTransaction
import vote.storage.VoteStorage
import java.time.LocalDate
import java.util.function.Function
import java.util.stream.Collectors

class DirectVoteStorageImpl(private val accessor: DbAccessor) : VoteStorage {
    override fun getCadence(number: Int): Cadence? = accessor.getCadence(number)
    
    override fun getCadences(): Collection<Cadence> = accessor.getCadences()
    
    override fun getVotingDay(date: LocalDate): VotingDay? = accessor.getVotingDay(date)
    
    override fun getVoting(name: String): Voting? = accessor.getVoting(name)
    
    override fun getPerson(name: String): Person? = accessor.getPerson(name)
    
    override fun getParty(name: String): Party? = accessor.getParty(name)
    
    override fun getVotings(): Map<String, Voting> = byName(accessor.getVotings(), Voting::name)
    
    override fun getPeople(): Map<String, Person> = byName(accessor.getPeople(), Person::name)
    
    override fun getParties(): Map<String, Party> = byName(accessor.getParties(), Party::name)
    
    override fun getVotes(): Collection<Vote> = accessor.getVotes()
    
    override fun getVotesFor(voting: Voting): Collection<Vote> = accessor.getVotesFor(voting)
    
    private fun <T> byName(set: Set<T>, getter: (T)->String): Map<String, T> = set.stream()
        .collect(Collectors.toMap(getter, Function.identity()))
    
    
    override fun saveVotesUnsafe(votes: Collection<Vote>) {
        accessor.doInTransaction { transaction ->
            saveVotes(transaction, votes)
        }
    }
    
    override fun saveParties(parties: Collection<Party>) {
        accessor.doInTransaction { transaction ->
            saveParties(transaction, parties)
        }
    }
    
    override fun saveVotingDays(votingDays: Collection<VotingDay>) {
        accessor.doInTransaction { transaction ->
            saveCadences(transaction, mapToUnique(votingDays, VotingDay::cadence))
            saveVotingDays(transaction, votingDays)
        }
    }
    
    override fun saveVotingDaysUnsafe(votingDays: Collection<VotingDay>) {
        accessor.doInTransaction { transaction ->
            saveVotingDays(transaction, votingDays)
        }
    }
    
    override fun saveVotings(votings: Collection<Voting>) {
        accessor.doInTransaction { transaction ->
            saveVotingDays(transaction, mapToUnique(votings, Voting::votingDay))
            saveVotings(transaction, votings)
        }
    }
    
    override fun saveVotingsUnsafe(votings: Collection<Voting>) {
        accessor.doInTransaction { transaction ->
            saveVotings(transaction, votings)
        }
    }
    
    override fun savePeople(people: Collection<Person>) {
        accessor.doInTransaction { transaction ->
            savePeople(transaction, people)
        }
    }
    
    override fun saveVotes(votes: Collection<Vote>) {
        accessor.doInTransaction { transaction ->
            saveParties(transaction, mapToUnique(votes, Vote::party))
            savePeople(transaction, mapToUnique(votes, Vote::person))
            saveVotings(transaction, mapToUnique(votes, Vote::voting))
            saveVotes(transaction, votes)
        }
    }
    
    override fun saveParty(party: Party) {
        accessor.doInTransaction { transaction ->
            saveParty(transaction, party)
        }
    }
    
    override fun saveVoting(voting: Voting) {
        accessor.doInTransaction { transaction ->
            saveVotingDay(transaction, voting.votingDay)
            saveVoting(transaction, voting)
        }
    }
    
    override fun saveVotingUnsafe(voting: Voting) {
        accessor.doInTransaction { transaction ->
            saveVoting(transaction, voting)
        }
    }
    
    override fun savePerson(person: Person) {
        accessor.doInTransaction { transaction ->
            savePerson(transaction, person)
        }
    }
    
    override fun saveCadence(cadence: Cadence) {
        accessor.doInTransaction { transaction ->
            saveCadence(transaction, cadence)
        }
    }
    
    override fun updateCadence(cadence: Cadence) {
        accessor.doInTransaction { transaction ->
            updateCadence(transaction, cadence)
        }
    }
    
    override fun saveVotingDay(votingDay: VotingDay) {
        accessor.doInTransaction { transaction ->
            saveCadence(transaction, votingDay.cadence)
            saveVotingDay(transaction, votingDay)
        }
    }
    
    override fun saveVotingDayUnsafe(votingDay: VotingDay) {
        accessor.doInTransaction { transaction ->
            saveVotingDay(transaction, votingDay)
        }
    }
    
    override fun saveVoteUnsafe(vote: Vote) {
        accessor.doInTransaction { transaction ->
            saveVote(transaction, vote)
        }
    }
    
    override fun saveVote(vote: Vote) {
        accessor.doInTransaction { transaction ->
            saveVoting(transaction, vote.voting)
            saveParty(transaction, vote.party)
            savePerson(transaction, vote.person)
            saveVote(transaction, vote)
        }
    }
    
    private fun savePerson(transaction: DbTransaction, person: Person) = transaction.tryAddPerson(person)
    
    private fun saveVoting(transaction: DbTransaction, voting: Voting) = transaction.tryAddVoting(voting)
    
    private fun saveParty(transaction: DbTransaction, party: Party) = transaction.tryAddParty(party)
    
    private fun saveVote(transaction: DbTransaction, vote: Vote) = transaction.tryAddVote(vote)
    
    private fun saveCadence(transaction: DbTransaction, cadence: Cadence) = transaction.tryAddCadence(cadence)
    
    private fun updateCadence(transaction: DbTransaction, cadence: Cadence) = transaction.updateCadence(cadence)
    
    private fun saveVotingDay(transaction: DbTransaction, votingDay: VotingDay) = transaction.tryAddVotingDay(votingDay)
    
    
    private fun saveCadences(transaction: DbTransaction, cadences: Collection<Cadence>) {
        for (cadence in cadences) {
            saveCadence(transaction, cadence)
        }
    }
    
    private fun saveVotingDays(transaction: DbTransaction, votingDays: Collection<VotingDay>) {
        for (votingDay in votingDays) {
            saveVotingDay(transaction, votingDay)
        }
    }
    
    
    private fun saveVotes(transaction: DbTransaction, votes: Collection<Vote>) {
        for (vote in votes) {
            saveVote(transaction, vote)
        }
    }
    
    private fun saveParties(transaction: DbTransaction, parties: Collection<Party>) {
        for (party in parties) {
            saveParty(transaction, party)
        }
    }
    
    private fun saveVotings(transaction: DbTransaction, votings: Collection<Voting>) {
        for (voting in votings) {
            saveVoting(transaction, voting)
        }
    }
    
    private fun savePeople(transaction: DbTransaction, people: Collection<Person>) {
        for (person in people) {
            savePerson(transaction, person)
        }
    }
    
    private fun <K,T> mapToUnique(votes: Collection<K>, to: (K) -> T): Set<T> =
        votes.stream().map(to).collect(Collectors.toSet())
}