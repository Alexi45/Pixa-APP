package com.riscart.pixa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import com.riscart.pixa.datos.Progreso
import com.riscart.pixa.datos.semillaDelDia
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
    data class Jugando(val tamano: Int, val semilla: Long, val titulo: String, val esDiario: Boolean) : Pantalla
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
    var version by remember { mutableIntStateOf(0) }

    fun refrescar() {
        completados = progreso.completados
        racha = progreso.racha
        pistas = progreso.pistas
        diarioHecho = progreso.diarioHechoHoy()
        version++
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
                mejorTiempo = { progreso.mejorTiempo(it) },
                onJugarDiario = {
                    pantalla = Pantalla.Jugando(
                        tamano = 10,
                        semilla = semillaDelDia(),
                        titulo = "Puzzle del día",
                        esDiario = true,
                    )
                },
                onJugar = { dificultad ->
                    pantalla = Pantalla.Jugando(
                        tamano = dificultad.tamano,
                        semilla = Random.nextLong(),
                        titulo = dificultad.nombre,
                        esDiario = false,
                    )
                },
            )

            is Pantalla.Jugando -> {
                BackHandler { pantalla = Pantalla.Inicio }

                // Generar un 15x15 cuesta un momento: fuera del hilo principal.
                var puzzle by remember(actual) { mutableStateOf<Puzzle?>(null) }
                LaunchedEffect(actual) {
                    puzzle = withContext(Dispatchers.Default) {
                        Generator.generar(actual.tamano, actual.tamano, actual.semilla)
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
                        onVictoria = { segundos ->
                            progreso.registrarVictoria(actual.tamano, segundos)
                            if (actual.esDiario) progreso.registrarDiario()
                            refrescar()
                        },
                        onSalir = { pantalla = Pantalla.Inicio },
                        // Solo al terminar un puzzle, nunca a mitad: el intersticial
                        // va aquí y no en el botón de atrás.
                        onContinuar = {
                            anuncios.alTerminarPartida { pantalla = Pantalla.Inicio }
                        },
                    )
                }
            }
        }
    }
}
