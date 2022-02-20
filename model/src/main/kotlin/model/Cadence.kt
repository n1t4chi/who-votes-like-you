package model

data class Cadence(
    val number: Int,
    val status: CadenceStatus = CadenceStatus.unknown,
    val daysWithVotes: Int = -1
) {
    companion object {
        @JvmStatic
        fun newCadence(number: Int): Cadence = Cadence(number)
    }
    
    fun isActive(): Boolean = status == CadenceStatus.active
    
    fun sameNumber(other: Cadence): Boolean = this.number.equals(other.number)
}
