package vote.storage;

import model.*;

import java.time.LocalDate;
import java.util.*;

public interface VoteStorage {
    Cadence getCadence(int number);
    
    VotingDay getVotingDay(LocalDate date);
    
    Voting getVoting(String name);
    
    Person getPerson(String name);
    
    Party getParty(String name);
    
    Map<String,Voting> getVotings();
    
    Map<String,Person> getPeople();
    
    Map<String,Party> getParties();
    
    Collection<Vote> getVotes();
    
    Collection<Vote> getVotesFor(Voting voting);
    
    void saveCadence(Cadence cadence);
    
    /**
     * Saves VotingDay to store. Assumes that Cadence is already added!
     * @param votingDay votingDay to save
     */
    void saveVotingDayUnsafe(VotingDay votingDay);
    
    /**
     * Saves VotingDay to store. will also add Cadence if necessary!
     * @param votingDay votingDay to save
     */
    default void saveVotingDay(VotingDay votingDay) {
        saveCadence(votingDay.getCadence());
        saveVotingDayUnsafe(votingDay);
    }
    
    /**
     * Saves Vote to store. Assumes that Person, Party and Voting are already added!
     * @param vote vote to save
     */
    void saveVoteUnsafe(Vote vote);
    
    /**
     * Saves Vote to store, will also add Person, Party or Voting if necessary
     * @param vote vote to save
     */
    default void saveVote(Vote vote) {
        saveParty(vote.getParty());
        savePerson(vote.getPerson());
        saveVoting(vote.getVoting());
        saveVoteUnsafe(vote);
    }
    
    void saveParty(Party party);
    
    /**
     * Saves Voting to store. Assumes that VotingDay is already added!
     * @param voting voting to save
     */
    void saveVotingUnsafe(Voting voting);
    
    /**
     * Saves voting to store, will also add VotingDay if necessary
     * @param voting voting to save
     */
    default void saveVoting(Voting voting) {
        saveVotingDay(voting.getVotingDay());
        saveVotingUnsafe(voting);
    }
    
    void savePerson(Person person);
    
    /**
     * Saves Votes to store, will also add Person, Party or Voting if necessary
     * @param votes votes to save
     */
    default void saveVotes(Vote... votes) {
        saveVotes(List.of(votes));
    }
    
    /**
     * Saves Votes to store, will also add Person, Party or Voting if necessary
     * @param votes votes to save
     */
    default void saveVotes(Collection<Vote> votes) {
        votes.forEach(this::saveVote);
    }
    
    /**
     * Saves Votes to store. Assumes that Person, Party and Voting are already added!
     * @param votes votes to save
     */
    default void saveVotesUnsafe(Vote... votes) {
        saveVotes(List.of(votes));
    }
    
    /**
     * Saves Votes to store. Assumes that Person, Party and Voting are already added!
     * @param votes votes to save
     */
    default void saveVotesUnsafe(Collection<Vote> votes) {
        votes.forEach(this::saveVote);
    }
    
    default void saveParties(Party... parties) {
        saveParties(List.of(parties));
    }
    
    default void saveParties(Collection<Party> parties) {
        parties.forEach(this::saveParty);
    }
    
    /**
     * Saves Votings to store. Assumes that VotingDay is already added!
     * @param votings voting to save
     */
    default void saveVotingsUnsafe(Voting... votings) {
        saveVotingsUnsafe(List.of(votings));
    }
    
    /**
     * Saves Votings to store. Assumes that VotingDay is already added!
     * @param votings voting to save
     */
    default void saveVotingsUnsafe(Collection<Voting> votings) {
        votings.forEach(this::saveVotingUnsafe);
    }
    
    default void saveVotings(Voting... votings) {
        saveVotings(List.of(votings));
    }
    
    default void saveVotings(Collection<Voting> votings) {
        votings.forEach(this::saveVoting);
    }
    
    default void savePeople(Person... people) {
        savePeople(List.of(people));
    }
    
    default void savePeople(Collection<Person> people) {
        people.forEach(this::savePerson);
    }
    
    default void saveCadences(Cadence... cadences) {
        saveCadences(List.of(cadences));
    }
    
    default void saveCadences(Collection<Cadence> cadences) {
        cadences.forEach(this::saveCadence);
    }
    
    default void saveVotingDays(VotingDay... votingDays) {
        saveVotingDays(List.of(votingDays));
    }
    
    default void saveVotingDays(Collection<VotingDay> votingDays) {
        votingDays.forEach(this::saveVotingDay);
    }
    
    /**
     * Saves VotingDays to store. Assumes that cadences are already added!
     * @param votingDays voting days to save
     */
    default void saveVotingDaysUnsafe(VotingDay... votingDays) {
        saveVotingDaysUnsafe(List.of(votingDays));
    }
    
    /**
     * Saves VotingDays to store. Assumes that cadences are already added!
     * @param votingDays voting to save
     */
    default void saveVotingDaysUnsafe(Collection<VotingDay> votingDays) {
        votingDays.forEach(this::saveVotingDay);
    }
}
