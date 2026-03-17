package it.gmmz.anncsu.types

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