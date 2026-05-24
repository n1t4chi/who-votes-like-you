package storage.inmemory

import wvly.models.tenants.VotingTenant
import wvly.models.votes.Vote
import wvly.storage.api.cache.VoteCache

class VoteCacheInMemoryImpl : VoteCache {
    private val cache = mutableMapOf<VotingTenant, MutableList<Vote>>()

    override fun put(
        handledTenant: VotingTenant,
        vote: Vote,
    ) {
        cache.computeIfAbsent(handledTenant) { mutableListOf() }.add(vote)
    }

    override fun get(handledTenant: VotingTenant): List<Vote> = cache[handledTenant].orEmpty()

    override fun reset() {
        cache.clear()
    }
}
