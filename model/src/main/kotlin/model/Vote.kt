package model

data class Vote(val voting: Voting, val party: Party, val person: Person, val result: VoteResult)