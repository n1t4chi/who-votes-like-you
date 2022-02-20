package log

import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant

class ConsoleLogWriter : LogWriter {
    override fun write(time: Instant, loggerName: String, level: LogLevel, message: String, throwable: Throwable?) {
        println("[$time][$loggerName][$level] $message" + format(throwable))
    }
    
    private fun format(throwable: Throwable?) =
        if (throwable != null) {
            val stringWriter = StringWriter()
            throwable.printStackTrace(PrintWriter(stringWriter))
            "\n" + stringWriter.toString()
        } else {
            ""
        }
}