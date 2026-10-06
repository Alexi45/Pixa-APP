package com.riscart.pixa.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
            Text(
                text = "‹  Atrás",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onSalir)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(titulo, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f))
            Text(
                text = formatearTiempo(estado.segundos),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
        }

        Spacer(Modifier.height(10.dp))

        BarraDeProgreso(progreso, colores)

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "${estado.pintadasCorrectas} / ${puzzle.totalFilled}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (estado.errores > 0) {
                Text(
                    "${estado.errores} ${if (estado.errores == 1) "error" else "errores"}",
                    style = MaterialTheme.typography.bodySmall,
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
        // todo: el panel terminado es el premio, y hay que poder verlo.
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
                colores = colores,
                onModo = estado::cambiarModo,
                onDeshacer = estado::deshacer,
                onPista = { if (onPedirPista()) estado.usarPista() },
                onVerAnuncio = onVerAnuncio,
            )
        }

        Spacer(Modifier.height(20.dp))
    }
}

/** Barra propia en vez de la de Material, que trae su punto y su hueco de serie. */
@Composable
private fun BarraDeProgreso(progreso: Float, colores: ColoresTablero) {
    val relleno = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(7.dp),
    ) {
        val radio = CornerRadius(size.height / 2f, size.height / 2f)
        drawRoundRect(color = colores.junta, cornerRadius = radio)
        if (progreso > 0f) {
            drawRoundRect(
                color = relleno,
                size = Size(size.width * progreso.coerceIn(0f, 1f), size.height),
                cornerRadius = radio,
            )
        }
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
    colores: ColoresTablero,
    onModo: (Modo) -> Unit,
    onDeshacer: () -> Unit,
    onPista: () -> Unit,
    onVerAnuncio: (() -> Unit)?,
) {
    Column {
        // Dos piezas asentadas sobre la junta, igual que en el tablero: el
        // selector de modo es lo que más se toca, así que ocupa toda la fila.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(15.dp))
                .background(colores.junta)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PiezaBoton(
                texto = "Pintar",
                color = if (modo == Modo.PINTAR) {
                    MaterialTheme.colorScheme.primary
                } else {
                    colores.piezaVacia
                },
                colorTexto = if (modo == Modo.PINTAR) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.weight(1f),
                onClick = { onModo(Modo.PINTAR) },
            )
            PiezaBoton(
                texto = "Tachar  ✕",
                color = if (modo == Modo.TACHAR) {
                    MaterialTheme.colorScheme.primary
                } else {
                    colores.piezaVacia
                },
                colorTexto = if (modo == Modo.TACHAR) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.weight(1f),
                onClick = { onModo(Modo.TACHAR) },
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PiezaBoton(
                texto = "Deshacer",
                color = MaterialTheme.colorScheme.surface,
                colorTexto = if (puedeDeshacer) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                },
                habilitado = puedeDeshacer,
                modifier = Modifier.weight(1f),
                onClick = onDeshacer,
            )

            // Sin pistas, el botón se convierte en la oferta de verlas por un
            // anuncio. Siempre voluntario y siempre dicho claramente — es lo que
            // pide la política de Play y además es lo que no molesta al jugador.
            if (pistasDisponibles == 0 && onVerAnuncio != null) {
                PiezaBoton(
                    texto = "Ver anuncio  +$PISTAS_POR_ANUNCIO",
                    color = MaterialTheme.colorScheme.secondary,
                    colorTexto = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.weight(1f),
                    onClick = onVerAnuncio,
                )
            } else {
                PiezaBoton(
                    texto = "Pista  ($pistasDisponibles)",
                    color = MaterialTheme.colorScheme.surface,
                    colorTexto = if (pistasDisponibles > 0) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    },
                    habilitado = pistasDisponibles > 0,
                    modifier = Modifier.weight(1f),
                    onClick = onPista,
                )
            }
        }
    }
}

/** Un botón que es, literalmente, una pieza vidriada. */
@Composable
private fun PiezaBoton(
    texto: String,
    color: Color,
    colorTexto: Color,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    onClick: () -> Unit,
) {
    val forma = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .height(50.dp)
            .clip(forma)
            .fondoDePieza(color = color, radio = 12.dp, relieve = if (habilitado) 0.7f else 0.25f)
            .clickable(enabled = habilitado, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelLarge,
            color = colorTexto,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun CarteldeVictoria(segundos: Int, errores: Int, onSalir: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .fondoDePieza(
                color = MaterialTheme.colorScheme.surface,
                radio = 20.dp,
                relieve = 0.3f,
            )
            .padding(horizontal = 28.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("¡Panel terminado!", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            text = formatearTiempo(segundos) +
                if (errores == 0) " · sin fallos" else " · $errores fallos",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        PiezaBoton(
            texto = "Continuar",
            color = MaterialTheme.colorScheme.primary,
            colorTexto = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.width(190.dp),
            onClick = onSalir,
        )
    }
}
