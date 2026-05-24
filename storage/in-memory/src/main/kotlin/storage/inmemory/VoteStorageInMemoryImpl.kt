package storage.inmemory

import wvly.models.tenants.VotingTenant
import wvly.models.votes.Vote
import wvly.storage.api.votes.*

open class VoteStorageInMemoryImpl : VoteStorage {
    private val cache = mutableMapOf<VotingTenant, List<Vote>>()

    override fun getAll(): List<TenantVotes> =
        cache.map {
            TenantVotes(
                tenant = it.key,
                votes = it.value,
            )
        }

    override fun putAll(
        tenant: VotingTenant,
        votes: List<Vote>,
    ): VoteInsertStatistics {
        cache[tenant] = votes
        return VoteInsertStatistics(
            voteCount = votes.size,
            voterCount = votes.map { it.castBy }.distinct().size,
            partyCount = votes.map { it.castFor }.distinct().size,
            votingSessionCount = votes.map { it.castDuring }.distinct().size,
        )
    }
}
