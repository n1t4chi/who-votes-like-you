package vote.fetcher

import model.*
import okhttp3.HttpUrl

data class VotingInformation(val voting: Voting, val partyVotes: Map<Party, HttpUrl>  ) {

}
