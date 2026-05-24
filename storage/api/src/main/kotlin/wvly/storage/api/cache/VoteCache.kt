package wvly.storage.api.cache

import wvly.models.tenants.VotingTenant
import wvly.models.votes.Vote

interface VoteCache {
    fun put(
        handledTenant: VotingTenant,
        vote: Vote,
    )

    fun get(handledTenant: VotingTenant): List<Vote>

    fun reset()
}
