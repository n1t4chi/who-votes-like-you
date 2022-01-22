package move.storage.queries

import model.Voting
import move.storage.access.*
import org.neo4j.driver.summary.SummaryCounters

class VotingQueries {
}

fun getVotingQuery(name: String) = ReadQuery(
    """MATCH (voting:Voting)
           WHERE voting.name = '$name'
       MATCH (voting)-[r1:on]->(votingDay:VotingDay),
         (votingDay)-[r2:in]->(cadence:Cadence)
           RETURN voting, votingDay, cadence
        """.trimMargin(),
    ObjectFactory::parseVoting
)

fun getVotingsQuery() = ReadSetQuery(
    """MATCH (voting:Voting),
         (voting)-[r1:on]->(votingDay:VotingDay),
         (votingDay)-[r2:in]->(cadence:Cadence)
        RETURN voting, votingDay, cadence
    """.trimIndent(),
    ObjectFactory::parseVoting
)

fun addVotingQuery(voting: Voting) = WriteQuery(
    insertVotingQuery("CREATE"),
    mapOf(
        "name" to voting.name,
        "number" to voting.number,
        "votesCast" to voting.votesCast,
        "votingDay" to voting.votingDay.date,
    ),
    WriteVerifier()
        .verify(
            SummaryCounters::nodesCreated,
            1,
            "Voting $voting could not be added"
        )
        .verify(
            SummaryCounters::relationshipsCreated,
            1,
            "Voting $voting could not be linked properly"
        )
)

fun tryAddVotingQuery(voting: Voting) = WriteQuery(
    insertVotingQuery("MERGE"),
    mapOf(
        "name" to voting.name,
        "number" to voting.number,
        "votesCast" to voting.votesCast,
        "votingDay" to voting.votingDay.date,
    )
)

private fun insertVotingQuery(operation: String) = """
MATCH (votingDay:VotingDay)
WHERE votingDay.date = ${'$'}votingDay
$operation (voting:Voting { name: ${'$'}name, number: ${'$'}number, votesCast: ${'$'}votesCast } )
$operation (voting)-[r1:on]->(votingDay)
""".trimIndent()