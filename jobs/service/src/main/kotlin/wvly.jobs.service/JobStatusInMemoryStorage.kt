package wvly.jobs.service

import wvly.models.jobs.*
import java.time.Instant

class JobStatusInMemoryStorage {
    private val jobStatuses = mutableMapOf<JobId, JobStatusEntity>()
    private val jobStepStatuses = mutableMapOf<JobStepId, JobStepStatusEntity>()

    fun add(jobStatus: JobStatus) {
        val entity = JobStatusEntity(jobStatus)
        jobStatuses[jobStatus.id] = entity
        jobStatus.steps.forEach { step ->
            add(
                parentJob = entity,
                jobStepStatus = step,
            )
        }
    }

    private fun add(
        parentJob: JobStatusEntity,
        parentStep: JobStepStatusEntity? = null,
        jobStepStatus: JobStepStatus,
    ) {
        val entity = JobStepStatusEntity(
            parentJob = parentJob,
            parentStep = parentStep,
            jobStepStatus = jobStepStatus,
        )
        jobStepStatuses[jobStepStatus.id] = entity
        parentJob.steps.add(entity)
        parentStep?.childSteps?.add(entity)
        jobStepStatus.childSteps.forEach { step ->
            add(parentJob = parentJob, parentStep = entity, jobStepStatus = step)
        }
    }

    fun get(jobId: JobId): JobStatus =
        jobStatuses[jobId]
            ?.toJobStatus()
            ?: throw NoSuchElementException("No job with ID $jobId was found")

    fun update(jobStatus: JobStatus) {
        remove(jobStatus)
        add(jobStatus)
    }

    private fun remove(jobStatus: JobStatus) {
        val job = jobStatuses[jobStatus.id]
            ?: throw NoSuchElementException("No job with ID ${jobStatus.id} was found")

        jobStatuses.remove(job.id)

        val jobStepStatusIterator = jobStepStatuses.iterator()
        while (jobStepStatusIterator.hasNext()) {
            if (jobStepStatusIterator
                    .next()
                    .value.parentJob.id == job.id
            ) {
                jobStepStatusIterator.remove()
            }
        }
    }

    fun getStep(stepId: JobStepId): JobStepStatus =
        jobStepStatuses[stepId]
            ?.toJobStepStatus()
            ?: throw NoSuchElementException("No step with ID $stepId was found")

    fun update(jobStepStatus: JobStepStatus) {
        val entity = jobStepStatuses[jobStepStatus.id]
            ?: throw NoSuchElementException("No step with ID ${jobStepStatus.id} was found")
        remove(entity)
        add(
            parentJob = entity.parentJob,
            parentStep = entity.parentStep,
            jobStepStatus = jobStepStatus,
        )
    }

    private fun remove(entity: JobStepStatusEntity) {
        entity.parentJob.steps.remove(entity)
        entity.parentStep?.childSteps?.remove(entity)

        entity.childSteps.toList().forEach { child ->
            remove(child)
        }
    }
}

private class JobStatusEntity(
    var id: JobId,
    var state: JobState,
    var name: String,
    var description: String,
    var statusMessage: String,
    var startedAt: Instant,
    var finishedAt: Instant?,
    var steps: MutableList<JobStepStatusEntity> = mutableListOf(),
) {
    constructor(jobStatus: JobStatus) : this(
        id = jobStatus.id,
        state = jobStatus.state,
        name = jobStatus.name,
        description = jobStatus.description,
        statusMessage = jobStatus.statusMessage,
        startedAt = jobStatus.startedAt,
        finishedAt = jobStatus.finishedAt,
    )

    fun toJobStatus(): JobStatus =
        JobStatus(
            id = id,
            state = state,
            name = name,
            description = description,
            statusMessage = statusMessage,
            startedAt = startedAt,
            finishedAt = finishedAt,
            steps = steps
                .filter { it.parentStep == null } // only direct steps
                .map { it.toJobStepStatus() },
        )
}

private class JobStepStatusEntity(
    var id: JobStepId,
    var state: JobStepState,
    var name: String,
    var description: String,
    var statusMessage: String,
    var startedAt: Instant,
    var finishedAt: Instant?,
    var parentJob: JobStatusEntity,
    var parentStep: JobStepStatusEntity?,
    var childSteps: MutableList<JobStepStatusEntity> = mutableListOf(),
) {
    constructor(
        parentJob: JobStatusEntity,
        parentStep: JobStepStatusEntity? = null,
        jobStepStatus: JobStepStatus,
    ) : this(
        id = jobStepStatus.id,
        state = jobStepStatus.state,
        name = jobStepStatus.name,
        description = jobStepStatus.description,
        statusMessage = jobStepStatus.statusMessage,
        startedAt = jobStepStatus.startedAt,
        finishedAt = jobStepStatus.finishedAt,
        parentJob = parentJob,
        parentStep = parentStep,
    )

    fun toJobStepStatus(): JobStepStatus =
        JobStepStatus(
            id = id,
            state = state,
            name = name,
            description = description,
            statusMessage = statusMessage,
            startedAt = startedAt,
            finishedAt = finishedAt,
            childSteps = childSteps.map { it.toJobStepStatus() },
        )
}
