package vote.fetcher.message.handlers

import message.system.*
import vote.fetcher.message.ProducedCadence
import vote.fetcher.services.VotingsArchiveOpener
import vote.storage.VoteStorage

class ResolveVotingDaysForCadencesHandler(
    private val messageSystem:MessageSystem,
    private val votingsArchiveOpener: VotingsArchiveOpener,
    val voteStorage:VoteStorage
) : MessageSubscriber<ProducedCadence> {
        
    override fun receive(message: ProducedCadence) {
        val votingsInDayUrls = votingsArchiveOpener.getVotingsInDayUrls(message.cadence)
        votingsInDayUrls.forEach(messageSystem::sendMessage)
    }
}
