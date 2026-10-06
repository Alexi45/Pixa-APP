package com.riscart.pixa.juego

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.riscart.pixa.engine.Cell
import com.riscart.pixa.engine.Puzzle

/** Qué hace tocar una casilla. */
enum class Modo { PINTAR, TACHAR }

/**
 * La partida en curso.
 *
 * Decisión de diseño: pintar está validado (si pintas donde no toca, cuenta
 * error y la casilla se tacha sola), pero tachar es libre. Así el tablero nunca
 * se queda en un estado sin solución y la partida fluye, que es lo que hace que
 * la gente siga jugando. Las cruces son apuntes personales, no se penalizan.
 */
class EstadoJuego(val puzzle: Puzzle) {

    val rejilla = mutableStateListOf<Cell>().apply {
        repeat(puzzle.width * puzzle.height) { add(Cell.UNKNOWN) }
    }

    var modo by mutableStateOf(Modo.PINTAR)
        private set

    var errores by mutableIntStateOf(0)
        private set

    var segundos by mutableIntStateOf(0)

    var completado by mutableStateOf(false)
        private set

    var pistasUsadas by mutableIntStateOf(0)
        private set

    /** Última casilla tocada por error, para poder resaltarla un instante. */
    var ultimoError by mutableStateOf<Int?>(null)
        private set

    private val historial = ArrayDeque<Pair<Int, Cell>>()

    val puedeDeshacer: Boolean get() = historial.isNotEmpty()

    val pintadasCorrectas: Int
        get() = rejilla.indices.count { rejilla[it] == Cell.FILLED }

    val progreso: Float
        get() = if (puzzle.totalFilled == 0) 1f
        else pintadasCorrectas.toFloat() / puzzle.totalFilled

    fun cambiarModo(nuevo: Modo) {
        modo = nuevo
    }

    fun alternarModo() {
        modo = if (modo == Modo.PINTAR) Modo.TACHAR else Modo.PINTAR
    }

    /**
     * Aplica la acción del modo actual sobre una casilla.
     * @return true si algo cambió (para que el tablero sepa si vibrar/sonar)
     */
    fun tocar(x: Int, y: Int, arrastrando: Boolean = false): Boolean {
        if (completado) return false
        val i = y * puzzle.width + x
        val actual = rejilla[i]
        // Una casilla ya resuelta bien no se toca más.
        if (actual == Cell.FILLED) return false

        return when (modo) {
            Modo.PINTAR -> pintar(i, actual)
            Modo.TACHAR -> tachar(i, actual, arrastrando)
        }
    }

    private fun pintar(i: Int, actual: Cell): Boolean {
        if (puzzle.solution[i]) {
            registrar(i, actual)
            rejilla[i] = Cell.FILLED
            comprobarVictoria()
            return true
        }
        // Pintaste donde no había: error, y la casilla queda tachada.
        if (actual != Cell.CROSSED) {
            registrar(i, actual)
            rejilla[i] = Cell.CROSSED
            errores++
            ultimoError = i
            return true
        }
        return false
    }

    private fun tachar(i: Int, actual: Cell, arrastrando: Boolean): Boolean {
        // Un toque suelto alterna la cruz; arrastrando solo se marca, para no
        // ir borrando lo que acabas de poner al pasar el dedo.
        val destino = if (arrastrando || actual != Cell.CROSSED) Cell.CROSSED else Cell.UNKNOWN
        if (destino == actual) return false
        registrar(i, actual)
        rejilla[i] = destino
        return true
    }

    fun limpiarUltimoError() {
        ultimoError = null
    }

    fun deshacer() {
        val (i, anterior) = historial.removeLastOrNull() ?: return
        rejilla[i] = anterior
        completado = false
    }

    /** Revela una casilla pintada que aún no se haya descubierto. */
    fun usarPista(): Boolean {
        if (completado) return false
        val candidatas = rejilla.indices.filter { puzzle.solution[it] && rejilla[it] != Cell.FILLED }
        val elegida = candidatas.randomOrNull() ?: return false
        registrar(elegida, rejilla[elegida])
        rejilla[elegida] = Cell.FILLED
        pistasUsadas++
        comprobarVictoria()
        return true
    }

    private fun registrar(i: Int, anterior: Cell) {
        historial.addLast(i to anterior)
        if (historial.size > 200) historial.removeFirst()
    }

    private fun comprobarVictoria() {
        val gano = puzzle.solution.indices.all { i ->
            !puzzle.solution[i] || rejilla[i] == Cell.FILLED
        }
        if (gano) {
            completado = true
            // Al ganar, se tachan las casillas vacías que quedaran sueltas:
            // deja el dibujo limpio para la pantalla de victoria.
            for (i in rejilla.indices) {
                if (!puzzle.solution[i]) rejilla[i] = Cell.CROSSED
            }
        }
    }
}

/** Formatea segundos como 7:05 */
fun formatearTiempo(segundos: Int): String {
    val m = segundos / 60
    val s = segundos % 60
    return "%d:%02d".format(m, s)
}
