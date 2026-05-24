@file:OptIn(ExperimentalStdlibApi::class)

package vote.fetcher.polishsejm.pipeline

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import vote.fetcher.polishsejm.client.apis.DefaultApi
import vote.fetcher.polishsejm.client.apis.VotingsApi
import vote.fetcher.polishsejm.data.VotingDetails
import vote.fetcher.polishsejm.pipeline.mapper.VoteConverter
import vote.fetcher.polishsejm.pipeline.producer.Exceptions
import vote.fetcher.polishsejm.pipeline.producer.RawVoteReader
import vote.fetcher.polishsejm.pipeline.producer.TermDiscoveryProducer
import vote.fetcher.polishsejm.pipeline.producer.VotingMetadataReader
import vote.fetcher.polishsejm.pipeline.writer.RawVoteWriter
import wvly.jobs.api.JobStatusService
import wvly.models.jobs.JobId
import wvly.models.jobs.JobStepId
import wvly.models.tenants.VotingTenant
import wvly.models.votes.Vote
import wvly.models.votes.VotingSession
import wvly.models.vsmetadata.MetadataSource
import wvly.models.vsmetadata.VotingSessionDescription
import wvly.models.vsmetadata.VotingSessionDescriptionId
import wvly.models.vsmetadata.VotingSessionTag
import wvly.storage.api.cache.RawVoteCache
import wvly.storage.api.cache.VoteCache
import wvly.storage.api.cache.VotingSessionMetadataCache
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

