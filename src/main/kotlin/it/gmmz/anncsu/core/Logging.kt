package it.gmmz.anncsu.core

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.logging.Formatter
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.LogRecord
import java.util.logging.Logger

private const val FALLBACK_FORMAT = "[%1\$tF %1\$tT] [%4\$-7s] [%3\$s] %5\$s%6\$s%n"

object LoggingStyle {
    @Volatile
    private var configured = false

    @Synchronized
    fun configure() {
        if (configured) return

        val resource = Thread.currentThread().contextClassLoader.getResourceAsStream("logging.properties")
        if (resource != null) {
            resource.use { LogManager.getLogManager().readConfiguration(it) }
        } else {
            System.setProperty("java.util.logging.SimpleFormatter.format", FALLBACK_FORMAT)
        }

        Logger.getLogger("").handlers.forEach { it.formatter = AnsiLogFormatter() }

        configured = true
    }
}

class AnsiLogFormatter : Formatter() {
    override fun format(record: LogRecord): String {
        val ts = FORMATTER.format(Instant.ofEpochMilli(record.millis).atZone(ZoneId.systemDefault()))
        val level = record.level.name.padEnd(7)
        val logger = record.loggerName ?: "root"
        val message = formatMessage(record)
        val throwable = record.thrown?.let { "\n" + it.stackTraceToString() } ?: ""

        val levelColor = when {
            record.level.intValue() >= Level.SEVERE.intValue() -> RED
            record.level.intValue() >= Level.WARNING.intValue() -> YELLOW
            record.level.intValue() >= Level.INFO.intValue() -> GREEN
            else -> CYAN
        }

        return "$DIM[$ts]$RESET $levelColor[$level]$RESET $BLUE[$logger]$RESET $message$throwable\n"
    }

    companion object {
        private val FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        private const val RESET = "\u001B[0m"
        private const val DIM = "\u001B[2m"
        private const val BLUE = "\u001B[34m"
        private const val CYAN = "\u001B[36m"
        private const val GREEN = "\u001B[32m"
        private const val YELLOW = "\u001B[33m"
        private const val RED = "\u001B[31m"
    }
}


