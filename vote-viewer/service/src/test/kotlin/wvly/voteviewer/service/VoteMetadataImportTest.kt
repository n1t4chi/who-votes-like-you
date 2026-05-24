package wvly.voteviewer.service

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.*
import io.kotest.matchers.should
import storage.inmemory.*
import wvly.jobs.api.*
import wvly.jobs.service.*
import wvly.models.jobs.*
import wvly.models.votes.VotingSession
import wvly.models.vsmetadata.*
import wvly.storage.api.vsmetadata.TenantVotingSessionMetadata
import wvly.votingtenant.pluginapi.DummyPlugin
import wvly.votingtenant.service.VotingTenantPluginRegistryImpl
import java.time.Instant

class VoteMetadataImportTest : BehaviorSpec({
    val jobStatusStorage = JobStatusInMemoryStorage()
    val jobStatusService = JobStatusServiceImpl(jobStatusStorage)
    val jobStatusViewer = JobStatusViewerImpl(jobStatusStorage)
    val votingSessionMetadataCache = VotingSessionMetadataCacheInMemoryImpl()
    val votingSessionMetadataStorage = VotingSessionMetadataStorageInMemoryImpl()

    Given("metadata import and plugin registry") {
        val registry = VotingTenantPluginRegistryImpl()
        val voteViewer = VoteViewer(
            registry = registry,
            jobStatusService = jobStatusService,
            votingSessionMetadataCache = votingSessionMetadataCache,
            votingSessionMetadataStorage = votingSessionMetadataStorage,
        )
        And("no plugins that are registered") {
            When("metadata import is started") {
                val jobId: JobId = voteViewer.startMetadataImport()
                Then("it finishes with 0 tenants processed") {
                    jobStatusViewer.getJobStatus(jobId) shouldMatch metadataImportJob(
                        jobId = jobId,
                        steps = listOf(
                            startedImportStep(tenants = emptyList()),
                            finishedImportStep(tenants = emptyList()),
                        ),
                    )
                }
                Then("no voting session metadata are in storage") {
                    votingSessionMetadataStorage.getAll() should beEmpty()
                }
            }
        }

        And("registered dummy plugin") {
            val dummyPlugin = DummyPlugin()
            registry.register(dummyPlugin)
            When("metadata import is started") {
                val jobId: JobId = voteViewer.startMetadataImport()
                Then("it finishes with 1 tenants processed") {
                    jobStatusViewer.getJobStatus(jobId) shouldMatch metadataImportJob(
                        jobId = jobId,
                        steps = listOf(
                            startedImportStep(tenants = listOf("Dummy Tenant")),
                            importingTenantStep(tenantName = "Dummy Tenant"),
                            importedTenantSummaryStep(
                                tenantName = "Dummy Tenant",
                                descriptionCount = 0,
                                tagCount = 0,
                            ),
                            finishedImportStep(tenants = listOf("Dummy Tenant")),
                        ),
                    )
                }
                Then("no votes are in storage") {
                    votingSessionMetadataStorage.getAll() shouldContainExactly listOf(
                        TenantVotingSessionMetadata(
                            tenant = DummyPlugin.dummyVotingTenant,
                            descriptions = emptyList(),
                            tags = emptyList(),
                        ),
                    )
                }
            }
            And("saved description and tag in cache") {
                val votingSession = VotingSession("session1", heldOn = Instant.ofEpochSecond(1))
                votingSessionMetadataCache.putDescription(
                    handledTenant = DummyPlugin.dummyVotingTenant,
                    content = VotingSessionDescription(
                        id = dummyDescriptionId,
                        votingSession = votingSession,
                        parent = null,
                        description = "description1",
                        shortDescription = "shortDescription1",
                        source = MetadataSource.VOTING_TENANT_SITE,
                    ),
                )
                votingSessionMetadataCache.putTag(
                    handledTenant = DummyPlugin.dummyVotingTenant,
                    content = VotingSessionTag(
                        votingSession = votingSession,
                        text = "tag1",
                        source = MetadataSource.VOTING_TENANT_SITE,
                    ),
                )
                When("metadata import is started") {
                    val jobId: JobId = voteViewer.startMetadataImport()
                    Then("it finishes with correct statistics") {
                        jobStatusViewer.getJobStatus(jobId) shouldMatch metadataImportJob(
                            jobId = jobId,
                            steps = listOf(
                                startedImportStep(tenants = listOf("Dummy Tenant")),
                                importingTenantStep(tenantName = "Dummy Tenant"),
                                importedTenantSummaryStep(
                                    tenantName = "Dummy Tenant",
                                    descriptionCount = 1,
                                    tagCount = 1,
                                ),
                                finishedImportStep(tenants = listOf("Dummy Tenant")),
                            ),
                        )
                    }
                    Then("correct description and tag are in storage") {
                        votingSessionMetadataStorage.getAll() shouldMatch listOf(
                            TenantVotingSessionMetadata(
                                tenant = dummyPlugin.handledTenant,
                                descriptions = listOf(
                                    VotingSessionDescription(
                                        id = dummyDescriptionId,
                                        votingSession = votingSession,
                                        parent = null,
                                        description = "description1",
                                        shortDescription = "shortDescription1",
                                        source = MetadataSource.VOTING_TENANT_SITE,
                                    ),
                                ),
                                tags = listOf(
                                    VotingSessionTag(
                                        votingSession = votingSession,
                                        text = "tag1",
                                        source = MetadataSource.VOTING_TENANT_SITE,
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

private fun metadataImportJob(
    jobId: JobId,
    steps: List<JobStepStatus>,
): JobStatus =
    JobStatus(
        id = jobId,
        state = JobState.FINISHED,
        name = "Voting Session Metadata import job.",
        description = "Job that imports voting session descriptions and tags from registered Voting Tenants.",
        statusMessage = "Completed successfully",
        startedAt = Instant.now(),
        finishedAt = Instant.now(),
        steps = steps,
    )

private fun startedImportStep(tenants: List<String>): JobStepStatus =
    finishedStep(
        name = "Starting importing voting session metadata for [${tenants.size}] Voting Tenants.",
        description = "Importing voting session descriptions and tags for following Voting Tenants:\n" +
            tenants.joinToString { "-$it\n" },
    )

private fun finishedImportStep(tenants: List<String>): JobStepStatus =
    finishedStep(
        name = "Finished importing voting session metadata for [${tenants.size}] Voting Tenants.",
        description = "Successfully imported voting session descriptions and tags for following Voting Tenants:\n" +
            tenants.joinToString { "-$it\n" },
    )

private fun importingTenantStep(tenantName: String): JobStepStatus =
    finishedStep(
        name = "Importing voting session metadata for [$tenantName] Voting Tenant.",
        description = "Started importing voting session descriptions and tags for [$tenantName] Voting Tenant.",
    )

private fun importedTenantSummaryStep(
    tenantName: String,
    descriptionCount: Int,
    tagCount: Int,
): JobStepStatus =
    finishedStep(
        name = "Finished importing voting session metadata for [$tenantName] Voting Tenant.",
        description = "Successfully imported voting session descriptions and tags for [$tenantName] Voting Tenant.\n" +
            "- imported descriptions: [$descriptionCount]\n" +
            "- imported tags: [$tagCount]\n",
    )
