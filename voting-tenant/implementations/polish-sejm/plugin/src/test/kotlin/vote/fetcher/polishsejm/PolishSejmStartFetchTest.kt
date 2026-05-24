@file:OptIn(ExperimentalStdlibApi::class)

package vote.fetcher.polishsejm

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.jsonResponse
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import storage.inmemory.RawVoteCacheInMemoryImpl
import vote.fetcher.polishsejm.client.apis.VotingsApi
import vote.fetcher.polishsejm.client.models.*
import wvly.jobs.api.finishedStep
import wvly.jobs.api.shouldMatch
import wvly.jobs.service.*
import wvly.models.jobs.JobId
import wvly.models.jobs.JobState
import wvly.models.jobs.JobStatus
import wvly.models.jobs.JobStepStatus
import wvly.models.votes.RawVote
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

class PolishSejmStartFetchTest : BehaviorSpec({
    val storage = JobStatusInMemoryStorage()
    val rawVoteCache = RawVoteCacheInMemoryImpl()
    val jobStatusService = JobStatusServiceImpl(storage)
    val jobStatusViewer = JobStatusViewerImpl(storage)
    val jobId = jobStatusService.createJobStatus("test", "test")
    val wireMockServer = WireMockServer(8089)
    wireMockServer.start()
    afterSpec { wireMockServer.stop() }

    Given("a Polish Sejm plugin configured with WireMock") {
        val plugin = PolishSejmPlugin(
            votingsApi = VotingsApi(basePath = wireMockServer.baseUrl()),
        )

        And("there are no terms") {
            wireMockServer.setupTerms {}
            When("startFetch is performed") {
                plugin.startFetch(jobId, jobStatusService, rawVoteCache)
                Then("job should fail") {
                    jobStatusViewer.getJobStatus(jobId) shouldMatch voteFetchJob(
                        jobId = jobId,
                        jobState = JobState.FAILED,
                        steps = listOf(
                            startedImportStep(),
                        ),
                    )
                }

                And("raw vote cache is empty") {
                    rawVoteCache.get(polishSejmTenant) shouldBe emptyList()
                }
            }
        }
        And("has one active term") {
            wireMockServer.setupTerms {
                setupActiveTerm(number = 1, from = "2000-06-01")
            }
            And("term has no proceeding days") {
                wireMockServer.setupProceedingDays(termNumber = 1) {}
                When("startFetch is performed") {
                    plugin.startFetch(jobId, jobStatusService, rawVoteCache)

                    Then("job should complete successfully") {
                        jobStatusViewer.getJobStatus(jobId) shouldMatch voteFetchJob(
                            jobId = jobId,
                            steps = listOf(
                                startedImportStep(),
                            ),
                        )
                    }

                    And("raw vote cache is empty") {
                        rawVoteCache.get(polishSejmTenant) shouldBe emptyList()
                    }
                }
            }

            And("term has has one proceeding day with no votings") {
                wireMockServer.setupProceedingDays(termNumber = 1) {
                    setupProceedingDay(number = 1, date = "2000-06-02", votingsCount = 0)
                }
                When("startFetch is performed") {
                    plugin.startFetch(jobId, jobStatusService, rawVoteCache)
                    Then("job should complete successfully") {
                        jobStatusViewer.getJobStatus(jobId) shouldMatch voteFetchJob(
                            jobId = jobId,
                            steps = listOf(
                                startedImportStep(),
                            ),
                        )
                    }

                    And("raw vote cache is empty") {
                        rawVoteCache.get(polishSejmTenant) shouldBe emptyList()
                    }
                }
            }

            And("term has has one proceeding day with one voting") {
                wireMockServer.setupProceedingDays(termNumber = 1) {
                    setupProceedingDay(number = 1, date = "2000-06-02", votingsCount = 1)
                }

                And("sitting has one voting") {
                    wireMockServer.setupSitting(termNumber = 1, sittingNumber = 1) {
                        setupElectronicVoting(votingNumber = 1, dateTime = "2000-06-02T12:00:00", yes = 1, no = 0)
                    }
                    And("Voting has one vote") {
                        wireMockServer.setupVoting(termNumber = 1, sittingNumber = 1, votingNumber = 1) {
                            setupAsElectronicVoting(votingNumber = 1, dateTime = "2000-06-02T12:00:00", yes = 1, no = 0)
                            withVote(
                                name = "Jan Kowalski",
                                party = "Jedyna",
                                vote = SejmVoteValueDto.YES,
                            )
                        }
                        When("startFetch is performed") {
                            plugin.startFetch(jobId, jobStatusService, rawVoteCache)
                            Then("job should complete successfully") {
                                jobStatusViewer.getJobStatus(jobId) shouldMatch voteFetchJob(
                                    jobId = jobId,
                                    steps = listOf(
                                        startedImportStep(),
                                    ),
                                )
                            }

                            And("raw vote cache has one entry") {
                                rawVoteCache.get(polishSejmTenant) shouldBe listOf(
                                    RawVote("test"),
                                )
                            }
                        }
                    }
                    And("request for voting throws error") {
                        wireMockServer.setupVotingWithServerError(termNumber = 1, sittingNumber = 1, votingNumber = 1)

                        When("startFetch is performed") {
                            plugin.startFetch(jobId, jobStatusService, rawVoteCache)

                            Then("job should fail") {
                                jobStatusViewer.getJobStatus(jobId) shouldMatch voteFetchJob(
                                    jobId = jobId,
                                    steps = listOf(
                                        startedImportStep(),
                                    ),
                                    jobState = JobState.FAILED,
                                )
                            }

                            And("raw vote cache is empty") {
                                rawVoteCache.get(polishSejmTenant) shouldBe emptyList()
                            }
                        }
                    }
                }

                And("request for sitting throws error") {
                    wireMockServer.setupSittingWithServerError(termNumber = 1, sittingNumber = 1)

                    When("startFetch is performed") {
                        plugin.startFetch(jobId, jobStatusService, rawVoteCache)

                        Then("job should fail") {
                            jobStatusViewer.getJobStatus(jobId) shouldMatch voteFetchJob(
                                jobId = jobId,
                                steps = listOf(
                                    startedImportStep(),
                                ),
                                jobState = JobState.FAILED,
                            )
                        }

                        And("raw vote cache is empty") {
                            rawVoteCache.get(polishSejmTenant) shouldBe emptyList()
                        }
                    }
                }
            }

            And("request for proceeding days throws error") {
                wireMockServer.setupProceedingDaysWithServerError(termNumber = 1)

                When("startFetch is performed") {
                    plugin.startFetch(jobId, jobStatusService, rawVoteCache)

                    Then("job should fail") {
                        jobStatusViewer.getJobStatus(jobId) shouldMatch voteFetchJob(
                            jobId = jobId,
                            steps = listOf(
                                startedImportStep(),
                            ),
                            jobState = JobState.FAILED,
                        )
                    }

                    And("raw vote cache is empty") {
                        rawVoteCache.get(polishSejmTenant) shouldBe emptyList()
                    }
                }
            }
        }

        And("has one past term") {
            wireMockServer.setupTerms {
                setupPastTerm(number = 1, from = "2000-06-01", to = "2001-05-30")
            }
            TODO()
        }

        And("has two past terms") {
            wireMockServer.setupTerms {
                setupPastTerm(number = 1, from = "2000-06-01", to = "2001-05-30")
                setupPastTerm(number = 2, from = "2001-06-01", to = "2002-05-30")
            }
            TODO()
        }

        And("has two active terms") {
            wireMockServer.setupTerms {
                setupActiveTerm(number = 1, from = "2000-06-01")
                setupActiveTerm(number = 2, from = "2001-06-01")
            }
            TODO()
        }

        And("request for terms throws error") {
            wireMockServer.setupTermsWithServerError()
            When("startFetch is performed") {
                plugin.startFetch(jobId, jobStatusService, rawVoteCache)

                Then("job should fail") {
                    jobStatusViewer.getJobStatus(jobId) shouldMatch voteFetchJob(
                        jobId = jobId,
                        steps = listOf(
                            startedImportStep(),
                        ),
                        jobState = JobState.FAILED,
                    )
                }

                And("raw vote cache is empty") {
                    rawVoteCache.get(polishSejmTenant) shouldBe emptyList()
                }
            }
        }
    }
})

