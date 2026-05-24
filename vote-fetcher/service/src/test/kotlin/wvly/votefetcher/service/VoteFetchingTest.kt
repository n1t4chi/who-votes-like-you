package wvly.votefetcher.service

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import storage.inmemory.*
import wvly.jobs.api.*
import wvly.jobs.service.*
import wvly.models.jobs.*
import wvly.models.votes.RawVote
import wvly.votingtenant.pluginapi.DummyPlugin
import wvly.votingtenant.service.VotingTenantPluginRegistryImpl
import java.time.Instant

class VoteFetchingTest : BehaviorSpec({
    val jobStatusStorage = JobStatusInMemoryStorage()
    val jobStatusService = JobStatusServiceImpl(jobStatusStorage)
    val jobStatusViewer = JobStatusViewerImpl(jobStatusStorage)
    val rawVoteCache = RawVoteCacheInMemoryImpl()

    Given("vote fetcher and plugin registry") {
        val registry = VotingTenantPluginRegistryImpl()
        val voteFetcher = VoteFetcher(
            registry = registry,
            jobStatusService = jobStatusService,
            rawVoteCache = rawVoteCache,
            voteCache = VoteCacheInMemoryImpl(),
            votingSessionMetadataCache = VotingSessionMetadataCacheInMemoryImpl(),
            voteStorage = VoteStorageInMemoryImpl(),
        )
        And("no plugins that are registered") {
            When("vote fetching is started") {
                val jobId: JobId = voteFetcher.startVoteFetching()
                Then("it finishes with 0 tenants processed") {
                    jobStatusViewer.getJobStatus(jobId) shouldMatch voteFetchingJob(
                        jobId = jobId,
                        steps = listOf(
                            startedFetchingStep(activeTenants = emptyList()),
                            finishedFetchingStep(successfulTenants = emptyList()),
                        ),
                    )
                }
            }
        }

        And("registered dummy plugin") {
            val dummyPlugin = DummyPlugin()
            registry.register(dummyPlugin)
            When("vote fetching is started") {
                val jobId: JobId = voteFetcher.startVoteFetching()
                Then("it finishes with 0 active tenants processed out of 1 total") {
                    jobStatusViewer.getJobStatus(jobId) shouldMatch voteFetchingJob(
                        jobId = jobId,
                        steps = listOf(
                            startedFetchingStep(activeTenants = emptyList(), disabledTenants = listOf("Dummy Tenant")),
                            finishedFetchingStep(successfulTenants = emptyList()),
                        ),
                    )
                }
            }
            And("also activated dummy plugin") {
                registry.activate(DummyPlugin.dummyVotingTenant)
                When("vote fetching is started") {
                    val jobId: JobId = voteFetcher.startVoteFetching()
                    Then("it finishes with 1 active tenants processed") {
                        jobStatusViewer.getJobStatus(jobId) shouldMatch voteFetchingJob(
                            jobId = jobId,
                            steps = listOf(
                                startedFetchingStep(activeTenants = listOf("Dummy Tenant")),
                                finishedStep(
                                    name = "Dummy fetch.",
                                    description = "This is root step of a startFetch from Dummy Plugin.",
                                    childSteps = listOf(
                                        finishedStep(
                                            name = "Adding first entry into cache.",
                                            description = "This step adds first entry.",
                                        ),
                                        finishedStep(
                                            name = "Adding second entry into cache.",
                                            description = "This step adds second entry.",
                                        ),
                                    ),
                                ),
                                finishedFetchingStep(successfulTenants = listOf("Dummy Tenant")),
                            ),
                        )
                    }
                    Then("startFetch was called once") {
                        dummyPlugin.fetchJobs shouldContainExactly listOf(jobId)
                    }
                    Then("cache is populated with expected entries") {
                        rawVoteCache.get(DummyPlugin.dummyVotingTenant) shouldContainExactly listOf(
                            RawVote("cache entry 1"),
                            RawVote("cache entry 2"),
                        )
                    }
                }
            }
        }
    }
})

private fun voteFetchingJob(
    jobId: JobId,
    steps: List<JobStepStatus>,
): JobStatus =
    JobStatus(
        id = jobId,
        state = JobState.FINISHED,
        name = "Vote fetching job.",
        description = "Job that fetches votes from active Voting Tenants.",
        statusMessage = "Completed successfully",
        startedAt = Instant.now(),
        finishedAt = Instant.now(),
        steps = steps,
    )

private fun startedFetchingStep(
    activeTenants: List<String>,
    disabledTenants: List<String> = emptyList(),
): JobStepStatus =
    finishedStep(
        name = "Starting fetching votes for [${activeTenants.size}] active Voting Tenants" +
            " out of [${activeTenants.size + disabledTenants.size}].",
        description = "Fetching votes for following Voting Tenants:\n" +
            activeTenants.joinToString { "-$it\n" } +
            "Currently disabled Voting Tenants:\n" +
            disabledTenants.joinToString { "-$it\n" },
    )

private fun finishedFetchingStep(
    successfulTenants: List<String>,
    failedTenants: List<String> = emptyList(),
): JobStepStatus =
    finishedStep(
        name = "Finished fetching votes for [${successfulTenants.size + failedTenants.size}] Voting Tenants." +
            " Successful ones: [${successfulTenants.size}]." +
            " Unsuccessful ones: [${failedTenants.size}].",
        description = "Successfully fetched votes for following Voting Tenants:\n" +
            successfulTenants.joinToString { "-$it\n" } +
            "Tenants that failed:\n" +
            failedTenants.joinToString { "-$it\n" },
    )
