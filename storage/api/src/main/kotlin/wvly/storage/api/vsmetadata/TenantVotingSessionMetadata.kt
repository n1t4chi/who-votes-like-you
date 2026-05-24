package wvly.storage.api.vsmetadata

import wvly.models.tenants.VotingTenant
import wvly.models.vsmetadata.*

data class TenantVotingSessionMetadata(
    val tenant: VotingTenant,
    val descriptions: List<VotingSessionDescription>,
    val tags: List<VotingSessionTag>,
)
