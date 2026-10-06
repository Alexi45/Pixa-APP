package com.riscart.pixa.engine

/** Estado de una casilla mientras se resuelve. */
enum class Cell { UNKNOWN, FILLED, CROSSED }

/**
 * Un nonograma: la rejilla oculta más las pistas de filas y columnas.
 *
 * [solution] va en orden de filas (row-major): solution[y * width + x].
 */
data class Puzzle(
    val width: Int,
    val height: Int,
    val rowClues: List<List<Int>>,
    val colClues: List<List<Int>>,
    val solution: List<Boolean>,
) {
    init {
        require(width > 0 && height > 0) { "La rejilla no puede estar vacía" }
        require(solution.size == width * height) { "La solución no cuadra con el tamaño" }
        require(rowClues.size == height) { "Faltan pistas de filas" }
        require(colClues.size == width) { "Faltan pistas de columnas" }
    }

    fun filledAt(x: Int, y: Int): Boolean = solution[y * width + x]

    /** Cuántas casillas hay que pintar en total. */
    val totalFilled: Int get() = solution.count { it }
}

/** Calcula las pistas de una línea: [false,true,true,false,true] -> [2,1] */
fun cluesOf(line: List<Boolean>): List<Int> {
    val pistas = mutableListOf<Int>()
    var run = 0
    for (filled in line) {
        if (filled) {
            run++
        } else if (run > 0) {
            pistas += run
            run = 0
        }
    }
    if (run > 0) pistas += run
    // Una línea vacía se representa como [0], que es como se dibuja en los juegos.
    return if (pistas.isEmpty()) listOf(0) else pistas
}

/** Las pistas de todas las filas de una rejilla. */
fun rowCluesOf(solution: List<Boolean>, width: Int, height: Int): List<List<Int>> =
    (0 until height).map { y ->
        cluesOf((0 until width).map { x -> solution[y * width + x] })
    }

/** Las pistas de todas las columnas de una rejilla. */
fun colCluesOf(solution: List<Boolean>, width: Int, height: Int): List<List<Int>> =
    (0 until width).map { x ->
        cluesOf((0 until height).map { y -> solution[y * width + x] })
    }
