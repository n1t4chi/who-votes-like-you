package vote.fetcher.data

import model.*
import okhttp3.HttpUrl

data class PartyVotingReference(val voting:Voting,val party: Party, val url: HttpUrl)
