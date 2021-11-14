package vote.fetcher;

import model.Party;
import model.Person;
import model.Vote;
import model.Voting;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface VoteStorage {
    Voting getVoting(String name);
    
    Person getPerson(String name);
    
    Party getParty(String name);
    
    Map<String,Voting> getVotings();
    
    Map<String,Person> getPeolpe();
    
    Map<String,Party> getParties();
    
    Collection<Vote> getVotes();
    
    Collection<Vote> getVotesFor(Voting voting);
    
    /**
     * Saves Vote to store. Assumes that Person, Party and Voting are already added!
     *
     * @param vote vote to save
     */
    void saveVoteUnsafe(Vote vote);
    
    void saveParty(Party party);
    
    void saveVoting(Voting voting);
    
    void savePerson(Person person);
    
    /**
     * Saves Votes to store, will also add Person, Party or Voting if necessary
     *
     * @param votes votes to save
     */
    default void saveVotes(Vote... votes) {
        saveVotes(List.of(votes));
    }
    
    /**
     * Saves Votes to store, will also add Person, Party or Voting if necessary
     *
     * @param votes votes to save
     */
    default void saveVotes(Collection<Vote> votes) {
        votes.forEach(this::saveVote);
    }
    
    /**
     * Saves Vote to store, will also add Person, Party or Voting if necessary
     *
     * @param vote vote to save
     */
    default void saveVote(Vote vote) {
        saveParty(vote.getParty());
        savePerson(vote.getPerson());
        saveVoting(vote.getVoting());
        saveVoteUnsafe(vote);
    }
    
    /**
     * Saves Votes to store. Assumes that Person, Party and Voting are already added!
     *
     * @param votes
     */
    default void saveVotesUnsafe(Vote... votes) {
        saveVotes(List.of(votes));
    }
    
    /**
     * Saves Votes to store. Assumes that Person, Party and Voting are already added!
     *
     * @param votes
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
}
