package vote.fetcher

import model.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.*
import org.mockito.Mockito
import java.time.LocalDate

class DirectVoteFetcherImplTest {
    val availableCadenceResolver: AvailableCadenceResolver = Mockito.mock(AvailableCadenceResolver::class.java)
    val votingsArchiveOpener: VotingsArchiveOpener = Mockito.mock(VotingsArchiveOpener::class.java)
    val votingsInDayOpener: VotingsInDayOpener = Mockito.mock(VotingsInDayOpener::class.java)
    val voteOpener: VoteOpener = Mockito.mock(VoteOpener::class.java)
    val partyVoteOpener: PartyVoteOpener = Mockito.mock(PartyVoteOpener::class.java)
    val voteFetcher: DirectVoteFetcherImpl = DirectVoteFetcherImpl(
        availableCadenceResolver,
        votingsArchiveOpener,
        votingsInDayOpener,
        voteOpener,
        partyVoteOpener
    )
    
    
    @Test
    @Throws(Exception::class)
    fun resolvesAllVotesCorrectly() {
        //prepare
        val cadence1 = Cadence(1,0)
        val cadence2 = Cadence(2,0)
        Mockito.doReturn(listOf(cadence1, cadence2)).`when`(availableCadenceResolver).getCurrentCadences()
        
        val date1 = LocalDate.of(2001, 1, 1)
        val votesInDayUrl1 = "http://votes.in.day/1".toHttpUrl()
        val date2 = LocalDate.of(2002, 2, 2)
        val votesInDayUrl2 = "http://votes.in.day/2".toHttpUrl()
        val date3 = LocalDate.of(2003, 3, 3)
        val votesInDayUrl3 = "http://votes.in.day/3".toHttpUrl()
        val date4 = LocalDate.of(2004, 4, 4)
        val votesInDayUrl4 = "http://votes.in.day/4".toHttpUrl()
        Mockito.doReturn(
            listOf(
                VotingsInDay(cadence1, date1, votesInDayUrl1),
                VotingsInDay(cadence1, date2, votesInDayUrl2)
            )
        ).`when`(votingsArchiveOpener).getVotingsInDayUrls(cadence1)
        Mockito.doReturn(
            listOf(
                VotingsInDay(cadence2, date3, votesInDayUrl3),
                VotingsInDay(cadence2, date4, votesInDayUrl4)
            )
        ).`when`(votingsArchiveOpener).getVotingsInDayUrls(cadence2)
        
        
        val voting11 = Voting("Głosowanie nr 11", 11, cadence1, date1,0)
        val voting11Url = "http://voting/11".toHttpUrl()
        val voting12 = Voting("Głosowanie nr 12", 12, cadence1, date1,0)
        val voting12Url = "http://voting/12".toHttpUrl()
        
        val voting21 = Voting("Głosowanie nr 21", 21, cadence1, date2,0)
        val voting21Url = "http://voting/21".toHttpUrl()
        val voting22 = Voting("Głosowanie nr 22", 22, cadence1, date2,0)
        val voting22Url = "http://voting/22".toHttpUrl()
        
        val voting31 = Voting("Głosowanie nr 31", 31, cadence2, date3,0)
        val voting31Url = "http://voting/31".toHttpUrl()
        val voting32 = Voting("Głosowanie nr 32", 32, cadence2, date3,0)
        val voting32Url = "http://voting/32".toHttpUrl()
        
        val voting41 = Voting("Głosowanie nr 41", 41, cadence2, date4,0)
        val voting41Url = "http://voting/41".toHttpUrl()
        val voting42 = Voting("Głosowanie nr 42", 42, cadence2, date4,0)
        val voting42Url = "http://voting/42".toHttpUrl()
        
        Mockito.doReturn(listOf(VotingWithUrl(voting11, voting11Url), VotingWithUrl(voting12, voting12Url))).`when`(votingsInDayOpener).fetchVotingUrls(VotingsInDay(cadence1, date1, votesInDayUrl1))
        Mockito.doReturn(listOf(VotingWithUrl(voting21, voting21Url), VotingWithUrl(voting22, voting22Url))).`when`(votingsInDayOpener).fetchVotingUrls(VotingsInDay(cadence1, date2, votesInDayUrl2))
        Mockito.doReturn(listOf(VotingWithUrl(voting31, voting31Url), VotingWithUrl(voting32, voting32Url))).`when`(votingsInDayOpener).fetchVotingUrls(VotingsInDay(cadence2, date3, votesInDayUrl3))
        Mockito.doReturn(listOf(VotingWithUrl(voting41, voting41Url), VotingWithUrl(voting42, voting42Url))).`when`(votingsInDayOpener).fetchVotingUrls(VotingsInDay(cadence2, date4, votesInDayUrl4))
        
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
        
        Mockito.doReturn(
            setOf(
                PartyVotingReference(voting11, party1, voting11party1Url),
                PartyVotingReference(voting11, party2, voting11party2Url)
            )
        ).`when`(voteOpener).fetchVotingUrlsForParties(VotingWithUrl(voting11, voting11Url))
        Mockito.doReturn(
            setOf(
                PartyVotingReference(voting12, party1, voting12party1Url),
                PartyVotingReference(voting12, party2, voting12party2Url)
            )
        ).`when`(voteOpener).fetchVotingUrlsForParties(VotingWithUrl(voting12, voting12Url))
        Mockito.doReturn(
            setOf(
                PartyVotingReference(voting21, party1, voting21party1Url),
                PartyVotingReference(voting21, party2, voting21party2Url)
            )
        ).`when`(voteOpener).fetchVotingUrlsForParties(VotingWithUrl(voting21, voting21Url))
        Mockito.doReturn(
            setOf(
                PartyVotingReference(voting22, party1, voting22party1Url),
                PartyVotingReference(voting22, party2, voting22party2Url)
            )
        ).`when`(voteOpener).fetchVotingUrlsForParties(VotingWithUrl(voting22, voting22Url))
        Mockito.doReturn(
            setOf(
                PartyVotingReference(voting31, party1, voting31party1Url),
                PartyVotingReference(voting31, party2, voting31party2Url)
            )
        ).`when`(voteOpener).fetchVotingUrlsForParties(VotingWithUrl(voting31, voting31Url))
        Mockito.doReturn(
            setOf(
                PartyVotingReference(voting32, party1, voting32party1Url),
                PartyVotingReference(voting32, party2, voting32party2Url)
            )
        ).`when`(voteOpener).fetchVotingUrlsForParties(VotingWithUrl(voting32, voting32Url))
        Mockito.doReturn(
            setOf(
                PartyVotingReference(voting41, party1, voting41party1Url),
                PartyVotingReference(voting41, party2, voting41party2Url)
            )
        ).`when`(voteOpener).fetchVotingUrlsForParties(VotingWithUrl(voting41, voting41Url))
        Mockito.doReturn(
            setOf(
                PartyVotingReference(voting42, party1, voting42party1Url),
                PartyVotingReference(voting42, party2, voting42party2Url)
            )
        ).`when`(voteOpener).fetchVotingUrlsForParties(VotingWithUrl(voting42, voting42Url))
        
        val person11 = Person("Grzegorz Dupny")
        val person12 = Person("Jan Dupowaty")
        val person21 = Person("Honoracjusz Kupsko")
        val person22 = Person("Genowefa Kupna")
        
        Mockito.doReturn(VotesForParty(party1, mapOf(Pair(person11, VoteResult.yes), Pair(person12, VoteResult.yes))))
            .`when`(partyVoteOpener).fetchVotesForParty(PartyVotingReference(voting11, party1, voting11party1Url))
        Mockito.doReturn(VotesForParty(party2, mapOf(Pair(person21, VoteResult.no), Pair(person22, VoteResult.no))))
            .`when`(partyVoteOpener).fetchVotesForParty(PartyVotingReference(voting11, party2, voting11party2Url))
        Mockito.doReturn(VotesForParty(party1, mapOf(Pair(person11, VoteResult.no), Pair(person12, VoteResult.no))))
            .`when`(partyVoteOpener).fetchVotesForParty(PartyVotingReference(voting12, party1, voting12party1Url))
        Mockito.doReturn(VotesForParty(party2, mapOf(Pair(person21, VoteResult.yes), Pair(person22, VoteResult.yes))))
            .`when`(partyVoteOpener).fetchVotesForParty(PartyVotingReference(voting12, party2, voting12party2Url))
        Mockito.doReturn(VotesForParty(party1, mapOf(Pair(person11, VoteResult.yes), Pair(person12, VoteResult.yes))))
            .`when`(partyVoteOpener).fetchVotesForParty(PartyVotingReference(voting21, party1, voting21party1Url))
        Mockito.doReturn(VotesForParty(party2, mapOf(Pair(person21, VoteResult.no), Pair(person22, VoteResult.no))))
            .`when`(partyVoteOpener).fetchVotesForParty(PartyVotingReference(voting21, party2, voting21party2Url))
        Mockito.doReturn(VotesForParty(party1, mapOf(Pair(person11, VoteResult.no), Pair(person12, VoteResult.no))))
            .`when`(partyVoteOpener).fetchVotesForParty(PartyVotingReference(voting22, party1, voting22party1Url))
        Mockito.doReturn(VotesForParty(party2, mapOf(Pair(person21, VoteResult.yes), Pair(person22, VoteResult.yes))))
            .`when`(partyVoteOpener).fetchVotesForParty(PartyVotingReference(voting22, party2, voting22party2Url))
        Mockito.doReturn(VotesForParty(party1, mapOf(Pair(person11, VoteResult.yes), Pair(person12, VoteResult.yes))))
            .`when`(partyVoteOpener).fetchVotesForParty(PartyVotingReference(voting31, party1, voting31party1Url))
        Mockito.doReturn(VotesForParty(party2, mapOf(Pair(person21, VoteResult.no), Pair(person22, VoteResult.no))))
            .`when`(partyVoteOpener).fetchVotesForParty(PartyVotingReference(voting31, party2, voting31party2Url))
        Mockito.doReturn(VotesForParty(party1, mapOf(Pair(person11, VoteResult.no), Pair(person12, VoteResult.no))))
            .`when`(partyVoteOpener).fetchVotesForParty(PartyVotingReference(voting32, party1, voting32party1Url))
        Mockito.doReturn(VotesForParty(party2, mapOf(Pair(person21, VoteResult.yes), Pair(person22, VoteResult.yes))))
            .`when`(partyVoteOpener).fetchVotesForParty(PartyVotingReference(voting32, party2, voting32party2Url))
        Mockito.doReturn(VotesForParty(party1, mapOf(Pair(person11, VoteResult.yes), Pair(person12, VoteResult.yes))))
            .`when`(partyVoteOpener).fetchVotesForParty(PartyVotingReference(voting41, party1, voting41party1Url))
        Mockito.doReturn(VotesForParty(party2, mapOf(Pair(person21, VoteResult.no), Pair(person22, VoteResult.no))))
            .`when`(partyVoteOpener).fetchVotesForParty(PartyVotingReference(voting41, party2, voting41party2Url))
        Mockito.doReturn(VotesForParty(party1, mapOf(Pair(person11, VoteResult.no), Pair(person12, VoteResult.no))))
            .`when`(partyVoteOpener).fetchVotesForParty(PartyVotingReference(voting42, party1, voting42party1Url))
        Mockito.doReturn(VotesForParty(party2, mapOf(Pair(person21, VoteResult.yes), Pair(person22, VoteResult.yes))))
            .`when`(partyVoteOpener).fetchVotesForParty(PartyVotingReference(voting42, party2, voting42party2Url))
        
        //execute
        val allVotes = voteFetcher.getAllVotes()
            .collectRemianing()
            .toCollection(mutableSetOf())
        
        //verify
        //no need to verify mock calls, if code gets all votes right, you can be sure it works
        Assertions.assertEquals(
            setOf(
                Vote(voting11, party1, person11, VoteResult.yes),
                Vote(voting11, party1, person12, VoteResult.yes),
                Vote(voting11, party2, person21, VoteResult.no),
                Vote(voting11, party2, person22, VoteResult.no),
                Vote(voting12, party1, person11, VoteResult.no),
                Vote(voting12, party1, person12, VoteResult.no),
                Vote(voting12, party2, person21, VoteResult.yes),
                Vote(voting12, party2, person22, VoteResult.yes),
                
                Vote(voting21, party1, person11, VoteResult.yes),
                Vote(voting21, party1, person12, VoteResult.yes),
                Vote(voting21, party2, person21, VoteResult.no),
                Vote(voting21, party2, person22, VoteResult.no),
                Vote(voting22, party1, person11, VoteResult.no),
                Vote(voting22, party1, person12, VoteResult.no),
                Vote(voting22, party2, person21, VoteResult.yes),
                Vote(voting22, party2, person22, VoteResult.yes),
                
                Vote(voting31, party1, person11, VoteResult.yes),
                Vote(voting31, party1, person12, VoteResult.yes),
                Vote(voting31, party2, person21, VoteResult.no),
                Vote(voting31, party2, person22, VoteResult.no),
                Vote(voting32, party1, person11, VoteResult.no),
                Vote(voting32, party1, person12, VoteResult.no),
                Vote(voting32, party2, person21, VoteResult.yes),
                Vote(voting32, party2, person22, VoteResult.yes),
                
                Vote(voting41, party1, person11, VoteResult.yes),
                Vote(voting41, party1, person12, VoteResult.yes),
                Vote(voting41, party2, person21, VoteResult.no),
                Vote(voting41, party2, person22, VoteResult.no),
                Vote(voting42, party1, person11, VoteResult.no),
                Vote(voting42, party1, person12, VoteResult.no),
                Vote(voting42, party2, person21, VoteResult.yes),
                Vote(voting42, party2, person22, VoteResult.yes),
            ),
            allVotes
        )
    }
}