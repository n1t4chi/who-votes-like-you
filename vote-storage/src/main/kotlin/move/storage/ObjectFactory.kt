package move.storage

import model.*
import org.neo4j.driver.Record

object ObjectFactory {
    fun parsePerson(record: Record): Person {
        val person = record.get("person")
        return Person(person.get("name").asString())
    }

    fun parseParty(record: Record): Party {
        val party = record.get("party")
        return Party(party.get("name").asString())
    }

    fun parseVoting(record: Record): Voting {
        val voting = record.get("voting")
        return Voting(voting.get("name").asString())
    }
}