package vote.fetcher.polishsejm.pipeline

import wvly.jobs.api.JobStatusService
import wvly.models.jobs.JobId
import wvly.models.jobs.JobStepId

/**
 * A step that transforms each item from the previous step into zero or more new items.
 * Used to expand a single input (e.g., a cadence) into many outputs (e.g., votings in days).
 */
abstract class MapperStep<I, O>(val name: String) {
    abstract fun map(
        jobId: JobId,
        jobStatusService: JobStatusService,
        parentStepId: JobStepId?,
        input: I,
    ): List<O>
}

/**
 * A step that filters items — keeps only those for which [filter] returns true.
 */
abstract class FilterStep<T>(val name: String) {
    abstract fun filter(input: T): Boolean
}
