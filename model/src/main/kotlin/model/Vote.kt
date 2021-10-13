package model

data class Vote(val voting: Voting, val person: Person, val result: VoteResult, val party: Party)