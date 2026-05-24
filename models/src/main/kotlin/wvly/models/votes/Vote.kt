package wvly.models.votes

data class Vote(
    val castBy: Voter,
    val castFor: Party,
    val castDuring: VotingSession,
    val result: VoteResult,
)
