package wvly.jobs.service

import wvly.jobs.api.JobStatusService
import wvly.models.jobs.*
import java.time.Instant
import java.util.*

class JobStatusServiceImpl(private val jobStatusStorage: JobStatusInMemoryStorage) : JobStatusService {
    override fun createJobStatus(
        name: String,
        description: String,
    ): JobId {
        val jobId = JobId(UUID.randomUUID())
        jobStatusStorage.add(
            JobStatus(
                id = jobId,
                state = JobState.FINISHED,
                name = name,
                description = description,
                statusMessage = "Completed successfully",
                startedAt = Instant.now(),
                finishedAt = Instant.now(),
                steps = emptyList(),
            ),
        )
        return jobId
    }

    override fun addStepToJob(
        jobId: JobId,
        name: String,
        description: String,
    ): JobStepId {
        val stepId = JobStepId(UUID.randomUUID())
        val jobStatus = jobStatusStorage.get(jobId)
        jobStatusStorage.update(
            jobStatus.copy(
                steps = jobStatus.steps + JobStepStatus(
                    id = stepId,
                    state = JobStepState.FINISHED,
                    name = name,
                    description = description,
                    statusMessage = "Completed successfully",
                    startedAt = Instant.now(),
                    finishedAt = Instant.now(),
                    childSteps = emptyList(),
                ),
            ),
        )
        return stepId
    }

    override fun addStepToStep(
        stepId: JobStepId,
        name: String,
        description: String,
    ): JobStepId {
        val jobStepStatus = jobStatusStorage.getStep(stepId)
        val newStepId = JobStepId(UUID.randomUUID())
        jobStatusStorage.update(
            jobStepStatus.copy(
                childSteps = jobStepStatus.childSteps + JobStepStatus(
                    id = newStepId,
                    state = JobStepState.FINISHED,
                    name = name,
                    description = description,
                    statusMessage = "Completed successfully",
                    startedAt = Instant.now(),
                    finishedAt = Instant.now(),
                    childSteps = emptyList(),
                ),
            ),
        )
        return newStepId
    }
}
