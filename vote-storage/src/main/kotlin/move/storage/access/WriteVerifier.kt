package move.storage.access

import org.neo4j.driver.summary.SummaryCounters
import java.util.stream.*

class WriteVerifier () {
    private val verifiers : MutableList<SingleVerifier> = mutableListOf()

    fun verify(getter : (SummaryCounters) -> Int,
               expectedCounterValue : Int,
               errorMessage : String
    ) : WriteVerifier {
        verifiers.add( SingleVerifier(expectedCounterValue, errorMessage, getter) )
        return this
    }

    fun verify( counters : SummaryCounters ): VerifyResult {
        val errors = verifiers.stream()
            .map{ verifier -> verifier.verify( counters ) }
            .filter{ result -> !result.success }
            .collect( Collectors.toList() )
        return if( errors.isEmpty() )
            ok()
        else
            error(
                errors.stream()
                    .map { result -> result.status }
                    .collect( Collectors.joining("\n") )
            )
    }

    private class SingleVerifier(
        val expectedCounterValue : Int,
        val errorMessage : String,
        val getter : (SummaryCounters) -> Int
    ) {
        fun verify( counters: SummaryCounters ): VerifyResult {
            return if (getter.invoke(counters) == expectedCounterValue)
                ok()
            else
                error(errorMessage)
        }
    }
}

