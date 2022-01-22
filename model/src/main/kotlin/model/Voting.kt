package model

data class Voting(
    val name: String,
    val number: Int,
    val votingDay: VotingDay,
    val votesCast: Int = 0
)
