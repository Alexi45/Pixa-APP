package com.riscart.pixa.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Atajo para escribir líneas: '#' pintada, '.' tachada, '?' sin saber. */
private fun linea(texto: String): List<Cell> = texto.map {
    when (it) {
        '#' -> Cell.FILLED
        '.' -> Cell.CROSSED
        else -> Cell.UNKNOWN
    }
}

private fun texto(linea: List<Cell>): String = linea.map {
    when (it) {
        Cell.FILLED -> '#'
        Cell.CROSSED -> '.'
        Cell.UNKNOWN -> '?'
    }
}.joinToString("")

class CluesTest {

    @Test
    fun `calcula las pistas de una linea`() {
        assertEquals(listOf(2, 1), cluesOf(listOf(false, true, true, false, true)))
        assertEquals(listOf(5), cluesOf(List(5) { true }))
        assertEquals(listOf(1, 1, 1), cluesOf(listOf(true, false, true, false, true)))
    }

    @Test
    fun `una linea vacia se representa con un cero`() {
        assertEquals(listOf(0), cluesOf(List(5) { false }))
    }

    @Test
    fun `las pistas de filas y columnas cuadran con la rejilla`() {
        // # .
        // # #
        val solucion = listOf(true, false, true, true)
        assertEquals(listOf(listOf(1), listOf(2)), rowCluesOf(solucion, 2, 2))
        assertEquals(listOf(listOf(2), listOf(1)), colCluesOf(solucion, 2, 2))
    }
}

class LineSolverTest {

    @Test
    fun `un bloque que ocupa toda la linea se deduce entero`() {
        val r = LineSolver.resolver(linea("?????"), listOf(5))
        assertEquals("#####", texto(r!!))
    }

    @Test
    fun `una linea sin pistas se tacha entera`() {
        val r = LineSolver.resolver(linea("?????"), listOf(0))
        assertEquals(".....", texto(r!!))
    }

    @Test
    fun `deduce el solape de un bloque grande`() {
        // Un bloque de 4 en 5 casillas: las 3 del medio están pintadas sí o sí.
        val r = LineSolver.resolver(linea("?????"), listOf(4))
        assertEquals("?###?", texto(r!!))
    }

    @Test
    fun `no deduce nada cuando hay varias opciones`() {
        val r = LineSolver.resolver(linea("?????"), listOf(1))
        assertEquals("?????", texto(r!!))
    }

    @Test
    fun `aprovecha las casillas ya conocidas`() {
        // Con la primera pintada, el bloque de 2 queda fijado y el resto se tacha.
        val r = LineSolver.resolver(linea("#????"), listOf(2))
        assertEquals("##...", texto(r!!))
    }

    @Test
    fun `respeta las casillas tachadas`() {
        // Con la casilla 3 tachada, el bloque de 3 solo cabe al principio.
        val r = LineSolver.resolver(linea("???.?"), listOf(3))
        assertEquals("###..", texto(r!!))
    }

    @Test
    fun `resuelve varios bloques encajados`() {
        val r = LineSolver.resolver(linea("???????"), listOf(3, 2))
        // 3+1+2 = 6 en 7 casillas: hay un hueco de holgura.
        assertEquals("?##??#?", texto(r!!))
    }

    @Test
    fun `detecta pistas imposibles`() {
        // No caben 3+3 en 5 casillas.
        assertNull(LineSolver.resolver(linea("?????"), listOf(3, 3)))
    }

    @Test
    fun `detecta contradiccion con el estado actual`() {
        // La línea entera debería ir pintada, pero hay una tachada.
        assertNull(LineSolver.resolver(linea("??.??"), listOf(5)))
    }

    @Test
    fun `nunca contradice lo que ya estaba puesto`() {
        val inicial = linea("#?#??")
        val r = LineSolver.resolver(inicial, listOf(1, 1, 1))
        assertNotNull(r)
        inicial.forEachIndexed { i, celda ->
            if (celda != Cell.UNKNOWN) assertEquals(celda, r!![i])
        }
    }
}

class SolverTest {

