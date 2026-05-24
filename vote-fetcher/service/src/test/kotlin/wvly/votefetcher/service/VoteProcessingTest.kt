package wvly.votefetcher.service

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.beEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.should
import storage.inmemory.*
import wvly.jobs.api.*
import wvly.jobs.service.*
import wvly.models.jobs.*
import wvly.models.votes.*
import wvly.votingtenant.pluginapi.DummyPlugin
import wvly.votingtenant.service.VotingTenantPluginRegistryImpl
import java.time.Instant

class VoteProcessingTest : BehaviorSpec({
    val jobStatusStorage = JobStatusInMemoryStorage()
    val jobStatusService = JobStatusServiceImpl(jobStatusStorage)
    val jobStatusViewer = JobStatusViewerImpl(jobStatusStorage)
    val rawVoteCache = RawVoteCacheInMemoryImpl()
    val voteCache = VoteCacheInMemoryImpl()

    Given("vote fetcher and plugin registry") {
        val registry = VotingTenantPluginRegistryImpl()
        val voteFetcher = VoteFetcher(
            registry = registry,
            jobStatusService = jobStatusService,
            rawVoteCache = rawVoteCache,
            voteCache = voteCache,
            votingSessionMetadataCache = VotingSessionMetadataCacheInMemoryImpl(),
            voteStorage = VoteStorageInMemoryImpl(),
        )
        And("no plugins that are registered") {
            When("vote processing is started") {
                val jobId: JobId = voteFetcher.startVoteProcessing()
                Then("it finishes with 0 tenants processed") {
                    jobStatusViewer.getJobStatus(jobId) shouldMatch voteProcessingJob(
                        jobId = jobId,
                        steps = listOf(
                            startedProcessingStep(activeTenants = emptyList()),
                            finishedProcessingStep(successfulTenants = emptyList()),
                        ),
                    )
                }
            }
        }

        And("registered dummy plugin") {
            val dummyPlugin = DummyPlugin()
            registry.register(dummyPlugin)
            When("vote processing is started") {
                val jobId: JobId = voteFetcher.startVoteProcessing()
                Then("it finishes with 0 active tenants processed out of 1 total") {
                    jobStatusViewer.getJobStatus(jobId) shouldMatch voteProcessingJob(
                        jobId = jobId,
                        steps = listOf(
                            startedProcessingStep(
                                activeTenants = emptyList(),
                                disabledTenants = listOf("Dummy Tenant"),
                            ),
                            finishedProcessingStep(successfulTenants = emptyList()),
                        ),
                    )
                }
            }
            And("also activated dummy plugin") {
                registry.activate(DummyPlugin.dummyVotingTenant)
                And("empty cache") {
                    rawVoteCache.reset()
                    When("vote processing is started") {
                        dummyPlugin.reset()
                        val jobId: JobId = voteFetcher.startVoteProcessing()
                        Then("it finishes with 1 active tenants processed") {
                            jobStatusViewer.getJobStatus(jobId) shouldMatch voteProcessingJob(
                                jobId = jobId,
                                steps = listOf(
                                    startedProcessingStep(activeTenants = listOf("Dummy Tenant")),
                                    pluginStepTree(),
                                    finishedProcessingStep(successfulTenants = listOf("Dummy Tenant")),
                                ),
                            )
                        }
                        Then("startVoteProcessing was called once") {
                            dummyPlugin.voteProcessingJobs shouldContainExactly listOf(jobId)
                        }
                        Then("vote cache is populated with expected entries") {
                            voteCache.get(DummyPlugin.dummyVotingTenant) should beEmpty()
                        }
                    }
                }
                And("one entry in cache") {
                    rawVoteCache.reset()
                    rawVoteCache.put(DummyPlugin.dummyVotingTenant, RawVote("e1"))
                    When("vote processing is started") {
                        dummyPlugin.reset()
                        val jobId: JobId = voteFetcher.startVoteProcessing()
                        Then("it finishes with 1 active tenants processed") {
                            jobStatusViewer.getJobStatus(jobId) shouldMatch voteProcessingJob(
                                jobId = jobId,
                                steps = listOf(
                                    startedProcessingStep(activeTenants = listOf("Dummy Tenant")),
                                    pluginStepTree(
                                        finishedStep(
                                            name = "Processing entry [0] with content [e1].",
                                            description = "This step processes entry.",
                                        ),
                                    ),
                                    finishedProcessingStep(successfulTenants = listOf("Dummy Tenant")),
                                ),
                            )
                        }
                        Then("startVoteProcessing was called once") {
                            dummyPlugin.voteProcessingJobs shouldContainExactly listOf(jobId)
                        }
                        Then("vote cache is populated with expected entries") {
                            voteCache.get(DummyPlugin.dummyVotingTenant) shouldContainExactly listOf(
                                Vote(
                                    castBy = Voter(name = "Person[0]"),
                                    castFor = Party(name = "Party[0]"),
                                    castDuring = VotingSession(
                                        identifier = "e1",
                                        heldOn = Instant.ofEpochSecond(0),
                                    ),
                                    result = VoteResult.ABSENT,
                                ),
                            )
                        }
                    }
                }
                And("multiple entries in cache") {
                    rawVoteCache.reset()
                    rawVoteCache.put(DummyPlugin.dummyVotingTenant, RawVote("a1"))
                    rawVoteCache.put(DummyPlugin.dummyVotingTenant, RawVote("b2"))
                    rawVoteCache.put(DummyPlugin.dummyVotingTenant, RawVote("c3"))
                    When("vote processing is started") {
                        dummyPlugin.reset()
                        voteCache.reset()
                        val jobId: JobId = voteFetcher.startVoteProcessing()
                        Then("it finishes with 1 active tenants processed") {
                            jobStatusViewer.getJobStatus(jobId) shouldMatch voteProcessingJob(
                                jobId = jobId,
                                steps = listOf(
                                    startedProcessingStep(activeTenants = listOf("Dummy Tenant")),
                                    pluginStepTree(
                                        finishedStep(
                                            name = "Processing entry [0] with content [a1].",
                                            description = "This step processes entry.",
                                        ),
                                        finishedStep(
                                            name = "Processing entry [1] with content [b2].",
                                            description = "This step processes entry.",
                                        ),
                                        finishedStep(
                                            name = "Processing entry [2] with content [c3].",
                                            description = "This step processes entry.",
                                        ),
                                    ),
                                    finishedProcessingStep(successfulTenants = listOf("Dummy Tenant")),
                                ),
                            )
                        }
                        Then("startVoteProcessing was called once") {
                            dummyPlugin.voteProcessingJobs shouldContainExactly listOf(jobId)
                        }
                        Then("vote cache is populated with expected entries") {
                            voteCache.get(DummyPlugin.dummyVotingTenant) shouldContainExactly listOf(
                                Vote(
                                    castBy = Voter(name = "Person[0]"),
                                    castFor = Party(name = "Party[0]"),
                                    castDuring = VotingSession(
                                        identifier = "a1",
                                        heldOn = Instant.ofEpochSecond(0),
                                    ),
                                    result = VoteResult.ABSENT,
                                ),
                                Vote(
                                    castBy = Voter(name = "Person[1]"),
                                    castFor = Party(name = "Party[1]"),
                                    castDuring = VotingSession(
                                        identifier = "b2",
                                        heldOn = Instant.ofEpochSecond(1),
                                    ),
                                    result = VoteResult.ABSTAINED,
                                ),
                                Vote(
                                    castBy = Voter(name = "Person[2]"),
                                    castFor = Party(name = "Party[2]"),
                                    castDuring = VotingSession(
                                        identifier = "c3",
                                        heldOn = Instant.ofEpochSecond(2),
                                    ),
                                    result = VoteResult.NO,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
})

private fun pluginStepTree(vararg processingSteps: JobStepStatus): JobStepStatus =
    finishedStep(
        name = "Dummy vote processing.",
        description = "This is root step of a startVoteProcessing from Dummy Plugin.",
        childSteps = listOf(
            finishedStep(
                name = "Current raw vote cache count: [${processingSteps.size}].",
                description = "This step mentions raw vote count.",
            ),
            *processingSteps,
            finishedStep(
                name = "Processed all [${processingSteps.size}] entries.",
                description = "This summarises amount of entries processed.",
            ),
        ),
    )

private fun voteProcessingJob(
    jobId: JobId,
    steps: List<JobStepStatus>,
): JobStatus =
    JobStatus(
        id = jobId,
        state = JobState.FINISHED,
        name = "Vote processing job.",
        description = "Job that processes previously fetched raw votes for active Voting Tenants.",
        statusMessage = "Completed successfully",
        startedAt = Instant.now(),
        finishedAt = Instant.now(),
        steps = steps,
    )

private fun startedProcessingStep(
    activeTenants: List<String>,
    disabledTenants: List<String> = emptyList(),
): JobStepStatus =
    finishedStep(
        name = "Starting processing votes for [${activeTenants.size}] active Voting Tenants" +
            " out of [${activeTenants.size + disabledTenants.size}].",
        description = "Processing votes for following Voting Tenants:\n" +
            activeTenants.joinToString { "-$it\n" } +
            "Currently disabled Voting Tenants:\n" +
            disabledTenants.joinToString { "-$it\n" },
    )

private fun finishedProcessingStep(
    successfulTenants: List<String>,
    failedTenants: List<String> = emptyList(),
): JobStepStatus =
    finishedStep(
        name = "Finished processing votes for [${successfulTenants.size + failedTenants.size}] Voting Tenants." +
            " Successful ones: [${successfulTenants.size}]." +
            " Unsuccessful ones: [${failedTenants.size}].",
        description = "Successfully processed votes for following Voting Tenants:\n" +
            successfulTenants.joinToString { "-$it\n" } +
            "Tenants that failed:\n" +
            failedTenants.joinToString { "-$it\n" },
    )
