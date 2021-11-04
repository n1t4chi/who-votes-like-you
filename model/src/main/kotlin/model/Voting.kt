package model

import java.time.LocalDate

data class Voting(
    val name: String,
    val number: Int,
    val date: LocalDate
)
