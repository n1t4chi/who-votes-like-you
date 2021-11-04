package vote.fetcher

import model.*
import okhttp3.*
import java.util.HashSet

class DirectVoteFetcherImpl(
    private val cadenceResolver: AvailableCadenceResolver,
    private val votingsArchiveOpener: VotingsArchiveOpener,
    private val votesInDayOpener: VotesInDayOpener,
    private val voteOpener: VoteOpener,
    private val partyVoteOpener: PartyVoteOpener
) : VoteFetcher {
    
    constructor(client: OkHttpClient = OkHttpClient(), baseUrl: String) : this(
        AvailableCadenceResolver(client, baseUrl),
        VotingsArchiveOpener(client, baseUrl),
        VotesInDayOpener(client, baseUrl),
        VoteOpener(client, baseUrl),
        PartyVoteOpener(client),
    )
    
    
    override fun getAllVotes(): Set<Vote> {
        val votes = HashSet<Vote>()
        val cadences = cadenceResolver.getCurrentCadences()
        for (cadence in cadences) {
            val votesInDayUrls = votingsArchiveOpener.getVotesInDayUrls(cadence.number)
            for (votesInDayUrl in votesInDayUrls) {
                val votingUrls = votesInDayOpener.fetchVotingUrls(votesInDayUrl.second)
                for (votingUrl in votingUrls) {
                    val votingInformation = voteOpener.fetchVotingUrlsForParties(votingUrl)
                    val voting = votingInformation.voting
                    for ((party, url) in votingInformation.partyVotes) {
                        val votesForParty = partyVoteOpener.fetchVotingUrlsForParties(party, url)
                        for ((person, voteResult) in votesForParty.getVotes()) {
                            votes.add(Vote(voting, person, voteResult, party))
                        }
                    }
                }
            }
        }
        return votes
    }
}