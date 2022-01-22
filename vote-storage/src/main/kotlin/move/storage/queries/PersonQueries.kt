package move.storage.queries

import model.Person
import move.storage.access.*
import org.neo4j.driver.summary.SummaryCounters

class PersonQueries {
}

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