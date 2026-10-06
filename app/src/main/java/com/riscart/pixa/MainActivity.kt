package com.riscart.pixa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.riscart.pixa.anuncios.GestorAnuncios
import com.riscart.pixa.datos.PartidaGuardada
import com.riscart.pixa.datos.Progreso
import com.riscart.pixa.datos.semillaDelDia
import com.riscart.pixa.engine.Catalogo
import com.riscart.pixa.engine.Generator
import com.riscart.pixa.engine.Puzzle
import com.riscart.pixa.ui.Dificultad
import com.riscart.pixa.ui.PantallaInicio
import com.riscart.pixa.ui.PantallaJuego
import com.riscart.pixa.ui.TemaPixa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

/** Qué se está mostrando ahora mismo. */
private sealed interface Pantalla {
    data object Inicio : Pantalla

    data class Jugando(
        val tamano: Int,
        val semilla: Long,
        /** Si viene del catálogo, el nombre del dibujo; si no, se genera. */
        val dibujo: String?,
        val titulo: String,
        val esDiario: Boolean,
        val celdas: String? = null,
        val segundos: Int = 0,
        val errores: Int = 0,
    ) : Pantalla
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val anuncios = GestorAnuncios(this).also { it.iniciar() }
        setContent {
            TemaPixa {
                App(anuncios)
            }
        }
    }
}

@Composable
private fun App(anuncios: GestorAnuncios) {
    val contexto = LocalContext.current
    val progreso = remember { Progreso(contexto) }

    var pantalla by remember { mutableStateOf<Pantalla>(Pantalla.Inicio) }

    // Se recargan de SharedPreferences tras cada partida.
    var completados by remember { mutableIntStateOf(progreso.completados) }
    var racha by remember { mutableIntStateOf(progreso.racha) }
    var pistas by remember { mutableIntStateOf(progreso.pistas) }
    var diarioHecho by remember { mutableStateOf(progreso.diarioHechoHoy()) }
    var descubiertos by remember { mutableStateOf(progreso.dibujosDescubiertos) }
    var guardada by remember { mutableStateOf(progreso.partidaGuardada()) }

    fun refrescar() {
        completados = progreso.completados
        racha = progreso.racha
        pistas = progreso.pistas
        diarioHecho = progreso.diarioHechoHoy()
        descubiertos = progreso.dibujosDescubiertos
        guardada = progreso.partidaGuardada()
    }

    /**
     * Para cada dificultad se sirve primero un dibujo del catálogo que aún no
     * hayas descubierto. Cuando se acaban, se vuelve a los generados, que son
     * infinitos: así nunca te quedas sin jugar, pero mientras haya dibujos hay
     * algo que coleccionar.
     */
    fun nuevaPartida(dificultad: Dificultad): Pantalla.Jugando {
        val pendientes = Catalogo.porLado(dificultad.tamano)
            .filterNot { progreso.descubierto(dificultad.tamano, it.nombre) }
        return Pantalla.Jugando(
            tamano = dificultad.tamano,
            semilla = Random.nextLong(),
            dibujo = pendientes.randomOrNull()?.nombre,
            titulo = dificultad.nombre,
            esDiario = false,
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        when (val actual = pantalla) {
            Pantalla.Inicio -> PantallaInicio(
                completados = completados,
                racha = racha,
                pistas = pistas,
                diarioHecho = diarioHecho,
                descubiertos = descubiertos,
                guardada = guardada,
                sabeJugar = progreso.sabeJugar,
                mejorTiempo = { progreso.mejorTiempo(it) },
                onEntendido = { progreso.sabeJugar = true },
                onSeguir = { partida ->
                    pantalla = Pantalla.Jugando(
                        tamano = partida.tamano,
                        semilla = partida.semilla,
                        dibujo = partida.dibujo,
                        titulo = partida.titulo,
                        esDiario = partida.esDiario,
                        celdas = partida.celdas,
                        segundos = partida.segundos,
                        errores = partida.errores,
                    )
                },
                onDescartar = {
                    progreso.descartarPartida()
                    guardada = null
                },
                onJugarDiario = {
                    // El diario se genera: así nunca se repite y es el mismo para
                    // todo el mundo. Los dibujos del catálogo son la colección.
                    pantalla = Pantalla.Jugando(
                        tamano = 10,
                        semilla = semillaDelDia(),
                        dibujo = null,
                        titulo = "Puzzle del día",
                        esDiario = true,
                    )
                },
                onJugar = { dificultad -> pantalla = nuevaPartida(dificultad) },
            )

            is Pantalla.Jugando -> {
                // El botón de atrás lo gestiona PantallaJuego, que antes de
                // salir apunta la partida a medias.

                // Generar un 15x15 cuesta un momento: fuera del hilo principal.
                var puzzle by remember(actual) { mutableStateOf<Puzzle?>(null) }
                LaunchedEffect(actual) {
                    puzzle = withContext(Dispatchers.Default) {
                        val delCatalogo = actual.dibujo?.let { nombre ->
                            Catalogo.porLado(actual.tamano).firstOrNull { it.nombre == nombre }
                        }
                        delCatalogo?.aPuzzle()
                            ?: Generator.generar(actual.tamano, actual.tamano, actual.semilla)
                    }
                }

                val listo = puzzle
                if (listo == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    PantallaJuego(
                        puzzle = listo,
                        titulo = actual.titulo,
                        pistasDisponibles = pistas,
                        celdasIniciales = actual.celdas,
                        segundosIniciales = actual.segundos,
                        erroresIniciales = actual.errores,
                        onPedirPista = {
                            val pudo = progreso.gastarPista()
                            if (pudo) pistas = progreso.pistas
                            pudo
                        },
                        onVerAnuncio = {
                            anuncios.mostrarRecompensado {
                                progreso.regalarPistas(GestorAnuncios.PISTAS_POR_ANUNCIO)
                                pistas = progreso.pistas
                            }
                        },
                        onVictoria = { segundos, errores ->
                            val record = progreso.registrarVictoria(actual.tamano, segundos, errores)
                            listo.nombre?.let { progreso.descubrir(actual.tamano, it) }
                            if (actual.esDiario) progreso.registrarDiario()
                            progreso.descartarPartida()
                            refrescar()
                            record
                        },
                        onGuardar = { celdas, segundos, errores ->
                            progreso.guardarPartida(
                                PartidaGuardada(
                                    tamano = actual.tamano,
                                    semilla = actual.semilla,
                                    dibujo = actual.dibujo,
                                    titulo = actual.titulo,
                                    esDiario = actual.esDiario,
                                    celdas = celdas,
                                    segundos = segundos,
                                    errores = errores,
                                ),
                            )
                        },
                        onOlvidar = { progreso.descartarPartida() },
                        onSalir = {
                            pantalla = Pantalla.Inicio
                            refrescar()
                        },
                        // Solo al terminar un puzzle, nunca a mitad: el intersticial
                        // va aquí y no en el botón de atrás.
                        onContinuar = {
                            anuncios.alTerminarPartida {
                                pantalla = Pantalla.Inicio
                                refrescar()
                            }
                        },
                    )
                }
            }
        }
    }
}
