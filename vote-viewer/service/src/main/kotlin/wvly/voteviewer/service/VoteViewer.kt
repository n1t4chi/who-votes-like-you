package wvly.voteviewer.service

import storage.inmemory.VotingSessionMetadataStorageInMemoryImpl
import wvly.jobs.api.JobStatusService
import wvly.models.jobs.*
import wvly.storage.api.cache.*
import wvly.votingtenant.pluginapi.VotingTenantPluginRegistry

class VoteViewer(
    private val registry: VotingTenantPluginRegistry,
    private val jobStatusService: JobStatusService,
    private val votingSessionMetadataCache: VotingSessionMetadataCache,
    private val votingSessionMetadataStorage: VotingSessionMetadataStorageInMemoryImpl,
) {
    fun startMetadataImport(): JobId {
        val tenants = registry.getAllVotingTenants()

        val jobId = jobStatusService.createJobStatus(
            name = "Voting Session Metadata import job.",
            description = "Job that imports voting session descriptions and tags from registered Voting Tenants.",
        )

        jobStatusService.addStepToJob(
            jobId = jobId,
            name = "Starting importing voting session metadata for [${tenants.size}] Voting Tenants.",
            description = "Importing voting session descriptions and tags for following Voting Tenants:\n" +
                tenants.joinToString { "-${it.handledTenantName}\n" },
        )

        tenants.forEach { tenant ->
            val handledTenant = tenant.plugin.handledTenant
            val tenantName = handledTenant.name

            jobStatusService.addStepToJob(
                jobId = jobId,
                name = "Importing voting session metadata for [$tenantName] Voting Tenant.",
                description = "Started importing voting session descriptions and tags for [$tenantName] Voting Tenant.",
            )

            val descriptions = votingSessionMetadataCache.getDescriptions(handledTenant)
            val tags = votingSessionMetadataCache.getTags(handledTenant)
            val statistics = votingSessionMetadataStorage.putAll(
                tenant = handledTenant,
                descriptions = descriptions,
                tags = tags,
            )

            jobStatusService.addStepToJob(
                jobId = jobId,
                name = "Finished importing voting session metadata for [$tenantName] Voting Tenant.",
                description = "Successfully imported voting session descriptions and tags for [$tenantName] Voting Tenant.\n" +
                    "- imported descriptions: [${statistics.descriptionCount}]\n" +
                    "- imported tags: [${statistics.tagCount}]\n",
            )
        }

        jobStatusService.addStepToJob(
            jobId = jobId,
            name = "Finished importing voting session metadata for [${tenants.size}] Voting Tenants.",
            description = "Successfully imported voting session descriptions and tags for following Voting Tenants:\n" +
                tenants.joinToString { "-${it.handledTenantName}\n" },
        )
        return jobId
    }
}
