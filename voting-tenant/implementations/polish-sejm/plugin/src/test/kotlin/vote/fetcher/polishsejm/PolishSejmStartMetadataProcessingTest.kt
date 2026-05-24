package vote.fetcher.polishsejm

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import storage.inmemory.RawVoteCacheInMemoryImpl
import storage.inmemory.VotingSessionMetadataCacheInMemoryImpl
import vote.fetcher.polishsejm.TestFixtures.shouldMatchIgnoringIds
import vote.fetcher.polishsejm.TestFixtures.shouldMatchIgnoringTimestamps
import wvly.jobs.api.finishedStep
import wvly.jobs.api.shouldMatch
import wvly.jobs.service.JobStatusInMemoryStorage
import wvly.jobs.service.JobStatusServiceImpl
import wvly.jobs.service.JobStatusViewerImpl
import wvly.models.jobs.JobId
import wvly.models.jobs.JobState
import wvly.models.jobs.JobStatus
import wvly.models.jobs.JobStepStatus
import wvly.models.votes.RawVote
import java.time.Instant

class PolishSejmStartMetadataProcessingTest : BehaviorSpec({
    val plugin = PolishSejmPlugin()

    Given("RawVotes with single voting session") {
        val storage = JobStatusInMemoryStorage()
        val rawVoteCache = RawVoteCacheInMemoryImpl()
        val metadataCache = VotingSessionMetadataCacheInMemoryImpl()
        val jobStatusService = JobStatusServiceImpl(storage)
        val jobId = jobStatusService.createJobStatus("test", "test")

        And("RawVotes contain voting session with term, date, number, and name") {
            rawVoteCache.put(
                handledTenant = polishSejmTenant,
                content = TestFixtures.votingMetadataRawVote(
                    term = 10,
                    date = "2024-05-15",
                    number = 123,
                    name = "Ustawa o ochronie danych",
                ),
            )
        }

        When("startVotingSessionMetadataProcessing is called") {
            plugin.startVotingSessionMetadataProcessing(jobId, jobStatusService, rawVoteCache, metadataCache)

            Then(
                "job finishes with full step tree and produces 1 description with correct identifier, source, short description, and 2 tags",
            ) {
                val actualJob = JobStatusViewerImpl(storage).getJobStatus(jobId)
                actualJob shouldMatch expectedMetadataProcessingJob(
                    jobId = jobId,
                    readCount = 1,
                    descriptionCount = 1,
                    tagCount = 2,
                )

                val descriptions = metadataCache.getDescriptions(polishSejmTenant).shouldNotBeNull()
                val tags = metadataCache.getTags(polishSejmTenant).shouldNotBeNull()
                val expectedDescription = TestFixtures.expectedVotingSessionDescription(
                    term = 10,
                    date = "2024-05-15",
                    number = 123,
                    name = "Ustawa o ochronie danych",
                )
                descriptions shouldMatchIgnoringIds listOf(expectedDescription)
                tags shouldMatchIgnoringTimestamps listOf(
                    TestFixtures.expectedVotingSessionTag(term = 10, date = "2024-05-15", number = 123, tagText = "term-10"),
                    TestFixtures.expectedVotingSessionTag(term = 10, date = "2024-05-15", number = 123, tagText = "voting-123"),
                )
            }
        }
    }

    Given("RawVotes with multiple voting sessions (same term, different dates)") {
        val storage = JobStatusInMemoryStorage()
        val rawVoteCache = RawVoteCacheInMemoryImpl()
        val metadataCache = VotingSessionMetadataCacheInMemoryImpl()
        val jobStatusService = JobStatusServiceImpl(storage)
        val jobId = jobStatusService.createJobStatus("test", "test")

        And("RawVotes contain 2 voting sessions from same term but different days") {
            rawVoteCache.put(
                handledTenant = polishSejmTenant,
                content = TestFixtures.votingMetadataRawVote(
                    term = 10,
                    date = "2024-05-15",
                    number = 123,
                    name = "Session 1",
                ),
            )
            rawVoteCache.put(
                handledTenant = polishSejmTenant,
                content = TestFixtures.votingMetadataRawVote(
                    term = 10,
                    date = "2024-05-16",
                    number = 124,
                    name = "Session 2",
                ),
            )
        }

        When("startVotingSessionMetadataProcessing is called") {
            plugin.startVotingSessionMetadataProcessing(jobId, jobStatusService, rawVoteCache, metadataCache)

            Then("job finishes with full step tree and produces 2 descriptions with unique voting session identifiers") {
                val actualJob = JobStatusViewerImpl(storage).getJobStatus(jobId)
                actualJob shouldMatch expectedMetadataProcessingJob(
                    jobId = jobId,
                    readCount = 2,
                    descriptionCount = 2,
                    tagCount = 4,
                )

                val descriptions = metadataCache.getDescriptions(polishSejmTenant).shouldNotBeNull()
                val identifiers = descriptions.map { it.votingSession.identifier }
                identifiers.distinct().shouldHaveSize(2)
            }
        }
    }

    Given("RawVotes with duplicate voting sessions (same term, date, number)") {
        val storage = JobStatusInMemoryStorage()
        val rawVoteCache = RawVoteCacheInMemoryImpl()
        val metadataCache = VotingSessionMetadataCacheInMemoryImpl()
        val jobStatusService = JobStatusServiceImpl(storage)
        val jobId = jobStatusService.createJobStatus("test", "test")

        And("RawVotes contain 2 identical voting sessions (same term, date, number)") {
            rawVoteCache.put(
                handledTenant = polishSejmTenant,
                content = TestFixtures.votingMetadataRawVote(
                    term = 10,
                    date = "2024-05-15",
                    number = 123,
                    name = "Session 1",
                ),
            )
            rawVoteCache.put(
                handledTenant = polishSejmTenant,
                content = TestFixtures.votingMetadataRawVote(
                    term = 10,
                    date = "2024-05-15",
                    number = 123,
                    name = "Session 1 (duplicate)",
                ),
            )
        }

        When("startVotingSessionMetadataProcessing is called") {
            plugin.startVotingSessionMetadataProcessing(jobId, jobStatusService, rawVoteCache, metadataCache)

            Then("job finishes with full step tree and produces only 1 description with deduplication by session identifier") {
                val actualJob = JobStatusViewerImpl(storage).getJobStatus(jobId)
                actualJob shouldMatch expectedMetadataProcessingJob(
                    jobId = jobId,
                    readCount = 2,
                    descriptionCount = 1,
                    tagCount = 4,
                )

                val descriptions = metadataCache.getDescriptions(polishSejmTenant).shouldNotBeNull()
                descriptions[0].description shouldBe "Session 1"
            }
        }
    }

    Given("Empty RawVoteCache") {
        val storage = JobStatusInMemoryStorage()
        val rawVoteCache = RawVoteCacheInMemoryImpl()
        val metadataCache = VotingSessionMetadataCacheInMemoryImpl()
        val jobStatusService = JobStatusServiceImpl(storage)
        val jobId = jobStatusService.createJobStatus("test", "test")

        When("startVotingSessionMetadataProcessing is called with empty cache") {
            plugin.startVotingSessionMetadataProcessing(jobId, jobStatusService, rawVoteCache, metadataCache)

            Then("job finishes with full step tree and produces no descriptions or tags") {
                val actualJob = JobStatusViewerImpl(storage).getJobStatus(jobId)
                actualJob shouldMatch expectedMetadataProcessingJob(
                    jobId = jobId,
                    readCount = 0,
                    descriptionCount = 0,
                    tagCount = 0,
                    noRawVotesToProcess = true,
                )

                val descriptions = metadataCache.getDescriptions(polishSejmTenant).shouldNotBeNull()
                val tags = metadataCache.getTags(polishSejmTenant).shouldNotBeNull()
                descriptions.shouldHaveSize(0)
                tags.shouldHaveSize(0)
            }
        }
    }

    Given("RawVotes with malformed voting format (missing pipe separator)") {
        val storage = JobStatusInMemoryStorage()
        val rawVoteCache = RawVoteCacheInMemoryImpl()
        val metadataCache = VotingSessionMetadataCacheInMemoryImpl()
        val jobStatusService = JobStatusServiceImpl(storage)
        val jobId = jobStatusService.createJobStatus("test", "test")

        And("RawVotes contain invalid format without pipe separator") {
            rawVoteCache.put(
                handledTenant = polishSejmTenant,
                content = RawVote("invalid_format_no_pipe"),
            )
        }

        When("startVotingSessionMetadataProcessing is called") {
            plugin.startVotingSessionMetadataProcessing(jobId, jobStatusService, rawVoteCache, metadataCache)

            Then("job finishes with full step tree and produces no descriptions (malformed data silently skipped)") {
                val actualJob = JobStatusViewerImpl(storage).getJobStatus(jobId)
                actualJob shouldMatch expectedMetadataProcessingJob(
                    jobId = jobId,
                    readCount = 1,
                    descriptionCount = 0,
                    tagCount = 0,
                    silentlySkipped = true,
                )

                val descriptions = metadataCache.getDescriptions(polishSejmTenant).shouldNotBeNull()
                descriptions.shouldHaveSize(0)
            }
        }
    }

    Given("RawVotes with malformed voting session fields") {
        val storage = JobStatusInMemoryStorage()
        val rawVoteCache = RawVoteCacheInMemoryImpl()
        val metadataCache = VotingSessionMetadataCacheInMemoryImpl()
        val jobStatusService = JobStatusServiceImpl(storage)
        val jobId = jobStatusService.createJobStatus("test", "test")

        And("RawVotes contain missing required fields (no term)") {
            rawVoteCache.put(
                handledTenant = polishSejmTenant,
                content = RawVote("voting|date=2024-05-15&number=123&name=Session"),
            )
        }

        When("startVotingSessionMetadataProcessing is called") {
            plugin.startVotingSessionMetadataProcessing(jobId, jobStatusService, rawVoteCache, metadataCache)

            Then("job finishes with full step tree and produces no descriptions (missing required field silently skipped)") {
                val actualJob = JobStatusViewerImpl(storage).getJobStatus(jobId)
                actualJob shouldMatch expectedMetadataProcessingJob(
                    jobId = jobId,
                    readCount = 1,
                    descriptionCount = 0,
                    tagCount = 0,
                    silentlySkipped = true,
                )

                val descriptions = metadataCache.getDescriptions(polishSejmTenant).shouldNotBeNull()
                descriptions.shouldHaveSize(0)
            }
        }
    }

    Given("RawVotes with invalid date format") {
        val storage = JobStatusInMemoryStorage()
        val rawVoteCache = RawVoteCacheInMemoryImpl()
        val metadataCache = VotingSessionMetadataCacheInMemoryImpl()
        val jobStatusService = JobStatusServiceImpl(storage)
        val jobId = jobStatusService.createJobStatus("test", "test")

        And("RawVotes contain invalid date format (not YYYY-MM-DD)") {
            rawVoteCache.put(
                handledTenant = polishSejmTenant,
                content = TestFixtures.votingMetadataRawVote(term = 10, date = "2024/05/15", number = 123, name = "Session"),
            )
        }

        When("startVotingSessionMetadataProcessing is called") {
            plugin.startVotingSessionMetadataProcessing(jobId, jobStatusService, rawVoteCache, metadataCache)

            Then("job finishes with full step tree and produces no descriptions (invalid date silently skipped)") {
                val actualJob = JobStatusViewerImpl(storage).getJobStatus(jobId)
                actualJob shouldMatch expectedMetadataProcessingJob(
                    jobId = jobId,
                    readCount = 1,
                    descriptionCount = 0,
                    tagCount = 0,
                    silentlySkipped = true,
                )

                val descriptions = metadataCache.getDescriptions(polishSejmTenant).shouldNotBeNull()
                descriptions.shouldHaveSize(0)
            }
        }
    }

    Given("RawVotes with non-numeric term or number") {
        val storage = JobStatusInMemoryStorage()
        val rawVoteCache = RawVoteCacheInMemoryImpl()
        val metadataCache = VotingSessionMetadataCacheInMemoryImpl()
        val jobStatusService = JobStatusServiceImpl(storage)
        val jobId = jobStatusService.createJobStatus("test", "test")

        And("RawVotes contain non-numeric term (e.g., 'abc')") {
            rawVoteCache.put(
                handledTenant = polishSejmTenant,
                content = RawVote("voting|term=abc&date=2024-05-15&number=123&name=Session"),
            )
        }

        When("startVotingSessionMetadataProcessing is called") {
            plugin.startVotingSessionMetadataProcessing(jobId, jobStatusService, rawVoteCache, metadataCache)

            Then("job finishes with full step tree and produces no descriptions (non-numeric term silently skipped)") {
                val actualJob = JobStatusViewerImpl(storage).getJobStatus(jobId)
                actualJob shouldMatch expectedMetadataProcessingJob(
                    jobId = jobId,
                    readCount = 1,
                    descriptionCount = 0,
                    tagCount = 0,
                    silentlySkipped = true,
                )

                val descriptions = metadataCache.getDescriptions(polishSejmTenant).shouldNotBeNull()
                descriptions.shouldHaveSize(0)
            }
        }
    }

    Given("RawVotes with empty name field") {
        val storage = JobStatusInMemoryStorage()
        val rawVoteCache = RawVoteCacheInMemoryImpl()
        val metadataCache = VotingSessionMetadataCacheInMemoryImpl()
        val jobStatusService = JobStatusServiceImpl(storage)
        val jobId = jobStatusService.createJobStatus("test", "test")

        And("RawVotes contain empty name field") {
            rawVoteCache.put(
                handledTenant = polishSejmTenant,
                content = TestFixtures.votingMetadataRawVote(term = 10, date = "2024-05-15", number = 123, name = ""),
            )
        }

        When("startVotingSessionMetadataProcessing is called") {
            plugin.startVotingSessionMetadataProcessing(jobId, jobStatusService, rawVoteCache, metadataCache)

            Then("job finishes with full step tree and produces 1 description with empty name") {
                val actualJob = JobStatusViewerImpl(storage).getJobStatus(jobId)
                actualJob shouldMatch expectedMetadataProcessingJob(
                    jobId = jobId,
                    readCount = 1,
                    descriptionCount = 1,
                    tagCount = 2,
                )

                val descriptions = metadataCache.getDescriptions(polishSejmTenant).shouldNotBeNull()
                descriptions[0].description shouldBe ""
            }
        }
    }

    Given("RawVotes with very long name (over 100 characters)") {
        val storage = JobStatusInMemoryStorage()
        val rawVoteCache = RawVoteCacheInMemoryImpl()
        val metadataCache = VotingSessionMetadataCacheInMemoryImpl()
        val jobStatusService = JobStatusServiceImpl(storage)
        val jobId = jobStatusService.createJobStatus("test", "test")

        And("RawVotes contain name longer than 100 characters") {
            val longName = "A".repeat(150)
            rawVoteCache.put(
                handledTenant = polishSejmTenant,
                content = TestFixtures.votingMetadataRawVote(term = 10, date = "2024-05-15", number = 123, name = longName),
            )
        }

        When("startVotingSessionMetadataProcessing is called") {
            plugin.startVotingSessionMetadataProcessing(jobId, jobStatusService, rawVoteCache, metadataCache)

            Then("job finishes with full step tree and shortDescription is truncated to 100 characters") {
                val actualJob = JobStatusViewerImpl(storage).getJobStatus(jobId)
                actualJob shouldMatch expectedMetadataProcessingJob(
                    jobId = jobId,
                    readCount = 1,
                    descriptionCount = 1,
                    tagCount = 2,
                )

                val descriptions = metadataCache.getDescriptions(polishSejmTenant).shouldNotBeNull()
                descriptions[0].shortDescription.length shouldBe 100
            }
        }
    }
})

