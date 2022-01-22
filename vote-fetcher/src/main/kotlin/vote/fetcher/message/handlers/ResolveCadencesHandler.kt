package vote.fetcher.message.handlers

import message.system.*
import model.Cadence
import vote.fetcher.message.*
import vote.fetcher.services.AvailableCadenceResolver
import vote.storage.VoteStorage

class ResolveCadencesHandler(
    private val messageSystem: MessageSystem,
    private val availableCadenceResolver: AvailableCadenceResolver,
    val voteStorage: VoteStorage
) : MessageSubscriber<ResolveCadences> {
    
    override fun receive(message: ResolveCadences) {
        val cadences = availableCadenceResolver.getCurrentCadences()
        if (cadences.isNotEmpty()) {
            val lastCadence = cadences.stream().max(compareBy(Cadence::number)).orElseThrow()
            val producedCadences = cadences.mapNotNull { this.mapCadence(it) }
            if (producedCadences.isNotEmpty()) {
                producedCadences.forEach(messageSystem::sendMessage)
            } else {
                messageSystem.sendMessage(CurrentCadence(lastCadence))
            }
        }
    }
    
    private fun mapCadence(cadence: Cadence): ProducedCadence? =
        if (voteStorage.getCadence(cadence.number) != null) {
            null
        } else {
            NewCadence(cadence)
        }
}
