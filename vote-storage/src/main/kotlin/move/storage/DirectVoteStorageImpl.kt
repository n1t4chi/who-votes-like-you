package move.storage

import model.*
import vote.fetcher.VoteStorage
import java.util.function.Consumer
import java.util.stream.Collectors

class DirectVoteStorageImpl(private val accessor: DbAccessor) : VoteStorage {
    
    override fun saveVotes(votes: Collection<Vote>) {
        accessor.doInTransaction { transaction ->
            prepareParties(transaction, votes)
            preparePeople(transaction, votes)
            prepareVotings(transaction, votes)
            for (vote in votes) {
                saveVoteInTransaction(transaction, vote)
            }
        }
    }
    
    private fun prepareParties(tx: DbTransaction, votes: Collection<Vote>) {
        addMissing(votes, tx.getParties(), Vote::party, Party::name, tx::addParty)
    }
    
    private fun preparePeople(tx: DbTransaction, votes: Collection<Vote>) {
        addMissing(votes, tx.getPeople(), Vote::person, Person::name, tx::addPerson)
    }
    
    private fun prepareVotings(tx: DbTransaction, votes: Collection<Vote>) {
        addMissing(votes, tx.getVotings(), Vote::voting, Voting::name, tx::addVoting)
    }
    
    private fun <T> addMissing(
        votes: Collection<Vote>,
        map: Map<String,T>,
        tGetter: (Vote)->T,
        nameGetter: (T)->String,
        adder: Consumer<T>
    ) {
        votes.stream()
            .map(tGetter)
            .collect(Collectors.toSet())
            .stream()
            .filter { t -> !map.containsKey(nameGetter.invoke(t))}
            .forEach(adder)
    }
    
    override fun saveVote(vote: Vote) {
        accessor.doInTransaction { transaction ->
            prepareVote(transaction, vote)
            saveVoteInTransaction(transaction, vote)
        }
    }
    
    private fun prepareVote(transaction: DbTransaction, vote: Vote) {
        if (transaction.getVoting(vote.voting.name) == null) {
            transaction.addVoting(vote.voting)
        }
        if (transaction.getParty(vote.party.name) == null) {
            transaction.addParty(vote.party)
        }
        if (transaction.getPerson(vote.person.name) == null) {
            transaction.addPerson(vote.person)
        }
    }
    
    private fun saveVoteInTransaction(transaction: DbTransaction, vote: Vote) {
        transaction.addVote(vote)
    }
    
    
}