package com.riscart.pixa.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.riscart.pixa.anuncios.GestorAnuncios.Companion.PISTAS_POR_ANUNCIO
import com.riscart.pixa.engine.Puzzle
import com.riscart.pixa.juego.Efecto
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
    onVictoria: (segundos: Int, errores: Int) -> Boolean,
    onSalir: () -> Unit,
    modifier: Modifier = Modifier,
    celdasIniciales: String? = null,
    segundosIniciales: Int = 0,
    erroresIniciales: Int = 0,
    onContinuar: () -> Unit = onSalir,
    onVerAnuncio: (() -> Unit)? = null,
    onGuardar: (celdas: String, segundos: Int, errores: Int) -> Unit = { _, _, _ -> },
    onOlvidar: () -> Unit = {},
) {
    val estado = remember(puzzle) {
        EstadoJuego(puzzle, celdasIniciales, segundosIniciales, erroresIniciales)
    }
    val colores = coloresTablero()
    val haptica = recordarHaptica()
    val contexto = LocalContext.current
    var fueRecord by remember(puzzle) { mutableStateOf(false) }

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

    // El rebote de las piezas recién puestas solo mueve fotogramas mientras hay
    // algo que animar; el resto del tiempo el tablero no se repinta.
    LaunchedEffect(estado.animando) {
        while (estado.animando) {
            withFrameMillis { estado.avanzarAnimacion(System.currentTimeMillis()) }
        }
    }

    LaunchedEffect(estado.completado) {
        if (estado.completado) {
            fueRecord = onVictoria(estado.segundos, estado.errores)
            onOlvidar()
        }
    }

    // El destello rojo del error se apaga solo.
    LaunchedEffect(estado.ultimoError) {
        if (estado.ultimoError != null) {
            delay(420)
            estado.limpiarUltimoError()
        }
    }

    // Guardar la partida al salir o al irse la app a segundo plano.
    //
    // Se apunta ANTES de navegar, no al destruirse la pantalla: si se deja para
    // el onDispose, la pantalla de inicio ya se ha compuesto y enseña el estado
    // viejo — la tarjeta de «seguir» no aparecía hasta la siguiente vuelta.
    val guardar by rememberUpdatedState(onGuardar)
    val olvidar by rememberUpdatedState(onOlvidar)
    val apuntar: () -> Unit = {
        if (estado.valeLaPenaGuardar) {
            guardar(estado.serializar(), estado.segundos, estado.errores)
        } else if (estado.completado) {
            olvidar()
        }
    }
    val salir: () -> Unit = { apuntar(); onSalir() }

    BackHandler { salir() }

    val propietario = LocalLifecycleOwner.current
    DisposableEffect(propietario, estado) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_PAUSE) apuntar()
        }
        propietario.lifecycle.addObserver(observador)
        onDispose {
            propietario.lifecycle.removeObserver(observador)
            apuntar()
        }
    }

    val progreso by animateFloatAsState(estado.progreso, label = "progreso")

    val tablero = @Composable { ancho: Modifier ->
        Tablero(
            puzzle = puzzle,
            rejilla = estado.rejilla,
            colores = colores,
            onCelda = { x, y, arrastrando -> haptica(estado.tocar(x, y, arrastrando)) },
            modifier = ancho,
            asentamiento = estado::asentamiento,
            fallo = estado.ultimoError,
        )
    }

    val controles = @Composable {
        if (estado.completado) {
            CarteldeVictoria(
                puzzle = puzzle,
                segundos = estado.segundos,
                errores = estado.errores,
                record = fueRecord,
                onCompartir = { Compartir.panel(contexto, puzzle, estado.segundos, estado.errores) },
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
                onPista = {
                    if (onPedirPista() && estado.usarPista()) haptica(Efecto.PIEZA)
                },
                onVerAnuncio = onVerAnuncio,
            )
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // En apaisado el tablero se va a un lado y los controles al otro: en una
        // columna no cabrían los dos y el panel se quedaría en un sello.
        val apaisado = maxWidth > maxHeight
        // En una tablet el tablero no debe estirarse sin fin: a partir de cierto
        // tamaño las casillas dejan de ser cómodas y pasan a ser ridículas.
        val tableroMaximo = Modifier.sizeIn(maxWidth = 560.dp, maxHeight = 620.dp)
        val anchoPanel = (maxWidth * 0.32f).coerceIn(260.dp, 400.dp)

        if (apaisado) {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxHeight()
                    // En una tablet apaisada, sin tope el tablero y el panel se
                    // quedan cada uno en una punta de la pantalla.
                    .widthIn(max = 980.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    tablero(tableroMaximo)
                }
                Spacer(Modifier.width(18.dp))
                Column(
                    modifier = Modifier
                        .width(anchoPanel)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Cabecera(titulo, estado.segundos, salir, compacta = true)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        titulo,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 10.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    BarraDeProgreso(progreso, colores)
                    Spacer(Modifier.height(6.dp))
                    Marcador(estado.pintadasCorrectas, puzzle.totalFilled, estado.errores)
                    Spacer(Modifier.height(16.dp))
                    controles()
                }
            }
        } else {
            val margen = if (maxWidth < 380.dp) 8.dp else 16.dp
            Column(Modifier.fillMaxSize().padding(horizontal = margen)) {
                Spacer(Modifier.height(8.dp))
                Cabecera(titulo, estado.segundos, salir, compacta = false)
                Spacer(Modifier.height(10.dp))
                BarraDeProgreso(progreso, colores)
                Spacer(Modifier.height(6.dp))
                Marcador(estado.pintadasCorrectas, puzzle.totalFilled, estado.errores)
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    tablero(tableroMaximo)
                }
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.widthIn(max = 560.dp)) { controles() }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun Cabecera(titulo: String, segundos: Int, onSalir: () -> Unit, compacta: Boolean) {
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
        if (!compacta) {
            Text(titulo, style = MaterialTheme.typography.titleLarge, maxLines = 1)
            Spacer(Modifier.weight(1f))
        }
        Text(
            text = formatearTiempo(segundos),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp),
        )
    }
}

@Composable
private fun Marcador(puestas: Int, total: Int, errores: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            "$puestas / $total",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (errores > 0) {
            Text(
                "$errores ${if (errores == 1) "error" else "errores"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
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
fun PiezaBoton(
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
            modifier = Modifier.padding(horizontal = 6.dp),
        )
    }
}

@Composable
private fun CarteldeVictoria(
    puzzle: Puzzle,
    segundos: Int,
    errores: Int,
    record: Boolean,
    onCompartir: () -> Unit,
    onSalir: () -> Unit,
) {
    AnimatedVisibility(visible = true, enter = fadeIn() + scaleIn(initialScale = 0.94f)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .fondoDePieza(
                    color = MaterialTheme.colorScheme.surface,
                    radio = 20.dp,
                    relieve = 0.3f,
                )
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Si el panel era un dibujo, el nombre es el premio de verdad.
            Text(
                text = puzzle.nombre ?: "¡Panel terminado!",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = buildString {
                    append(formatearTiempo(segundos))
                    append(if (errores == 0) " · sin fallos" else " · $errores fallos")
                    if (record) append(" · récord")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (record) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PiezaBoton(
                    texto = "Compartir",
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    colorTexto = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    onClick = onCompartir,
                )
                PiezaBoton(
                    texto = "Continuar",
                    color = MaterialTheme.colorScheme.primary,
                    colorTexto = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = onSalir,
                )
            }
        }
    }
}
