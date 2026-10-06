package com.riscart.pixa.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El catálogo tiene que cumplir lo mismo que los puzzles generados: que se pueda
 * terminar razonando, sin adivinar. Un dibujo bonito pero ambiguo es peor que no
 * tener dibujo, así que aquí se cae antes de llegar al jugador.
 */
class CatalogoTest {

    @Test
    fun `todos los dibujos son cuadrados y del tamano que dicen`() {
        for (dibujo in Catalogo.todos) {
            val lado = dibujo.filas.size
            assertTrue(
                "«${dibujo.nombre}» tiene filas de distinto ancho",
                dibujo.filas.all { it.length == lado },
            )
            assertTrue(
                "«${dibujo.nombre}» usa caracteres raros",
                dibujo.filas.all { fila -> fila.all { it == '#' || it == '.' } },
            )
        }
    }

    @Test
    fun `todos los dibujos se resuelven solo con logica`() {
        val fallan = Catalogo.todos.filter { dibujo ->
            val puzzle = dibujo.aPuzzle()
            val resultado = Solver.resolver(
                puzzle.width, puzzle.height, puzzle.rowClues, puzzle.colClues,
            )
            resultado !is Solver.Resultado.Resuelto ||
                resultado.rejilla != puzzle.solution.map { if (it) Cell.FILLED else Cell.CROSSED }
        }
        assertEquals(
            "Estos dibujos obligan a adivinar: ${fallan.map { "${it.nombre} (${it.lado})" }}",
            emptyList<String>(),
            fallan.map { it.nombre },
        )
    }

    @Test
    fun `ningun dibujo esta casi vacio ni casi lleno`() {
        for (dibujo in Catalogo.todos) {
            val puzzle = dibujo.aPuzzle()
            val proporcion = puzzle.totalFilled.toDouble() / (puzzle.width * puzzle.height)
            assertTrue(
                "«${dibujo.nombre}» (${dibujo.lado}) tiene una proporción rara: $proporcion",
                proporcion in 0.2..0.85,
            )
        }
    }

    @Test
    fun `no hay dos dibujos iguales en el mismo tamano`() {
        for (lado in listOf(5, 10, 15)) {
            val soluciones = Catalogo.porLado(lado).map { it.aPuzzle().solution }
            assertEquals(soluciones.size, soluciones.distinct().size)
        }
    }
}
