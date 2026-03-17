package it.gmmz.anncsu.process

import com.univocity.parsers.csv.CsvParser
import com.univocity.parsers.csv.CsvParserSettings
import it.gmmz.anncsu.types.Comune
import it.gmmz.anncsu.types.Indirizzo
import klite.info
import klite.logger
import java.io.File

private val log = logger("ParseCSV")

private fun findIndex(header: List<String>, vararg names: String): Int =
    header.indexOfFirst { h -> names.all { h.contains(it, true) } }
        .takeIf { it >= 0 }
        ?: throw IllegalArgumentException("Missing required column: ${names.joinToString(", ")}")

fun parseComuniCSV(csv: File): List<Comune> {
    log.info("parseComuniCSV started (${csv.absolutePath})")
    val parser = CsvParser(CsvParserSettings().apply {
        format.delimiter = ';'
        maxCharsPerColumn = -1
    })
    parser.beginParsing(csv, Charsets.ISO_8859_1)

    val header = parser.parseNext()?.map { normalizeHeader(it ?: "") }
        ?: throw IllegalArgumentException("Empty comuni csv")

    val codiceIstatIndex = findIndex(header, "codice", "catastale", "comune")
    val comuneIndex = findIndex(header, "denominazione", "italiano")
    val regioneIndex = findIndex(header, "denominazione", "regione")
    val provinciaIndex = findIndex(header, "denominazione", "territoriale", "sovracomunale")
    val siglaAutomobilisticaIndex = findIndex(header, "sigla", "automobilistica")
    log.info("Comuni header indexes: codice=$codiceIstatIndex comune=$comuneIndex regione=$regioneIndex provincia=$provinciaIndex sigla=$siglaAutomobilisticaIndex")

    val rows = mutableListOf<Comune>()
    var parsedRows = 0
    while (true) {
        val row = parser.parseNext() ?: break
        rows += Comune(
            codiceIstat = row.getOrElse(codiceIstatIndex) { "" }.trim(),
            comune = row.getOrElse(comuneIndex) { "" }.trim(),
            regione = row.getOrElse(regioneIndex) { "" }.trim(),
            provincia = row.getOrElse(provinciaIndex) { "" }.trim(),
            siglaAutomobilistica = row.getOrElse(siglaAutomobilisticaIndex) { "" }.trim(),
        )
        parsedRows++
        if (parsedRows % 10000 == 0) log.info("Comuni rows parsed: $parsedRows")
    }
    log.info("parseComuniCSV completed (rows=${rows.size})")
    return rows
}

private fun normalizeHeader(value: String) = value.lowercase().replace("\n", " ").replace(Regex("\\s+"), " ").trim()

fun parseAnncsuCSV(
    csv: File,
    parseLine: ((Indirizzo) -> Unit)
) {
    log.info("parseAnncsuCSV started (${csv.absolutePath})")
    val parser = CsvParser(CsvParserSettings().apply {
        format.delimiter = ';'
        maxCharsPerColumn = -1
    })
    parser.beginParsing(csv, Charsets.ISO_8859_1)
    log.info("Processing started")

    var parsedRows = 0L
    var skippedRows = 0L
    val header = parser.parseNext()?.map { normalizeHeader(it ?: "") }
        ?: throw IllegalArgumentException("Empty ANNCSU csv")
    val codiceComuneIndex = findIndex(header, "codice", "comune")
    val codiceIstatIndex = findIndex(header, "codice", "istat")
    val viaIndex = findIndex(header, "odonimo")
    val civicoIndex = findIndex(header, "civico")
    log.info("ANNCSU header indexes: codiceComune=$codiceComuneIndex codiceIstat=$codiceIstatIndex via=$viaIndex civico=$civicoIndex")

    while (true) {
        val row = parser.parseNext() ?: break
        if (row.size <= maxOf(codiceComuneIndex, codiceIstatIndex, viaIndex, civicoIndex)) {
            skippedRows++
            if (skippedRows % 100000 == 0L) log.info("ANNCSU malformed/short rows skipped: $skippedRows")
            continue
        }

        val codiceComune = row[codiceComuneIndex]?.trim().orEmpty()
        val codiceIstat = row[codiceIstatIndex]?.trim().orEmpty()
        val via = row[viaIndex]?.trim().orEmpty()
        val civico = row[civicoIndex]?.trim().orEmpty()
        if (codiceComune.isEmpty() || via.isEmpty()) {
            skippedRows++
            if (skippedRows % 100000 == 0L) log.info("ANNCSU malformed/short rows skipped: $skippedRows")
            continue
        }

        parseLine(
            Indirizzo(
                codiceIstat = codiceComune,
                comune = codiceIstat,
                regione = "",
                provincia = "",
                siglaAutomobilistica = "",
                via = via,
                civico = civico
            )
        )
        parsedRows++
        if (parsedRows % 100000 == 0L) log.info("ANNCSU rows parsed: $parsedRows")
    }

    log.info("Processing ended (parsed=$parsedRows, skipped=$skippedRows)")
}