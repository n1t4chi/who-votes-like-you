package log

import java.time.Instant

class WhoLogger(private val name: String) {
    companion object WhoLogger {
        private fun name(func: () -> Unit): String {
            val name = func.javaClass.name
            return when {
                name.contains("Kt$") -> name.substringBefore("Kt$")
                name.contains("$") -> name.substringBefore("$")
                else -> name
            }
        }
        
        private val threshold = LogLevel.find(System.getProperty("who.logger.level"), LogLevel.info)
    }
    
    constructor(func: () -> Unit) : this(name(func))
    constructor(clazz: Class<*>) : this(clazz.name)
    
    private val logger = ConsoleLogWriter()
    
    fun debug(throwable: Throwable, supplier: () -> String) {
        log(LogLevel.debug, throwable, supplier)
    }
    
    fun debug(supplier: () -> String) {
        log(LogLevel.debug, supplier)
    }
    
    fun trace(throwable: Throwable, supplier: () -> String) {
        log(LogLevel.trace, throwable, supplier)
    }
    
    fun trace(supplier: () -> String) {
        log(LogLevel.trace, supplier)
    }
    
    fun info(throwable: Throwable, supplier: () -> String) {
        log(LogLevel.info, throwable, supplier)
    }
    
    fun info(supplier: () -> String) {
        log(LogLevel.info, supplier)
    }
    
    fun warn(throwable: Throwable, supplier: () -> String) {
        log(LogLevel.warning, throwable, supplier)
    }
    
    fun warn(supplier: () -> String) {
        log(LogLevel.warning, supplier)
    }
    
    fun error(throwable: Throwable, supplier: () -> String) {
        log(LogLevel.error, throwable, supplier)
    }
    
    fun error(supplier: () -> String) {
        log(LogLevel.error, supplier)
    }
    
    fun log(level: LogLevel, supplier: () -> String) {
        write(level, null, supplier)
    }
    
    fun log(level: LogLevel, throwable: Throwable, supplier: () -> String) {
        write(level, throwable, supplier)
    }
    
    private fun write(level: LogLevel, throwable: Throwable?, supplier: () -> String) {
        if (level.canApply(threshold)) {
            synchronized(WhoLogger) {
                logger.write(Instant.now(), name, level, supplier.invoke(), throwable)
            }
        }
    }
}