private fun voteFetchJob(
    jobId: JobId,
    steps: List<JobStepStatus>,
    jobState: JobState = JobState.FINISHED,
): JobStatus =
    JobStatus(
        id = jobId,
        state = jobState,
        name = "test",
        description = "test",
        statusMessage = "Completed successfully",
        startedAt = Instant.now(),
        finishedAt = Instant.now(),
        steps = steps,
    )

private fun startedImportStep(): JobStepStatus =
    finishedStep(
        name = "Collecting terms",
        description = "Collecting term information from the API.",
    )

fun WireMockServer.setupTermsWithServerError() {
    this.stubFor(
        WireMock
            .get(WireMock.urlPathEqualTo("/sejm/term"))
            .willReturn(WireMock.serverError()),
    )
}

fun WireMockServer.setupTerms(configure: TermBuilder.() -> Unit) {
    val builder = TermBuilder()
    configure(builder)

    this.stubFor(
        WireMock
            .get(WireMock.urlPathEqualTo("/sejm/term"))
            .willReturn(jsonResponse(builder.termRequests, 200)),
    )
}

class TermBuilder {
    val termRequests = mutableListOf<SejmTermDto>()

    fun setupActiveTerm(
        number: Int,
        from: String,
    ) {
        termRequests.add(SejmTermDto(num = number, current = true, from = from.toLocalDate(), to = null))
    }

