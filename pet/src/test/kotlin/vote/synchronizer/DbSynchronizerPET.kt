package vote.synchronizer

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import model.VoteResult
import move.storage.*
import org.junit.jupiter.api.*
import vote.fetcher.*
import java.io.*
import java.time.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.*
import kotlin.math.ceil

class DbSynchronizerPET {
    companion object {
        val server = WireMockServer(WireMockConfiguration.wireMockConfig().port(0))
        
        val connector: TestDbConnector = TestDbConnector()
        val dbAccessor = DbAccessor(connector)
        
        @BeforeAll
        @JvmStatic
        fun setUp() {
            server.start()
        }
        
        @AfterAll
        @JvmStatic
        fun tearDown() {
            connector.close()
            server.stop()
        }
    }
    
    @BeforeEach
    fun reset() {
        connector.db.defaultDatabaseService().executeTransactionally("MATCH (m) DETACH DELETE (m)")
    }
    
    val synchronizer = Synchronizer(
        DirectVoteFetcherImpl(baseUrl = server.baseUrl()),
        DirectVoteStorageImpl(dbAccessor)
    )
    
    @Test
    fun checkInitializePerformance() {
        //prepare
        val daysPerCadence = 10
        val votesPerDay = 10
        val parties = 10
        val yesPerParty = 51
        val noPerParty = 30
        val absentPerParty = 20
        val abstainPerPart = 10
        for (cadence in 7..8) {
            addCadence(cadence, daysPerCadence, votesPerDay)
            for (day in 1..daysPerCadence) {
                addVotesInDay(cadence, day, votesPerDay)
                for (voting in 1..votesPerDay) {
                    addVoting(
                        cadence,
                        day,
                        voting,
                        parties,
                        yesPerParty,
                        noPerParty,
                        absentPerParty,
                        abstainPerPart
                    )
                    for( party in 1..parties ) {
                        addPartyVote(
                            cadence,
                            day,
                            voting,
                            party,
                            yesPerParty,
                            noPerParty,
                            absentPerParty,
                            abstainPerPart
                        )
                    }
                }
            }
        }
        
        //execute
        val start = LocalTime.now()
        synchronizer.initialize()
        val end = LocalTime.now()
        
        val duration = Duration.between(start, end)
        println("Duration of the initialize:" + duration.toSeconds() + "." + duration.toMillisPart() + " seconds.")
        //verify
        
        
    }
    
    private fun addCadence(cadence: Int, days: Int, votesPerDay: Int) {
        server.stubFor(
            WireMock.get("/agent.xsp?symbol=posglos&NrKadencji=$cadence")
                .willReturn(
                    WireMock.okXml(
                        readFile("/vote/synchronizer/cadenceTemplate.html").replace(
                            "{table}",
                            makeCadenceContent(
                                cadence,
                                days,
                                votesPerDay
                            )
                        )
                    )
                )
        )
    }
    
    private fun addVotesInDay(cadence: Int, day: Int, votesPerDay: Int) {
        server.stubFor(
            WireMock.get("/"+votingsInDayUrl(cadence, day))
                .willReturn(
                    WireMock.okXml(
                        readFile("/vote/synchronizer/votesInDayTemplate.html").replace(
                            "{table}",
                            makeVotesInDayContent(
                                cadence,
                                day,
                                votesPerDay
                            )
                        )
                    )
                )
        )
    }
    
    private fun addVoting(
        cadence: Int,
        day: Int,
        voting: Int,
        parties: Int,
        yesPerParty: Int,
        noPerParty: Int,
        absentPerParty: Int,
        abstainPerParty: Int
    ) {
        server.stubFor(
            WireMock.get("/"+votingUrl(cadence, day,voting))
                .willReturn(
                    WireMock.okXml(
                        votingContent(
                            cadence,
                            day,
                            voting,
                            parties,
                            yesPerParty,
                            noPerParty,
                            absentPerParty,
                            abstainPerParty
                        )
                    )
                )
        )
    }
    
    private fun addPartyVote(
        cadence: Int,
        day: Int,
        voting: Int,
        party: Int,
        yesPerParty: Int,
        noPerParty: Int,
        absentPerParty: Int,
        abstainPerParty: Int
    ) {
        server.stubFor(
            WireMock.get("/"+partyVotingUrl(cadence, day, voting, party))
                .willReturn(
                    WireMock.okXml(
                        readFile("/vote/synchronizer/votesInDayTemplate.html").replace(
                            "{table}",
                            makePartyVoteContent(
                                yesPerParty,
                                noPerParty,
                                absentPerParty,
                                abstainPerParty
                            )
                        )
                    )
                )
        )
    }
    
