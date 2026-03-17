package it.gmmz.anncsu

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.default
import it.gmmz.anncsu.core.LoggingStyle
import it.gmmz.anncsu.core.server
import it.gmmz.anncsu.process.downloadFiles
import it.gmmz.anncsu.process.processIndexing
import klite.info
import klite.logger
import java.nio.file.Path

private val log = logger("Main")

private class DebugCli : CliktCommand(name = "debug") {
    private val process by argument(help = "init | serve | server").choice("init", "serve", "server")
    private val dataDir by option("--data-dir", help = "working directory for files/db").default("files")

    override fun run() {
        val dir = Path.of(dataDir)

        when (process) {
            "init" -> {
                log.info("Dispatching init (download + build)")
                downloadFiles(dir)
                processIndexing(dir)
                log.info("Init completed")
            }

            "serve", "server" -> {
                log.info("Dispatching server")
                server(8080)
                log.info("Server completed")
            }
        }
    }
}

fun main(args: Array<String>) {
    LoggingStyle.configure()
    DebugCli().main(args)
}
