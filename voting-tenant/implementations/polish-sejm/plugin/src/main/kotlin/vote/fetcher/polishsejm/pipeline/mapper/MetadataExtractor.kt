package vote.fetcher.polishsejm.pipeline.mapper

import wvly.models.votes.RawVote
import wvly.models.vsmetadata.MetadataSource
import wvly.models.vsmetadata.VotingSessionDescription
import wvly.models.vsmetadata.VotingSessionDescriptionId
import wvly.models.vsmetadata.VotingSessionTag
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Mapper step that extracts both VotingSessionDescription and VotingSessionTag from a RawVote.
 */
class MetadataExtractor {
    fun map(input: RawVote): Pair<VotingSessionDescription?, List<VotingSessionTag>>? {
        val parts = input.content.split(FIELD_SEPARATOR, limit = 2)
        if (parts.size != 2 || parts[0] != TYPE_VOTING) return null

        // Parse fields by finding first '=' in each KV-separated segment
        val fields = mutableMapOf<String, String>()
        for (field in parts[1].split(KV_SEPARATOR)) {
            val eqIndex = field.indexOf('=')
            if (eqIndex > 0) {
                val key = field.substring(0, eqIndex).trim()
                val value = field.substring(eqIndex + 1).trim()
                fields[key] = value
            }
        }

        return extractMetadataFromFields(fields)
    }

    companion object {
        const val TYPE_VOTING = "voting"
        const val FIELD_SEPARATOR = "|" // Separates type from fields
        const val KV_SEPARATOR = "&" // Separates key=value pairs within fields (avoids ambiguity with person_to_result values)

        fun extractMetadataFromFields(fields: Map<String, String>): Pair<VotingSessionDescription?, List<VotingSessionTag>>? {
            val termStr = fields["term"] ?: return null
            val dateStr = fields["date"] ?: return null
            val numberStr = fields["number"] ?: return null
            val name = fields["name"] ?: "Unknown Voting Session"

            val term = termStr.toIntOrNull() ?: return null
            val number = numberStr.toIntOrNull() ?: return null

            // Validate date format strictly (YYYY-MM-DD) - reject invalid dates
            val dateFormat: LocalDate = try {
                LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (e: Exception) {
                return null // Invalid date format
            }

            val votingSessionId = "$term-$dateStr-$number"
            val heldOn = dateFormat.atStartOfDay().toInstant(ZoneOffset.UTC)

            val votingSession = wvly.models.votes.VotingSession(votingSessionId, heldOn)

            val description = VotingSessionDescription(
                id = VotingSessionDescriptionId(UUID.randomUUID()),
                votingSession = votingSession,
                parent = null,
                description = name,
                shortDescription = name.take(100),
                source = MetadataSource.VOTING_TENANT_SITE,
            )

            val tags = listOf(
                VotingSessionTag(votingSession, "term-$term", MetadataSource.VOTING_TENANT_SITE),
                VotingSessionTag(votingSession, "voting-$number", MetadataSource.VOTING_TENANT_SITE),
            )

            return Pair(description, tags)
        }
    }
}
