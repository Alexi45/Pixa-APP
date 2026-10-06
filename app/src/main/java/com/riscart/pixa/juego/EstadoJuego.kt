package com.riscart.pixa.juego

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.riscart.pixa.engine.Cell
import com.riscart.pixa.engine.LineSolver
import com.riscart.pixa.engine.Puzzle

/** Qué hace tocar una casilla. */
enum class Modo { PINTAR, TACHAR }

/** Qué ha pasado al tocar, para saber qué vibración y qué sonido tocan. */
enum class Efecto { NADA, PIEZA, LINEA, FALLO, MARCA, VICTORIA }

/**
 * La partida en curso.
 *
 * Decisión de diseño: pintar está validado (si pintas donde no toca, cuenta
 * error y la casilla se tacha sola), pero tachar es libre. Así el tablero nunca
 * se queda en un estado sin solución y la partida fluye, que es lo que hace que
 * la gente siga jugando. Las cruces son apuntes personales, no se penalizan.
 */
class EstadoJuego(val puzzle: Puzzle, inicial: String? = null, segundosPrevios: Int = 0, erroresPrevios: Int = 0) {

    val rejilla = mutableStateListOf<Cell>().apply {
        val guardadas = inicial?.takeIf { it.length == puzzle.width * puzzle.height }
        repeat(puzzle.width * puzzle.height) { i ->
            add(
                when (guardadas?.get(i)) {
                    '1' -> Cell.FILLED
                    '2' -> Cell.CROSSED
                    else -> Cell.UNKNOWN
                },
            )
        }
    }

    var modo by mutableStateOf(Modo.PINTAR)
        private set

    var errores by mutableIntStateOf(erroresPrevios)
        private set

    var segundos by mutableIntStateOf(segundosPrevios)

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

    // ── Animación de asentado ───────────────────────────────────────────────
    // Cada pieza recién puesta entra con un pequeño rebote. Es medio segundo de
    // trabajo y es la diferencia entre "marcar casillas" y "colocar azulejos".

    private val asentando = mutableStateMapOf<Int, Long>()
    private var reloj by mutableLongStateOf(0L)

    val animando: Boolean get() = asentando.isNotEmpty()

    fun avanzarAnimacion(ahora: Long) {
        reloj = ahora
        val terminadas = asentando.filterValues { ahora - it > DURACION_ASENTADO }.keys
        terminadas.forEach { asentando.remove(it) }
    }

    /** 0 = recién puesta, 1 = ya asentada. */
    fun asentamiento(indice: Int): Float {
        val inicio = asentando[indice] ?: return 1f
        return ((reloj - inicio).toFloat() / DURACION_ASENTADO).coerceIn(0f, 1f)
    }

    fun cambiarModo(nuevo: Modo) {
        modo = nuevo
    }

    /**
     * Aplica la acción del modo actual sobre una casilla.
     * @return qué ha pasado, para el aviso háptico
     */
    fun tocar(x: Int, y: Int, arrastrando: Boolean = false): Efecto {
        if (completado) return Efecto.NADA
        val i = y * puzzle.width + x
        val actual = rejilla[i]
        // Una casilla ya resuelta bien no se toca más.
        if (actual == Cell.FILLED) return Efecto.NADA

        return when (modo) {
            Modo.PINTAR -> pintar(i, actual)
            Modo.TACHAR -> tachar(i, actual, arrastrando)
        }
    }

    private fun pintar(i: Int, actual: Cell): Efecto {
        if (puzzle.solution[i]) {
            registrar(i, actual)
            rejilla[i] = Cell.FILLED
            asentando[i] = System.currentTimeMillis()
            comprobarVictoria()
            if (completado) return Efecto.VICTORIA
            // Cerrar una línea entera merece su propio aviso: es el micrologro
            // del que tira todo el juego.
            val fila = i / puzzle.width
            val columna = i % puzzle.width
            return if (lineaCompleta(fila = fila) || lineaCompleta(columna = columna)) {
                Efecto.LINEA
            } else {
                Efecto.PIEZA
            }
        }
        // Pintaste donde no había: error, y la casilla queda tachada.
        if (actual != Cell.CROSSED) {
            registrar(i, actual)
            rejilla[i] = Cell.CROSSED
            errores++
            ultimoError = i
            return Efecto.FALLO
        }
        return Efecto.NADA
    }

