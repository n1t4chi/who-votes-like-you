package vote.storage;

import model.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class TestableVoteStorage implements VoteStorage {
    private final Map<Integer, Cadence> cadences = new HashMap<>();
    private final Map<LocalDate, VotingDay> votingDays = new HashMap<>();
    private final Map<String, Voting> votings = new HashMap<>();
    private final Map<String, Person> people = new HashMap<>();
    private final Map<String, Party> parties = new HashMap<>();
    private final Set<Vote> votes = new HashSet<>();
    
    public Set<Vote> getVotes() {
        return votes;
    }
    
    @Override
    public Collection<Vote> getVotesFor( Voting voting ) {
        return votes.stream()
                    .filter( vote -> vote.getVoting().equals( voting ) )
                    .collect( Collectors.toList() );
    }
    
    @Override
    public void saveCadence( Cadence cadence ) {
        cadences.put( cadence.getNumber(), cadence );
    }
    
    @Override
    public Cadence getCadence( int number ) {
        return cadences.get( number );
    }
    
    @Override
    public VotingDay getVotingDay( LocalDate date ) {
        return votingDays.get( date );
    }
    
    @Override
    public Voting getVoting( String name ) {
        return votings.get( name );
    }
    
    @Override
    public Person getPerson( String name ) {
        return people.get( name );
    }
    
    @Override
    public Party getParty( String name ) {
        return parties.get( name );
    }
    
    @Override
    public Map<String, Voting> getVotings() {
        return votings;
    }
    
    @Override
    public Map<String, Person> getPeople() {
        return people;
    }
    
    @Override
    public Map<String, Party> getParties() {
        return parties;
    }
    
    @Override
    public void saveVoteUnsafe( Vote vote ) {
        votes.add( vote );
    }
    
    @Override
    public void saveParty( Party party ) {
        parties.put( party.getName(), party );
    }
    
    @Override
    public void saveVotingUnsafe( Voting voting ) {
        votings.put( voting.getName(), voting );
    }
    
    @Override
    public void saveVotingDayUnsafe( VotingDay votingDay ) {
        votingDays.put( votingDay.getDate(), votingDay );
    }
    
    @Override
    public void savePerson( Person person ) {
        people.put( person.getName(), person );
    }
}
