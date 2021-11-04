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
            for ((date,votesInDayUrl) in votesInDayUrls) {
                val votesWithUrls = votesInDayOpener.fetchVotingUrls(votesInDayUrl, date)
                for ((voting,votingUrl) in votesWithUrls) {
                    val votingInformation = voteOpener.fetchVotingUrlsForParties(votingUrl)
                    for ((party, url) in votingInformation) {
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