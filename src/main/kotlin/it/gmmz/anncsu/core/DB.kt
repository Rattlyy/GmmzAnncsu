package it.gmmz.anncsu.core

import klite.info
import klite.logger
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager

object DB {
    private val log = logger("DB")
    private val dbPath = Path.of("files", "dati.sqlite").toAbsolutePath().normalize()
    private var connectionRef: Connection? = null

    val connection: Connection
        @Synchronized get() {
            connectionRef?.takeIf { !it.isClosed }?.let { return it }
            return open()
        }

    @Synchronized
    private fun open(): Connection {
        Files.createDirectories(dbPath.parent)
        val opened = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        opened.createStatement().use { st ->
            st.execute("PRAGMA journal_mode = WAL;")
            st.execute("PRAGMA synchronous = normal;")
            st.execute("PRAGMA temp_store = memory;")
            st.execute("PRAGMA mmap_size = 30000000000;")
        }
        connectionRef = opened
        log.info("Opened sqlite connection: $dbPath")
        return opened
    }

    @Synchronized
    fun close() {
        connectionRef?.let {
            if (!it.isClosed) it.close()
            log.info("Closed sqlite connection: $dbPath")
        }
        connectionRef = null
    }
}