internal fun expectedMetadataProcessingJob(
    jobId: JobId,
    readCount: Int,
    descriptionCount: Int,
    tagCount: Int,
    noRawVotesToProcess: Boolean = false,
    silentlySkipped: Boolean = false,
): JobStatus {
    val steps = mutableListOf<JobStepStatus>()

    if (noRawVotesToProcess) {
        steps.add(
            finishedStep(
                name = "Polish Sejm Metadata Processing",
                description = "Extracts voting session descriptions and tags from RawVotes.",
                childSteps = listOf(
                    finishedStep(
                        name = "Read [$readCount] raw votes",
                        description = "Retrieved all raw vote entries for metadata extraction.",
                    ),
                    finishedStep(
                        name = "No raw votes to process",
                        description = "Pipeline terminated: zero raw votes found.",
                    ),
                ),
            ),
        )
    } else if (silentlySkipped) {
        steps.add(
            finishedStep(
                name = "Polish Sejm Metadata Processing",
                description = "Extracts voting session descriptions and tags from RawVotes.",
                childSteps = listOf(
                    finishedStep(
                        name = "Read [$readCount] raw votes",
                        description = "Retrieved all raw vote entries for metadata extraction.",
                    ),
                    finishedStep(
                        name = "Wrote [0] descriptions",
                        description = "Saved unique voting session descriptions.",
                    ),
                    finishedStep(
                        name = "Wrote [0] tags",
                        description = "Saved voting session tags.",
                    ),
                    finishedStep(
                        name = "Metadata processing complete",
                        description = "Pipeline completed successfully.",
                    ),
                ),
            ),
        )
    } else {
        steps.add(
            finishedStep(
                name = "Polish Sejm Metadata Processing",
                description = "Extracts voting session descriptions and tags from RawVotes.",
                childSteps = listOf(
                    finishedStep(
                        name = "Read [$readCount] raw votes",
                        description = "Retrieved all raw vote entries for metadata extraction.",
                    ),
                    finishedStep(
                        name = "Wrote [$descriptionCount] descriptions",
                        description = "Saved unique voting session descriptions.",
                    ),
                    finishedStep(
                        name = "Wrote [$tagCount] tags",
                        description = "Saved voting session tags.",
                    ),
                    finishedStep(
                        name = "Metadata processing complete",
                        description = "Pipeline completed successfully.",
                    ),
                ),
            ),
        )
    }

    return JobStatus(
        id = jobId,
        state = JobState.FINISHED,
        name = "test",
        description = "test",
        statusMessage = "Completed successfully",
        startedAt = Instant.now(),
        finishedAt = Instant.now(),
        steps = steps,
    )
}
