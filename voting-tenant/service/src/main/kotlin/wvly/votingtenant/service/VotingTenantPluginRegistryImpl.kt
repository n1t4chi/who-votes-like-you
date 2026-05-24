package wvly.votingtenant.service

import wvly.models.tenants.VotingTenant
import wvly.votingtenant.pluginapi.VotingTenantPlugin
import wvly.votingtenant.pluginapi.VotingTenantPluginRegistry
import wvly.votingtenant.pluginapi.VotingTenantPluginWithMetadata

class VotingTenantPluginRegistryImpl : VotingTenantPluginRegistry {
    private val plugins: MutableMap<VotingTenant, VotingTenantPluginWithMetadata> = mutableMapOf()

    override fun register(plugin: VotingTenantPlugin) {
        if (plugins.containsKey(plugin.handledTenant)) {
            throw IllegalStateException("Plugin [${plugin.handledTenant.name}] is already registered")
        }
        plugins[plugin.handledTenant] = VotingTenantPluginWithMetadata(
            plugin = plugin,
            isActive = false,
        )
    }

    override fun getAllVotingTenants(): List<VotingTenantPluginWithMetadata> = plugins.values.toList()

    override fun activate(dummyPluginName: VotingTenant) {
        val plugin = plugins[dummyPluginName]
            ?: throw IllegalStateException("Plugin [${dummyPluginName.name}] was not registered before")
        if (plugin.isActive) {
            throw IllegalStateException("Plugin [${dummyPluginName.name}] is already active")
        }
        plugins[dummyPluginName] = VotingTenantPluginWithMetadata(
            plugin = plugin.plugin,
            isActive = true,
        )
    }
}
