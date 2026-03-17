package it.gmmz.anncsu.process

import it.gmmz.anncsu.core.DB
import klite.info
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipInputStream
import klite.logger
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

private val log = logger("IndexingProcess")

fun processIndexing(dataDir: Path = Path.of("files")) {
    log.info("Indexing started (dataDir=$dataDir)")
    val (zipPath, comuniPath) = if (Files.exists(dataDir.resolve("indirizziautocomplete_ita.zip")) && Files.exists(dataDir.resolve("Elenco-comuni-italiani.csv"))) {
        log.info("Source files already present, skipping download")
        dataDir.resolve("indirizziautocomplete_ita.zip") to dataDir.resolve("Elenco-comuni-italiani.csv")
    } else {
        log.info("Missing source files, downloading now")
        downloadFiles(dataDir)
    }

    val comuniByIstat = parseComuniCSV(comuniPath.toFile()).associateBy { it.codiceIstat }
    log.info("Loaded comuni map entries: ${comuniByIstat.size}")
    val anncsuCsv = extractAnncsuCsv(zipPath, dataDir)
    log.info("Using ANNCSU csv: $anncsuCsv")

    DB.connection.let { db ->
        log.info("Connected to sqlite shared singleton connection")
        db.createStatement().use { st ->
            log.info("Recreating STRADARIO and STRADARIO_FTS")
            st.execute("DROP TABLE IF EXISTS STRADARIO;")
            st.execute("DROP TABLE IF EXISTS STRADARIO_FTS;")
            st.execute("CREATE TABLE STRADARIO(codice_istat TEXT, via TEXT, civico TEXT, comune TEXT, regione TEXT, provincia TEXT, sigla_automobilistica TEXT);")
            st.execute("CREATE INDEX IF NOT EXISTS idx_civico ON STRADARIO(civico);")
        }

        db.autoCommit = false
        db.prepareStatement("INSERT INTO STRADARIO(codice_istat, via, civico, comune, regione, provincia, sigla_automobilistica) VALUES (?, ?, ?, ?, ?, ?, ?)").use { insert ->
            var rows = 0
            var skipped = 0
            parseAnncsuCSV(anncsuCsv.toFile()) { indirizzo ->
                val comune = comuniByIstat[indirizzo.codiceIstat] ?: run {
                    skipped++
                    if (skipped % 100000 == 0) log.info("Skipped rows without comune match: $skipped")
                    return@parseAnncsuCSV
                }
                insert.setString(1, indirizzo.codiceIstat)
                insert.setString(2, indirizzo.via)
                insert.setString(3, indirizzo.civico)
                insert.setString(4, comune.comune)
                insert.setString(5, comune.regione)
                insert.setString(6, comune.provincia)
                insert.setString(7, comune.siglaAutomobilistica)
                insert.addBatch()
                rows++
                if (rows % 5000 == 0) {
                    insert.executeBatch()
                    db.commit()
                }
                if (rows % 100000 == 0) log.info("Inserted rows so far: $rows")
            }
            insert.executeBatch()
            db.commit()
            log.info("Insert completed (rows=$rows, skippedNoComune=$skipped)")
        }

        db.autoCommit = true;
        db.createStatement().use { st ->
            log.info("Building FTS table")
            st.execute("CREATE VIRTUAL TABLE STRADARIO_FTS USING fts5(via, civico, comune, regione, provincia, sigla_automobilistica, content='STRADARIO', content_rowid='rowid', tokenize='trigram');")
            log.info("Created FTS table")
            st.execute("INSERT INTO STRADARIO_FTS(rowid, via, civico, comune, regione, provincia, sigla_automobilistica) SELECT rowid, via, civico, comune, regione, provincia, sigla_automobilistica FROM STRADARIO;")
            log.info("Inserted, optimizing...")
            st.execute("INSERT INTO STRADARIO_FTS(STRADARIO_FTS) VALUES('optimize');")
            log.info("FTS table ready")
        }
    }
    log.info("Indexing finished")
}

private fun extractAnncsuCsv(zipPath: Path, dataDir: Path): Path {
    log.info("Searching extracted ANNCSU csv in $dataDir")
    Files.newDirectoryStream(dataDir, "INDIR_ITA_*.csv").use { existing ->
        existing.firstOrNull()?.let {
            log.info("Found existing ANNCSU csv: $it")
            return it
        }
    }

    log.info("No extracted ANNCSU csv found, extracting from zip: $zipPath")
    ZipInputStream(zipPath.inputStream()).use { zip ->
        while (true) {
            val entry = zip.nextEntry ?: break
            if (!entry.isDirectory && entry.name.substringAfterLast('/').startsWith("INDIR_ITA_") && entry.name.endsWith(".csv", true)) {
                val target = dataDir.resolve(entry.name.substringAfterLast('/'))
                target.outputStream().use { zip.copyTo(it) }
                log.info("Extracted ANNCSU csv: $target")
                return target
            }
        }
    }
    error("INDIR_ITA csv not found inside ${zipPath.fileName}")
}