package vote.fetcher.data

import model.VotingDay
import okhttp3.HttpUrl

data class VotingsInDay(val votingDay: VotingDay, val votingUrl: HttpUrl)