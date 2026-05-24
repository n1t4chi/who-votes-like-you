package wvly.storage.api.vsmetadata

import wvly.models.tenants.VotingTenant
import wvly.models.vsmetadata.*

interface VotingSessionMetadataStorage {
    fun getAll(): List<TenantVotingSessionMetadata>

    fun putAll(
        tenant: VotingTenant,
        descriptions: List<VotingSessionDescription>,
        tags: List<VotingSessionTag>,
    ): VotingSessionMetadataStatistics
}
