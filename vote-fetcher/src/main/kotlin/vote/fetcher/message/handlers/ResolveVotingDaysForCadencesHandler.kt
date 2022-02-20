package vote.fetcher.message.handlers

import message.subscriber.RestartableMessageSubscriber
import message.system.MessageSystem
import vote.fetcher.data.VotingDayWithUrl
import vote.fetcher.message.ProducedCadence
import vote.fetcher.services.VotingsArchiveOpener
import vote.storage.VoteStorage

class ResolveVotingDaysForCadencesHandler(
    messageSystem: MessageSystem,
    private val votingsArchiveOpener: VotingsArchiveOpener,
    val voteStorage: VoteStorage
) : RestartableMessageSubscriber<ProducedCadence>("ResolveVotingDaysForCadencesHandler", messageSystem) {
    
    override fun receiveUnsafe(message: ProducedCadence) {
        val votingsInDayUrls = votingsArchiveOpener.getVotingsInDayUrls(message.cadence)
        logger.trace { "voting day urls to resolve:\n${votingsInDayUrls.joinToString("\n")}" }
        val votingDaysToUpdate = votingsInDayUrls.filter(this::notInDb)
        logger.debug { "voting day urls to update:\n${votingsInDayUrls.joinToString("\n")}" }
        votingDaysToUpdate.forEach(messageSystem::sendMessage)
        updateCadence(message, votingsInDayUrls)
    }
    
    private fun updateCadence(message: ProducedCadence, votingsInDayUrls: List<VotingDayWithUrl>) {
        val cadence = voteStorage.getCadence(message.cadence)
        if (cadence.daysWithVotes != votingsInDayUrls.size) {
            val updatedCadence = cadence.copy(daysWithVotes = votingsInDayUrls.size)
            logger.debug { "Updating cadence:$updatedCadence" }
            voteStorage.updateCadence(updatedCadence)
        }
    }
    
    private fun notInDb(votingDayWithUrl: VotingDayWithUrl): Boolean =
        voteStorage.getVotingDay(votingDayWithUrl.votingDay.date) == null
}
