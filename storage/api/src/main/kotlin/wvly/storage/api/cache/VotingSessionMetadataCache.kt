package wvly.storage.api.cache

import wvly.models.tenants.VotingTenant
import wvly.models.vsmetadata.*

interface VotingSessionMetadataCache {
    fun putDescription(
        handledTenant: VotingTenant,
        content: VotingSessionDescription,
    )

    fun getDescriptions(handledTenant: VotingTenant): List<VotingSessionDescription>

    fun putTag(
        handledTenant: VotingTenant,
        content: VotingSessionTag,
    )

    fun getTags(handledTenant: VotingTenant): List<VotingSessionTag>

    fun reset()
}
