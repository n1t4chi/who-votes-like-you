package vote.fetcher

import model.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.*
import org.mockito.Mockito
import java.time.LocalDate

class DirectVoteFetcherImplTest {
    val availableCadenceResolver: AvailableCadenceResolver = Mockito.mock(AvailableCadenceResolver::class.java)
    val votingsArchiveOpener: VotingsArchiveOpener = Mockito.mock(VotingsArchiveOpener::class.java)
    val votesInDayOpener: VotesInDayOpener = Mockito.mock(VotesInDayOpener::class.java)
    val voteOpener: VoteOpener = Mockito.mock(VoteOpener::class.java)
    val partyVoteOpener: PartyVoteOpener = Mockito.mock(PartyVoteOpener::class.java)
    val voteFetcher: DirectVoteFetcherImpl = DirectVoteFetcherImpl(
        availableCadenceResolver,
        votingsArchiveOpener,
        votesInDayOpener,
        voteOpener,
        partyVoteOpener
    )

    @Test
    @Throws(Exception::class)
    fun resolvesAllVotesCorrectly() {
        //prepare
        val cadence1 = Cadence(1)
        val cadence2 = Cadence(2)
        Mockito.doReturn(listOf(cadence1, cadence2)).`when`(availableCadenceResolver).getCurrentCadences()

        val date1 = LocalDate.of(2001, 1, 1)
        val votesInDayUrl1 = "http://votes.in.day/1".toHttpUrl()
        val date2 = LocalDate.of(2002, 2, 2)
        val votesInDayUrl2 = "http://votes.in.day/2".toHttpUrl()
        val date3 = LocalDate.of(2003, 3, 3)
        val votesInDayUrl3 = "http://votes.in.day/3".toHttpUrl()
        val date4 = LocalDate.of(2004, 4, 4)
        val votesInDayUrl4 = "http://votes.in.day/4".toHttpUrl()
        Mockito.doReturn(listOf(Pair(date1, votesInDayUrl1), Pair(date2, votesInDayUrl2))).`when`(votingsArchiveOpener).getVotesInDayUrls(cadence1)
        Mockito.doReturn(listOf(Pair(date3, votesInDayUrl3), Pair(date4, votesInDayUrl4))).`when`(votingsArchiveOpener).getVotesInDayUrls(cadence2)


        val voting11 = Voting("Głosowanie nr 11", 11, cadence1, date1)
        val voting11Url = "http://voting/11".toHttpUrl()
        val voting12 = Voting("Głosowanie nr 12", 12, cadence1, date1)
        val voting12Url = "http://voting/12".toHttpUrl()

        val voting21 = Voting("Głosowanie nr 21", 21, cadence1, date2)
        val voting21Url = "http://voting/21".toHttpUrl()
        val voting22 = Voting("Głosowanie nr 22", 22, cadence1, date2)
        val voting22Url = "http://voting/22".toHttpUrl()

        val voting31 = Voting("Głosowanie nr 31", 31, cadence2, date3)
        val voting31Url = "http://voting/31".toHttpUrl()
        val voting32 = Voting("Głosowanie nr 32", 32, cadence2, date3)
        val voting32Url = "http://voting/32".toHttpUrl()

        val voting41 = Voting("Głosowanie nr 41", 41, cadence2, date4)
        val voting41Url = "http://voting/41".toHttpUrl()
        val voting42 = Voting("Głosowanie nr 42", 42, cadence2, date4)
        val voting42Url = "http://voting/42".toHttpUrl()

        Mockito.doReturn(listOf(Pair(voting11, voting11Url), Pair(voting12, voting12Url))).`when`(votesInDayOpener).fetchVotingUrls(votesInDayUrl1, cadence1, date1)
        Mockito.doReturn(listOf(Pair(voting21, voting21Url), Pair(voting22, voting22Url))).`when`(votesInDayOpener).fetchVotingUrls(votesInDayUrl2, cadence1, date2)
        Mockito.doReturn(listOf(Pair(voting31, voting31Url), Pair(voting32, voting32Url))).`when`(votesInDayOpener).fetchVotingUrls(votesInDayUrl3, cadence2, date3)
        Mockito.doReturn(listOf(Pair(voting41, voting41Url), Pair(voting42, voting42Url))).`when`(votesInDayOpener).fetchVotingUrls(votesInDayUrl4, cadence2, date4)

        val party1 = Party("Dupa2024")
        val party2 = Party("Koalicja Kupy")

        val voting11party1Url = "http://voting/11/d".toHttpUrl()
        val voting11party2Url = "http://voting/11/kk".toHttpUrl()
        val voting12party1Url = "http://voting/12/d".toHttpUrl()
        val voting12party2Url = "http://voting/12/kk".toHttpUrl()
        val voting21party1Url = "http://voting/21/d".toHttpUrl()
        val voting21party2Url = "http://voting/21/kk".toHttpUrl()
        val voting22party1Url = "http://voting/22/d".toHttpUrl()
        val voting22party2Url = "http://voting/22/kk".toHttpUrl()
        val voting31party1Url = "http://voting/31/d".toHttpUrl()
        val voting31party2Url = "http://voting/31/kk".toHttpUrl()
        val voting32party1Url = "http://voting/32/d".toHttpUrl()
        val voting32party2Url = "http://voting/32/kk".toHttpUrl()
        val voting41party1Url = "http://voting/41/d".toHttpUrl()
        val voting41party2Url = "http://voting/41/kk".toHttpUrl()
        val voting42party1Url = "http://voting/42/d".toHttpUrl()
        val voting42party2Url = "http://voting/42/kk".toHttpUrl()

        Mockito.doReturn(mapOf(Pair(party1, voting11party1Url), Pair(party2, voting11party2Url))).`when`(voteOpener).fetchVotingUrlsForParties(voting11Url)
        Mockito.doReturn(mapOf(Pair(party1, voting12party1Url), Pair(party2, voting12party2Url))).`when`(voteOpener).fetchVotingUrlsForParties(voting12Url)
        Mockito.doReturn(mapOf(Pair(party1, voting21party1Url), Pair(party2, voting21party2Url))).`when`(voteOpener).fetchVotingUrlsForParties(voting21Url)
        Mockito.doReturn(mapOf(Pair(party1, voting22party1Url), Pair(party2, voting22party2Url))).`when`(voteOpener).fetchVotingUrlsForParties(voting22Url)
        Mockito.doReturn(mapOf(Pair(party1, voting31party1Url), Pair(party2, voting31party2Url))).`when`(voteOpener).fetchVotingUrlsForParties(voting31Url)
        Mockito.doReturn(mapOf(Pair(party1, voting32party1Url), Pair(party2, voting32party2Url))).`when`(voteOpener).fetchVotingUrlsForParties(voting32Url)
        Mockito.doReturn(mapOf(Pair(party1, voting41party1Url), Pair(party2, voting41party2Url))).`when`(voteOpener).fetchVotingUrlsForParties(voting41Url)
        Mockito.doReturn(mapOf(Pair(party1, voting42party1Url), Pair(party2, voting42party2Url))).`when`(voteOpener).fetchVotingUrlsForParties(voting42Url)

        val person11 = Person( "Grzegorz Dupny" )
        val person12 = Person( "Jan Dupowaty" )
        val person21 = Person( "Honoracjusz Kupsko" )
        val person22 = Person( "Genowefa Kupna" )

        Mockito.doReturn(VotesForParty(party1,mapOf(Pair(person11,VoteResult.yes),Pair(person12,VoteResult.yes)))).`when`(partyVoteOpener).fetchVotingUrlsForParties(party1,voting11party1Url)
        Mockito.doReturn(VotesForParty(party2,mapOf(Pair(person21,VoteResult.no ),Pair(person22,VoteResult.no )))).`when`(partyVoteOpener).fetchVotingUrlsForParties(party2,voting11party2Url)
        Mockito.doReturn(VotesForParty(party1,mapOf(Pair(person11,VoteResult.no ),Pair(person12,VoteResult.no )))).`when`(partyVoteOpener).fetchVotingUrlsForParties(party1,voting12party1Url)
        Mockito.doReturn(VotesForParty(party2,mapOf(Pair(person21,VoteResult.yes),Pair(person22,VoteResult.yes)))).`when`(partyVoteOpener).fetchVotingUrlsForParties(party2,voting12party2Url)

        Mockito.doReturn(VotesForParty(party1,mapOf(Pair(person11,VoteResult.yes),Pair(person12,VoteResult.yes)))).`when`(partyVoteOpener).fetchVotingUrlsForParties(party1,voting21party1Url)
        Mockito.doReturn(VotesForParty(party2,mapOf(Pair(person21,VoteResult.no ),Pair(person22,VoteResult.no )))).`when`(partyVoteOpener).fetchVotingUrlsForParties(party2,voting21party2Url)
        Mockito.doReturn(VotesForParty(party1,mapOf(Pair(person11,VoteResult.no ),Pair(person12,VoteResult.no )))).`when`(partyVoteOpener).fetchVotingUrlsForParties(party1,voting22party1Url)
        Mockito.doReturn(VotesForParty(party2,mapOf(Pair(person21,VoteResult.yes),Pair(person22,VoteResult.yes)))).`when`(partyVoteOpener).fetchVotingUrlsForParties(party2,voting22party2Url)

        Mockito.doReturn(VotesForParty(party1,mapOf(Pair(person11,VoteResult.yes),Pair(person12,VoteResult.yes)))).`when`(partyVoteOpener).fetchVotingUrlsForParties(party1,voting31party1Url)
        Mockito.doReturn(VotesForParty(party2,mapOf(Pair(person21,VoteResult.no ),Pair(person22,VoteResult.no )))).`when`(partyVoteOpener).fetchVotingUrlsForParties(party2,voting31party2Url)
        Mockito.doReturn(VotesForParty(party1,mapOf(Pair(person11,VoteResult.no ),Pair(person12,VoteResult.no )))).`when`(partyVoteOpener).fetchVotingUrlsForParties(party1,voting32party1Url)
        Mockito.doReturn(VotesForParty(party2,mapOf(Pair(person21,VoteResult.yes),Pair(person22,VoteResult.yes)))).`when`(partyVoteOpener).fetchVotingUrlsForParties(party2,voting32party2Url)

        Mockito.doReturn(VotesForParty(party1,mapOf(Pair(person11,VoteResult.yes),Pair(person12,VoteResult.yes)))).`when`(partyVoteOpener).fetchVotingUrlsForParties(party1,voting41party1Url)
        Mockito.doReturn(VotesForParty(party2,mapOf(Pair(person21,VoteResult.no ),Pair(person22,VoteResult.no )))).`when`(partyVoteOpener).fetchVotingUrlsForParties(party2,voting41party2Url)
        Mockito.doReturn(VotesForParty(party1,mapOf(Pair(person11,VoteResult.no ),Pair(person12,VoteResult.no )))).`when`(partyVoteOpener).fetchVotingUrlsForParties(party1,voting42party1Url)
        Mockito.doReturn(VotesForParty(party2,mapOf(Pair(person21,VoteResult.yes),Pair(person22,VoteResult.yes)))).`when`(partyVoteOpener).fetchVotingUrlsForParties(party2,voting42party2Url)

        //execute
        val allVotes = voteFetcher.getAllVotes()
            .asSequence()
            .toCollection(mutableSetOf())

        //verify
        //no need to verify mock calls, if code gets all votes right, you can be sure it works
        Assertions.assertEquals(
            setOf(
                Vote(voting11,person11,VoteResult.yes,party1),
                Vote(voting11,person12,VoteResult.yes,party1),
                Vote(voting11,person21,VoteResult.no, party2),
                Vote(voting11,person22,VoteResult.no, party2),
                Vote(voting12,person11,VoteResult.no, party1),
                Vote(voting12,person12,VoteResult.no, party1),
                Vote(voting12,person21,VoteResult.yes,party2),
                Vote(voting12,person22,VoteResult.yes,party2),

                Vote(voting21,person11,VoteResult.yes,party1),
                Vote(voting21,person12,VoteResult.yes,party1),
                Vote(voting21,person21,VoteResult.no, party2),
                Vote(voting21,person22,VoteResult.no, party2),
                Vote(voting22,person11,VoteResult.no, party1),
                Vote(voting22,person12,VoteResult.no, party1),
                Vote(voting22,person21,VoteResult.yes,party2),
                Vote(voting22,person22,VoteResult.yes,party2),

                Vote(voting31,person11,VoteResult.yes,party1),
                Vote(voting31,person12,VoteResult.yes,party1),
                Vote(voting31,person21,VoteResult.no, party2),
                Vote(voting31,person22,VoteResult.no, party2),
                Vote(voting32,person11,VoteResult.no, party1),
                Vote(voting32,person12,VoteResult.no, party1),
                Vote(voting32,person21,VoteResult.yes,party2),
                Vote(voting32,person22,VoteResult.yes,party2),

                Vote(voting41,person11,VoteResult.yes,party1),
                Vote(voting41,person12,VoteResult.yes,party1),
                Vote(voting41,person21,VoteResult.no, party2),
                Vote(voting41,person22,VoteResult.no, party2),
                Vote(voting42,person11,VoteResult.no, party1),
                Vote(voting42,person12,VoteResult.no, party1),
                Vote(voting42,person21,VoteResult.yes,party2),
                Vote(voting42,person22,VoteResult.yes,party2),
            ),
            allVotes
        )
    }
}