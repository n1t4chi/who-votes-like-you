package wvly.storage.api.votes

data class VoteInsertStatistics(
    val voteCount: Int,
    val voterCount: Int,
    val partyCount: Int,
    val votingSessionCount: Int,
)
