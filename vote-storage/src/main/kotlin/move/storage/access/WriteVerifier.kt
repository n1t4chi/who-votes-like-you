package move.storage.access

import org.neo4j.driver.summary.SummaryCounters
import java.util.stream.Collectors

class WriteVerifier() {
    private val verifiers: MutableList<SingleVerifier> = mutableListOf()
    
    fun verify(
        getter: (SummaryCounters) -> Int,
        expectedCounterValue: Int,
        errorMessage: String
    ): WriteVerifier {
        verifiers.add(ExactVerifier(expectedCounterValue, errorMessage, getter))
        return this
    }
    
    fun verifyAtleast(
        getter: (SummaryCounters) -> Int,
        minCounterValue: Int,
        errorMessage: String
    ): WriteVerifier {
        verifiers.add(AtLeastVerifier(minCounterValue, errorMessage, getter))
        return this
    }
    
    fun verify(counters: SummaryCounters): VerifyResult {
        val errors = verifiers.stream()
            .map { verifier -> verifier.verify(counters) }
            .filter { result -> !result.success }
            .collect(Collectors.toList())
        return if (errors.isEmpty())
            ok()
        else
            error(
                errors.stream()
                    .map { result -> result.status }
                    .collect(Collectors.joining("\n"))
            )
    }
    
    private class ExactVerifier(
        val expectedCounterValue: Int,
        errorMessage: String,
        val getter: (SummaryCounters) -> Int
    ) : SingleVerifier(errorMessage) {
        override fun test(summaryCounters: SummaryCounters) = getter.invoke(summaryCounters) == expectedCounterValue
    }
    
    private class AtLeastVerifier(
        val minCounterValue: Int,
        errorMessage: String,
        val getter: (SummaryCounters) -> Int
    ) : SingleVerifier(errorMessage) {
        override fun test(summaryCounters: SummaryCounters) = getter.invoke(summaryCounters) >= minCounterValue
    }
    
    private abstract class SingleVerifier(
        val errorMessage: String
    ) {
        abstract fun test(summaryCounters: SummaryCounters): Boolean
        
        fun verify(counters: SummaryCounters): VerifyResult {
            return if (test(counters))
                ok()
            else
                error(errorMessage)
        }
    }
}

