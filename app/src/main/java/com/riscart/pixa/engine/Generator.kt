package com.riscart.pixa.engine

import kotlin.random.Random

/**
 * Genera nonogramas que se pueden resolver razonando, sin adivinar.
 *
 * El método: pintar una rejilla al azar, calcular sus pistas y comprobar con el
 * [Solver] que se deja resolver solo con lógica. Si no, se descarta y se prueba
 * otra. Es el mismo filtro que distingue un buen juego de nonogramas de uno que
 * te obliga a probar suerte.
 *
 * Al depender solo de una semilla, el mismo número siempre da el mismo puzzle:
 * así el "puzzle del día" sale idéntico para todo el mundo sin necesitar servidor.
 */
object Generator {

    private const val MAX_INTENTOS = 600

    fun generar(
        width: Int,
        height: Int,
        semilla: Long,
        densidad: Double = 0.58,
    ): Puzzle {
        val random = Random(semilla)

        repeat(MAX_INTENTOS) { intento ->
            // Si cuesta encontrarlo, se van probando densidades algo distintas.
            val d = (densidad + (intento % 7 - 3) * 0.03).coerceIn(0.35, 0.72)
            val solucion = rejillaAleatoria(width, height, d, random)

            if (!esInteresante(solucion, width, height)) return@repeat

            val rowClues = rowCluesOf(solucion, width, height)
            val colClues = colCluesOf(solucion, width, height)

            val resultado = Solver.resolver(width, height, rowClues, colClues)
            if (resultado is Solver.Resultado.Resuelto) {
                return Puzzle(width, height, rowClues, colClues, solucion)
            }
        }

        // Último recurso: un patrón sencillo que siempre es resoluble.
        return puzzleDeRespaldo(width, height, random)
    }

    private fun rejillaAleatoria(
        width: Int,
        height: Int,
        densidad: Double,
        random: Random,
    ): List<Boolean> = List(width * height) { random.nextDouble() < densidad }

    /**
     * Descarta rejillas aburridas: casi vacías, casi llenas, o con demasiadas
     * líneas completamente en blanco.
     */
    private fun esInteresante(solucion: List<Boolean>, width: Int, height: Int): Boolean {
        val pintadas = solucion.count { it }
        val total = width * height
        val proporcion = pintadas.toDouble() / total
        if (proporcion < 0.3 || proporcion > 0.78) return false

        val filasVacias = (0 until height).count { y ->
            (0 until width).none { x -> solucion[y * width + x] }
        }
        val columnasVacias = (0 until width).count { x ->
            (0 until height).none { y -> solucion[y * width + x] }
        }
        val maxVacias = if (width <= 5) 1 else width / 4

        return filasVacias <= maxVacias && columnasVacias <= maxVacias
    }

    /** Patrón de tablero de ajedrez con bordes: siempre resoluble por lógica. */
    private fun puzzleDeRespaldo(width: Int, height: Int, random: Random): Puzzle {
        val desplazamiento = random.nextInt(2)
        val solucion = List(width * height) { indice ->
            val x = indice % width
            val y = indice / width
            x == 0 || y == 0 || (x + y + desplazamiento) % 2 == 0
        }
        return Puzzle(
            width = width,
            height = height,
            rowClues = rowCluesOf(solucion, width, height),
            colClues = colCluesOf(solucion, width, height),
            solution = solucion,
        )
    }
}
