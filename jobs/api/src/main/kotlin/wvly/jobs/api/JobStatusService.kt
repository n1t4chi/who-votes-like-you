package wvly.jobs.api

import wvly.models.jobs.JobId
import wvly.models.jobs.JobStepId

interface JobStatusService {
    fun createJobStatus(
        name: String,
        description: String,
    ): JobId

    fun addStepToJob(
        jobId: JobId,
        name: String,
        description: String,
    ): JobStepId

    fun addStepToStep(
        stepId: JobStepId,
        name: String,
        description: String,
    ): JobStepId
}
