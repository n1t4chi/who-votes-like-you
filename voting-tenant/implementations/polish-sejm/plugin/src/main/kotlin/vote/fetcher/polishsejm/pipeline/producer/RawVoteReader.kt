package vote.fetcher.polishsejm.pipeline.producer

import wvly.models.tenants.VotingTenant
import wvly.models.votes.RawVote
import wvly.storage.api.cache.RawVoteCache

/**
 * Producer step that reads all RawVotes from cache for the Polish Sejm tenant.
 */
class RawVoteReader {
    fun produce(rawVoteCache: RawVoteCache): List<RawVote> = rawVoteCache.get(polishSejmTenant())

    private fun polishSejmTenant() = VotingTenant("polish-sejm")
}
