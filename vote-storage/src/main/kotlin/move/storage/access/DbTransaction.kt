package move.storage.access

import model.*
import move.storage.queries.*
import org.neo4j.driver.Transaction
import java.util.function.Function
import java.util.stream.*

class DbTransaction(private val transaction: Transaction) {
    fun addParty(party: Party) {
        addPartyQuery(party).writeOrThrow(transaction)
    }
    
    fun tryAddParty(party: Party) {
        tryAddPartyQuery(party).writeOrThrow(transaction)
    }
    
    fun addPerson(person: Person) {
        addPersonQuery(person).writeOrThrow(transaction)
    }
    
    fun tryAddPerson(person: Person) {
        tryAddPersonQuery(person).writeOrThrow(transaction)
    }
    
    fun addVoting(voting: Voting) {
        addVotingQuery(voting).writeOrThrow(transaction)
    }
    
    fun tryAddVoting(voting: Voting) {
        tryAddVotingQuery(voting).writeOrThrow(transaction)
    }
    
    fun addCadence(cadence: Cadence) {
        addCadenceQuery(cadence).writeOrThrow(transaction)
    }
    
    fun tryAddCadence(cadence: Cadence) {
        tryAddCadenceQuery(cadence).writeOrThrow(transaction)
    }
    
    fun addVotingDay(votingDay: VotingDay) {
        addVotingDayQuery(votingDay).writeOrThrow(transaction)
    }
    
    fun tryAddVotingDay(votingDay: VotingDay) {
        tryAddVotingDayQuery(votingDay).writeOrThrow(transaction)
    }
    
    fun addVote(vote: Vote) {
        addVoteQuery(vote).writeOrThrow(transaction)
    }
    
    fun tryAddVote(vote: Vote) {
        tryAddVoteQuery(vote).writeOrThrow(transaction)
    }
    
    fun getParty(name: String): Party? {
        return getPartyQuery(name).read(transaction)
    }
    
    fun getPerson(name: String): Person? {
        return getPersonQuery(name).read(transaction)
    }
    
    fun getVoting(name: String): Voting? {
        return getVotingQuery(name).read(transaction)
    }
    
    fun getParties(): Map<String,Party> {
        return getPartiesQuery().read(transaction)
            .stream()
            .collect(Collectors.toMap(Party::name, Function.identity()))
    }
    
    fun getPeople(): Map<String,Person> {
        return getPeopleQuery().read(transaction)
            .stream()
            .collect(Collectors.toMap(Person::name, Function.identity()))
    }
    
    fun getVotings(): Map<String,Voting> {
        return getVotingsQuery().read(transaction)
            .stream()
            .collect(Collectors.toMap(Voting::name, Function.identity()))
    }
}