    @Test
    fun `resuelve un puzzle sencillo por logica`() {
        // # #
        // # #
        val solucion = listOf(true, true, true, true)
        val resultado = Solver.resolver(
            2, 2,
            rowCluesOf(solucion, 2, 2),
            colCluesOf(solucion, 2, 2),
        )
        assertTrue(resultado is Solver.Resultado.Resuelto)
    }

    @Test
    fun `la solucion que encuentra es la correcta`() {
        val solucion = listOf(
            true, false, true,
            false, true, false,
            true, true, true,
        )
        val resultado = Solver.resolver(
            3, 3,
            rowCluesOf(solucion, 3, 3),
            colCluesOf(solucion, 3, 3),
        )
        val resuelto = resultado as Solver.Resultado.Resuelto
        val comoBooleanos = resuelto.rejilla.map { it == Cell.FILLED }
        assertEquals(solucion, comoBooleanos)
    }

    @Test
    fun `detecta pistas contradictorias`() {
        // La fila pide 2 pero la columna dice que no hay nada pintado.
        val resultado = Solver.resolver(
            2, 1,
            rowClues = listOf(listOf(2)),
            colClues = listOf(listOf(0), listOf(0)),
        )
        assertEquals(Solver.Resultado.Imposible, resultado)
    }

    @Test
    fun `detecta los puzzles que obligarian a adivinar`() {
        // Clásico ambiguo: dos soluciones en diagonal, imposible decidir sin probar.
        val resultado = Solver.resolver(
            2, 2,
            rowClues = listOf(listOf(1), listOf(1)),
            colClues = listOf(listOf(1), listOf(1)),
        )
        assertEquals(Solver.Resultado.Atascado, resultado)
    }
}

class GeneratorTest {

    @Test
    fun `genera puzzles resolubles sin adivinar en todos los tamanos`() {
        for (tamano in listOf(5, 10, 15)) {
            val puzzle = Generator.generar(tamano, tamano, semilla = tamano * 7919L)
            val resultado = Solver.resolver(
                puzzle.width, puzzle.height, puzzle.rowClues, puzzle.colClues,
            )
            assertTrue(
                "Un puzzle de ${tamano}x$tamano debería resolverse solo con lógica",
                resultado is Solver.Resultado.Resuelto,
            )
        }
    }

    @Test
    fun `las pistas del puzzle corresponden a su solucion`() {
        val puzzle = Generator.generar(10, 10, semilla = 42)
        assertEquals(rowCluesOf(puzzle.solution, 10, 10), puzzle.rowClues)
        assertEquals(colCluesOf(puzzle.solution, 10, 10), puzzle.colClues)
    }

    @Test
    fun `la solucion unica coincide con la que se genero`() {
        val puzzle = Generator.generar(10, 10, semilla = 123)
        val resultado = Solver.resolver(
            puzzle.width, puzzle.height, puzzle.rowClues, puzzle.colClues,
        ) as Solver.Resultado.Resuelto
        assertEquals(puzzle.solution, resultado.rejilla.map { it == Cell.FILLED })
    }

    @Test
    fun `la misma semilla da siempre el mismo puzzle`() {
        // De esto depende que el puzzle del día salga igual para todo el mundo.
        val a = Generator.generar(10, 10, semilla = 20261006)
        val b = Generator.generar(10, 10, semilla = 20261006)
        assertEquals(a.solution, b.solution)
    }

    @Test
    fun `semillas distintas dan puzzles distintos`() {
        val a = Generator.generar(10, 10, semilla = 1)
        val b = Generator.generar(10, 10, semilla = 2)
        assertTrue(a.solution != b.solution)
    }

    @Test
    fun `no genera puzzles ni casi vacios ni casi llenos`() {
        repeat(12) { i ->
            val puzzle = Generator.generar(10, 10, semilla = i.toLong())
            val proporcion = puzzle.totalFilled.toDouble() / (puzzle.width * puzzle.height)
            assertTrue("Proporción rara: $proporcion", proporcion in 0.25..0.85)
        }
    }
}
