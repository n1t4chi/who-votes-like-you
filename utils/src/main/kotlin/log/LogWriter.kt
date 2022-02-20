package log

import java.time.Instant

interface LogWriter {
    fun write(time: Instant, loggerName: String, level: LogLevel, message: String, throwable: Throwable? = null)
}