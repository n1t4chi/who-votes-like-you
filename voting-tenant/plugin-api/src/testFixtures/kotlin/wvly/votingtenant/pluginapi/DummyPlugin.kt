package wvly.votingtenant.pluginapi

import wvly.jobs.api.JobStatusService
import wvly.models.jobs.*
import wvly.models.tenants.VotingTenant
import wvly.models.votes.*
import wvly.models.vsmetadata.*
import wvly.storage.api.cache.*
import java.time.Instant
import java.util.*

class DummyPlugin : VotingTenantPlugin(handledTenant = dummyVotingTenant) {
    companion object {
        val dummyVotingTenant = VotingTenant("Dummy Tenant")
    }

    val fetchJobs = mutableListOf<JobId>()
    val voteProcessingJobs = mutableListOf<JobId>()
    val votingSessionMetadataProcessingJobs = mutableListOf<JobId>()

    override fun startFetch(
        jobId: JobId,
        jobStatusService: JobStatusService,
        cache: RawVoteCache,
    ): JobStepId {
        fetchJobs.add(jobId)
        val stepId = jobStatusService.addStepToJob(
            jobId = jobId,
            name = "Dummy fetch.",
            description = "This is root step of a startFetch from Dummy Plugin.",
        )
        jobStatusService.addStepToStep(
            stepId = stepId,
            name = "Adding first entry into cache.",
            description = "This step adds first entry.",
        )
        cache.put(handledTenant, RawVote("cache entry 1"))
        jobStatusService.addStepToStep(
            stepId = stepId,
            name = "Adding second entry into cache.",
            description = "This step adds second entry.",
        )
        cache.put(handledTenant, RawVote("cache entry 2"))
        return stepId
    }

    override fun startVoteProcessing(
        jobId: JobId,
        jobStatusService: JobStatusService,
        rawVoteCache: RawVoteCache,
        voteCache: VoteCache,
    ): JobStepId {
        voteProcessingJobs.add(jobId)
        val stepId = jobStatusService.addStepToJob(
            jobId = jobId,
            name = "Dummy vote processing.",
            description = "This is root step of a startVoteProcessing from Dummy Plugin.",
        )
        val rawVoteCacheEntries = rawVoteCache.get(handledTenant)
        jobStatusService.addStepToStep(
            stepId = stepId,
            name = "Current raw vote cache count: [${rawVoteCacheEntries.size}].",
            description = "This step mentions raw vote count.",
        )
        val voteResultEntries = VoteResult.entries.sortedBy { it.name }
        rawVoteCacheEntries.forEachIndexed { index, entry ->
            jobStatusService.addStepToStep(
                stepId = stepId,
                name = "Processing entry [$index] with content [${entry.content}].",
                description = "This step processes entry.",
            )
            voteCache.put(
                handledTenant = handledTenant,
                vote = Vote(
                    castBy = Voter(name = "Person[$index]"),
                    castFor = Party(name = "Party[$index]"),
                    castDuring = votingSession(entry, index),
                    result = voteResultEntries[index % voteResultEntries.size],
                ),
            )
        }
        jobStatusService.addStepToStep(
            stepId = stepId,
            name = "Processed all [${rawVoteCacheEntries.size}] entries.",
            description = "This summarises amount of entries processed.",
        )
        return stepId
    }

    override fun startVotingSessionMetadataProcessing(
        jobId: JobId,
        jobStatusService: JobStatusService,
        rawVoteCache: RawVoteCache,
        votingSessionMetadataCache: VotingSessionMetadataCache,
    ): JobStepId {
        votingSessionMetadataProcessingJobs.add(jobId)
        val stepId = jobStatusService.addStepToJob(
            jobId,
            name = "Dummy voting session metadata processing.",
            description = "This is root step of a startVotingSessionMetadataProcessing from Dummy Plugin.",
        )
        val rawVoteCacheEntries = rawVoteCache.get(handledTenant)
        jobStatusService.addStepToStep(
            stepId,
            name = "Current raw vote cache count: [${rawVoteCacheEntries.size}].",
            description = "This step mentions raw vote count.",
        )
        rawVoteCacheEntries.forEachIndexed { index, entry ->
            jobStatusService.addStepToStep(
                stepId,
                name = "Processing entry [$index] with content [${entry.content}].",
                description = "This step processes entry.",
            )
            val votingSession = votingSession(entry, index)
            votingSessionMetadataCache.putDescription(
                handledTenant = handledTenant,
                content = VotingSessionDescription(
                    id = VotingSessionDescriptionId(UUID.randomUUID()),
                    votingSession = votingSession,
                    parent = null,
                    description = "description[$index][${entry.content}]",
                    shortDescription = "shortDescription[$index]",
                    source = MetadataSource.VOTING_TENANT_SITE,
                ),
            )
            votingSessionMetadataCache.putTag(
                handledTenant = handledTenant,
                content = VotingSessionTag(
                    votingSession = votingSession,
                    text = "tag$index",
                    source = MetadataSource.VOTING_TENANT_SITE,
                ),
            )
        }
        jobStatusService.addStepToStep(
            stepId,
            name = "Processed all [${rawVoteCacheEntries.size}] entries.",
            description = "This summarises amount of entries processed.",
        )
        return stepId
    }

    fun reset() {
        fetchJobs.clear()
        voteProcessingJobs.clear()
        votingSessionMetadataProcessingJobs.clear()
    }

    private fun votingSession(
        entry: RawVote,
        index: Int,
    ): VotingSession =
        VotingSession(
            identifier = entry.content,
            heldOn = Instant.ofEpochSecond(index.toLong()),
        )
}
