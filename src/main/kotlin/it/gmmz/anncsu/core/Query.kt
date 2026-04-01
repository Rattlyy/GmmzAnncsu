package it.gmmz.anncsu.core

import it.gmmz.anncsu.core.DB.connection
import it.gmmz.anncsu.types.Indirizzo
import it.gmmz.anncsu.types.resultSetMapper
import klite.info
import klite.logger
import java.text.Normalizer

private val SIGLA_PATTERN = Regex("\\(([A-Za-z]{2,3})\\)")

object Query {
    private val log = logger("Query")

    fun search(rawInput: String, limit: Int = 30): List<Indirizzo> {
        val scopedQuery = buildFtsQuery(rawInput)
        val civicFilter = extractCivic(rawInput)
        if (scopedQuery == null) return emptyList()

        log.info("FTS scoped query: $scopedQuery | civic: $civicFilter")
        val scopedResults = executeSearch(scopedQuery, civicFilter, limit)
        if (scopedResults.isNotEmpty()) return scopedResults

        val fallbackQuery = buildGenericFtsQuery(rawInput) ?: return emptyList()
        log.info("Scoped query returned 0 rows, falling back to generic query: $fallbackQuery")
        return executeSearch(fallbackQuery, civicFilter, limit)
    }

    private fun executeSearch(matchQuery: String, civicFilter: String?, limit: Int): List<Indirizzo> {
        val whereClause = if (civicFilter != null) "AND STRADARIO_FTS.civico = ?" else ""
        val sql = """
            SELECT DISTINCT STRADARIO.*
            FROM STRADARIO_FTS
            JOIN STRADARIO ON STRADARIO.rowid = STRADARIO_FTS.rowid
            WHERE STRADARIO_FTS MATCH ?
            $whereClause
            LIMIT ?;
        """.trimIndent()

        connection.prepareStatement(sql).use { ps ->
            ps.setString(1, matchQuery)
            var idx = 2
            if (civicFilter != null) ps.setString(idx++, civicFilter)
            ps.setInt(idx, limit)

            return ps.executeQuery().use { rs ->
                mutableListOf<Indirizzo>().apply {
                    while (rs.next()) add(resultSetMapper(rs))
                }
            }
        }
    }

    private fun extractCivic(rawInput: String): String? {
        val parts = normalize(rawInput).split(Regex("[,\\s]+"))
        return parts.firstOrNull { it.any { c -> c.isDigit() } }
    }

    private fun buildFtsQuery(rawInput: String): String? {
        val parts = normalize(rawInput).replace(Regex("\\d+"), "").split(',').map { it.trim() }
            .filter { it.isNotBlank() }
        if (parts.isEmpty()) return null

        val fragments = mutableListOf<String>()

        for ((i, part) in parts.withIndex()) {
            val tokens = tokenize(part)
            if (tokens.isEmpty()) continue

            when (i) {
                0 -> fragments.add(scopedQuery("via", tokens))
                1 -> fragments.add(scopedQuery("comune", tokens))
                2 -> {
                    val sigla = SIGLA_PATTERN.find(part)?.groupValues?.get(1)?.uppercase()
                    if (sigla != null) {
                        fragments.add("sigla_automobilistica:\"${escape(sigla)}\"")
                    } else if (tokens.size == 1 && tokens[0].length <= 3) {
                        fragments.add("sigla_automobilistica:\"${escape(tokens[0].uppercase())}\"")
                    } else {
                        fragments.add(scopedQuery("provincia", tokens))
                    }
                }

                3 -> fragments.add(scopedQuery("regione", tokens))
            }
        }

        return fragments.takeIf { it.isNotEmpty() }?.joinToString(" ")
    }

    private fun buildGenericFtsQuery(rawInput: String): String? =
        tokenize(rawInput)
            .filterNot { it.any(Char::isDigit) }
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" ") { "\"${escape(it)}\"" }
}

private fun scopedQuery(column: String, tokens: List<String>): String =
    tokens.joinToString(" ") { "$column:\"${escape(it)}\"" }

private fun tokenize(value: String): List<String> = normalize(value)
    .split(Regex("\\s+"))
    .map { it.replace(Regex("[^\\p{L}\\p{N}]"), "") }
    .filter { it.isNotBlank() }

private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKC).trim()

private fun escape(value: String): String = value.replace("\"", "\"\"")