    private fun votingContent(
        cadence: Int,
        day: Int,
        voting: Int,
        parties: Int,
        yesPerParty: Int,
        noPerParty: Int,
        absentPerParty: Int,
        abstainPerParty: Int
    ): String {
        var fileContent = readFile("/vote/synchronizer/votingTemplate.html")
        fileContent = fileContent.replace(
            "{table}",
            makeVotesTable(cadence, day, voting, parties, yesPerParty, noPerParty, absentPerParty, abstainPerParty)
        )
        fileContent = fileContent.replace("{title}", votingTitle(cadence, day, voting))
        fileContent = fileContent.replace("{time}", votingTime(voting))
        fileContent = fileContent.replace("{date}", votingDay(cadence, day))
        return fileContent
    }
    
    private fun makeCadenceContent(cadence: Int, days: Int, votesPerDay: Int): String {
        val sb = StringBuilder()
        sb.append("<table>")
        sb.append("<thead>")
        sb.append("<tr>")
        sb.append("<th>Nr</th>")
        sb.append("<th>Data</th>")
        sb.append("<th>Liczba głosowań</th>")
        sb.append("</tr>")
        sb.append("</thead>")
        sb.append("<tbody>")
        
        
        for (day in 1..days) {
            sb.append("<tr>")
            sb.append("<td class=\"center\">$day</td>")
            sb.append("<td class=\"left\">")
            sb.append("<A HREF=\"${votingsInDayUrl(cadence, day)}\">")
            sb.append(votingDay(cadence, day))
            sb.append("</A>")
            sb.append("</td>")
            sb.append("<td class=\"right\">$votesPerDay</td>")
            sb.append("</tr>")
        }
        
        sb.append("</tbody>")
        sb.append("</table>")
        return sb.toString()
    }
    
    private fun makeVotesInDayContent(cadence: Int, day: Int, votesPerDay: Int): String {
        val sb = StringBuilder()
        sb.append("<table>")
        sb.append("<thead>")
        sb.append("<tr>")
        sb.append("<th>Nr</th>")
        sb.append("<th>godzina</th>")
        sb.append("<th>temat</th>")
        sb.append("</tr>")
        sb.append("</thead>")
        sb.append("<tbody>")
        
        for (voting in 1..votesPerDay) {
            sb.append("<tr>")
            
            sb.append("<TD class=\"bold\">")
            sb.append("<A HREF=\"${votingUrl(cadence, day, voting)}\">$voting</A>")
            sb.append("</TD>")
            
            sb.append("<TD>${votingTime(voting)}</TD>")
    
    
            sb.append("<TD class=\"left\">Głosowanie na tym posiedzeniu")
            sb.append("<A HREF=\"${votingUrl(cadence, day, voting)}\">${votingTitle(cadence, day, voting)}</A>")
            sb.append("</TD>")
            
            sb.append("</tr>")
        }
        
        sb.append("</tbody>")
        sb.append("</table>")
        return sb.toString()
    }
    
    private fun makeVotesTable(
        cadence: Int,
        day: Int,
        voting: Int,
        parties: Int,
        yesPerParty: Int,
        noPerParty: Int,
        absentPerParty: Int,
        abstainPerParty: Int
    ): String {
        val sb = StringBuilder()
        sb.append("<table class=\"kluby\">")
        sb.append("<thead>")
        sb.append("<tr>")
        sb.append("<th class=\"left\">Klub/Koło</th>")
        sb.append("<th>Liczba czł.</th>")
        sb.append("<th>Głosowało</th>")
        sb.append("<th>Za</th>")
        sb.append("<th>Przeciw</th>")
        sb.append("<th>Wstrzymało się</th>")
        sb.append("<th>Nie głosowało</th>")
        sb.append("</tr>")
        sb.append("</thead>")
        sb.append("<tbody class=\"center\">")
        
        for (party in 1..parties) {
            sb.append("<tr>")
            
            sb.append("<td class=\"center\">")
            sb.append("<a href=\"${partyVotingUrl(cadence, day, voting, party)}\"><strong>party$party</strong></a>")
            sb.append("</td")
            
            val all = yesPerParty + noPerParty + absentPerParty + abstainPerParty
            sb.append("<td class=\"center\">${all}</td>")
            sb.append("<td class=\"center\">${yesPerParty}</td>")
            sb.append("<td class=\"center\">${noPerParty}</td>")
            sb.append("<td class=\"center\">${abstainPerParty}</td>")
            sb.append("<td class=\"center\">${absentPerParty}</td>")
            sb.append("</tr>")
        }
        
        sb.append("</tbody>")
        sb.append("</table>")
        return sb.toString()
    }
    
