package vote.fetcher.polishsejm

import io.kotest.matchers.equality.*
import wvly.models.votes.Party
import wvly.models.votes.RawVote
import wvly.models.votes.Vote
import wvly.models.votes.VoteResult
import wvly.models.votes.Voter
import wvly.models.votes.VotingSession
import wvly.models.vsmetadata.MetadataSource
import wvly.models.vsmetadata.VotingSessionDescription
import wvly.models.vsmetadata.VotingSessionDescriptionId
import wvly.models.vsmetadata.VotingSessionTag
import wvly.utilities.test.shouldContainExactlyInAnyOrderUsingFields
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

internal object TestFixtures {
    fun votingMetadata(
        term: Int = 10,
        date: String = "2024-05-15",
        number: Int = 123,
        name: String = "Ustawa o ochronie danych",
    ): RawVote =
        RawVote(
            content = buildString {
                append("voting|")
                append("term=").append(term).append("&")
                append("date=").append(date).append("&")
                append("number=").append(number).append("&")
                append("name=").append(name)
            },
        )

    fun partyVotes(
        party: String = "PiS",
        personsToResults: Map<String, String> = mapOf(
            "Jan Kowalski" to "Za",
            "Alicja Nowak" to "Przeciw",
        ),
    ): RawVote {
        val personResultStr = personsToResults.entries.joinToString(";") { (person, result) ->
            "$person:$result"
        }
        return RawVote(
            content = buildString {
                append("votes_for_party|")
                append("party=").append(party).append("&")
                append("person_to_result=").append(personResultStr)
            },
        )
    }

    fun votingMetadataContent(
        term: Int = 10,
        date: String = "2024-05-15",
        number: Int = 123,
        name: String = "Ustawa o ochronie danych",
    ): String =
        buildString {
            append("voting|")
            append("term=").append(term).append("&")
            append("date=").append(date).append("&")
            append("number=").append(number).append("&")
            append("name=").append(name)
        }

    fun partyVotesContent(
        party: String = "PiS",
        personToResultPairs: List<Pair<String, String>> = listOf(
            "Jan Kowalski" to "Za",
            "Alicja Nowak" to "Przeciw",
        ),
    ): String {
        val personResultStr = personToResultPairs.joinToString(";") { (person, result) ->
            "$person:$result"
        }
        return buildString {
            append("votes_for_party|")
            append("party=").append(party).append("&")
            append("person_to_result=").append(personResultStr)
        }
    }

    fun hasVotingMetadataPrefix(content: String): Boolean = content.startsWith("voting|")

    fun isVotesForParty(content: String): Boolean = content.startsWith("votes_for_party|")

    fun extractPartyName(content: String): String? {
        if (!isVotesForParty(content)) return null
        val fields = content.substringAfter("|").split("&")
        return fields.find { it.startsWith("party=") }?.substringAfter("=")?.trim()
    }

    fun extractPersonToResultMap(content: String): Map<String, String>? {
        if (!isVotesForParty(content)) return null
        val fields = content.substringAfter("|").split("&")
        val personResultStr = fields.find { it.startsWith("person_to_result=") }?.substringAfter("=")?.trim() ?: return null
        if (personResultStr.isBlank()) return emptyMap()

        return personResultStr
            .split(";")
            .mapNotNull { pair ->
                val parts = pair.split(":", limit = 2)
                if (parts.size == 2) Pair(parts[0].trim(), parts[1].trim()) else null
            }.toMap()
    }

    fun hasValidFormat(content: String): Boolean = content.contains("|") && !content.startsWith("invalid_format")

    fun votingMetadataRawVote(
        term: Int = 10,
        date: String = "2024-05-15",
        number: Int = 123,
        name: String = "Ustawa o ochronie danych",
    ): RawVote = RawVote(votingMetadataContent(term, date, number, name))

    fun partyVotesRawVote(
        party: String = "PiS",
        personToResultPairs: List<Pair<String, String>> = listOf("Jan Kowalski" to "Za"),
    ): RawVote = RawVote(partyVotesContent(party, personToResultPairs))

    fun expectedVote(
        personName: String,
        partyName: String,
        term: Int = 0,
        date: String = "",
        votingNumber: Int = 0,
        resultPolish: String = "Za",
    ): Vote {
        val sessionId = "$term-$date-$votingNumber"
        val heldOn = if (date.isNotEmpty()) {
            LocalDate.parse(date).atStartOfDay().toInstant(ZoneOffset.UTC)
        } else {
            Instant.now()
        }
        val voteResult = mapPolishVoteResult(resultPolish)
        return Vote(
            castBy = Voter(name = personName.trim()),
            castFor = Party(name = partyName),
            castDuring = VotingSession(identifier = sessionId, heldOn = heldOn),
            result = voteResult,
        )
    }

    fun expectedVotingSessionDescription(
        term: Int = 10,
        date: String = "2024-05-15",
        number: Int = 123,
        name: String = "Test Name",
    ): VotingSessionDescription {
        val sessionId = "$term-$date-$number"
        val heldOn = LocalDate
            .parse(date)
            .atStartOfDay()
            .toInstant(ZoneOffset.UTC)
        return VotingSessionDescription(
            id = VotingSessionDescriptionId(UUID.randomUUID()),
            votingSession = VotingSession(identifier = sessionId, heldOn = heldOn),
            parent = null,
            description = name,
            shortDescription = name.take(100),
            source = MetadataSource.VOTING_TENANT_SITE,
        )
    }

    fun expectedVotingSessionTag(
        term: Int = 10,
        date: String = "2024-05-15",
        number: Int = 123,
        tagText: String,
    ): VotingSessionTag {
        val sessionId = "$term-$date-$number"
        val heldOn = LocalDate
            .parse(date)
            .atStartOfDay()
            .toInstant(ZoneOffset.UTC)
        return VotingSessionTag(
            votingSession = VotingSession(identifier = sessionId, heldOn = heldOn),
            text = tagText,
            source = MetadataSource.VOTING_TENANT_SITE,
        )
    }

    infix fun List<VotingSessionDescription>.shouldMatchIgnoringIds(other: List<VotingSessionDescription>) {
        this shouldContainExactlyInAnyOrderUsingFields {
            this.excludedProperties = listOf(VotingSessionDescription::id)
            other
        }
    }

    infix fun List<VotingSessionTag>.shouldMatchIgnoringTimestamps(other: List<VotingSessionTag>) {
        this shouldContainExactlyInAnyOrderUsingFields {
            this.overrideMatchers = mapOf(
                VotingSession::heldOn to timestampMatcher,
            )
            other
        }
    }

    infix fun List<Vote>.shouldMatchIgnoringSessionTimestamps(other: List<Vote>) {
        this shouldContainExactlyInAnyOrderUsingFields {
            this.overrideMatchers = mapOf(
                VotingSession::heldOn to timestampMatcher,
            )
            other
        }
    }

    private val timestampMatcher = matchInstantsWithTolerance(1.seconds)

    private fun mapPolishVoteResult(polish: String): VoteResult =
        when (polish.trim().lowercase()) {
            "za" -> VoteResult.YES
            "przeciw" -> VoteResult.NO
            "obecny" -> VoteResult.ABSENT
            "wstrzymam_się", "wstrzymam się" -> VoteResult.ABSTAINED
            else -> throw IllegalArgumentException("Unknown vote result: '$polish'")
        }
}
