package move.storage

import model.*
import org.neo4j.driver.summary.SummaryCounters

class Queries {
}

fun getPartiesQuery() = ReadSetQuery(
    "MATCH (party:Party) RETURN party",
    ObjectFactory::parseParty
)

fun getPartyQuery(name: String) = ReadQuery(
    """MATCH (party:Party)
       WHERE party.name = '$name'
       RETURN party
    """.trimMargin(), ObjectFactory::parseParty
)

fun getPersonQuery(name: String) = ReadQuery(
    """MATCH (person:Person)
           WHERE person.name = '$name'
           RETURN person
        """.trimMargin(),
    ObjectFactory::parsePerson
)

fun getPeopleQuery() = ReadSetQuery(
    "MATCH (person:Person) RETURN person",
    ObjectFactory::parsePerson
)

fun getVotingQuery(name: String) = ReadQuery(
    """MATCH (voting:Voting)
           WHERE voting.name = '$name'
           RETURN voting
        """.trimMargin(),
    ObjectFactory::parseVoting
)

fun getVotingsQuery() = ReadSetQuery(
    "MATCH (voting:Voting) RETURN voting",
    ObjectFactory::parseVoting
)

fun getVotesForQuery(voting: Voting) = ReadSetQuery(
    """MATCH (vote:Vote),(voting:Voting),
              (vote)-[:castBy]->(person),
              (vote)-[:castFor]->(party),
              (vote)-[:castAt]->(voting)
        WHERE
            voting.name = '${voting.name}'
        RETURN vote,person,party,voting
        """.trimIndent(),
    ObjectFactory::parseVote
)

fun addPartyQuery(party: Party) = WriteQuery(
    "CREATE (party:Party { name: \$name } )",
    mapOf("name" to party.name),
    WriteVerifier()
        .verify(
            SummaryCounters::nodesCreated,
            1,
            "Party $party could not be added"
        )
)

fun addPersonQuery(person: Person) = WriteQuery(
    "CREATE (person:Person { name: \$name } )",
    mapOf("name" to person.name),
    WriteVerifier()
        .verify(
            SummaryCounters::nodesCreated,
            1,
            "Party $person could not be added"
        )
)

fun addVotingQuery(voting: Voting) = WriteQuery(
    "CREATE (voting:Voting { name: ${"$"}name, number: ${"$"}number, cadence: ${"$"}cadence, date: ${"$"}date } )",
    mapOf<String, Any>(
        "name" to voting.name,
        "number" to voting.number,
        "cadence" to voting.cadence.number,
        "date" to voting.date,
    ),
    WriteVerifier()
        .verify(
            SummaryCounters::nodesCreated,
            1,
            "Party $voting could not be added"
        )
)

fun addVoteQuery(vote: Vote) = WriteQuery(
    """
    MATCH (voting:Voting), (person:Person), (party:Party)
    WHERE voting.name = '${vote.voting.name}' AND person.name = '${vote.person.name}' AND party.name = '${vote.party.name}'
    CREATE (vote:Vote {result:${'$'}vote} )
    CREATE (vote)-[r1:castBy]->(person)
    CREATE (vote)-[r2:castFor]->(party)
    CREATE (vote)-[r3:castAt]->(voting)
    """.trimIndent(),
    mapOf(
        "votingName" to vote.voting.name,
        "personName" to vote.person.name,
        "partyName" to vote.party.name,
        "vote" to vote.result.name,
    ),
    WriteVerifier()
        .verify(
            SummaryCounters::nodesCreated,
            1,
            "Party $vote could not be added"
        )
        .verify(
            SummaryCounters::relationshipsCreated,
            3,
            "Vote $vote could not been linked properly"
        )
)

fun getVotesQuery() = ReadSetQuery(
    """MATCH (vote:Vote),
              (vote)-[:castBy]->(person),
              (vote)-[:castFor]->(party),
              (vote)-[:castAt]->(voting)
        RETURN vote,person,party,voting
        """.trimIndent(),
    ObjectFactory::parseVote
)