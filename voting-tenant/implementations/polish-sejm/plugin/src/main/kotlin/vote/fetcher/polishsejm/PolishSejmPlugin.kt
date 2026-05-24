package vote.fetcher.polishsejm

import vote.fetcher.polishsejm.client.apis.DefaultApi
import vote.fetcher.polishsejm.client.apis.VotingsApi
import vote.fetcher.polishsejm.pipeline.PipelineOrchestrator
import wvly.jobs.api.JobStatusService
import wvly.models.jobs.JobId
import wvly.models.jobs.JobStepId
import wvly.models.tenants.VotingTenant
import wvly.storage.api.cache.RawVoteCache
import wvly.storage.api.cache.VoteCache
import wvly.storage.api.cache.VotingSessionMetadataCache
import wvly.votingtenant.pluginapi.VotingTenantPlugin

val polishSejmTenant = VotingTenant("polish-sejm")

class PolishSejmPlugin(
    private val votingsApi: VotingsApi = VotingsApi(),
    private val defaultApi: DefaultApi = DefaultApi(),
) : VotingTenantPlugin(polishSejmTenant) {
    override fun startFetch(
        jobId: JobId,
        jobStatusService: JobStatusService,
        cache: RawVoteCache,
    ): JobStepId = orchestrator.executeFetchPipeline(jobId, jobStatusService, cache)

    override fun startVoteProcessing(
        jobId: JobId,
        jobStatusService: JobStatusService,
        rawVoteCache: RawVoteCache,
        voteCache: VoteCache,
    ): JobStepId = orchestrator.executeProcessingPipeline(jobId, jobStatusService, rawVoteCache, voteCache)

    override fun startVotingSessionMetadataProcessing(
        jobId: JobId,
        jobStatusService: JobStatusService,
        rawVoteCache: RawVoteCache,
        votingSessionMetadataCache: VotingSessionMetadataCache,
    ): JobStepId = orchestrator.executeMetadataPipeline(jobId, jobStatusService, rawVoteCache, votingSessionMetadataCache)

    private val orchestrator by lazy {
        PipelineOrchestrator(votingsApi, defaultApi)
    }
}
