package com.riscart.pixa.engine

/**
 * Resuelve un nonograma entero aplicando el resolutor de líneas una y otra vez
 * (filas, luego columnas) hasta que deja de deducirse nada nuevo.
 *
 * Importante: aquí *no* se adivina. Si el puzzle se resuelve así, significa que
 * una persona también puede resolverlo razonando, sin probar a ciegas — y además
 * garantiza que la solución es única. Ese es el filtro de calidad del generador.
 */
object Solver {

    sealed interface Resultado {
        /** Se resolvió entero solo con lógica. */
        data class Resuelto(val rejilla: List<Cell>) : Resultado
        /** Las pistas son coherentes, pero haría falta adivinar para terminar. */
        data object Atascado : Resultado
        /** Las pistas se contradicen: no hay solución. */
        data object Imposible : Resultado
    }

    fun resolver(
        width: Int,
        height: Int,
        rowClues: List<List<Int>>,
        colClues: List<List<Int>>,
    ): Resultado {
        val rejilla = MutableList(width * height) { Cell.UNKNOWN }

        var huboCambio = true
        while (huboCambio) {
            huboCambio = false

            for (y in 0 until height) {
                val fila = (0 until width).map { x -> rejilla[y * width + x] }
                val deducida = LineSolver.resolver(fila, rowClues[y]) ?: return Resultado.Imposible
                for (x in 0 until width) {
                    if (deducida[x] != rejilla[y * width + x]) {
                        rejilla[y * width + x] = deducida[x]
                        huboCambio = true
                    }
                }
            }

            for (x in 0 until width) {
                val columna = (0 until height).map { y -> rejilla[y * width + x] }
                val deducida = LineSolver.resolver(columna, colClues[x]) ?: return Resultado.Imposible
                for (y in 0 until height) {
                    if (deducida[y] != rejilla[y * width + x]) {
                        rejilla[y * width + x] = deducida[y]
                        huboCambio = true
                    }
                }
            }
        }

        return if (rejilla.none { it == Cell.UNKNOWN }) {
            Resultado.Resuelto(rejilla)
        } else {
            Resultado.Atascado
        }
    }
}
