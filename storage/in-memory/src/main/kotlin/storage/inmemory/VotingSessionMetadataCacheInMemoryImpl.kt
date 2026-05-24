package storage.inmemory

import wvly.models.tenants.VotingTenant
import wvly.models.vsmetadata.*
import wvly.storage.api.cache.VotingSessionMetadataCache

class VotingSessionMetadataCacheInMemoryImpl : VotingSessionMetadataCache {
    private val descriptionCache = mutableMapOf<VotingTenant, MutableList<VotingSessionDescription>>()
    private val tagCache = mutableMapOf<VotingTenant, MutableList<VotingSessionTag>>()

    override fun putDescription(
        handledTenant: VotingTenant,
        content: VotingSessionDescription,
    ) {
        descriptionCache.computeIfAbsent(handledTenant) { mutableListOf() }.add(content)
    }

    override fun getDescriptions(handledTenant: VotingTenant): List<VotingSessionDescription> = descriptionCache[handledTenant].orEmpty()

    override fun putTag(
        handledTenant: VotingTenant,
        content: VotingSessionTag,
    ) {
        tagCache.computeIfAbsent(handledTenant) { mutableListOf() }.add(content)
    }

    override fun getTags(handledTenant: VotingTenant): List<VotingSessionTag> = tagCache[handledTenant].orEmpty()

    override fun reset() {
        descriptionCache.clear()
        tagCache.clear()
    }
}
