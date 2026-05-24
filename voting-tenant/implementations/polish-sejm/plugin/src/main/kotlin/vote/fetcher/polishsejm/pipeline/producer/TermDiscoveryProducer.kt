package vote.fetcher.polishsejm.pipeline.producer

import vote.fetcher.polishsejm.client.apis.DefaultApi
import vote.fetcher.polishsejm.client.apis.VotingsApi
import vote.fetcher.polishsejm.data.Sitting
import vote.fetcher.polishsejm.data.Term
import vote.fetcher.polishsejm.data.TermStatus
import vote.fetcher.polishsejm.data.Voting
import vote.fetcher.polishsejm.data.VotingDetails
import vote.fetcher.polishsejm.pipeline.call
import wvly.jobs.api.JobStatusService
import wvly.models.jobs.JobStepId

class PluginException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

object Exceptions {
    fun fetchFailed(
        operation: String,
        exception: Exception,
    ) = PluginException("[$operation] failed", exception)

    fun noDataReturned(operation: String) = PluginException("[$operation] returned empty data set")

    fun unexpectedData(
        operation: String,
        situation: String,
    ) = PluginException("[$operation] returned unexpected data. Cause: [$situation]")
}

class TermDiscoveryProducer(
    private val votingsApi: VotingsApi,
    private val defaultApi: DefaultApi,
) {
    fun produce(
        jobStatusService: JobStatusService,
        parentStepId: JobStepId,
    ): List<VotingDetails> {
        jobStatusService.addStepToStep(
            stepId = parentStepId,
            name = "Collecting terms",
            description = "Collecting term information from the API.",
        )

        val terms = call {
            defaultApi.sejmTermGet()
        }.onAnyError {
            jobStatusService.addStepToStep(
                stepId = parentStepId,
                name = "Failed to collect terms",
                description = "Failed to collect term information from the API.",
            )
        }.orThrow {
            Exceptions.fetchFailed(operation = "Get terms", it)
        }.also {
            jobStatusService.addStepToStep(
                stepId = parentStepId,
                name = "Successfully collected terms",
                description = "Collected [${it.size}] terms from the API.",
            )
        }

        terms.ifEmpty {
            throw Exceptions.noDataReturned(operation = "Get terms")
        }

        val allVotes = mutableListOf<VotingDetails>()
        terms.forEach { termFromApi ->
            val termNumber = termFromApi.num ?: throw Exceptions.unexpectedData(
                operation = "Get term",
                situation = "Term has no number",
            )
            val term = Term(
                number = termNumber,
                status = if (termFromApi.current == true) TermStatus.active else TermStatus.old,
            )
            jobStatusService.addStepToStep(
                stepId = parentStepId,
                name = "Collecting proceeding days for term [$termNumber]",
                description = "Collecting term information from the API.",
            )

            val proceedingDays = call {
                votingsApi.sejmTermtermVotingsGet(termNumber)
            }.onAnyError {
                jobStatusService.addStepToStep(
                    stepId = parentStepId,
                    name = "Failed to collect proceeding days for term [$termNumber]",
                    description = "Failed to collect proceeding days for term [$termNumber] from the API.",
                )
            }.orThrow {
                Exceptions.fetchFailed(operation = "Get proceeding days for term [$termNumber]", it)
            }.also {
                jobStatusService.addStepToStep(
                    stepId = parentStepId,
                    name = "Successfully collected proceeding days for term [$termNumber]",
                    description = "Successfully collected [${it.size}] proceeding days from the API.",
                )
            }
            val (sittings, emptySittings) = proceedingDays
                .map {
                    Sitting(
                        term = term,
                        sittingNumber = it.proceeding ?: throw Exceptions.unexpectedData(
                            operation = "Get proceeding days",
                            situation = "Proceeding Day in term [$termNumber] at date [${it.date}] has no proceeding set",
                        ),
                        votingsCount = it.votingsNum ?: throw Exceptions.unexpectedData(
                            operation = "Get proceeding days",
                            situation = "Proceeding Day in term [$termNumber] at date [${it.date}] has votes set",
                        ),
                    )
                }.groupBy { it.sittingNumber }
                .map { (sittingNumber, sittings) ->
                    sittings.reduce { accumulator, sitting ->
                        accumulator.copy(
                            votingsCount = accumulator.votingsCount + sitting.votingsCount,
                        )
                    }
                }.partition { it.votingsCount == 0 }

            jobStatusService.addStepToStep(
                stepId = parentStepId,
                name = "Term [$termNumber] has [${sittings.size}] sittings with votings",
                description = "Term [$termNumber] has [${sittings.size}] proceedings" +
                    " that contain votings and [${emptySittings.size}] without votings.",
            )

            for (sitting in sittings) {
                val sittingNumber = sitting.sittingNumber
                val votings = call {
                    votingsApi.sejmTermtermVotingsSittingGet(sitting = sittingNumber, term = termNumber)
                }.onAnyError {
                    jobStatusService.addStepToStep(
                        stepId = parentStepId,
                        name = "Failed to collect votings for sitting [$sittingNumber] in term [$termNumber]",
                        description = "Failed to collect votings for sitting [$sittingNumber] in term [$termNumber] from the API.",
                    )
                }.orThrow {
                    Exceptions.fetchFailed(operation = "Get votings for sitting [$sittingNumber] in term [$termNumber]", it)
                }.also {
                    jobStatusService.addStepToStep(
                        stepId = parentStepId,
                        name = "Successfully collected [${it.size}] votings" +
                            " for sitting [$sittingNumber] in term [$termNumber]",
                        description = "Successfully collected [${it.size}] votings" +
                            " for sitting [$sittingNumber] in term [$termNumber] from the API.",
                    )
                }

                votings.ifEmpty {
                    throw Exceptions.noDataReturned(operation = "Get terms")
                }

                for (voting in votings) {
                    val votingNumber = voting.votingNumber ?: throw Exceptions.unexpectedData(
                        operation = "Get votings for sitting",
                        situation = "Voting in sitting [$sittingNumber] in term [$term] has no number",
                    )
                    val votingDetails = call {
                        votingsApi.sejmTermtermVotingsSittingNumGet(
                            sitting = sittingNumber,
                            term = termNumber,
                            num = votingNumber,
                        )
                    }.onAnyError {
                        jobStatusService.addStepToStep(
                            stepId = parentStepId,
                            name = "Failed to collect voting details for voting [$votingNumber]" +
                                " from sitting [$sittingNumber] in term [$termNumber]",
                            description = "Failed to collect voting details for voting [$votingNumber]" +
                                " from sitting [$sittingNumber] in term [$termNumber] from the API.",
                        )
                    }.orThrow {
                        Exceptions.fetchFailed(
                            operation = "Get voting details for voting [$votingNumber]" +
                                " from sitting [$sittingNumber] in term [$termNumber]",
                            it,
                        )
                    }.also {
                        jobStatusService.addStepToStep(
                            stepId = parentStepId,
                            name = "Successfully collected voting details for voting [$votingNumber]" +
                                " from sitting [$sittingNumber] in term [$termNumber]",
                            description = "Successfully collected voting details for voting [$votingNumber]" +
                                " from sitting [$sittingNumber] in term [$termNumber] from the API.",
                        )
                    }

                    fun missingParam(paramName: String): Nothing =
                        throw Exceptions.unexpectedData(
                            operation = "Get votings details",
                            situation = "Voting [$votingNumber] in sitting [$sittingNumber] from term [$term] has no $paramName",
                        )
                    allVotes.add(
                        VotingDetails(
                            termNumber = termNumber,
                            sittingNumber = sittingNumber,
                            votingNumber = votingNumber,
                            votingDetails = votingDetails,
                            voting = Voting(
                                name = votingDetails.title ?: missingParam("title"),
                                type = votingDetails.kind ?: missingParam("kind"),
                                votingNumber = votingDetails.votingNumber ?: missingParam("votingNumber"),
                                date = votingDetails.date ?: missingParam("date"),
                                sitting = sitting,
                                votesCast = votingDetails.totalVoted ?: missingParam("totalVoted"),
                            ),
                        ),
                    )
                }
            }
        }

        if (allVotes.isEmpty()) {
            jobStatusService.addStepToStep(
                stepId = parentStepId,
                name = "No voting days found across all terms",
                description = "Pipeline terminated: zero votings collected.",
            )
        }

        return allVotes
    }
}
