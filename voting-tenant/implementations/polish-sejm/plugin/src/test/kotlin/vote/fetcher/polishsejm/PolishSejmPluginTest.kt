package vote.fetcher.polishsejm

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import storage.inmemory.RawVoteCacheInMemoryImpl
import storage.inmemory.VoteCacheInMemoryImpl
import storage.inmemory.VotingSessionMetadataCacheInMemoryImpl
import vote.fetcher.polishsejm.TestFixtures.shouldMatchIgnoringIds
import vote.fetcher.polishsejm.TestFixtures.shouldMatchIgnoringSessionTimestamps
import vote.fetcher.polishsejm.TestFixtures.shouldMatchIgnoringTimestamps
import wvly.jobs.api.finishedStep
import wvly.jobs.api.shouldMatch
import wvly.jobs.service.JobStatusInMemoryStorage
import wvly.jobs.service.JobStatusServiceImpl
import wvly.jobs.service.JobStatusViewerImpl
import wvly.models.jobs.JobState
import wvly.models.jobs.JobStatus
import wvly.models.votes.RawVote
import java.time.Instant

class PolishSejmPluginTest : BehaviorSpec({
    val storage = JobStatusInMemoryStorage()
    val rawVoteCache = RawVoteCacheInMemoryImpl()
    val voteCache = VoteCacheInMemoryImpl()
    val jobStatusService = JobStatusServiceImpl(storage)
    val plugin = PolishSejmPlugin()

    Given("RawVotes with single party vote entry") {
        val jobId = jobStatusService.createJobStatus("test", "test")

        And("RawVotes contain votes_for_party with 2 voters") {
            rawVoteCache.put(
                handledTenant = polishSejmTenant,
                content = RawVote("votes_for_party|party=PiS&person_to_result=Jan Kowalski:Za;Alicja Nowak:Przeciw"),
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

    Given("RawVotes with voting session metadata") {
        val storage = JobStatusInMemoryStorage()
        val rawVoteCache = RawVoteCacheInMemoryImpl()
        val metadataCache = VotingSessionMetadataCacheInMemoryImpl()
        val jobStatusService = JobStatusServiceImpl(storage)
        val jobId = jobStatusService.createJobStatus("test", "test")

        And("RawVotes contain voting session data") {
            rawVoteCache.put(
                handledTenant = polishSejmTenant,
                content = RawVote("voting|term=10&date=2024-05-15&number=123&name=Ustawa o ochronie danych"),
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

    Given("RawVotes with both voting session and vote data") {
        val storage = JobStatusInMemoryStorage()
        val rawVoteCache = RawVoteCacheInMemoryImpl()
        val voteCache = VoteCacheInMemoryImpl()
        val jobStatusService = JobStatusServiceImpl(storage)
        val jobId = jobStatusService.createJobStatus("test", "test")

        And("RawVotes contain both voting and votes_for_party entries") {
            rawVoteCache.put(
                handledTenant = polishSejmTenant,
                content = RawVote("voting|term=10&date=2024-05-15&number=123&name=Test Voting"),
            )
            rawVoteCache.put(
                handledTenant = polishSejmTenant,
                content = RawVote("votes_for_party|party=PiS&person_to_result=Jan Kowalski:Za;Alicja Nowak:Przeciw"),
            )
        }

        When("startVoteProcessing is called") {
            plugin.startVoteProcessing(jobId, jobStatusService, rawVoteCache, voteCache)

            Then("job finishes with full step tree and produces 2 votes from RawVotes with no data loss") {
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

    Given("Empty RawVoteCache") {
        val storage = JobStatusInMemoryStorage()
        val rawVoteCache = RawVoteCacheInMemoryImpl()
        val voteCache = VoteCacheInMemoryImpl()
        val jobStatusService = JobStatusServiceImpl(storage)
        val jobId = jobStatusService.createJobStatus("test", "test")

        When("startVoteProcessing is called with empty cache") {
            plugin.startVoteProcessing(jobId, jobStatusService, rawVoteCache, voteCache)

            Then("job finishes with full step tree") {
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
            }
            Then("Produces no votes") {
                val actualVotes = voteCache.get(polishSejmTenant).shouldNotBeNull()
                actualVotes.shouldHaveSize(0)
            }
        }
    }

    Given("RawVotes with one malformed and one valid entry") {
        val storage = JobStatusInMemoryStorage()
        val rawVoteCache = RawVoteCacheInMemoryImpl()
        val voteCache = VoteCacheInMemoryImpl()
        val jobStatusService = JobStatusServiceImpl(storage)
        val jobId = jobStatusService.createJobStatus("test", "test")

        And("RawVotes contain one malformed entry and one valid votes_for_party with 2 voters") {
            rawVoteCache.put(
                handledTenant = polishSejmTenant,
                content = RawVote("invalid_format_without_pipe"),
            )
            rawVoteCache.put(
                handledTenant = polishSejmTenant,
                content = RawVote("votes_for_party|party=X&person_to_result=Person1:Za;Person2:Przeciw"),
            )
        }

        When("startVoteProcessing is called") {
            plugin.startVoteProcessing(jobId, jobStatusService, rawVoteCache, voteCache)

            Then("job finishes with full step tree") {
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
            }
            Then("produces 2 votes from valid entries") {
                val actualVotes = voteCache.get(polishSejmTenant).shouldNotBeNull()
                val expectedVotes = listOf(
                    TestFixtures.expectedVote("Person1", "X", resultPolish = "Za"),
                    TestFixtures.expectedVote("Person2", "X", resultPolish = "Przeciw"),
                )
                actualVotes.shouldMatchIgnoringSessionTimestamps(expectedVotes)
            }
        }
    }
})
