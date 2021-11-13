package vote.fetcher

import model.Vote
import okhttp3.OkHttpClient

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
    
    
    override fun getAllVotes(): Iterator<Vote> {
        val votes = HashSet<Vote>()
        val cadences = cadenceResolver.getCurrentCadences()
        var cadenceIt = 0
        val cadencesSize = cadences.size
        println("Cadences to process: " + cadencesSize)
        for (cadence in cadences) {
            cadenceIt++
            val votesInDayUrls = votingsArchiveOpener.getVotesInDayUrls(cadence)
            val votesInDaySize = votesInDayUrls.size
            println("  [$cadenceIt/$cadencesSize] Votes in day to process: " + votesInDaySize)
            var voteInDayIt = 0
            for ((date,votesInDayUrl) in votesInDayUrls) {
                voteInDayIt++
                val votesWithUrls = votesInDayOpener.fetchVotingUrls(votesInDayUrl, cadence, date)
                val votesSize = votesWithUrls.size
                println("    [$cadenceIt/$cadencesSize][$voteInDayIt/$votesInDaySize] Votes on $date to process: " + votesSize)
                var votingIt = 0
                for ((voting,votingUrl) in votesWithUrls) {
                    votingIt++
                    val votingInformation = voteOpener.fetchVotingUrlsForParties(votingUrl)
                    val partiesSize = votingInformation.size
                    println("      [$cadenceIt/$cadencesSize][$voteInDayIt/$votesInDaySize][$votingIt/$votesSize] Parties to process: " + partiesSize)
                    var partiesIt = 0
                    for ((party, url) in votingInformation) {
                        partiesIt++
                        val votesForParty = partyVoteOpener.fetchVotingUrlsForParties(party, url)
                        println("        [$cadenceIt/$cadencesSize][$voteInDayIt/$votesInDaySize][$votingIt/$votesSize][$partiesIt/$partiesSize] votes for ${party.name}: " + votesForParty.size())
                        for ((person, voteResult) in votesForParty.getVotes()) {
                            votes.add(Vote(voting, person, voteResult, party))
                        }
                    }
                }
            }
        }
        return votes.iterator()
    }
}