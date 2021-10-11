package storage

import org.neo4j.driver.*

class DbConnector {
    fun setup() {
        val database = GraphDatabase.driver( "bolt://localhost:7687", AuthTokens.basic( "neo4j", "neo4j" ))
        val session = database.session()
        val result = session.writeTransaction( { tx -> tx.run {
                "CREATE (a:Greeting) " +
                "SET a.message = hello " +
                "RETURN a.message + ', from node ' + id(a)"
            }
        } )
        println( result )
    }
}

fun main() {
    DbConnector().setup()
}