package move.storage.queries

import model.*
import move.storage.access.*
import org.neo4j.driver.summary.SummaryCounters

class VotesQueries {
}fun getVotesForQuery(voting: Voting) = ReadSetQuery(
    """MATCH (vote:Vote),(voting:Voting),
              (vote)-[:castBy]->(person),
              (vote)-[:castFor]->(party),
              (vote)-[:castAt]->(voting),
              (voting)-[:on]->(votingDay),
              (votingDay)-[:in]->(cadence)
        WHERE
            voting.name = '${voting.name}'
        RETURN vote,person,party,voting,votingDay,cadence
        """.trimIndent(),
    ObjectFactory::parseVote
)

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
          (vote)-[:castAt]->(voting),
          (voting)-[:on]->(votingDay),
          (votingDay)-[:in]->(cadence)
    RETURN vote,person,party,voting,votingDay,cadence
    """.trimIndent(),
    ObjectFactory::parseVote
)