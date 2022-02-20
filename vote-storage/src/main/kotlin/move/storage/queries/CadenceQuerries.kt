package move.storage.queries

import model.Cadence
import move.storage.access.ObjectFactory
import move.storage.access.WriteVerifier
import org.neo4j.driver.summary.SummaryCounters

class CadenceQuerries {
}

fun getCadencesQuery() = ReadSetQuery(
    "MATCH (cadence:Cadence) RETURN cadence",
    ObjectFactory::parseCadence
)

fun getCadenceQuery(number: Int) = ReadQuery(
    """MATCH (cadence:Cadence)
       WHERE cadence.number = $number
       RETURN cadence
    """.trimMargin(), ObjectFactory::parseCadence
)

fun addCadenceQuery(cadence: Cadence) = WriteQuery(
    insertCadenceQuery("CREATE"),
    mapOf<String, Any>(
        "number" to cadence.number,
        "status" to cadence.status.name,
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
        "status" to cadence.status.name,
        "daysWithVotes" to cadence.daysWithVotes,
    ),
    WriteVerifier()
        .verify(
            SummaryCounters::nodesCreated,
            1,
            "Cadence $cadence could not be added"
        )
)

fun updateCadenceQuery(cadence: Cadence) = WriteQuery(
    """MATCH (cadence:Cadence)
        WHERE cadence.number = ${'$'}number
        SET cadence.daysWihVotes = ${'$'}daysWithVotes,
            cadence.status = ${'$'}status
    """.trimMargin(),
    mapOf<String, Any>(
        "number" to cadence.number,
        "status" to cadence.status.name,
        "daysWithVotes" to cadence.daysWithVotes,
    ),
    WriteVerifier()
        .verifyAtleast(
            SummaryCounters::propertiesSet,
            1,
            "Cadence $cadence could not be updated"
        )
)

private fun insertCadenceQuery(operation: String) =
    "$operation (cadence:Cadence { number: \$number, status: \$status, daysWithVotes: \$daysWithVotes } )"