class PipelineOrchestrator(
    private val votingsApi: VotingsApi,
    private val defaultApi: DefaultApi,
) {
    fun executeFetchPipeline(
        jobId: JobId,
        jobStatusService: JobStatusService,
        rawVoteCache: RawVoteCache,
    ): JobStepId {
        val rootStepId = jobStatusService.addStepToJob(
            jobId = jobId,
            name = "Polish Sejm fetch",
            description = "ETL pipeline: terms → days → votes → party refs → raw votes → cache",
        )

        try {
            val votings = termDiscoveryProducer.produce(jobStatusService, rootStepId)

            if (votings.isEmpty()) {
                jobStatusService.addStepToStep(
                    stepId = rootStepId,
                    name = "No votings found across all terms",
                    description = "Pipeline terminated: zero votings collected.",
                )
                throw PipelineException("No votings found across all terms.")
            }

            rawVoteWriter.writeAll(jobId, jobStatusService, rootStepId, votings, rawVoteCache)
        } catch (e: Exception) {
            jobStatusService.addStepToStep(
                stepId = rootStepId,
                name = "Polish Sejm fetch failed",
                description = e.message ?: "Unknown error during ETL pipeline execution.",
            )
            throw PipelineException("Polish Sejm startFetch failed: ${e.message}", e)
        }

        return rootStepId
    }

    /**
     * Executes the vote processing phase: reads RawVotes → parses party entries → converts to Vote objects → writes to cache.
     */
    fun executeProcessingPipeline(
        jobId: JobId,
        jobStatusService: JobStatusService,
        rawVoteCache: RawVoteCache,
        voteCache: VoteCache,
    ): JobStepId {
        val stepId = jobStatusService.addStepToJob(
            jobId = jobId,
            name = "Polish Sejm Vote Processing",
            description = "Converts RawVotes (JSON metadata) to structured Vote objects using new core model.",
        )

        val rawVotes = rawVoteReader.produce(rawVoteCache)
        jobStatusService.addStepToStep(
            stepId = stepId,
            name = "Read [${rawVotes.size}] raw votes from cache",
            description = "Retrieved all raw vote entries for processing.",
        )

        if (rawVotes.isEmpty()) {
            jobStatusService.addStepToStep(
                stepId = stepId,
                name = "No raw votes to process",
                description = "Pipeline terminated: zero raw votes found.",
            )
            throw Exceptions.noDataReturned("Raw vote cache for Polish Sejm tenant")
        }

        // Step 3: Convert each PartyVoteEntry to Vote objects
        val allVotes = mutableListOf<Vote>()
        for (entry in rawVotes) {
            val moshiAdapter = Moshi.Builder().build().adapter<VotingDetails>()
            val vote = moshiAdapter.fromJson(entry.content) ?: throw Exceptions.unexpectedData(
                operation = "Raw Vote Cache",
                situation = "Parsing [${entry.content}]",
            )

            val votesForEntry = voteConverter.convert(vote)
            allVotes.addAll(votesForEntry)
        }

        for (vote in allVotes) {
            voteCache.put(VotingTenant("polish-sejm"), vote)
        }
        jobStatusService.addStepToStep(
            stepId = stepId,
            name = "Wrote [${allVotes.size}] votes to cache",
            description = "Pipeline completed successfully.",
        )

        return stepId
    }

    /**
     * Executes the metadata processing phase: extracts voting session descriptions and tags from RawVotes.
     */
    fun executeMetadataPipeline(
        jobId: JobId,
        jobStatusService: JobStatusService,
        rawVoteCache: RawVoteCache,
        votingSessionMetadataCache: VotingSessionMetadataCache,
    ): JobStepId {
        val stepId = jobStatusService.addStepToJob(
            jobId = jobId,
            name = "Polish Sejm Metadata Processing",
            description = "Extracts voting session descriptions and tags from RawVotes.",
        )

        try {
            // Step 1: Read all RawVotes from cache
            val rawVotes = votingMetadataReader.produce(jobId, jobStatusService, stepId, rawVoteCache)
            jobStatusService.addStepToStep(
                stepId = stepId,
                name = "Read [${rawVotes.size}] raw votes",
                description = "Retrieved all raw vote entries for metadata extraction.",
            )

            if (rawVotes.isEmpty()) {
                jobStatusService.addStepToStep(
                    stepId = stepId,
                    name = "No raw votes to process",
                    description = "Pipeline terminated: zero raw votes found.",
                )
                return stepId
            }

            // Step 2: Filter for "voting" type and extract metadata
            val descriptions = mutableListOf<VotingSessionDescription>()
            val tags = mutableListOf<VotingSessionTag>()

            val allVotes = mutableListOf<Vote>()
            for (entry in rawVotes) {
                val moshiAdapter = Moshi.Builder().build().adapter<VotingDetails>()
                val vote = moshiAdapter.fromJson(entry.content) ?: throw Exceptions.unexpectedData(
                    operation = "Raw Vote Cache",
                    situation = "Parsing [${entry.content}]",
                )

                val votesForEntry = voteConverter.convert(vote)
                allVotes.addAll(votesForEntry)
            }
            // TODO: Extract metadata from the votes

            // Step 3: Deduplicate descriptions by voting session identifier
            val deduplicatedDescriptions = descriptions.distinctBy { it.votingSession.identifier }.toList()

            // Step 4: Write to cache (one at a time)
            for (desc in deduplicatedDescriptions) {
                votingSessionMetadataCache.putDescription(VotingTenant("polish-sejm"), desc)
            }

            jobStatusService.addStepToStep(
                stepId = stepId,
                name = "Wrote [${deduplicatedDescriptions.size}] descriptions",
                description = "Saved unique voting session descriptions.",
            )

            for (tag in tags) {
                votingSessionMetadataCache.putTag(VotingTenant("polish-sejm"), tag)
            }

            jobStatusService.addStepToStep(
                stepId = stepId,
                name = "Wrote [${tags.size}] tags",
                description = "Saved voting session tags.",
            )

            jobStatusService.addStepToStep(
                stepId = stepId,
                name = "Metadata processing complete",
                description = "Pipeline completed successfully.",
            )
        } catch (e: Exception) {
            jobStatusService.addStepToStep(
                stepId = stepId,
                name = "Polish Sejm metadata processing failed",
                description = e.message ?: "Unknown error during metadata processing.",
            )
            throw PipelineException("Polish Sejm startVotingSessionMetadataProcessing failed: ${e.message}", e)
        }

        return stepId
    }

    // ──────────────────────────────────────────────────────────────
    // Step instances (created once, reused across phases)
    // ──────────────────────────────────────────────────────────────

    private val termDiscoveryProducer = TermDiscoveryProducer(votingsApi, defaultApi)
    private val rawVoteWriter = RawVoteWriter()

    private val voteConverter = VoteConverter()
    private val rawVoteReader = RawVoteReader()
    private val votingMetadataReader = VotingMetadataReader()

    // ──────────────────────────────────────────────────────────────
    // Helpers (extracted from PolishSejmPlugin.kt for reuse)
    // ──────────────────────────────────────────────────────────────

    companion object {
        const val TYPE_VOTING = "voting"
        const val FIELD_SEPARATOR = "|" // Separates type from fields
        const val KV_SEPARATOR = "&" // Separates key=value pairs within fields (avoids ambiguity with person_to_result values)

        const val TERMS_RESOLVER_STEP = "Resolving available terms"
        const val DAYS_IN_TERM_STEP = "Fetching votings in day for term %d"
        const val VOTES_IN_DAY_STEP = "Expanding votings in day [%s]"
        const val PARTY_REFS_STEP = "Fetching party references for voting #%d: %s"
        const val VOTES_FOR_PARTY_STEP = "Fetching votes for [%s] in voting #%d"

        fun createPolishSejmTenant() = VotingTenant("polish-sejm")
    }

    /**
     * Exception type for pipeline failures.
     */
    class PipelineException(
        message: String,
        cause: Throwable? = null,
    ) : RuntimeException(message, cause)
}

/**
 * Parses a separator-separated string of "person:result" pairs into a map.
 */
private fun parsePersonResultMap(
    personResultStr: String,
    separator: String = ",",
): Map<String, String> {
    if (personResultStr.isBlank()) return emptyMap()
    return personResultStr
        .split(separator)
        .mapNotNull { pair ->
            val parts = pair.split(":", limit = 2)
            if (parts.size == 2) Pair(parts[0].trim(), parts[1].trim()) else null
        }.toMap()
}

/**
 * Extracts metadata from parsed fields map for a "voting" type RawVote.
 */
private fun extractMetadataFromFields(fields: Map<String, String>): Pair<VotingSessionDescription, List<VotingSessionTag>>? {
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

    val votingSession = VotingSession(votingSessionId, heldOn)

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
