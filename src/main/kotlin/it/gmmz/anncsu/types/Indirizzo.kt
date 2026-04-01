package it.gmmz.anncsu.types

import java.sql.ResultSet

class Indirizzo(
    val codiceIstat: String,
    val comune: String,
    val regione: String,
    val provincia: String,
    val siglaAutomobilistica: String,

    val via: String,
    val civico: String,
) {
    val displayName: String
        get() = "$via $civico, $comune ($siglaAutomobilistica), $provincia, $regione"

    override fun toString() = displayName
}

fun resultSetMapper(rs: ResultSet) = Indirizzo(
    codiceIstat = rs.getString("codice_istat") ?: "",
    via = rs.getString("via") ?: "",
    civico = rs.getString("civico") ?: "",
    comune = rs.getString("comune") ?: "",
    provincia = rs.getString("provincia") ?: "",
    regione = rs.getString("regione") ?: "",
    siglaAutomobilistica = rs.getString("sigla_automobilistica") ?: "",
)