    fun setupPastTerm(
        number: Int,
        from: String,
        to: String,
    ) {
        termRequests.add(SejmTermDto(num = number, current = false, from = from.toLocalDate(), to = to.toLocalDate()))
    }
}

fun WireMockServer.setupProceedingDaysWithServerError(termNumber: Int) {
    this.stubFor(
        WireMock
            .get(WireMock.urlPathEqualTo("/sejm/term$termNumber/votings"))
            .willReturn(WireMock.serverError()),
    )
}

fun WireMockServer.setupProceedingDays(
    termNumber: Int,
    configure: ProceedingDayBuilder.() -> Unit,
) {
    val builder = ProceedingDayBuilder()
    configure(builder)

    this.stubFor(
        WireMock
            .get(WireMock.urlPathEqualTo("/sejm/term$termNumber/votings"))
            .willReturn(jsonResponse(builder.proceedingDayRequests, 200)),
    )
}

class ProceedingDayBuilder {
    val proceedingDayRequests = mutableListOf<SejmProceedingDayDto>()

    fun setupProceedingDay(
        number: Int,
        date: String,
        votingsCount: Int,
    ) {
        proceedingDayRequests.add(
            SejmProceedingDayDto(
                proceeding = number,
                date = date.toLocalDate(),
                votingsNum = votingsCount,
            ),
        )
    }
}

fun WireMockServer.setupSittingWithServerError(
    termNumber: Int,
    sittingNumber: Int,
) {
    this.stubFor(
        WireMock
            .get(WireMock.urlPathEqualTo("/sejm/term$termNumber/votings/$sittingNumber"))
            .willReturn(WireMock.serverError()),
    )
}

fun WireMockServer.setupSitting(
    termNumber: Int,
    sittingNumber: Int,
    configure: SittingBuilder.() -> Unit,
) {
    val builder = SittingBuilder(termNumber, sittingNumber)
    configure(builder)

    this.stubFor(
        WireMock
            .get(WireMock.urlPathEqualTo("/sejm/term$termNumber/votings/$sittingNumber"))
            .willReturn(jsonResponse(builder.votingDtos, 200)),
    )
}

class SittingBuilder(val termNumber: Int, val sittingNumber: Int) {
    val votingDtos = mutableListOf<SejmVotingDto>()

    fun setupElectronicVoting(
        votingNumber: Int,
        sittingDay: Int = 1,
        dateTime: String,
        yes: Int,
        no: Int,
        abstain: Int = 0,
        notParticipating: Int = 0,
        present: Int = yes + no + abstain,
        totalVoted: Int = yes + no,
    ) {
        votingDtos.add(
            SejmVotingDto(
                yes = yes,
                no = no,
                abstain = abstain,
                present = present,
                notParticipating = notParticipating,
                totalVoted = totalVoted,
                term = termNumber,
                sitting = sittingNumber,
                sittingDay = sittingDay,
                votingNumber = votingNumber,
                date = dateTime.toLocalDateTime(),
                title = "voting $termNumber/$sittingNumber/$votingNumber",
                description = "some description",
                topic = "some topic no $votingNumber",
                kind = SejmVotingKindDto.ELECTRONIC,
                majorityType = SejmVotingMajorityDto.SIMPLE_MAJORITY,
                majorityVotes = 230,
                votingOptions = null,
                againstAll = null,
            ),
        )
    }

    fun setupOnListVoting(
        votingNumber: Int,
        sittingDay: Int = 1,
        dateTime: String,
        totalVotes: Int,
        vararg options: Pair<String, Int>,
    ) {
        votingDtos.add(
            SejmVotingDto(
                yes = 0,
                no = 0,
                abstain = 0,
                present = 0,
                notParticipating = 1,
                totalVoted = totalVotes,
                term = termNumber,
                sitting = sittingNumber,
                sittingDay = sittingDay,
                votingNumber = votingNumber,
                date = dateTime.toLocalDateTime(),
                title = "voting $termNumber/$sittingNumber/$votingNumber",
                description = "some description",
                topic = "some topic no $votingNumber",
                kind = SejmVotingKindDto.ON_LIST,
                majorityType = SejmVotingMajorityDto.SIMPLE_MAJORITY,
                majorityVotes = 230,
                votingOptions = options.mapIndexed { index, option ->
                    SejmVotingOptionDto(
                        optionIndex = index,
                        option = option.first,
                        votes = option.second,
                    )
                },
                againstAll = null,
            ),
        )
    }
}

