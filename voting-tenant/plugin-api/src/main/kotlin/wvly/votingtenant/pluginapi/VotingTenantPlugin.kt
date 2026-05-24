package wvly.votingtenant.pluginapi

import wvly.jobs.api.JobStatusService
import wvly.models.jobs.*
import wvly.models.tenants.VotingTenant
import wvly.storage.api.cache.*

abstract class VotingTenantPlugin(
    val handledTenant: VotingTenant,
) {
    abstract fun startFetch(
        jobId: JobId,
        jobStatusService: JobStatusService,
        cache: RawVoteCache,
    ): JobStepId

    abstract fun startVoteProcessing(
        jobId: JobId,
        jobStatusService: JobStatusService,
        rawVoteCache: RawVoteCache,
        voteCache: VoteCache,
    ): JobStepId

    abstract fun startVotingSessionMetadataProcessing(
        jobId: JobId,
        jobStatusService: JobStatusService,
        rawVoteCache: RawVoteCache,
        votingSessionMetadataCache: VotingSessionMetadataCache,
    ): JobStepId
}
