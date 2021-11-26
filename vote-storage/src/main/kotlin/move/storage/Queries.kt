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

fun addCadenceQuery(cadence: Cadence) = WriteQuery(
    insertCadenceQuery("CREATE"),
    mapOf<String, Any>(
        "number" to cadence.number,
        "daysWithVotes" to cadence.daysWithVotes,
    ),
    WriteVerifier()
        .verify(
            SummaryCounters::nodesCreated,
            1,
            "Cadence $cadence could not be added"
        )
)

fun tryAddCadenceQuery(cadence: Cadence) = WriteQuery(
    insertCadenceQuery("MERGE"),
    mapOf<String, Any>(
        "number" to cadence.number,
        "daysWithVotes" to cadence.daysWithVotes,
    ),
    WriteVerifier()
        .verify(
            SummaryCounters::nodesCreated,
            1,
            "Cadence $cadence could not be added"
        )
)

private fun insertCadenceQuery(operation: String) =
    "$operation (cadence:Cadence { number: \$number, daysWithVotes: \$daysWithVotes } )"

fun addVotingDayQuery(votingDay: VotingDay) = WriteQuery(
    insertVotingDayQuery("CREATE"),
    mapOf<String, Any>(
        "date" to votingDay.date,
        "votingsInDay" to votingDay.votingsInDay,
        "cadence" to votingDay.cadence.number,
    ),
    WriteVerifier()
        .verify(
            SummaryCounters::nodesCreated,
            1,
            "VotingDay $votingDay could not be added"
        )
)
fun tryAddVotingDayQuery(votingDay: VotingDay) = WriteQuery(
    insertVotingDayQuery("MERGE"),
    mapOf<String, Any>(
        "date" to votingDay.date,
        "votingsInDay" to votingDay.votingsInDay,
        "cadence" to votingDay.cadence.number,
    ),
)

private fun insertVotingDayQuery(operation: String) = """
MATCH (cadence:Cadence)
WHERE cadence.number = ${'$'}cadence
$operation (votingDay:VotingDay { date: ${'$'}date, votingsInDay: ${'$'}votingsInDay } )" +
$operation (votingDay)-[r1:in]->(cadence)
""".trimIndent()

fun addVotingQuery(voting: Voting) = WriteQuery(
    insertVotingQuery("CREATE"),
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
            "Voting $voting could not be added"
        )
)

fun tryAddVotingQuery(voting: Voting) = WriteQuery(
    insertVotingQuery("MERGE"),
    mapOf<String, Any>(
        "name" to voting.name,
        "number" to voting.number,
        "cadence" to voting.cadence.number,
        "date" to voting.date,
    )
)

private fun insertVotingQuery(operation: String) =
    "$operation (voting:Voting { name: \$name, number: \$number, cadence: \$cadence, date: \$date } )"

fun addPartyQuery(party: Party) = WriteQuery(
    insertPartyQuery("CREATE"),
    mapOf("name" to party.name),
    WriteVerifier()
        .verify(
            SummaryCounters::nodesCreated,
            1,
            "Party $party could not be added"
        )
)

fun tryAddPartyQuery(party: Party) = WriteQuery(
    insertPartyQuery("MERGE"),
    mapOf("name" to party.name)
)

private fun insertPartyQuery(operation: String) = "$operation (party:Party { name: \$name } )"

fun addPersonQuery(person: Person) = WriteQuery(
    insertPersonQuery("CREATE"),
    mapOf("name" to person.name),
    WriteVerifier()
        .verify(
            SummaryCounters::nodesCreated,
            1,
            "Person $person could not be added"
        )
)

fun tryAddPersonQuery(person: Person) = WriteQuery(
    insertPersonQuery("MERGE"),
    mapOf("name" to person.name)
)

private fun insertPersonQuery(operation: String) = "$operation (person:Person { name: \$name } )"

fun tryAddVoteQuery(vote: Vote) = WriteQuery(
    insertVoteQuery("MERGE"),
    mapOf(
        "votingName" to vote.voting.name,
        "personName" to vote.person.name,
        "partyName" to vote.party.name,
        "vote" to vote.result.name,
    )
)

fun addVoteQuery(vote: Vote) = WriteQuery(
    insertVoteQuery("CREATE"),
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

private fun insertVoteQuery(operation: String) = """
    MATCH (voting:Voting), (person:Person), (party:Party)
    WHERE voting.name = ${'$'}votingName AND person.name = ${'$'}personName AND party.name = ${'$'}partyName
    $operation (vote:Vote {result:${'$'}vote} )
    $operation (vote)-[r1:castBy]->(person)
    $operation (vote)-[r2:castFor]->(party)
    $operation (vote)-[r3:castAt]->(voting)
    """.trimIndent()

fun getVotesQuery() = ReadSetQuery(
    """MATCH (vote:Vote),
          (vote)-[:castBy]->(person),
          (vote)-[:castFor]->(party),
          (vote)-[:castAt]->(voting)
    RETURN vote,person,party,voting
    """.trimIndent(),
    ObjectFactory::parseVote
)