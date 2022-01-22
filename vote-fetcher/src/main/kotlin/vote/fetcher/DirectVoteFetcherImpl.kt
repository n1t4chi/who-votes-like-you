package vote.fetcher

import okhttp3.HttpUrl
import vote.fetcher.restclient.RestClient
import vote.fetcher.services.*

class DirectVoteFetcherImpl(
    private val cadenceResolver: AvailableCadenceResolver,
    private val votingsArchiveOpener: VotingsArchiveOpener,
    private val votingsInDayOpener: VotingsInDayOpener,
    private val voteOpener: VoteOpener,
    private val partyVoteOpener: PartyVoteOpener
) : VoteFetcher {
    
    constructor(baseUrl: HttpUrl, client: RestClient) : this(
        AvailableCadenceResolver(baseUrl,client),
        VotingsArchiveOpener(baseUrl,client),
        VotingsInDayOpener(baseUrl,client),
        VoteOpener(baseUrl,client),
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