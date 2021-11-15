package vote.fetcher

import model.Voting
import okhttp3.HttpUrl

data class VotingWithUrl(val voting: Voting, val url: HttpUrl)
