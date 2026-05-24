package wvly.storage.api.votes

import wvly.models.tenants.VotingTenant
import wvly.models.votes.Vote

data class TenantVotes(
    val tenant: VotingTenant,
    val votes: List<Vote>,
)
