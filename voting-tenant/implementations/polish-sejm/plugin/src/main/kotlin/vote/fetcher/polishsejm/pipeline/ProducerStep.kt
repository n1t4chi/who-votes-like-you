package vote.fetcher.polishsejm.pipeline

import wvly.jobs.api.JobStatusService
import wvly.models.jobs.JobId
import wvly.models.jobs.JobStepId

/**
 * A step that produces items — the "source" of the pipeline.
 * Unlike MapperStep, it does not receive input from a previous step;
 * it generates new data by calling an external source (e.g., HTTP API).
 */
abstract class ProducerStep<T>(val name: String) {
    abstract fun produce(
        jobId: JobId,
        jobStatusService: JobStatusService,
        parentStepId: JobStepId?,
    ): List<T>
}
