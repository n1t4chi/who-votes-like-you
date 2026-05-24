package vote.fetcher.polishsejm.pipeline.producer

import wvly.jobs.api.JobStatusService
import wvly.models.jobs.JobId
import wvly.models.jobs.JobStepId
import wvly.models.votes.RawVote
import wvly.storage.api.cache.RawVoteCache

/**
 * Producer step that reads all RawVotes from cache for the Polish Sejm tenant (metadata phase).
 */
class VotingMetadataReader {
    fun produce(
        jobId: JobId,
        jobStatusService: JobStatusService,
        parentStepId: JobStepId?,
        rawVoteCache: RawVoteCache,
    ): List<RawVote> = rawVoteCache.get(polishSejmTenant())

    private fun polishSejmTenant() = wvly.models.tenants.VotingTenant("polish-sejm")
}
