package vote.fetcher.polishsejm.data

import vote.fetcher.polishsejm.client.models.SejmVotingDetailsDto

data class VotingDetails(
    val termNumber: Int,
    val sittingNumber: Int,
    val votingNumber: Int,
    val votingDetails: SejmVotingDetailsDto,
    val voting: Voting,
)
