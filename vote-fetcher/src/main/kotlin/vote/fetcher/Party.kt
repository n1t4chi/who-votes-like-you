package vote.fetcher

data class Party(private val name: String) {
    override fun toString(): String {
        return name
    }
}