package storage.inmemory

import wvly.models.tenants.VotingTenant
import wvly.models.vsmetadata.*
import wvly.storage.api.vsmetadata.*

class VotingSessionMetadataStorageInMemoryImpl : VotingSessionMetadataStorage {
    private val descriptionCache = mutableMapOf<VotingTenant, List<VotingSessionDescription>>()
    private val tagCache = mutableMapOf<VotingTenant, List<VotingSessionTag>>()

    override fun getAll(): List<TenantVotingSessionMetadata> {
        val tenants = descriptionCache.keys.plus(tagCache.keys)

        return tenants.map { tenant ->
            TenantVotingSessionMetadata(
                tenant = tenant,
                descriptions = descriptionCache.getOrElse(tenant) { emptyList() },
                tags = tagCache.getOrElse(tenant) { emptyList() },
            )
        }
    }

    override fun putAll(
        tenant: VotingTenant,
        descriptions: List<VotingSessionDescription>,
        tags: List<VotingSessionTag>,
    ): VotingSessionMetadataStatistics {
        descriptionCache[tenant] = descriptions
        tagCache[tenant] = tags
        return VotingSessionMetadataStatistics(
            descriptionCount = descriptions.size,
            tagCount = tags.size,
        )
    }
}
