package wvly.votingtenant.pluginapi

import wvly.models.tenants.VotingTenant

interface VotingTenantPluginRegistry {
    fun register(plugin: VotingTenantPlugin)

    fun getAllVotingTenants(): List<VotingTenantPluginWithMetadata>

    fun activate(dummyPluginName: VotingTenant)
}

data class VotingTenantPluginWithMetadata(
    val plugin: VotingTenantPlugin,
    val isActive: Boolean,
) {
    val handledTenantName get() = plugin.handledTenant.name
}