    private fun tachar(i: Int, actual: Cell, arrastrando: Boolean): Efecto {
        // Un toque suelto alterna la cruz; arrastrando solo se marca, para no
        // ir borrando lo que acabas de poner al pasar el dedo.
        val destino = if (arrastrando || actual != Cell.CROSSED) Cell.CROSSED else Cell.UNKNOWN
        if (destino == actual) return Efecto.NADA
        registrar(i, actual)
        rejilla[i] = destino
        return Efecto.MARCA
    }

    fun limpiarUltimoError() {
        ultimoError = null
    }

    fun deshacer() {
        val (i, anterior) = historial.removeLastOrNull() ?: return
        rejilla[i] = anterior
        completado = false
    }

    /**
     * Revela una casilla, pero no una cualquiera: busca una que **se pueda
     * deducir ahora mismo** con lo que ya hay en el tablero. Así una pista no es
     * un regalo aleatorio, es enseñarte la jugada que tenías delante.
     */
    fun usarPista(): Boolean {
        if (completado) return false
        val elegida = casillaDeducible() ?: casillaAlAzar() ?: return false
        registrar(elegida, rejilla[elegida])
        rejilla[elegida] = Cell.FILLED
        asentando[elegida] = System.currentTimeMillis()
        pistasUsadas++
        comprobarVictoria()
        return true
    }

    /**
     * Pasa el solucionador por cada línea tomando como cierto solo lo que está
     * pintado (las cruces del jugador pueden estar mal puestas, y deducir a
     * partir de ellas daría pistas falsas).
     */
    private fun casillaDeducible(): Int? {
        val candidatas = mutableListOf<Int>()

        for (y in 0 until puzzle.height) {
            val linea = (0 until puzzle.width).map { x -> conocida(y * puzzle.width + x) }
            val resuelta = LineSolver.resolver(linea, puzzle.rowClues[y]) ?: continue
            for (x in 0 until puzzle.width) {
                val i = y * puzzle.width + x
                if (resuelta[x] == Cell.FILLED && rejilla[i] != Cell.FILLED) candidatas += i
            }
        }
        for (x in 0 until puzzle.width) {
            val linea = (0 until puzzle.height).map { y -> conocida(y * puzzle.width + x) }
            val resuelta = LineSolver.resolver(linea, puzzle.colClues[x]) ?: continue
            for (y in 0 until puzzle.height) {
                val i = y * puzzle.width + x
                if (resuelta[y] == Cell.FILLED && rejilla[i] != Cell.FILLED) candidatas += i
            }
        }

        // La que aparece por fila y por columna a la vez es la más "evidente":
        // es justo la que el jugador debería haber visto.
        return candidatas.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
    }

    private fun conocida(i: Int): Cell =
        if (rejilla[i] == Cell.FILLED) Cell.FILLED else Cell.UNKNOWN

    private fun casillaAlAzar(): Int? =
        rejilla.indices.filter { puzzle.solution[it] && rejilla[it] != Cell.FILLED }.randomOrNull()

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

    /** ¿Está ya puesta entera esta fila o columna? */
    fun lineaCompleta(fila: Int = -1, columna: Int = -1): Boolean = when {
        fila >= 0 -> (0 until puzzle.width).all { x ->
            val i = fila * puzzle.width + x
            !puzzle.solution[i] || rejilla[i] == Cell.FILLED
        }
        columna >= 0 -> (0 until puzzle.height).all { y ->
            val i = y * puzzle.width + columna
            !puzzle.solution[i] || rejilla[i] == Cell.FILLED
        }
        else -> false
    }

    /** El tablero en una cadena, para guardar la partida a medias. */
    fun serializar(): String = buildString {
        for (celda in rejilla) {
            append(
                when (celda) {
                    Cell.FILLED -> '1'
                    Cell.CROSSED -> '2'
                    Cell.UNKNOWN -> '0'
                },
            )
        }
    }

    /** ¿Merece la pena guardarla, o está recién empezada? */
    val valeLaPenaGuardar: Boolean
        get() = !completado && rejilla.any { it != Cell.UNKNOWN }

    companion object {
        private const val DURACION_ASENTADO = 170L
    }
}

/** Formatea segundos como 7:05 */
fun formatearTiempo(segundos: Int): String {
    val m = segundos / 60
    val s = segundos % 60
    return "%d:%02d".format(m, s)
}
