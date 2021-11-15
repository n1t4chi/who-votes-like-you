package vote.fetcher

import model.*
import java.time.*
import java.util.*
import java.util.concurrent.Executors

class InitialVoteFetcher(
    private val cadenceResolver: AvailableCadenceResolver,
    private val votingsArchiveOpener: VotingsArchiveOpener,
    private val votingsInDayOpener: VotingsInDayOpener,
    private val voteOpener: VoteOpener,
    private val partyVoteOpener: PartyVoteOpener
) : VoteStream {
    
    private val cadenceService = Executors.newFixedThreadPool(1)
    private val fetchVotingsInDayService = Executors.newFixedThreadPool(2)
    private val fetchVotinsWithUrlService = Executors.newFixedThreadPool(4)
    private val fetchPartyVotesWithUrlService = Executors.newFixedThreadPool(8)
    private val fetchVotesService = Executors.newFixedThreadPool(16)
    private val services = listOf(
        cadenceService,
        fetchVotingsInDayService,
        fetchVotinsWithUrlService,
        fetchPartyVotesWithUrlService,
        fetchVotesService
    )
    
    private val start : LocalTime = LocalTime.now()
    
    private val cadencesQueue: TerminableQueue<Cadence> = TerminableQueue("cadences",1)
    private val votingsInDayQueue: TerminableQueue<VotingsInDay> = TerminableQueue("votingsInDay",2)
    private val votingWithUrlQueue: TerminableQueue<VotingWithUrl> = TerminableQueue("votingWithUrl",4)
    private val partyVotingReferenceQueue: TerminableQueue<PartyVotingReference> = TerminableQueue("partyVotingReference",8)
    private val votes: TerminableQueue<Vote> = TerminableQueue("votes",16)
    
    override fun next(): Optional<Vote> {
        val tryNext = votes.tryNext()
        if( tryNext.isEmpty ) {
            services.forEach{it.shutdown()}
            val end = LocalTime.now()
            val duration = Duration.between(start,end)
            println("Finished fetching votes in $duration")
        }
        return tryNext
    }
    
    
    fun start() {
        for(i in 1..1)
        cadenceService.submit { catchErrors { fetchCadencesToQueue() } }
        for(i in 1..2)
            fetchVotingsInDayService.submit { catchErrors { fetchVotingsInDayToQueue() } }
        for(i in 1..4)
            fetchVotinsWithUrlService.submit { catchErrors { fetchVotingsWithUrlToQueue() } }
        for(i in 1..8)
            fetchPartyVotesWithUrlService.submit { catchErrors { fetchPartyVotesWithUrlToQueue() } }
        for(i in 1..16)
            fetchVotesService.submit { catchErrors { fetchVotesToQueue() } }
    }
    
    fun catchErrors(exec: ()->Unit) {
        try {
            exec.invoke()
        } catch (ex:Throwable) {
            ex.printStackTrace()
            throw ex
        }
    }
    
    fun fetchCadencesToQueue() {
        println("Resolving cadences")
        for (cadence in cadenceResolver.getCurrentCadences()) {
            cadencesQueue.put(cadence)
        }
        println("Finished adding cadences")
        cadencesQueue.terminate()
    }
    
    fun fetchVotingsInDayToQueue() {
        println("Resolving VotingsInDay")
        while (true) {
            val optional = cadencesQueue.tryNext()
            if (optional.isEmpty)
                break
            val cadence = optional.get()
            val listOfVotingsInDay = votingsArchiveOpener.getVotingsInDayUrls(cadence)
            println("Adding ${listOfVotingsInDay.size} VotingsInDay for $cadence")
            for (votingsInDay in listOfVotingsInDay) {
                votingsInDayQueue.put(votingsInDay)
            }
        }
        println("Finished adding VotingsInDay")
        votingsInDayQueue.terminate()
    }
    
    fun fetchVotingsWithUrlToQueue() {
        println("Resolving VotingsWithUrl")
        while (true) {
            val optional = votingsInDayQueue.tryNext()
            if (optional.isEmpty)
                break
            val votingInDay = optional.get()
            val votingsWithUrls = votingsInDayOpener.fetchVotingUrls(votingInDay)
            println("Adding ${votingsWithUrls.size} VotingsWithUrl for $votingInDay")
            for (votingWithUrl in votingsWithUrls) {
                votingWithUrlQueue.put(votingWithUrl)
            }
        }
        println("Finished adding VotingsWithUrl")
        votingWithUrlQueue.terminate()
    }
    
    fun fetchPartyVotesWithUrlToQueue() {
        println("Resolving PartyVotesWithUrl")
        while (true) {
            val optional = votingWithUrlQueue.tryNext()
            if (optional.isEmpty)
                break
            val votingWithUrl = optional.get()
            val partiesWithVotingUrls = voteOpener.fetchVotingUrlsForParties(votingWithUrl)
            for (partyWithVotingUrl in partiesWithVotingUrls) {
                partyVotingReferenceQueue.put(partyWithVotingUrl)
            }
        }
        println("Finished adding PartyVotesWithUrl")
        partyVotingReferenceQueue.terminate()
    }
    
    fun fetchVotesToQueue() {
        println("Resolving Votes")
        while (true) {
            val optional = partyVotingReferenceQueue.tryNext()
            if(optional.isEmpty)
                break
            val partyVotingReference = optional.get()
            val votes = getVotes(partyVotingReference)
            for (vote in votes) {
                this.votes.put(vote)
            }
        }
        println("Finished adding Votes")
        votes.terminate()
    }
    
    private fun getVotes(partyVotingReference: PartyVotingReference): HashSet<Vote> {
        val votesForParty = partyVoteOpener.fetchVotesForParty(partyVotingReference)
        val votes = HashSet<Vote>()
        for ((person, voteResult) in votesForParty.getVotes()) {
            votes.add(Vote(partyVotingReference.voting, partyVotingReference.party, person, voteResult))
        }
        return votes
    }
    
}

