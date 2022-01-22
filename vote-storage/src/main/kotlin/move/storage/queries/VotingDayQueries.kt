package move.storage.queries

import model.VotingDay
import move.storage.access.*
import org.neo4j.driver.summary.SummaryCounters
import java.time.LocalDate

class VotingDayQueries {
}

fun getVotingDaysQuery() = ReadSetQuery(
    """MATCH (votingDay:VotingDay),
        (votingDay)-[:in]->(cadence)
        RETURN votingDay, cadence
    """.trimMargin(),
    ObjectFactory::parseVotingDay
)

fun getVotingDayQuery(date: LocalDate) = ReadQuery(
    """MATCH (votingDay:VotingDay)
       WHERE votingDay.date = date('$date')
       MATCH (votingDay)-[:in]->(cadence)
       RETURN votingDay, cadence
    """.trimMargin(),
    ObjectFactory::parseVotingDay
)

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
        .verify(
            SummaryCounters::relationshipsCreated,
            1,
            "VotingDay $votingDay could not be linked properly"
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
$operation (votingDay:VotingDay { date: ${'$'}date, votingsInDay: ${'$'}votingsInDay } )
$operation (votingDay)-[r1:in]->(cadence)
""".trimIndent()