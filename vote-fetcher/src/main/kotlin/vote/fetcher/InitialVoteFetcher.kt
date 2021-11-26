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
    
    private val cadenceThreads = 1
    private val votingsInDayThreads = 3
    private val votingsWithUrlThreads = 6
    private val partyVotesThreads = 12
    private val votesThreads = 32
    
    private val cadenceService = Executors.newFixedThreadPool(cadenceThreads)
    private val fetchVotingsInDayService = Executors.newFixedThreadPool(votingsInDayThreads)
    private val fetchVotingsWithUrlService = Executors.newFixedThreadPool(votingsWithUrlThreads)
    private val fetchPartyVotesWithUrlService = Executors.newFixedThreadPool(partyVotesThreads)
    private val fetchVotesService = Executors.newFixedThreadPool(votesThreads)
    private val services = listOf(
        cadenceService,
        fetchVotingsInDayService,
        fetchVotingsWithUrlService,
        fetchPartyVotesWithUrlService,
        fetchVotesService
    )
    
    private val start : LocalTime = LocalTime.now()
    
    private val cadencesQueue: TerminableQueue<Cadence> = TerminableQueue("cadences",cadenceThreads)
    private val votingsInDayQueue: TerminableQueue<VotingsInDay> = TerminableQueue("votingsInDay",votingsInDayThreads)
    private val votingWithUrlQueue: TerminableQueue<VotingWithUrl> = TerminableQueue("votingWithUrl",votingsWithUrlThreads)
    private val partyVotingReferenceQueue: TerminableQueue<PartyVotingReference> = TerminableQueue("partyVotingReference",partyVotesThreads)
    private val votes: TerminableQueue<Vote> = TerminableQueue("votes",votesThreads)
    
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
        for(i in 1..cadenceThreads)
            cadenceService.submit { catchErrors { fetchCadencesToQueue() } }
        for(i in 1..votingsInDayThreads)
            fetchVotingsInDayService.submit { catchErrors { fetchVotingsInDayToQueue() } }
        for(i in 1..votingsWithUrlThreads)
            fetchVotingsWithUrlService.submit { catchErrors { fetchVotingsWithUrlToQueue() } }
        for(i in 1..partyVotesThreads)
            fetchPartyVotesWithUrlService.submit { catchErrors { fetchPartyVotesWithUrlToQueue() } }
        for(i in 1..votesThreads)
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

