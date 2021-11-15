package vote.fetcher

import okhttp3.OkHttpClient

class DirectVoteFetcherImpl(
    private val cadenceResolver: AvailableCadenceResolver,
    private val votingsArchiveOpener: VotingsArchiveOpener,
    private val votingsInDayOpener: VotingsInDayOpener,
    private val voteOpener: VoteOpener,
    private val partyVoteOpener: PartyVoteOpener
) : VoteFetcher {
    
    constructor(client: OkHttpClient = OkHttpClient(), baseUrl: String) : this(
        AvailableCadenceResolver(client, baseUrl),
        VotingsArchiveOpener(client, baseUrl),
        VotingsInDayOpener(client, baseUrl),
        VoteOpener(client, baseUrl),
        PartyVoteOpener(client),
    )
    
    
    override fun getAllVotes(): VoteStream {
        val fetcher = InitialVoteFetcher(
            cadenceResolver,
            votingsArchiveOpener,
            votingsInDayOpener,
            voteOpener,
            partyVoteOpener
        )
        fetcher.start()
        return fetcher
    }
}