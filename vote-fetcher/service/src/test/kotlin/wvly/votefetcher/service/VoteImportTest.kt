package wvly.votefetcher.service

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.*
import io.kotest.matchers.should
import storage.inmemory.*
import wvly.jobs.api.*
import wvly.jobs.service.*
import wvly.models.jobs.*
import wvly.models.votes.*
import wvly.storage.api.votes.TenantVotes
import wvly.votingtenant.pluginapi.DummyPlugin
import wvly.votingtenant.service.VotingTenantPluginRegistryImpl
import java.time.Instant

class VoteImportTest : BehaviorSpec({
    val jobStatusStorage = JobStatusInMemoryStorage()
    val jobStatusService = JobStatusServiceImpl(jobStatusStorage)
    val jobStatusViewer = JobStatusViewerImpl(jobStatusStorage)
    val voteStorage = VoteStorageInMemoryImpl()
    val voteCache = VoteCacheInMemoryImpl()

    Given("vote import and plugin registry") {
        val registry = VotingTenantPluginRegistryImpl()
        val voteFetcher = VoteFetcher(
            registry = registry,
            jobStatusService = jobStatusService,
            rawVoteCache = RawVoteCacheInMemoryImpl(),
            voteCache = voteCache,
            votingSessionMetadataCache = VotingSessionMetadataCacheInMemoryImpl(),
            voteStorage = voteStorage,
        )
        And("no plugins that are registered") {
            When("vote import is started") {
                val jobId: JobId = voteFetcher.startVoteImport()
                Then("it finishes with 0 tenants processed") {
                    jobStatusViewer.getJobStatus(jobId) shouldMatch voteImportJob(
                        jobId = jobId,
                        steps = listOf(
                            startedImportStep(tenants = emptyList()),
                            finishedImportStep(tenants = emptyList()),
                        ),
                    )
                }
                Then("no votes are in storage") {
                    voteStorage.getAll() should beEmpty()
                }
            }
        }

        And("registered dummy plugin") {
            val dummyPlugin = DummyPlugin()
            registry.register(dummyPlugin)
            When("vote import is started") {
                val jobId: JobId = voteFetcher.startVoteImport()
                Then("it finishes with 1 tenants processed") {
                    jobStatusViewer.getJobStatus(jobId) shouldMatch voteImportJob(
                        jobId = jobId,
                        steps = listOf(
                            startedImportStep(tenants = listOf("Dummy Tenant")),
                            importingTenantStep(tenantName = "Dummy Tenant"),
                            importedTenantSummaryStep(
                                tenantName = "Dummy Tenant",
                                voteCount = 0,
                                voterCount = 0,
                                partyCount = 0,
                                votingSessionCount = 0,
                            ),
                            finishedImportStep(tenants = listOf("Dummy Tenant")),
                        ),
                    )
                }
                Then("no votes are in storage") {
                    voteStorage.getAll() shouldContainExactly listOf(
                        TenantVotes(
                            tenant = DummyPlugin.dummyVotingTenant,
                            votes = emptyList(),
                        ),
                    )
                }
            }
            And("saved vote in cache") {
                voteCache.put(
                    DummyPlugin.dummyVotingTenant,
                    Vote(
                        castBy = Voter(name = "Voter1"),
                        castFor = Party(name = "Party1"),
                        castDuring = VotingSession(
                            identifier = "VotingSession1",
                            heldOn = Instant.ofEpochSecond(1),
                        ),
                        result = VoteResult.YES,
                    ),
                )
                When("vote import is started") {
                    val jobId: JobId = voteFetcher.startVoteImport()
                    Then("it finishes with correct statistics") {
                        jobStatusViewer.getJobStatus(jobId) shouldMatch voteImportJob(
                            jobId = jobId,
                            steps = listOf(
                                startedImportStep(tenants = listOf("Dummy Tenant")),
                                importingTenantStep(tenantName = "Dummy Tenant"),
                                importedTenantSummaryStep(
                                    tenantName = "Dummy Tenant",
                                    voteCount = 1,
                                    voterCount = 1,
                                    partyCount = 1,
                                    votingSessionCount = 1,
                                ),
                                finishedImportStep(tenants = listOf("Dummy Tenant")),
                            ),
                        )
                    }
                    Then("correct vote is in storage") {
                        voteStorage.getAll() shouldContainExactly listOf(
                            TenantVotes(
                                tenant = DummyPlugin.dummyVotingTenant,
                                votes = listOf(
                                    Vote(
                                        castBy = Voter(name = "Voter1"),
                                        castFor = Party(name = "Party1"),
                                        castDuring = VotingSession(
                                            identifier = "VotingSession1",
                                            heldOn = Instant.ofEpochSecond(1),
                                        ),
                                        result = VoteResult.YES,
                                    ),
                                ),
                            ),
                        )
                    }
                }
            }
        }
    }
})

private fun voteImportJob(
    jobId: JobId,
    steps: List<JobStepStatus>,
): JobStatus =
    JobStatus(
        id = jobId,
        state = JobState.FINISHED,
        name = "Vote import job.",
        description = "Job that imports votes, voters, parties and voting sessions from registered Voting Tenants.",
        statusMessage = "Completed successfully",
        startedAt = Instant.now(),
        finishedAt = Instant.now(),
        steps = steps,
    )

private fun startedImportStep(tenants: List<String>): JobStepStatus =
    finishedStep(
        name = "Starting importing votes for [${tenants.size}] Voting Tenants.",
        description = "Importing votes, voters, parties and voting sessions for following Voting Tenants:\n" +
            tenants.joinToString { "-$it\n" },
    )

private fun finishedImportStep(tenants: List<String>): JobStepStatus =
    finishedStep(
        name = "Finished importing votes for [${tenants.size}] Voting Tenants.",
        description = "Successfully imported votes, voters, parties and voting sessions for following Voting Tenants:\n" +
            tenants.joinToString { "-$it\n" },
    )

private fun importingTenantStep(tenantName: String): JobStepStatus =
    finishedStep(
        name = "Importing votes for [$tenantName] Voting Tenant.",
        description = "Started importing votes, voters, parties and voting sessions for [$tenantName] Voting Tenant.",
    )

private fun importedTenantSummaryStep(
    tenantName: String,
    voteCount: Int,
    voterCount: Int,
    partyCount: Int,
    votingSessionCount: Int,
): JobStepStatus =
    finishedStep(
        name = "Finished importing votes for [$tenantName] Voting Tenant.",
        description = "Successfully imported votes for [$tenantName] Voting Tenant.\n" +
            "- imported votes: [$voteCount]\n" +
            "- imported voters: [$voterCount]\n" +
            "- imported parties: [$partyCount]\n" +
            "- imported voting sessions: [$votingSessionCount]\n",
    )
