package com.riscart.pixa.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.riscart.pixa.anuncios.GestorAnuncios.Companion.PISTAS_POR_ANUNCIO
import com.riscart.pixa.engine.Puzzle
import com.riscart.pixa.juego.EstadoJuego
import com.riscart.pixa.juego.Modo
import com.riscart.pixa.juego.formatearTiempo
import kotlinx.coroutines.delay

@Composable
fun PantallaJuego(
    puzzle: Puzzle,
    titulo: String,
    pistasDisponibles: Int,
    onPedirPista: () -> Boolean,
    onVictoria: (segundos: Int) -> Unit,
    onSalir: () -> Unit,
    onContinuar: () -> Unit = onSalir,
    onVerAnuncio: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val estado = remember(puzzle) { EstadoJuego(puzzle) }
    val colores = coloresTablero()

    // Cronómetro: se para al completar el puzzle y también mientras la app no
    // está en primer plano. Si no, ver un anuncio o atender una llamada te
    // arruinaría el récord — justo lo contrario de lo que queremos premiar.
    val cicloDeVida = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(puzzle, estado.completado) {
        cicloDeVida.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (!estado.completado) {
                delay(1000)
                estado.segundos += 1
            }
        }
    }

    LaunchedEffect(estado.completado) {
        if (estado.completado) onVictoria(estado.segundos)
    }

    // El destello rojo del error se apaga solo.
    LaunchedEffect(estado.ultimoError) {
        if (estado.ultimoError != null) {
            delay(500)
            estado.limpiarUltimoError()
        }
    }

    val progreso by animateFloatAsState(estado.progreso, label = "progreso")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onSalir) { Text("‹  Atrás") }
            Spacer(Modifier.weight(1f))
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = formatearTiempo(estado.segundos),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(10.dp))

        LinearProgressIndicator(
            progress = { progreso },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "${estado.pintadasCorrectas} / ${puzzle.totalFilled}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (estado.errores > 0) {
                Text(
                    "${estado.errores} ${if (estado.errores == 1) "error" else "errores"}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Tablero(
                puzzle = puzzle,
                rejilla = estado.rejilla,
                colores = colores,
                onCelda = { x, y, arrastrando -> estado.tocar(x, y, arrastrando) },
            )
        }

        // Al ganar, el cartel ocupa el sitio de los controles en vez de taparlo
        // todo: el dibujo terminado es el premio, y hay que poder verlo.
        if (estado.completado) {
            OverlayVictoria(
                visible = true,
                segundos = estado.segundos,
                errores = estado.errores,
                onSalir = onContinuar,
            )
        } else {
            BarraDeControles(
                modo = estado.modo,
                puedeDeshacer = estado.puedeDeshacer,
                pistasDisponibles = pistasDisponibles,
                activa = true,
                onModo = estado::cambiarModo,
                onDeshacer = estado::deshacer,
                onPista = { if (onPedirPista()) estado.usarPista() },
                onVerAnuncio = onVerAnuncio,
            )
        }

        Spacer(Modifier.height(20.dp))
    }
}

/**
 * Envuelto en su propia función a propósito: dentro de un Column, la llamada a
 * AnimatedVisibility se resolvería a la variante de ColumnScope, que no vale aquí.
 */
@Composable
private fun OverlayVictoria(
    visible: Boolean,
    segundos: Int,
    errores: Int,
    onSalir: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.94f),
    ) {
        CarteldeVictoria(segundos = segundos, errores = errores, onSalir = onSalir)
    }
}

@Composable
private fun BarraDeControles(
    modo: Modo,
    puedeDeshacer: Boolean,
    pistasDisponibles: Int,
    activa: Boolean,
    onModo: (Modo) -> Unit,
    onDeshacer: () -> Unit,
    onPista: () -> Unit,
    onVerAnuncio: (() -> Unit)?,
) {
    Column {
        // El selector de modo es lo que más se toca: ocupa toda la fila.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BotonModo(
                texto = "Pintar",
                seleccionado = modo == Modo.PINTAR,
                habilitado = activa,
                modifier = Modifier.weight(1f),
            ) { onModo(Modo.PINTAR) }
            BotonModo(
                texto = "Tachar  ✕",
                seleccionado = modo == Modo.TACHAR,
                habilitado = activa,
                modifier = Modifier.weight(1f),
            ) { onModo(Modo.TACHAR) }
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onDeshacer,
                enabled = puedeDeshacer && activa,
                modifier = Modifier.weight(1f),
            ) { Text("Deshacer") }

            // Sin pistas, el botón se convierte en la oferta de verlas por un
            // anuncio. Siempre voluntario y siempre dicho claramente — es lo que
            // pide la política de Play y además es lo que no molesta al jugador.
            if (pistasDisponibles == 0 && onVerAnuncio != null) {
                Button(
                    onClick = onVerAnuncio,
                    enabled = activa,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        "Ver anuncio  +${PISTAS_POR_ANUNCIO}",
                        fontSize = 14.sp,
                        maxLines = 1,
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onPista,
                    enabled = activa && pistasDisponibles > 0,
                    modifier = Modifier.weight(1f),
                ) { Text("Pista  ($pistasDisponibles)") }
            }
        }
    }
}

@Composable
private fun BotonModo(
    texto: String,
    seleccionado: Boolean,
    habilitado: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(11.dp),
        color = if (seleccionado) MaterialTheme.colorScheme.primary else Color.Transparent,
        onClick = onClick,
        enabled = habilitado,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = texto,
                color = if (seleccionado) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = if (seleccionado) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun CarteldeVictoria(segundos: Int, errores: Int, onSalir: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("¡Resuelto!", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                text = formatearTiempo(segundos) +
                    if (errores == 0) " · sin fallos" else " · $errores fallos",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))
            Button(onClick = onSalir, modifier = Modifier.width(180.dp)) {
                Text("Continuar")
            }
        }
    }
}
