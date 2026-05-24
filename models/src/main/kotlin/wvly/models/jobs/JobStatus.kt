package wvly.models.jobs

import java.time.Instant
import java.util.*

data class JobId(val value: UUID)

data class JobStatus(
    val id: JobId,
    val state: JobState,
    val name: String,
    val description: String,
    val statusMessage: String,
    val startedAt: Instant,
    val finishedAt: Instant?,
    val steps: List<JobStepStatus>,
)

enum class JobState(name: String) {
    FINISHED("finished"),
    FAILED("failed"),
}

data class JobStepId(val value: UUID)

data class JobStepStatus(
    val id: JobStepId,
    val state: JobStepState,
    val name: String,
    val description: String,
    val statusMessage: String,
    val startedAt: Instant,
    val finishedAt: Instant?,
    val childSteps: List<JobStepStatus>,
)

enum class JobStepState(name: String) {
    FINISHED("finished"),
    FAILED("failed"),
}
