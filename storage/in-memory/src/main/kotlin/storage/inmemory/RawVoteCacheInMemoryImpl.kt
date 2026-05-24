package storage.inmemory

import wvly.models.tenants.VotingTenant
import wvly.models.votes.RawVote
import wvly.storage.api.cache.RawVoteCache

class RawVoteCacheInMemoryImpl : RawVoteCache {
    private val cache = mutableMapOf<VotingTenant, MutableList<RawVote>>()

    override fun put(
        handledTenant: VotingTenant,
        content: RawVote,
    ) {
        cache.computeIfAbsent(handledTenant) { mutableListOf() }.add(content)
    }

    override fun get(handledTenant: VotingTenant): List<RawVote> = cache[handledTenant].orEmpty()

    override fun reset() {
        cache.clear()
    }
}
