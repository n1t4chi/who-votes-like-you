package wvly.models.vsmetadata

import wvly.models.votes.VotingSession

data class VotingSessionTag(
    val votingSession: VotingSession,
    val text: String,
    val source: MetadataSource,
)
