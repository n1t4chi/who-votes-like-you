package move.storage.queries

import model.Party
import move.storage.access.*
import org.neo4j.driver.summary.SummaryCounters

class PartyQueries {
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