package vote.fetcher.polishsejm

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import storage.inmemory.RawVoteCacheInMemoryImpl
import storage.inmemory.VoteCacheInMemoryImpl
import vote.fetcher.polishsejm.TestFixtures.shouldMatchIgnoringSessionTimestamps
import wvly.jobs.api.finishedStep
import wvly.jobs.api.shouldMatch
import wvly.jobs.service.JobStatusInMemoryStorage
import wvly.jobs.service.JobStatusServiceImpl
import wvly.jobs.service.JobStatusViewerImpl
import wvly.models.jobs.JobState
import wvly.models.jobs.JobStatus
import wvly.models.votes.RawVote
import java.time.Instant

class PolishSejmStartVoteProcessingTest : BehaviorSpec({
    val plugin = PolishSejmPlugin()

    Given("RawVotes with single party vote entry") {
        val storage = JobStatusInMemoryStorage()
        val rawVoteCache = RawVoteCacheInMemoryImpl()
        val voteCache = VoteCacheInMemoryImpl()
        val jobStatusService = JobStatusServiceImpl(storage)
        val jobId = jobStatusService.createJobStatus("test", "test")

        And("RawVotes contain votes_for_party with 2 voters") {
            rawVoteCache.put(
                handledTenant = polishSejmTenant,
                content = TestFixtures.partyVotesRawVote(
                    party = "PiS",
                    personToResultPairs = listOf(
                        "Jan Kowalski" to "Za",
                        "Alicja Nowak" to "Przeciw",
                    ),
                ),
            )
        }

        When("startVoteProcessing is called") {
            plugin.startVoteProcessing(jobId, jobStatusService, rawVoteCache, voteCache)

            Then("job finishes with full step tree and produces 2 votes with correct casters, parties, and results") {
                val actualJob = JobStatusViewerImpl(storage).getJobStatus(jobId)
                actualJob shouldMatch JobStatus(
                    id = jobId,
                    state = JobState.FINISHED,
                    name = "test",
                    description = "test",
                    statusMessage = "Completed successfully",
                    startedAt = Instant.now(),
                    finishedAt = Instant.now(),
                    steps = listOf(
                        finishedStep(
                            name = "Polish Sejm Vote Processing",
                            description = "Converts RawVotes (JSON metadata) to structured Vote objects using new core model.",
                            childSteps = listOf(
                                finishedStep(
                                    name = "Read [1] raw votes from cache",
                                    description = "Retrieved all raw vote entries for processing.",
                                ),
                                finishedStep(
                                    name = "Processed party [PiS]: 2 votes",
                                    description = "Converted 2 person-result pairs to Vote objects.",
                                ),
                                finishedStep(
                                    name = "Wrote [2] votes to cache",
                                    description = "Pipeline completed successfully.",
                                ),
                            ),
                        ),
                    ),
                )

                val actualVotes = voteCache.get(polishSejmTenant).shouldNotBeNull()
                val expectedVotes = listOf(
                    TestFixtures.expectedVote("Jan Kowalski", "PiS", resultPolish = "Za"),
                    TestFixtures.expectedVote("Alicja Nowak", "PiS", resultPolish = "Przeciw"),
                )
                actualVotes.shouldMatchIgnoringSessionTimestamps(expectedVotes)
            }
        }
    }

    Given("RawVotes with multiple party vote entries") {
        val storage = JobStatusInMemoryStorage()
        val rawVoteCache = RawVoteCacheInMemoryImpl()
        val voteCache = VoteCacheInMemoryImpl()
        val jobStatusService = JobStatusServiceImpl(storage)
        val jobId = jobStatusService.createJobStatus("test", "test")

        And("RawVotes contain 2 party entries with different parties") {
            rawVoteCache.put(
                handledTenant = polishSejmTenant,
                content = TestFixtures.partyVotesRawVote(
                    party = "PiS",
                    personToResultPairs = listOf("A" to "Za", "B" to "Przeciw"),
                ),
            )
            rawVoteCache.put(
                handledTenant = polishSejmTenant,
                content = TestFixtures.partyVotesRawVote(
                    party = "PO",
                    personToResultPairs = listOf("C" to "Wstrzymam_się", "D" to "Obecny"),
                ),
            )
        }

        When("startVoteProcessing is called") {
            plugin.startVoteProcessing(jobId, jobStatusService, rawVoteCache, voteCache)

            Then("job finishes with full step tree and produces 4 votes with correct casters, parties, and results") {
                val actualJob = JobStatusViewerImpl(storage).getJobStatus(jobId)
                actualJob shouldMatch JobStatus(
                    id = jobId,
                    state = JobState.FINISHED,
                    name = "test",
                    description = "test",
                    statusMessage = "Completed successfully",
                    startedAt = Instant.now(),
                    finishedAt = Instant.now(),
                    steps = listOf(
                        finishedStep(
                            name = "Polish Sejm Vote Processing",
                            description = "Converts RawVotes (JSON metadata) to structured Vote objects using new core model.",
                            childSteps = listOf(
                                finishedStep(
                                    name = "Read [2] raw votes from cache",
                                    description = "Retrieved all raw vote entries for processing.",
                                ),
                                finishedStep(
                                    name = "Processed party [PiS]: 2 votes",
                                    description = "Converted 2 person-result pairs to Vote objects.",
                                ),
                                finishedStep(
                                    name = "Processed party [PO]: 2 votes",
                                    description = "Converted 2 person-result pairs to Vote objects.",
                                ),
                                finishedStep(
                                    name = "Wrote [4] votes to cache",
                                    description = "Pipeline completed successfully.",
                                ),
                            ),
                        ),
                    ),
                )

                val actualVotes = voteCache.get(polishSejmTenant).shouldNotBeNull()
                val expectedVotes = listOf(
                    TestFixtures.expectedVote("A", "PiS", resultPolish = "Za"),
                    TestFixtures.expectedVote("B", "PiS", resultPolish = "Przeciw"),
                    TestFixtures.expectedVote("C", "PO", resultPolish = "Wstrzymam_się"),
                    TestFixtures.expectedVote("D", "PO", resultPolish = "Obecny"),
                )
                actualVotes.shouldMatchIgnoringSessionTimestamps(expectedVotes)
            }
        }
    }

    Given("Empty RawVoteCache") {
        val storage = JobStatusInMemoryStorage()
        val rawVoteCache = RawVoteCacheInMemoryImpl()
        val voteCache = VoteCacheInMemoryImpl()
        val jobStatusService = JobStatusServiceImpl(storage)
        val jobId = jobStatusService.createJobStatus("test", "test")

        When("startVoteProcessing is called with empty cache") {
            plugin.startVoteProcessing(jobId, jobStatusService, rawVoteCache, voteCache)

            Then("job finishes with full step tree and produces no votes") {
                val actualJob = JobStatusViewerImpl(storage).getJobStatus(jobId)
                actualJob shouldMatch JobStatus(
                    id = jobId,
                    state = JobState.FINISHED,
                    name = "test",
                    description = "test",
                    statusMessage = "Completed successfully",
                    startedAt = Instant.now(),
                    finishedAt = Instant.now(),
                    steps = listOf(
                        finishedStep(
                            name = "Polish Sejm Vote Processing",
                            description = "Converts RawVotes (JSON metadata) to structured Vote objects using new core model.",
                            childSteps = listOf(
                                finishedStep(
                                    name = "Read [0] raw votes from cache",
                                    description = "Retrieved all raw vote entries for processing.",
                                ),
                                finishedStep(
                                    name = "No raw votes to process",
                                    description = "Pipeline terminated: zero raw votes found.",
                                ),
                            ),
                        ),
                    ),
                )

                val actualVotes = voteCache.get(polishSejmTenant).shouldNotBeNull()
                actualVotes.shouldHaveSize(0)
            }
        }
    }

    Given("RawVotes with malformed format (missing pipe separator)") {
        val storage = JobStatusInMemoryStorage()
        val rawVoteCache = RawVoteCacheInMemoryImpl()
        val voteCache = VoteCacheInMemoryImpl()
        val jobStatusService = JobStatusServiceImpl(storage)
        val jobId = jobStatusService.createJobStatus("test", "test")

        And("RawVotes contain invalid format without pipe separator") {
            rawVoteCache.put(
                handledTenant = polishSejmTenant,
                content = RawVote("invalid_format_no_pipe"),
            )
        }

        When("startVoteProcessing is called") {
            plugin.startVoteProcessing(jobId, jobStatusService, rawVoteCache, voteCache)

            Then("job finishes with full step tree and produces no votes (malformed data skipped)") {
                val actualJob = JobStatusViewerImpl(storage).getJobStatus(jobId)
                actualJob shouldMatch JobStatus(
                    id = jobId,
                    state = JobState.FINISHED,
                    name = "test",
                    description = "test",
                    statusMessage = "Completed successfully",
                    startedAt = Instant.now(),
                    finishedAt = Instant.now(),
                    steps = listOf(
                        finishedStep(
                            name = "Polish Sejm Vote Processing",
                            description = "Converts RawVotes (JSON metadata) to structured Vote objects using new core model.",
                            childSteps = listOf(
                                finishedStep(
                                    name = "Read [1] raw votes from cache",
                                    description = "Retrieved all raw vote entries for processing.",
                                ),
                                finishedStep(
                                    name = "No party vote entries found",
                                    description = "Pipeline terminated: no votes_for_party type entries.",
                                ),
                            ),
                        ),
                    ),
                )

                val actualVotes = voteCache.get(polishSejmTenant).shouldNotBeNull()
                actualVotes.shouldHaveSize(0)
            }
        }
    }

    Given("RawVotes with malformed person_to_result format") {
        val storage = JobStatusInMemoryStorage()
        val rawVoteCache = RawVoteCacheInMemoryImpl()
        val voteCache = VoteCacheInMemoryImpl()
        val jobStatusService = JobStatusServiceImpl(storage)
        val jobId = jobStatusService.createJobStatus("test", "test")

        And("RawVotes contain invalid person_to_result format (missing semicolon)") {
            rawVoteCache.put(
                handledTenant = polishSejmTenant,
                content = TestFixtures.partyVotesRawVote(
                    party = "X",
                    personToResultPairs = listOf("Person1" to "Za"),
                ),
            )
        }

        When("startVoteProcessing is called") {
            plugin.startVoteProcessing(jobId, jobStatusService, rawVoteCache, voteCache)

            Then("job finishes with full step tree and produces 1 vote from valid data") {
                val actualJob = JobStatusViewerImpl(storage).getJobStatus(jobId)
                actualJob shouldMatch JobStatus(
                    id = jobId,
                    state = JobState.FINISHED,
                    name = "test",
                    description = "test",
                    statusMessage = "Completed successfully",
                    startedAt = Instant.now(),
                    finishedAt = Instant.now(),
                    steps = listOf(
                        finishedStep(
                            name = "Polish Sejm Vote Processing",
                            description = "Converts RawVotes (JSON metadata) to structured Vote objects using new core model.",
                            childSteps = listOf(
                                finishedStep(
                                    name = "Read [1] raw votes from cache",
                                    description = "Retrieved all raw vote entries for processing.",
                                ),
                                finishedStep(
                                    name = "Processed party [X]: 1 votes",
                                    description = "Converted 1 person-result pairs to Vote objects.",
                                ),
                                finishedStep(
                                    name = "Wrote [1] votes to cache",
                                    description = "Pipeline completed successfully.",
                                ),
                            ),
                        ),
                    ),
                )

                val actualVotes = voteCache.get(polishSejmTenant).shouldNotBeNull()
                actualVotes.shouldHaveSize(1)
            }
        }
    }

    Given("RawVotes with empty person_to_result field") {
        val storage = JobStatusInMemoryStorage()
        val rawVoteCache = RawVoteCacheInMemoryImpl()
        val voteCache = VoteCacheInMemoryImpl()
        val jobStatusService = JobStatusServiceImpl(storage)
        val jobId = jobStatusService.createJobStatus("test", "test")

        And("RawVotes contain empty person_to_result field") {
            rawVoteCache.put(
                handledTenant = polishSejmTenant,
                content = TestFixtures.partyVotesRawVote(
                    party = "X",
                    personToResultPairs = emptyList(),
                ),
            )
        }

        When("startVoteProcessing is called") {
            plugin.startVoteProcessing(jobId, jobStatusService, rawVoteCache, voteCache)

            Then("job finishes with full step tree and produces 0 votes (no person-result pairs)") {
                val actualJob = JobStatusViewerImpl(storage).getJobStatus(jobId)
                actualJob shouldMatch JobStatus(
                    id = jobId,
                    state = JobState.FINISHED,
                    name = "test",
                    description = "test",
                    statusMessage = "Completed successfully",
                    startedAt = Instant.now(),
                    finishedAt = Instant.now(),
                    steps = listOf(
                        finishedStep(
                            name = "Polish Sejm Vote Processing",
                            description = "Converts RawVotes (JSON metadata) to structured Vote objects using new core model.",
                            childSteps = listOf(
                                finishedStep(
                                    name = "Read [1] raw votes from cache",
                                    description = "Retrieved all raw vote entries for processing.",
                                ),
                                finishedStep(
                                    name = "Processed party [X]: 0 votes",
                                    description = "Converted 0 person-result pairs to Vote objects.",
                                ),
                                finishedStep(
                                    name = "Wrote [0] votes to cache",
                                    description = "Pipeline completed successfully.",
                                ),
                            ),
                        ),
                    ),
                )

                val actualVotes = voteCache.get(polishSejmTenant).shouldNotBeNull()
                actualVotes.shouldHaveSize(0)
            }
        }
    }

    Given("RawVotes with unknown VoteResult value") {
        val storage = JobStatusInMemoryStorage()
        val rawVoteCache = RawVoteCacheInMemoryImpl()
        val voteCache = VoteCacheInMemoryImpl()
        val jobStatusService = JobStatusServiceImpl(storage)
        val jobId = jobStatusService.createJobStatus("test", "test")

        And("RawVotes contain unknown result value (e.g., 'Unknown')") {
            rawVoteCache.put(
                handledTenant = polishSejmTenant,
                content = TestFixtures.partyVotesRawVote(
                    party = "X",
                    personToResultPairs = listOf("Person1" to "Unknown"),
                ),
            )
        }

        When("startVoteProcessing is called") {
            plugin.startVoteProcessing(jobId, jobStatusService, rawVoteCache, voteCache)

            Then("job finishes with full step tree and produces 0 votes (unknown result skipped)") {
                val actualJob = JobStatusViewerImpl(storage).getJobStatus(jobId)
                actualJob shouldMatch JobStatus(
                    id = jobId,
                    state = JobState.FINISHED,
                    name = "test",
                    description = "test",
                    statusMessage = "Completed successfully",
                    startedAt = Instant.now(),
                    finishedAt = Instant.now(),
                    steps = listOf(
                        finishedStep(
                            name = "Polish Sejm Vote Processing",
                            description = "Converts RawVotes (JSON metadata) to structured Vote objects using new core model.",
                            childSteps = listOf(
                                finishedStep(
                                    name = "Read [1] raw votes from cache",
                                    description = "Retrieved all raw vote entries for processing.",
                                ),
                                finishedStep(
                                    name = "Processed party [X]: 0 votes",
                                    description = "Converted 1 person-result pairs to Vote objects.",
                                ),
                                finishedStep(
                                    name = "Wrote [0] votes to cache",
                                    description = "Pipeline completed successfully.",
                                ),
                            ),
                        ),
                    ),
                )

                val actualVotes = voteCache.get(polishSejmTenant).shouldNotBeNull()
                actualVotes.shouldHaveSize(0)
            }
        }
    }

    Given("RawVotes with whitespace in person names and results") {
        val storage = JobStatusInMemoryStorage()
        val rawVoteCache = RawVoteCacheInMemoryImpl()
        val voteCache = VoteCacheInMemoryImpl()
        val jobStatusService = JobStatusServiceImpl(storage)
        val jobId = jobStatusService.createJobStatus("test", "test")

        And("RawVotes contain whitespace in values") {
            rawVoteCache.put(
                handledTenant = polishSejmTenant,
                content = TestFixtures.partyVotesRawVote(
                    party = "X",
                    personToResultPairs = listOf(
                        " Person1 " to " Za ",
                        " Person2 " to " Przeciw ",
                    ),
                ),
            )
        }

        When("startVoteProcessing is called") {
            plugin.startVoteProcessing(jobId, jobStatusService, rawVoteCache, voteCache)

            Then("job finishes with full step tree and produces 2 votes with trimmed values") {
                val actualJob = JobStatusViewerImpl(storage).getJobStatus(jobId)
                actualJob shouldMatch JobStatus(
                    id = jobId,
                    state = JobState.FINISHED,
                    name = "test",
                    description = "test",
                    statusMessage = "Completed successfully",
                    startedAt = Instant.now(),
                    finishedAt = Instant.now(),
                    steps = listOf(
                        finishedStep(
                            name = "Polish Sejm Vote Processing",
                            description = "Converts RawVotes (JSON metadata) to structured Vote objects using new core model.",
                            childSteps = listOf(
                                finishedStep(
                                    name = "Read [1] raw votes from cache",
                                    description = "Retrieved all raw vote entries for processing.",
                                ),
                                finishedStep(
                                    name = "Processed party [X]: 2 votes",
                                    description = "Converted 2 person-result pairs to Vote objects.",
                                ),
                                finishedStep(
                                    name = "Wrote [2] votes to cache",
                                    description = "Pipeline completed successfully.",
                                ),
                            ),
                        ),
                    ),
                )

                val actualVotes = voteCache.get(polishSejmTenant).shouldNotBeNull()
                val expectedVotes = listOf(
                    TestFixtures.expectedVote("Person1", "X", resultPolish = "Za"),
                    TestFixtures.expectedVote("Person2", "X", resultPolish = "Przeciw"),
                )
                actualVotes.shouldMatchIgnoringSessionTimestamps(expectedVotes)
            }
        }
    }

    Given("RawVotes with special characters in person names") {
        val storage = JobStatusInMemoryStorage()
        val rawVoteCache = RawVoteCacheInMemoryImpl()
        val voteCache = VoteCacheInMemoryImpl()
        val jobStatusService = JobStatusServiceImpl(storage)
        val jobId = jobStatusService.createJobStatus("test", "test")

        And("RawVotes contain special characters (polish diacritics)") {
            rawVoteCache.put(
                handledTenant = polishSejmTenant,
                content = TestFixtures.partyVotesRawVote(
                    party = "PiS",
                    personToResultPairs = listOf(
                        "Jan Kowalski" to "Za",
                        "Alicja Nowak" to "Przeciw",
                    ),
                ),
            )
        }

        When("startVoteProcessing is called") {
            plugin.startVoteProcessing(jobId, jobStatusService, rawVoteCache, voteCache)

            Then("job finishes with full step tree and produces 2 votes preserving special characters in person names") {
                val actualJob = JobStatusViewerImpl(storage).getJobStatus(jobId)
                actualJob shouldMatch JobStatus(
                    id = jobId,
                    state = JobState.FINISHED,
                    name = "test",
                    description = "test",
                    statusMessage = "Completed successfully",
                    startedAt = Instant.now(),
                    finishedAt = Instant.now(),
                    steps = listOf(
                        finishedStep(
                            name = "Polish Sejm Vote Processing",
                            description = "Converts RawVotes (JSON metadata) to structured Vote objects using new core model.",
                            childSteps = listOf(
                                finishedStep(
                                    name = "Read [1] raw votes from cache",
                                    description = "Retrieved all raw vote entries for processing.",
                                ),
                                finishedStep(
                                    name = "Processed party [PiS]: 2 votes",
                                    description = "Converted 2 person-result pairs to Vote objects.",
                                ),
                                finishedStep(
                                    name = "Wrote [2] votes to cache",
                                    description = "Pipeline completed successfully.",
                                ),
                            ),
                        ),
                    ),
                )

                val actualVotes = voteCache.get(polishSejmTenant).shouldNotBeNull()
                val expectedVotes = listOf(
                    TestFixtures.expectedVote("Jan Kowalski", "PiS", resultPolish = "Za"),
                    TestFixtures.expectedVote("Alicja Nowak", "PiS", resultPolish = "Przeciw"),
                )
                actualVotes.shouldMatchIgnoringSessionTimestamps(expectedVotes)
            }
        }
    }
})
