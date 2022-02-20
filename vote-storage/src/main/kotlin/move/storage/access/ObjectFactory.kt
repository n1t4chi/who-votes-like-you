package move.storage.access

import model.*
import org.neo4j.driver.Record

object ObjectFactory {
    fun parseCadence(record: Record): Cadence {
        val cadence = record.get("cadence")
        return Cadence(
            cadence.get("number").asInt(),
            CadenceStatus.valueOf(cadence.get("status").asString()),
            cadence.get("daysWithVotes").asInt()
        )
    }
    fun parseVotingDay(record: Record): VotingDay {
        val votingDay = record.get("votingDay")
        return VotingDay(
            parseCadence(record),
            votingDay.get("date").asLocalDate(),
            votingDay.get("votingsInDay").asInt()
        )
    }
    
    fun parsePerson(record: Record): Person {
        return Person(record.get("person").get("name").asString())
    }

    fun parseParty(record: Record): Party {
        return Party(record.get("party").get("name").asString())
    }

    fun parseVoting(record: Record): Voting {
        val voting = record.get("voting")
        return Voting(
            voting.get("name").asString(),
            voting.get("number").asInt(),
            parseVotingDay(record),
            voting.get("votesCast").asInt(),
        )
    }

    fun parseVote(record: Record): Vote {
        val vote = record.get("vote")
        val votingResult = vote.get("result")
        return Vote(
            parseVoting( record ),
            parseParty( record ),
            parsePerson( record ),
            VoteResult.parse( votingResult.asString() )
        )
    }
}