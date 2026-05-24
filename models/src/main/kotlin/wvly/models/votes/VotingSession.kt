package wvly.models.votes

import java.time.Instant

data class VotingSession(
    val identifier: String,
    val heldOn: Instant,
)
