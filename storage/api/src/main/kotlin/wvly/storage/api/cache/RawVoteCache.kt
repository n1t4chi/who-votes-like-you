package wvly.storage.api.cache

import wvly.models.tenants.VotingTenant
import wvly.models.votes.RawVote

interface RawVoteCache {
    fun put(
        handledTenant: VotingTenant,
        content: RawVote,
    )

    fun get(handledTenant: VotingTenant): List<RawVote>

    fun reset()
}
