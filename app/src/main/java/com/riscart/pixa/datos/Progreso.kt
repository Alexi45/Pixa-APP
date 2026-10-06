package com.riscart.pixa.datos

import android.content.Context
import com.riscart.pixa.engine.Catalogo
import java.time.LocalDate

/**
 * Lo que la app recuerda entre partidas: mejores tiempos, puzzles completados,
 * pistas, la racha del diario, los dibujos ya descubiertos y la partida a medias.
 *
 * Con SharedPreferences basta: son unos pocos números y se leen de golpe al
 * arrancar. Meter una base de datos aquí sería pegar un tiro a una mosca.
 */
class Progreso(contexto: Context) {

    private val prefs = contexto.getSharedPreferences("pixa", Context.MODE_PRIVATE)

    var completados: Int
        get() = prefs.getInt("completados", 0)
        private set(v) = prefs.edit().putInt("completados", v).apply()

    var racha: Int
        get() = prefs.getInt("racha", 0)
        private set(v) = prefs.edit().putInt("racha", v).apply()

    var pistas: Int
        get() = prefs.getInt("pistas", PISTAS_INICIALES)
        private set(v) = prefs.edit().putInt("pistas", v.coerceAtLeast(0)).apply()

    var sinFallos: Int
        get() = prefs.getInt("sinFallos", 0)
        private set(v) = prefs.edit().putInt("sinFallos", v).apply()

    private var ultimoDiario: String
        get() = prefs.getString("ultimoDiario", "") ?: ""
        set(v) = prefs.edit().putString("ultimoDiario", v).apply()

    fun mejorTiempo(tamano: Int): Int? =
        prefs.getInt("mejor_$tamano", 0).takeIf { it > 0 }

    fun diarioHechoHoy(hoy: LocalDate = LocalDate.now()): Boolean =
        ultimoDiario == hoy.toString()

    // ── La colección de dibujos ────────────────────────────────────────────

    /** Claves "lado:nombre", porque el mismo nombre existe en varios tamaños. */
    val dibujosDescubiertos: Set<String>
        get() = prefs.getStringSet("dibujos", emptySet()) ?: emptySet()

    fun descubierto(lado: Int, nombre: String): Boolean =
        clave(lado, nombre) in dibujosDescubiertos

    fun descubrir(lado: Int, nombre: String) {
        val nuevos = dibujosDescubiertos + clave(lado, nombre)
        prefs.edit().putStringSet("dibujos", nuevos).apply()
    }

    val totalDibujos: Int get() = Catalogo.todos.size

    // ── Victorias ──────────────────────────────────────────────────────────

    /** Registra una victoria y devuelve true si es un récord de tiempo. */
    fun registrarVictoria(tamano: Int, segundos: Int, errores: Int): Boolean {
        completados += 1
        if (errores == 0) sinFallos += 1
        val anterior = mejorTiempo(tamano)
        val esRecord = anterior == null || segundos < anterior
        if (esRecord) {
            prefs.edit().putInt("mejor_$tamano", segundos).apply()
        }
        return esRecord
    }

    /**
     * Cierra el puzzle del día y actualiza la racha.
     * Si el anterior fue ayer la racha sigue; si no, vuelve a empezar.
     */
    fun registrarDiario(hoy: LocalDate = LocalDate.now()) {
        if (diarioHechoHoy(hoy)) return
        racha = if (ultimoDiario == hoy.minusDays(1).toString()) racha + 1 else 1
        ultimoDiario = hoy.toString()
        // Pequeña recompensa por mantener el hábito.
        pistas += 1
    }

    fun gastarPista(): Boolean {
        if (pistas <= 0) return false
        pistas -= 1
        return true
    }

    fun regalarPistas(cantidad: Int) {
        pistas += cantidad
    }

    // ── La partida a medias ────────────────────────────────────────────────

    /**
     * Guardar la partida en curso es lo que evita el peor momento posible:
     * llevas medio 15 × 15, te llaman por teléfono y al volver no hay nada.
     */
    fun guardarPartida(partida: PartidaGuardada) {
        prefs.edit().putString("partida", partida.serializar()).apply()
    }

    fun partidaGuardada(): PartidaGuardada? =
        prefs.getString("partida", null)?.let { PartidaGuardada.leer(it) }

    fun descartarPartida() {
        prefs.edit().remove("partida").apply()
    }

    // ── Primera vez ────────────────────────────────────────────────────────

    var sabeJugar: Boolean
        get() = prefs.getBoolean("sabeJugar", false)
        set(v) = prefs.edit().putBoolean("sabeJugar", v).apply()

    companion object {
        const val PISTAS_INICIALES = 3

        fun clave(lado: Int, nombre: String) = "$lado:$nombre"
    }
}

/**
 * Una partida a medias, en una sola línea de texto.
 *
 * Se guarda lo justo para reconstruirla: de dónde salía el puzzle (un dibujo del
 * catálogo o una semilla), qué llevas puesto y el tiempo.
 */
data class PartidaGuardada(
    val tamano: Int,
    val semilla: Long,
    val dibujo: String?,
    val titulo: String,
    val esDiario: Boolean,
    val celdas: String,
    val segundos: Int,
    val errores: Int,
) {
    fun serializar(): String = listOf(
        tamano, semilla, dibujo ?: "", titulo.replace(SEPARADOR, " "),
        if (esDiario) 1 else 0, celdas, segundos, errores,
    ).joinToString(SEPARADOR)

    companion object {
        private const val SEPARADOR = "\u0001"

        fun leer(texto: String): PartidaGuardada? {
            val partes = texto.split(SEPARADOR)
            if (partes.size != 8) return null
            return runCatching {
                PartidaGuardada(
                    tamano = partes[0].toInt(),
                    semilla = partes[1].toLong(),
                    dibujo = partes[2].ifEmpty { null },
                    titulo = partes[3],
                    esDiario = partes[4] == "1",
                    celdas = partes[5],
                    segundos = partes[6].toInt(),
                    errores = partes[7].toInt(),
                )
            }.getOrNull()
        }
    }
}

/** La semilla del puzzle del día: la misma fecha da el mismo puzzle a todo el mundo. */
fun semillaDelDia(fecha: LocalDate = LocalDate.now()): Long =
    fecha.toEpochDay() * 2_654_435_761L
