package vote.fetcher

import model.Cadence
import okhttp3.HttpUrl
import java.time.LocalDate

data class VotingsInDay(val cadence: Cadence, val date: LocalDate, val votingUrl: HttpUrl)