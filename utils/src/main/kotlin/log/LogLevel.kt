package log

enum class LogLevel(private val level: Int) {
    error(10), warning(20), info(30), trace(40), debug(50);
    
    companion object {
        fun find(nameToFind: String?, default: LogLevel) =
            if (nameToFind == null)
                default
            else
                values()
                    .find { it.name.equals(nameToFind, true) }
                    ?: default
    }
    
    fun canApply(threshold: LogLevel) = this.level <= threshold.level
}