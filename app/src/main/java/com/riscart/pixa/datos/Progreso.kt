package com.riscart.pixa.datos

import android.content.Context
import java.time.LocalDate

/**
 * Lo que la app recuerda entre partidas: mejores tiempos, puzzles completados,
 * pistas disponibles y la racha del puzzle diario.
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

    private var ultimoDiario: String
        get() = prefs.getString("ultimoDiario", "") ?: ""
        set(v) = prefs.edit().putString("ultimoDiario", v).apply()

    fun mejorTiempo(tamano: Int): Int? =
        prefs.getInt("mejor_$tamano", 0).takeIf { it > 0 }

    fun diarioHechoHoy(hoy: LocalDate = LocalDate.now()): Boolean =
        ultimoDiario == hoy.toString()

    /** Registra una victoria y devuelve true si es un récord de tiempo. */
    fun registrarVictoria(tamano: Int, segundos: Int): Boolean {
        completados += 1
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

    companion object {
        const val PISTAS_INICIALES = 3
    }
}

/** La semilla del puzzle del día: la misma fecha da el mismo puzzle a todo el mundo. */
fun semillaDelDia(fecha: LocalDate = LocalDate.now()): Long =
    fecha.toEpochDay() * 2_654_435_761L