    private fun makePartyVoteContent(
        yesPerParty: Int,
        noPerParty: Int,
        absentPerParty: Int,
        abstainPerParty: Int
    ): String {
        val sb = StringBuilder()
        sb.append("<table class=\"kluby\">")
        sb.append("<thead>")
        sb.append("<tr>")
        sb.append("<th>Lp.</th>")
        sb.append("<th class=\"left\">Nazwisko i imię</th>")
        sb.append("<th class=\"left\">Głos</th>")
        sb.append("<th>Lp.</th>")
        sb.append("<th class=\"left\">Nazwisko i imię</th>")
        sb.append("<th class=\"left\">Głos</th>")
        sb.append("</tr>")
        sb.append("</thead>")
        sb.append("<tbody>")
        
        val all = yesPerParty + noPerParty + absentPerParty + abstainPerParty
        val rows = ceil( all.div(2.0) ).toInt()
        
        val yesCounter = AtomicInteger(yesPerParty)
        val noCounter = AtomicInteger(noPerParty)
        val absentCounter = AtomicInteger(absentPerParty)
        val abstainCounter = AtomicInteger(abstainPerParty)
        
        for (row in 1..rows) {
            sb.append("<tr>")
            
            for( person in (2 * row - 1)..(2 * row)) {
                val vote: String = selectVote(yesCounter, noCounter, absentCounter, abstainCounter)
                if( vote.isNotBlank() ) {
                    sb.append("<td>$person</td>")
                    sb.append("<td>Person McPerson the $person</td>")
                    sb.append("<td class=\"left\" style=\"color: #990000;\">$vote</td>")
                }
            }
            
            
            sb.append("</tr>")
        }
        
        sb.append("</tbody>")
        sb.append("</table>")
        return sb.toString()
    }
    
    private fun selectVote(
        yesCounter: AtomicInteger,
        noCounter: AtomicInteger,
        absentCounter: AtomicInteger,
        abstainCounter: AtomicInteger
    ): String {
        return if (yesCounter.get() > 0) {
            yesCounter.decrementAndGet()
            VoteResult.yes.polishText
        } else if (noCounter.get() > 0) {
            noCounter.decrementAndGet()
            VoteResult.no.polishText
        } else if (absentCounter.get() > 0) {
            absentCounter.decrementAndGet()
            VoteResult.absent.polishText
        } else if (abstainCounter.get() > 0) {
            abstainCounter.decrementAndGet()
            VoteResult.abstain.polishText
        } else {
            ""
        }
    }
    
    private fun votingTime(voting: Int) = LocalTime.of(10, 1, 1)
        .plusMinutes(voting.toLong())
        .toString()
    
    
    private fun votingDay(cadence: Int, day: Int) = mapMonth(
        LocalDate.of(2000 + cadence, 1, 1)
            .plusDays(day.toLong())
            .format(VotingsArchiveOpener.dateTimeFormatter)
    )
    
    private fun votingTitle(cadence: Int, day: Int, voting: Int) = "Głosowanie nr $cadence/$day/$voting"
    
    private fun votingsInDayUrl(cadence: Int, day: Int) = "agent.xsp?symbol=listaglos&IdDnia=${cadence}_${day}"
    
    private fun votingUrl(cadence: Int, day: Int, voting: Int) =
        "agent.xsp?symbol=glosowania&NrKadencji=${cadence}&NrPosiedzenia=${day}&NrGlosowania=${voting}"
    
    private fun partyVotingUrl(cadence: Int, day: Int, voting: Int, party: Int) =
        "agent.xsp?symbol=klubglos&IdGlosowania=${cadence}_${day}_${voting}&KodKlubu=${party}"
    
    
    private fun mapMonth(value: String): String {
        var result = value
        for ((target, source) in VotingsArchiveOpener.monthMappers) {
            result = result.replace(source, target)
        }
        return result
    }
    
    
    private fun readFile(path: String): String {
        return readFileToStream(path).collect(Collectors.joining("\n"))
    }
    
    private fun readFileToStream(path: String): Stream<String> {
        val resourceAsStream = javaClass.getResourceAsStream(path)
        Assertions.assertNotNull(resourceAsStream, "No file found $path")
        val reader = BufferedReader(InputStreamReader(resourceAsStream!!, Charsets.UTF_8))
        return reader.lines()
    }
}