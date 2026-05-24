package wvly.jobs.api

import io.kotest.matchers.equality.*
import wvly.models.jobs.*
import java.time.Instant
import java.util.*
import kotlin.time.Duration.Companion.seconds

fun failedStep(
    name: String,
    description: String,
    statusMessage: String,
    childSteps: List<JobStepStatus> = emptyList(),
): JobStepStatus =
    JobStepStatus(
        id = ignoredStepId,
        state = JobStepState.FAILED,
        name = name,
        description = description,
        statusMessage = statusMessage,
        startedAt = Instant.now(),
        finishedAt = Instant.now(),
        childSteps = childSteps,
    )

fun finishedStep(
    name: String,
    description: String,
    childSteps: List<JobStepStatus> = emptyList(),
): JobStepStatus =
    JobStepStatus(
        id = ignoredStepId,
        state = JobStepState.FINISHED,
        name = name,
        description = description,
        statusMessage = "Completed successfully",
        startedAt = Instant.now(),
        finishedAt = Instant.now(),
        childSteps = childSteps,
    )

val timestampMatcher = matchInstantsWithTolerance(1.seconds)

val ignoredStepId = JobStepId(UUID(0, 0))
val notIgnoredStatusIdMatcher = Assertable { _, actual ->
    if (actual == ignoredStepId) {
        CustomComparisonResult.Different(AssertionError("Value should not be dummy value of [$ignoredStepId]"))
    } else {
        CustomComparisonResult.Equal
    }
}

infix fun JobStatus.shouldMatch(other: JobStatus) {
    this.shouldBeEqualUsingFields {
        this.overrideMatchers = mapOf(
            JobStatus::startedAt to timestampMatcher,
            JobStatus::finishedAt to timestampMatcher,
            JobStepStatus::startedAt to timestampMatcher,
            JobStepStatus::finishedAt to timestampMatcher,
            JobStepStatus::id to notIgnoredStatusIdMatcher,
        )
        this.excludedProperties = listOf()
        other
    }
}