fun WireMockServer.setupVotingWithServerError(
    termNumber: Int,
    sittingNumber: Int,
    votingNumber: Int,
) {
    this.stubFor(
        WireMock
            .get(WireMock.urlPathEqualTo("/sejm/term$termNumber/votings/$sittingNumber/$votingNumber"))
            .willReturn(WireMock.serverError()),
    )
}

fun WireMockServer.setupVoting(
    termNumber: Int,
    sittingNumber: Int,
    votingNumber: Int,
    configure: VotingBuilder.() -> Unit,
) {
    val builder = VotingBuilder(termNumber, sittingNumber)
    configure(builder)

    this.stubFor(
        WireMock
            .get(WireMock.urlPathEqualTo("/sejm/term$termNumber/votings/$sittingNumber/$votingNumber"))
            .willReturn(jsonResponse(builder.build(), 200)),
    )
}

class VotingBuilder(val termNumber: Int, val sittingNumber: Int) {
    val votes = mutableListOf<SejmVoteDto>()
    var baseVotingDetails = SejmVotingDetailsDto()

    fun build() = baseVotingDetails.copy(votes = votes)

    fun withVote(
        name: String,
        party: String,
        vote: SejmVoteValueDto,
    ) {
        val nameSplit = name.split(" ")
        votes.add(
            SejmVoteDto(
                firstName = nameSplit.first(),
                lastName = nameSplit.lastOrNull(),
                secondName = if (nameSplit.size > 2) nameSplit[1] else null,
                club = party,
                vote = vote,
                listVotes = null,
                _MP = null,
                mP = null,
            ),
        )
    }

    fun withVote(
        name: String,
        party: String,
        listVotes: List<SejmVoteValueDto>,
    ) {
        val nameSplit = name.split(" ")
        votes.add(
            SejmVoteDto(
                firstName = nameSplit.first(),
                lastName = nameSplit.lastOrNull(),
                secondName = if (nameSplit.size > 2) nameSplit[1] else null,
                club = party,
                vote = null,
                listVotes = listVotes.mapIndexed { index, dto -> index.toString() to dto }.toMap(),
                _MP = null,
                mP = null,
            ),
        )
    }

    fun setupAsElectronicVoting(
        votingNumber: Int,
        sittingDay: Int = 1,
        dateTime: String,
        yes: Int,
        no: Int,
        abstain: Int = 0,
        notParticipating: Int = 0,
        present: Int = yes + no + abstain,
        totalVoted: Int = yes + no,
    ) {
        baseVotingDetails = SejmVotingDetailsDto(
            yes = yes,
            no = no,
            abstain = abstain,
            present = present,
            notParticipating = notParticipating,
            totalVoted = totalVoted,
            term = termNumber,
            sitting = sittingNumber,
            sittingDay = sittingDay,
            votingNumber = votingNumber,
            date = dateTime.toLocalDateTime(),
            title = "voting $termNumber/$sittingNumber/$votingNumber",
            description = "some description",
            topic = "some topic no $votingNumber",
            kind = SejmVotingKindDto.ELECTRONIC,
            majorityType = SejmVotingMajorityDto.SIMPLE_MAJORITY,
            majorityVotes = 230,
            votingOptions = null,
            againstAll = null,
        )
    }

    fun setupAsOnListVoting(
        votingNumber: Int,
        sittingDay: Int = 1,
        dateTime: String,
        totalVotes: Int,
        vararg options: Pair<String, Int>,
    ) {
        baseVotingDetails = SejmVotingDetailsDto(
            yes = 0,
            no = 0,
            abstain = 0,
            present = 0,
            notParticipating = 1,
            totalVoted = totalVotes,
            term = termNumber,
            sitting = sittingNumber,
            sittingDay = sittingDay,
            votingNumber = votingNumber,
            date = dateTime.toLocalDateTime(),
            title = "voting $termNumber/$sittingNumber/$votingNumber",
            description = "some description",
            topic = "some topic no $votingNumber",
            kind = SejmVotingKindDto.ON_LIST,
            majorityType = SejmVotingMajorityDto.SIMPLE_MAJORITY,
            majorityVotes = 230,
            votingOptions = options.mapIndexed { index, option ->
                SejmVotingOptionDto(
                    optionIndex = index,
                    option = option.first,
                    votes = option.second,
                )
            },
            againstAll = null,
        )
    }
}

fun String.toLocalDate(): LocalDate = LocalDate.parse(this)

fun String.toLocalDateTime(): LocalDateTime = LocalDateTime.parse(this)
