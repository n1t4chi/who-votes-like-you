package vote.fetcher.polishsejm.pipeline.mapper

import vote.fetcher.polishsejm.client.models.SejmVoteValueDto
import vote.fetcher.polishsejm.client.models.SejmVotingKindDto
import vote.fetcher.polishsejm.data.VotingDetails
import vote.fetcher.polishsejm.pipeline.producer.Exceptions
import wvly.models.votes.Party
import wvly.models.votes.Vote
import wvly.models.votes.VoteResult
import wvly.models.votes.Voter
import wvly.models.votes.VotingSession
import java.time.ZoneOffset

class VoteConverter {
    fun convert(votingDetails: VotingDetails): List<Vote> {
        val voting = votingDetails.voting
        val sitting = voting.sitting
        val term = sitting.term

        // Use entry's metadata as the session identifier
        val votingSessionId = "${term.number}-${sitting.sittingNumber}-${votingDetails.votingNumber}"
        val votingSession = VotingSession(
            votingSessionId,
            heldOn = voting.date.toInstant(ZoneOffset.of("CET")),
        )
        val votingKind = votingDetails.votingDetails.kind ?: votingDetails.missingParam("kind")
        when (votingKind) {
            SejmVotingKindDto.ELECTRONIC,
            SejmVotingKindDto.TRADITIONAL,
                -> return covertYesNoVoting(votingDetails, votingSession)

            SejmVotingKindDto.ON_LIST,
                -> return convertOnListVoting(votingDetails, votingSession)
        }
    }

    fun covertYesNoVoting(
        votingDetails: VotingDetails,
        votingSession: VotingSession,
    ): List<Vote> {
        val votes = votingDetails.votingDetails.votes ?: votingDetails.missingParam("votes")
        return votes.map { vote ->
            val partyName = vote.club ?: votingDetails.missingVoteParam("club")
            val firstName = vote.firstName ?: votingDetails.missingParam("firstName")
            val secondNamePart = vote.secondName?.let { " $it" } ?: ""
            val lastName = vote.lastName ?: votingDetails.missingParam("lastName")
            val voteValue = vote.vote ?: votingDetails.missingParam("vote")

            Vote(
                castBy = Voter(name = "$firstName$secondNamePart $lastName"),
                castFor = Party(partyName),
                castDuring = votingSession,
                result = voteValue.toVotingResultForYesNoVoting(),
            )
        }
    }

    fun convertOnListVoting(
        votingDetails: VotingDetails,
        votingSession: VotingSession,
    ): List<Vote> {
        val options = votingDetails.votingDetails.votingOptions ?: votingDetails.missingParam("options")
        val votes = votingDetails.votingDetails.votes ?: votingDetails.missingParam("votes")

        return options.flatMap { option ->
            val index = option.optionIndex ?: votingDetails.missingParam("optionIndex")
            val optionVotingSession = votingSession.copy(
                identifier = votingSession.identifier + "-opt$index",
            )
            votes.map { vote ->
                val partyName = vote.club ?: votingDetails.missingVoteParam("club")
                val firstName = vote.firstName ?: votingDetails.missingParam("firstName")
                val secondNamePart = vote.secondName?.let { " $it" } ?: ""
                val lastName = vote.lastName ?: votingDetails.missingParam("lastName")
                val voteValue = vote.vote ?: votingDetails.missingParam("vote")
                val listVotes = vote.listVotes ?: votingDetails.missingParam("listVotes")
                val result = toVotingResultForListVoting(voteValue, listVotes, index)

                Vote(
                    castBy = Voter(name = "$firstName$secondNamePart $lastName"),
                    castFor = Party(partyName),
                    castDuring = optionVotingSession,
                    result = result,
                )
            }
        }
    }

    private fun toVotingResultForListVoting(
        voteValue: SejmVoteValueDto,
        listVotes: Map<String, SejmVoteValueDto>,
        index: Int,
    ): VoteResult =
        when (voteValue) {
            SejmVoteValueDto.VOTE_VALID -> {
                listVotes[index.toString()]
                    ?.toVotingResultForYesNoVoting()
                    ?: VoteResult.UNKNOWN
            }
            SejmVoteValueDto.ABSTAIN,
            SejmVoteValueDto.PRESENT,
            SejmVoteValueDto.NO_VOTE,
                -> VoteResult.ABSTAINED
            SejmVoteValueDto.ABSENT,
                -> VoteResult.ABSENT
            SejmVoteValueDto.YES,
            SejmVoteValueDto.NO,
            SejmVoteValueDto.VOTE_INVALID,
                -> VoteResult.UNKNOWN
        }

    fun VotingDetails.missingParam(paramName: String): Nothing =
        throw Exceptions.unexpectedData(
            operation = "Get votings details",
            situation = "Voting [$votingNumber] in sitting [$sittingNumber] from term [${voting.sitting.term.number}] has no $paramName",
        )

    fun VotingDetails.missingVoteParam(paramName: String): Nothing =
        throw Exceptions.unexpectedData(
            operation = "Get votings details",
            situation = "Voting [$votingNumber] in sitting [$sittingNumber] from term [${voting.sitting.term.number}]" +
                " has vote with no $paramName",
        )

    private fun SejmVoteValueDto.toVotingResultForYesNoVoting(): VoteResult =
        when (this) {
            SejmVoteValueDto.YES,
                -> VoteResult.YES
            SejmVoteValueDto.NO,
                -> VoteResult.NO
            SejmVoteValueDto.ABSTAIN,
            SejmVoteValueDto.PRESENT,
            SejmVoteValueDto.NO_VOTE,
                -> VoteResult.ABSTAINED
            SejmVoteValueDto.ABSENT,
                -> VoteResult.ABSENT
            SejmVoteValueDto.VOTE_VALID,
            SejmVoteValueDto.VOTE_INVALID,
                -> VoteResult.UNKNOWN
        }
}
