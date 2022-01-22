package model

import java.time.LocalDate

data class VotingDay(
    val cadence: Cadence,
    val date: LocalDate,
    val votingsInDay: Int = 0
)
