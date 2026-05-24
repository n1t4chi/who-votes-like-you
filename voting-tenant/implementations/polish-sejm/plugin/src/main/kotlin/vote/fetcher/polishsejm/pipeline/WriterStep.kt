package vote.fetcher.polishsejm.pipeline

import wvly.jobs.api.JobStatusService
import wvly.models.jobs.JobId
import wvly.models.jobs.JobStepId

/**
 * A step that writes each item to an external destination.
 */
abstract class WriterStep<T>(val name: String) {
    abstract fun write(
        jobId: JobId,
        jobStatusService: JobStatusService,
        parentStepId: JobStepId?,
        input: T,
    )
}

/**
 * A step that writes all collected items at once (batch writer).
 */
abstract class BatchWriterStep<T>(val name: String) {
    abstract fun writeAll(
        jobId: JobId,
        jobStatusService: JobStatusService,
        parentStepId: JobStepId?,
        items: List<T>,
    )
}
