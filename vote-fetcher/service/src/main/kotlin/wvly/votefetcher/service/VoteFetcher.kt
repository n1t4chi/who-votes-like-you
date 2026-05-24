package wvly.votefetcher.service

import wvly.jobs.api.JobStatusService
import wvly.models.jobs.JobId
import wvly.storage.api.cache.*
import wvly.storage.api.votes.VoteStorage
import wvly.votingtenant.pluginapi.*

class VoteFetcher(
    private val registry: VotingTenantPluginRegistry,
    private val jobStatusService: JobStatusService,
    private val rawVoteCache: RawVoteCache,
    private val voteCache: VoteCache,
    private val votingSessionMetadataCache: VotingSessionMetadataCache,
    private val voteStorage: VoteStorage,
) {
    fun startVoteFetching(): JobId {
        val tenants = registry.getAllVotingTenants()
        val (activeTenants, inactiveTenants) = tenants.partition { it.isActive }

        val jobId = jobStatusService.createJobStatus(
            name = "Vote fetching job.",
            description = "Job that fetches votes from active Voting Tenants.",
        )
        jobStatusService.addStepToJob(
            jobId = jobId,
            name = "Starting fetching votes for [${activeTenants.size}] active Voting Tenants" +
                " out of [${tenants.size}].",
            description = "Fetching votes for following Voting Tenants:\n" +
                activeTenants.joinToString { "-${it.handledTenantName}\n" } +
                "Currently disabled Voting Tenants:\n" +
                inactiveTenants.joinToString { "-${it.handledTenantName}\n" },
        )

        val successfulTenants = mutableListOf<VotingTenantPluginWithMetadata>()
        val unsuccessfulTenants = mutableListOf<VotingTenantPluginWithMetadata>()
        activeTenants.forEach { tenant ->
            try {
                tenant.plugin.startFetch(
                    jobId = jobId,
                    jobStatusService = jobStatusService,
                    cache = rawVoteCache,
                )
                successfulTenants.add(tenant)
            } catch (e: Exception) {
                System.err.println(e.message)
                e.printStackTrace()
                unsuccessfulTenants.add(tenant)
            }
        }

        jobStatusService.addStepToJob(
            jobId = jobId,
            name = "Finished fetching votes for [${activeTenants.size}] Voting Tenants." +
                " Successful ones: [${successfulTenants.size}]." +
                " Unsuccessful ones: [${unsuccessfulTenants.size}].",
            description = "Successfully fetched votes for following Voting Tenants:\n" +
                successfulTenants.joinToString { "-${it.handledTenantName}\n" } +
                "Tenants that failed:\n" +
                unsuccessfulTenants.joinToString { "-${it.handledTenantName}\n" },
        )

        return jobId
    }

    fun startVoteProcessing(): JobId {
        val tenants = registry.getAllVotingTenants()
        val (activeTenants, inactiveTenants) = tenants.partition { it.isActive }

        val jobId = jobStatusService.createJobStatus(
            name = "Vote processing job.",
            description = "Job that processes previously fetched raw votes for active Voting Tenants.",
        )
        jobStatusService.addStepToJob(
            jobId = jobId,
            name = "Starting processing votes for [${activeTenants.size}] active Voting Tenants" +
                " out of [${tenants.size}].",
            description = "Processing votes for following Voting Tenants:\n" +
                activeTenants.joinToString { "-${it.handledTenantName}\n" } +
                "Currently disabled Voting Tenants:\n" +
                inactiveTenants.joinToString { "-${it.handledTenantName}\n" },
        )

        val successfulTenants = mutableListOf<VotingTenantPluginWithMetadata>()
        val unsuccessfulTenants = mutableListOf<VotingTenantPluginWithMetadata>()
        activeTenants.forEach { tenant ->
            try {
                tenant.plugin.startVoteProcessing(
                    jobId = jobId,
                    jobStatusService = jobStatusService,
                    rawVoteCache = rawVoteCache,
                    voteCache = voteCache,
                )
                successfulTenants.add(tenant)
            } catch (e: Exception) {
                System.err.println(e.message)
                e.printStackTrace()
                unsuccessfulTenants.add(tenant)
            }
        }

        jobStatusService.addStepToJob(
            jobId = jobId,
            name = "Finished processing votes for [${activeTenants.size}] Voting Tenants." +
                " Successful ones: [${successfulTenants.size}]." +
                " Unsuccessful ones: [${unsuccessfulTenants.size}].",
            description = "Successfully processed votes for following Voting Tenants:\n" +
                successfulTenants.joinToString { "-${it.handledTenantName}\n" } +
                "Tenants that failed:\n" +
                unsuccessfulTenants.joinToString { "-${it.handledTenantName}\n" },
        )

        return jobId
    }

    fun startVotingSessionMetadataProcessing(): JobId {
        val tenants = registry.getAllVotingTenants()
        val (activeTenants, inactiveTenants) = tenants.partition { it.isActive }

        val jobId = jobStatusService.createJobStatus(
            name = "Voting session metadata processing job.",
            description = "Job that processes previously fetched raw votes into voting session metadata for active Voting Tenants.",
        )
        jobStatusService.addStepToJob(
            jobId = jobId,
            name = "Starting processing voting session metadata for [${activeTenants.size}] active Voting Tenants" +
                " out of [${tenants.size}].",
            description = "Processing voting session metadata for following Voting Tenants:\n" +
                activeTenants.joinToString { "-${it.handledTenantName}\n" } +
                "Currently disabled Voting Tenants:\n" +
                inactiveTenants.joinToString { "-${it.handledTenantName}\n" },
        )

        val successfulTenants = mutableListOf<VotingTenantPluginWithMetadata>()
        val unsuccessfulTenants = mutableListOf<VotingTenantPluginWithMetadata>()
        activeTenants.forEach { tenant ->
            try {
                tenant.plugin.startVotingSessionMetadataProcessing(
                    jobId = jobId,
                    jobStatusService = jobStatusService,
                    rawVoteCache = rawVoteCache,
                    votingSessionMetadataCache = votingSessionMetadataCache,
                )
                successfulTenants.add(tenant)
            } catch (e: Exception) {
                System.err.println(e.message)
                e.printStackTrace()
                unsuccessfulTenants.add(tenant)
            }
        }

        jobStatusService.addStepToJob(
            jobId = jobId,
            name = "Finished processing voting session metadata for [${activeTenants.size}] Voting Tenants." +
                " Successful ones: [${successfulTenants.size}]." +
                " Unsuccessful ones: [${unsuccessfulTenants.size}].",
            description = "Successfully processed voting session metadata for following Voting Tenants:\n" +
                successfulTenants.joinToString { "-${it.handledTenantName}\n" } +
                "Tenants that failed:\n" +
                unsuccessfulTenants.joinToString { "-${it.handledTenantName}\n" },
        )

        return jobId
    }

    fun startVoteImport(): JobId {
        val tenants = registry.getAllVotingTenants()

        val jobId = jobStatusService.createJobStatus(
            name = "Vote import job.",
            description = "Job that imports votes, voters, parties and voting sessions from registered Voting Tenants.",
        )

        jobStatusService.addStepToJob(
            jobId = jobId,
            name = "Starting importing votes for [${tenants.size}] Voting Tenants.",
            description = "Importing votes, voters, parties and voting sessions for following Voting Tenants:\n" +
                tenants.joinToString { "-${it.handledTenantName}\n" },
        )

        tenants.forEach { tenant ->
            val handledTenant = tenant.plugin.handledTenant
            val tenantName = handledTenant.name

            jobStatusService.addStepToJob(
                jobId = jobId,
                name = "Importing votes for [$tenantName] Voting Tenant.",
                description = "Started importing votes, voters, parties and voting sessions for [$tenantName] Voting Tenant.",
            )

            val votes = voteCache.get(handledTenant)
            val statistics = voteStorage.putAll(handledTenant, votes)

            jobStatusService.addStepToJob(
                jobId = jobId,
                name = "Finished importing votes for [$tenantName] Voting Tenant.",
                description = "Successfully imported votes for [$tenantName] Voting Tenant.\n" +
                    "- imported votes: [${statistics.voteCount}]\n" +
                    "- imported voters: [${statistics.voterCount}]\n" +
                    "- imported parties: [${statistics.partyCount}]\n" +
                    "- imported voting sessions: [${statistics.votingSessionCount}]\n",
            )
        }

        jobStatusService.addStepToJob(
            jobId = jobId,
            name = "Finished importing votes for [${tenants.size}] Voting Tenants.",
            description = "Successfully imported votes, voters, parties and voting sessions for following Voting Tenants:\n" +
                tenants.joinToString { "-${it.handledTenantName}\n" },
        )
        return jobId
    }
}
