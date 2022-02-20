package vote.fetcher.message.handlers

import message.subscriber.RestartableMessageSubscriber
import message.system.MessageSystem
import model.Cadence
import model.CadenceStatus
import vote.fetcher.message.CurrentCadence
import vote.fetcher.message.NewCadence
import vote.fetcher.message.ProducedCadence
import vote.fetcher.message.ResolveCadences
import vote.fetcher.services.AvailableCadenceResolver
import vote.storage.VoteStorage

class ResolveCadencesHandler(
    messageSystem: MessageSystem,
    private val availableCadenceResolver: AvailableCadenceResolver,
    private val voteStorage: VoteStorage
) : RestartableMessageSubscriber<ResolveCadences>("ResolveCadencesHandler", messageSystem) {
    
    override fun receiveUnsafe(message: ResolveCadences) {
        val cadences = availableCadenceResolver.getCurrentCadences()
        logger.trace { "cadences to resolve:\n${cadences.joinToString("\n")}" }
        if (cadences.isNotEmpty()) {
            val lastCadence = getLastCadence(cadences)
            logger.debug { "Last cadence: $lastCadence" }
            val producedCadences = cadences.mapNotNull { this.mapCadence(it, lastCadence) }
            logger.debug { "Mapped cadences:\n${producedCadences.joinToString("\n")}" }
            if (producedCadences.isNotEmpty()) {
                handleProducedCadences(producedCadences, lastCadence)
            } else {
                handleOnlyActiveCadences(lastCadence)
            }
        }
    }
    
    private fun handleOnlyActiveCadences(lastCadence: Cadence) {
        handlePreviouslyActiveCadences(lastCadence)
        handleLastActiveCadence(lastCadence)
    }
    
    private fun handlePreviouslyActiveCadences(lastCadence: Cadence) {
        val previouslyActiveCadences = voteStorage.cadences
            .filter(Cadence::isActive)
            .filter { !lastCadence.sameNumber(it) }
            .map { cadence -> cadence.copy(status = CadenceStatus.old) }
        logger.debug { "Previously active cadences:\n${previouslyActiveCadences.joinToString("\n")}" }
        previouslyActiveCadences.forEach { cadence ->
            messageSystem.sendMessage(CurrentCadence(lastCadence))
            voteStorage.updateCadence(cadence)
        }
    }
    
    private fun handleLastActiveCadence(lastCadence: Cadence) {
        val cadence = voteStorage.getCadence(lastCadence)
        if (cadence == null) {
            handleNewActiveCadence(lastCadence)
        } else if (!cadence.isActive()) {
            handleUninitializedActiveCadence(cadence, lastCadence)
        } else {
            handleInitializedActiveCadence(lastCadence)
        }
    }
    
    private fun handleUninitializedActiveCadence(cadence: Cadence, lastCadence: Cadence) {
        val cadenceToUpdate = cadence.copy(status = CadenceStatus.active)
        logger.debug { "Updating previous active cadence:$cadenceToUpdate" }
        messageSystem.sendMessage(CurrentCadence(lastCadence))
    }
    
    private fun handleNewActiveCadence(lastCadence: Cadence) {
        val updatedLastCadence = lastCadence.copy(status = CadenceStatus.active)
        logger.debug { "Handling new active cadence:$updatedLastCadence" }
        messageSystem.sendMessage(CurrentCadence(lastCadence))
        voteStorage.saveCadence(updatedLastCadence)
    }
    
    private fun handleInitializedActiveCadence(lastCadence: Cadence) {
        logger.debug { "Handling already initialized active cadence:$lastCadence" }
        messageSystem.sendMessage(NewCadence(lastCadence))
    }
    
    private fun handleProducedCadences(producedCadences: List<ProducedCadence>, lastCadence: Cadence) {
        logger.debug { "Produced cadences:\n${producedCadences.joinToString("\n")}" }
        producedCadences.forEach { producedCadence ->
            when (producedCadence) {
                is NewCadence -> voteStorage.saveCadence(producedCadence.cadence)
                is CurrentCadence -> voteStorage.updateCadence(producedCadence.cadence)
            }
            messageSystem.sendMessage(producedCadence)
        }
    }
    
    private fun getLastCadence(cadences: List<Cadence>) = cadences.stream()
        .max(compareBy(Cadence::number))
        .orElseThrow()
    
    private fun mapCadence(cadence: Cadence, lastCadence: Cadence): ProducedCadence? {
        val cadenceInDb = voteStorage.getCadence(cadence.number)
        return if (cadenceInDb == null) {
            NewCadence(withUpdatedStatus(cadence, lastCadence))
        } else if (cadenceInDb.isActive()) {
            CurrentCadence(withUpdatedStatus(cadenceInDb, lastCadence))
        } else {
            null
        }
    }
    
    private fun withUpdatedStatus(cadence: Cadence, lastCadence: Cadence) =
        cadence.copy(status = calculateCadenceStatus(cadence, lastCadence))
    
    private fun calculateCadenceStatus(cadence: Cadence, lastCadence: Cadence) =
        if (cadence.sameNumber(lastCadence)) CadenceStatus.active else CadenceStatus.old
}
