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
import wvly.models.vsmetadata.*
import wvly.votingtenant.pluginapi.DummyPlugin
import wvly.votingtenant.service.VotingTenantPluginRegistryImpl
import java.time.Instant

class VotingSessionMetadataProcessingTest : BehaviorSpec({
    val jobStatusStorage = JobStatusInMemoryStorage()
    val jobStatusService = JobStatusServiceImpl(jobStatusStorage)
    val jobStatusViewer = JobStatusViewerImpl(jobStatusStorage)
    val rawVoteCache = RawVoteCacheInMemoryImpl()
    val votingSessionMetadataCache = VotingSessionMetadataCacheInMemoryImpl()

    Given("vote fetcher and plugin registry") {
        val registry = VotingTenantPluginRegistryImpl()
        val voteFetcher = VoteFetcher(
            registry = registry,
            jobStatusService = jobStatusService,
            rawVoteCache = rawVoteCache,
            voteCache = VoteCacheInMemoryImpl(),
            votingSessionMetadataCache = votingSessionMetadataCache,
            voteStorage = VoteStorageInMemoryImpl(),
        )
        And("no plugins that are registered") {
            When("voting session metadata processing is started") {
                val jobId: JobId = voteFetcher.startVotingSessionMetadataProcessing()
                Then("it finishes with 0 tenants processed") {
                    jobStatusViewer.getJobStatus(jobId) shouldMatch votingSessionMetadataProcessingJob(
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
                val jobId: JobId = voteFetcher.startVotingSessionMetadataProcessing()
                Then("it finishes with 0 active tenants processed out of 1 total") {
                    jobStatusViewer.getJobStatus(jobId) shouldMatch votingSessionMetadataProcessingJob(
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
                        val jobId: JobId = voteFetcher.startVotingSessionMetadataProcessing()
                        Then("it finishes with 1 active tenants processed") {
                            jobStatusViewer.getJobStatus(jobId) shouldMatch votingSessionMetadataProcessingJob(
                                jobId = jobId,
                                steps = listOf(
                                    startedProcessingStep(activeTenants = listOf("Dummy Tenant")),
                                    pluginStepTree(),
                                    finishedProcessingStep(successfulTenants = listOf("Dummy Tenant")),
                                ),
                            )
                        }
                        Then("startVotingSessionMetadataProcessing was called once") {
                            dummyPlugin.votingSessionMetadataProcessingJobs shouldContainExactly listOf(jobId)
                        }
                        Then("vote cache is populated with expected entries") {
                            votingSessionMetadataCache.getTags(DummyPlugin.dummyVotingTenant) should beEmpty()
                            votingSessionMetadataCache.getDescriptions(DummyPlugin.dummyVotingTenant) should beEmpty()
                        }
                    }
                }
                And("one entry in cache") {
                    rawVoteCache.reset()
                    rawVoteCache.put(DummyPlugin.dummyVotingTenant, RawVote("e1"))
                    When("vote processing is started") {
                        dummyPlugin.reset()
                        val jobId: JobId = voteFetcher.startVotingSessionMetadataProcessing()
                        Then("it finishes with 1 active tenants processed") {
                            jobStatusViewer.getJobStatus(jobId) shouldMatch votingSessionMetadataProcessingJob(
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
                        Then("startVotingSessionMetadataProcessing was called once") {
                            dummyPlugin.votingSessionMetadataProcessingJobs shouldContainExactly listOf(jobId)
                        }
                        Then("vote cache is populated with expected entries") {
                            val votingSession = VotingSession(
                                identifier = "e1",
                                heldOn = Instant.ofEpochSecond(0),
                            )
                            votingSessionMetadataCache.getTags(DummyPlugin.dummyVotingTenant) shouldContainExactly listOf(
                                VotingSessionTag(
                                    votingSession = votingSession,
                                    text = "tag0",
                                    source = MetadataSource.VOTING_TENANT_SITE,
                                ),
                            )
                            votingSessionMetadataCache.getDescriptions(DummyPlugin.dummyVotingTenant) shouldMatch listOf(
                                VotingSessionDescription(
                                    id = dummyDescriptionId,
                                    votingSession = votingSession,
                                    parent = null,
                                    description = "description[0][e1]",
                                    shortDescription = "shortDescription[0]",
                                    source = MetadataSource.VOTING_TENANT_SITE,
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
                        votingSessionMetadataCache.reset()
                        val jobId: JobId = voteFetcher.startVotingSessionMetadataProcessing()
                        Then("it finishes with 1 active tenants processed") {
                            jobStatusViewer.getJobStatus(jobId) shouldMatch votingSessionMetadataProcessingJob(
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
                        Then("startVotingSessionMetadataProcessing was called once") {
                            dummyPlugin.votingSessionMetadataProcessingJobs shouldContainExactly listOf(jobId)
                        }
                        Then("vote cache is populated with expected entries") {
                            val votingSession1 = VotingSession(
                                identifier = "a1",
                                heldOn = Instant.ofEpochSecond(0),
                            )
                            val votingSession2 = VotingSession(
                                identifier = "b2",
                                heldOn = Instant.ofEpochSecond(1),
                            )
                            val votingSession3 = VotingSession(
                                identifier = "c3",
                                heldOn = Instant.ofEpochSecond(2),
                            )
                            votingSessionMetadataCache.getTags(DummyPlugin.dummyVotingTenant) shouldContainExactly listOf(
                                VotingSessionTag(
                                    votingSession = votingSession1,
                                    text = "tag0",
                                    source = MetadataSource.VOTING_TENANT_SITE,
                                ),
                                VotingSessionTag(
                                    votingSession = votingSession2,
                                    text = "tag1",
                                    source = MetadataSource.VOTING_TENANT_SITE,
                                ),
                                VotingSessionTag(
                                    votingSession = votingSession3,
                                    text = "tag2",
                                    source = MetadataSource.VOTING_TENANT_SITE,
                                ),
                            )
                            votingSessionMetadataCache.getDescriptions(DummyPlugin.dummyVotingTenant) shouldMatch listOf(
                                VotingSessionDescription(
                                    id = dummyDescriptionId,
                                    votingSession = votingSession1,
                                    parent = null,
                                    description = "description[0][a1]",
                                    shortDescription = "shortDescription[0]",
                                    source = MetadataSource.VOTING_TENANT_SITE,
                                ),
                                VotingSessionDescription(
                                    id = dummyDescriptionId,
                                    votingSession = votingSession2,
                                    parent = null,
                                    description = "description[1][b2]",
                                    shortDescription = "shortDescription[1]",
                                    source = MetadataSource.VOTING_TENANT_SITE,
                                ),
                                VotingSessionDescription(
                                    id = dummyDescriptionId,
                                    votingSession = votingSession3,
                                    parent = null,
                                    description = "description[2][c3]",
                                    shortDescription = "shortDescription[2]",
                                    source = MetadataSource.VOTING_TENANT_SITE,
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
        name = "Dummy voting session metadata processing.",
        description = "This is root step of a startVotingSessionMetadataProcessing from Dummy Plugin.",
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

private fun votingSessionMetadataProcessingJob(
    jobId: JobId,
    steps: List<JobStepStatus>,
): JobStatus =
    JobStatus(
        id = jobId,
        state = JobState.FINISHED,
        name = "Voting session metadata processing job.",
        description = "Job that processes previously fetched raw votes into voting session metadata for active Voting Tenants.",
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
        name = "Starting processing voting session metadata for [${activeTenants.size}] active Voting Tenants" +
            " out of [${activeTenants.size + disabledTenants.size}].",
        description = "Processing voting session metadata for following Voting Tenants:\n" +
            activeTenants.joinToString { "-$it\n" } +
            "Currently disabled Voting Tenants:\n" +
            disabledTenants.joinToString { "-$it\n" },
    )

private fun finishedProcessingStep(
    successfulTenants: List<String>,
    failedTenants: List<String> = emptyList(),
): JobStepStatus =
    finishedStep(
        name = "Finished processing voting session metadata for [${successfulTenants.size + failedTenants.size}] Voting Tenants." +
            " Successful ones: [${successfulTenants.size}]." +
            " Unsuccessful ones: [${failedTenants.size}].",
        description = "Successfully processed voting session metadata for following Voting Tenants:\n" +
            successfulTenants.joinToString { "-$it\n" } +
            "Tenants that failed:\n" +
            failedTenants.joinToString { "-$it\n" },
    )
