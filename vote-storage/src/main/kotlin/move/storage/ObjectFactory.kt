package move.storage

import model.*
import org.neo4j.driver.Record

object ObjectFactory {
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
            Cadence(voting.get("cadence").asInt(),0),
            voting.get("date").asLocalDate(),
            0
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