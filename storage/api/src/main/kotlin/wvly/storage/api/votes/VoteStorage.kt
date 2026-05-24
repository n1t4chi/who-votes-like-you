package wvly.storage.api.votes

import wvly.models.tenants.VotingTenant
import wvly.models.votes.Vote

interface VoteStorage {
    fun getAll(): List<TenantVotes>

    fun putAll(
        tenant: VotingTenant,
        votes: List<Vote>,
    ): VoteInsertStatistics
}
