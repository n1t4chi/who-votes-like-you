package wvly.models.vsmetadata

import wvly.models.votes.VotingSession

data class VotingSessionDescription(
    val id: VotingSessionDescriptionId,
    val votingSession: VotingSession,
    val parent: VotingSessionDescription?,
    val description: String,
    val shortDescription: String,
    val source: MetadataSource,
)
