package it.gmmz.anncsu.types

data class Comune(
    val codiceIstat: String,
    val comune: String,
    val regione: String,
    val provincia: String,
    val siglaAutomobilistica: String,
)