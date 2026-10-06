package com.riscart.pixa.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.riscart.pixa.engine.Cell
import com.riscart.pixa.engine.Puzzle
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * El panel de azulejos: las pistas arriba y a la izquierda, y la obra.
 *
 * Todo en un único Canvas en vez de cientos de composables. Con un 15 × 15 son
 * 225 piezas; estamparlas de una pasada va mucho más fino al arrastrar el dedo,
 * que es justo cuando se nota.
 */
@Composable
fun Tablero(
    puzzle: Puzzle,
    rejilla: List<Cell>,
    colores: ColoresTablero,
    onCelda: (x: Int, y: Int, arrastrando: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    /** 0 = pieza recién puesta, 1 = ya asentada. */
    asentamiento: (Int) -> Float = { 1f },
    /** La casilla que acabas de fallar, para marcarla un instante. */
    fallo: Int? = null,
) {
    val medidor = rememberTextMeasurer()
    val densidad = LocalDensity.current

    val maxPistasFila = remember(puzzle) { puzzle.rowClues.maxOf { it.size } }
    val maxPistasCol = remember(puzzle) { puzzle.colClues.maxOf { it.size } }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val anchoDisponible = with(densidad) { maxWidth.toPx() }
        val altoDisponible = with(densidad) { maxHeight.toPx() }

        // Las casillas de pista son algo más estrechas que las del tablero.
        val factorPista = 0.68f
        val unidadesAncho = puzzle.width + maxPistasFila * factorPista
        val unidadesAlto = puzzle.height + maxPistasCol * factorPista

        val paso = minOf(anchoDisponible / unidadesAncho, altoDisponible / unidadesAlto)
        val ladoPista = paso * factorPista

        // Si sobra ancho (apaisado, tablet), el panel va centrado. Sin esto se
        // quedaba pegado a la izquierda con un hueco enorme al lado.
        val anchoUsado = unidadesAncho * paso
        val sobra = ((anchoDisponible - anchoUsado) / 2f).coerceAtLeast(0f)
        val origenX = sobra + maxPistasFila * ladoPista
        val origenY = maxPistasCol * ladoPista

        // La junta y el redondeo encogen con la pieza: en un 15 × 15 una junta
        // proporcional se comería el dibujo.
        val junta = (paso * 0.075f).coerceIn(1.4f, 9f)
        val radio = (paso * 0.11f).coerceIn(1.5f, 8f)

        val piezas = remember(paso, junta, radio, colores) {
            Piezas(
                vacia = cocerPieza(
                    paso, junta, colores.piezaVacia, radio,
                    destello = false, relieve = 0.5f,
                ),
                puesta = cocerPieza(paso, junta, colores.piezaPuesta, radio, destello = false),
                tachada = cocerPieza(
                    paso, junta, colores.piezaVacia, radio,
                    destello = false, cruz = colores.marcaTachada, relieve = 0.5f,
                ),
            )
        }

        val altoTotalDp = with(densidad) { (origenY + puzzle.height * paso + junta).toDp() }

        val estiloPista = TextStyle(
            fontFamily = FamiliaCifras,
            fontSize = with(densidad) { (paso * 0.44f).toSp() },
            fontWeight = FontWeight.SemiBold,
            color = colores.textoPista,
        )
        val estiloPistaHecha = estiloPista.copy(color = colores.textoPistaHecha)

        fun celdaEn(posicion: Offset): Pair<Int, Int>? {
            val x = floor((posicion.x - origenX) / paso).toInt()
            val y = floor((posicion.y - origenY) / paso).toInt()
            return if (x in 0 until puzzle.width && y in 0 until puzzle.height) x to y else null
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(altoTotalDp)
                .pointerInput(puzzle) {
                    detectTapGestures { posicion ->
                        celdaEn(posicion)?.let { (x, y) -> onCelda(x, y, false) }
                    }
                }
                .pointerInput(puzzle) {
                    var ultima: Pair<Int, Int>? = null
                    detectDragGestures(
                        onDragStart = { posicion ->
                            ultima = celdaEn(posicion)
                            ultima?.let { (x, y) -> onCelda(x, y, true) }
                        },
                        onDragEnd = { ultima = null },
                        onDragCancel = { ultima = null },
                    ) { cambio, _ ->
                        val celda = celdaEn(cambio.position)
                        if (celda != null && celda != ultima) {
                            ultima = celda
                            onCelda(celda.first, celda.second, true)
                        }
                    }
                },
        ) {
            dibujarBandaPistas(colores, origenX, origenY, paso, maxPistasFila * ladoPista, puzzle)
            dibujarJunta(colores, origenX, origenY, paso, junta, radio, puzzle)
            dibujarPiezas(puzzle, rejilla, piezas, origenX, origenY, paso, asentamiento)
            if (fallo != null) {
                dibujarFallo(fallo, puzzle, colores, origenX, origenY, paso, radio)
            }
            dibujarPistas(
                puzzle, rejilla, medidor, estiloPista, estiloPistaHecha,
                origenX, origenY, paso, ladoPista, maxPistasFila, maxPistasCol,
            )
        }
    }
}

/** Las tres estampas posibles de una casilla. */
private class Piezas(
    val vacia: ImageBitmap,
    val puesta: ImageBitmap,
    val tachada: ImageBitmap,
)

/** Un tono apenas perceptible tras las pistas, para separarlas de la obra. */
private fun DrawScope.dibujarBandaPistas(
    colores: ColoresTablero,
    origenX: Float,
    origenY: Float,
    paso: Float,
    anchoPistas: Float,
    puzzle: Puzzle,
) {
    drawRect(
        color = colores.bandaPistas,
        topLeft = Offset(origenX, 0f),
        size = Size(puzzle.width * paso, origenY),
    )
    // Arranca donde arrancan las pistas, no en el borde: con el tablero
    // centrado, la banda se estiraba hasta el margen izquierdo de la pantalla.
    drawRect(
        color = colores.bandaPistas,
        topLeft = Offset(origenX - anchoPistas, origenY),
        size = Size(anchoPistas, puzzle.height * paso),
    )
}

/**
 * La junta sobre la que se asientan las piezas. Cada cinco casillas la junta es
 * más oscura: es lo que ayuda a contar de un vistazo, igual que en un panel de
 * verdad los paños se separan con una llaga más marcada.
 */
private fun DrawScope.dibujarJunta(
    colores: ColoresTablero,
    origenX: Float,
    origenY: Float,
    paso: Float,
    junta: Float,
    radio: Float,
    puzzle: Puzzle,
) {
    val medio = junta / 2f
    drawRoundRect(
        color = colores.junta,
        topLeft = Offset(origenX - medio, origenY - medio),
        size = Size(puzzle.width * paso + junta, puzzle.height * paso + junta),
        cornerRadius = CornerRadius(radio * 1.8f, radio * 1.8f),
    )

    val llaga = junta * 1.9f
    for (x in 5 until puzzle.width step 5) {
        drawLine(
            color = colores.juntaFuerte,
            start = Offset(origenX + x * paso, origenY - medio),
            end = Offset(origenX + x * paso, origenY + puzzle.height * paso + medio),
            strokeWidth = llaga,
        )
    }
    for (y in 5 until puzzle.height step 5) {
        drawLine(
            color = colores.juntaFuerte,
            start = Offset(origenX - medio, origenY + y * paso),
            end = Offset(origenX + puzzle.width * paso + medio, origenY + y * paso),
            strokeWidth = llaga,
        )
    }
}

private fun DrawScope.dibujarPiezas(
    puzzle: Puzzle,
    rejilla: List<Cell>,
    piezas: Piezas,
    origenX: Float,
    origenY: Float,
    paso: Float,
    asentamiento: (Int) -> Float,
) {
    for (y in 0 until puzzle.height) {
        for (x in 0 until puzzle.width) {
            val i = y * puzzle.width + x
            val estampa = when (rejilla[i]) {
                Cell.FILLED -> piezas.puesta
                Cell.CROSSED -> piezas.tachada
                Cell.UNKNOWN -> piezas.vacia
            }
            // Se redondea a píxel entero para que el vidriado salga nítido.
            val esquina = Offset(
                (origenX + x * paso).roundToInt().toFloat(),
                (origenY + y * paso).roundToInt().toFloat(),
            )
            val avance = asentamiento(i)
            if (avance >= 1f) {
                drawImage(image = estampa, topLeft = esquina)
            } else {
                // La pieza entra pequeña, se pasa un pelín y se asienta.
                val suave = 1f - (1f - avance).pow(3)
                val escala = 0.66f + 0.34f * suave + 0.07f * sin(PI.toFloat() * avance)
                scale(escala, pivot = Offset(esquina.x + paso / 2f, esquina.y + paso / 2f)) {
                    drawImage(image = estampa, topLeft = esquina)
                }
            }
        }
    }
}

/** El aviso de que ahí no iba pieza: un destello que se apaga solo. */
private fun DrawScope.dibujarFallo(
    indice: Int,
    puzzle: Puzzle,
    colores: ColoresTablero,
    origenX: Float,
    origenY: Float,
    paso: Float,
    radio: Float,
) {
    val x = indice % puzzle.width
    val y = indice / puzzle.width
    drawRoundRect(
        color = colores.fallo,
        topLeft = Offset(origenX + x * paso, origenY + y * paso),
        size = Size(paso, paso),
        cornerRadius = CornerRadius(radio, radio),
    )
}

private fun DrawScope.dibujarPistas(
    puzzle: Puzzle,
    rejilla: List<Cell>,
    medidor: TextMeasurer,
    estilo: TextStyle,
    estiloHecha: TextStyle,
    origenX: Float,
    origenY: Float,
    paso: Float,
    ladoPista: Float,
    maxPistasFila: Int,
    maxPistasCol: Int,
) {
    for (y in 0 until puzzle.height) {
        val pistas = puzzle.rowClues[y].filter { it > 0 }
        val filaHecha = lineaCompleta(puzzle, rejilla, fila = y)
        pistas.forEachIndexed { indice, numero ->
            val hueco = maxPistasFila - pistas.size + indice
            val texto = medidor.measure(numero.toString(), if (filaHecha) estiloHecha else estilo)
            drawText(
                textLayoutResult = texto,
                topLeft = Offset(
                    hueco * ladoPista + (ladoPista - texto.size.width) / 2f,
                    origenY + y * paso + (paso - texto.size.height) / 2f,
                ),
            )
        }
    }

    for (x in 0 until puzzle.width) {
        val pistas = puzzle.colClues[x].filter { it > 0 }
        val columnaHecha = lineaCompleta(puzzle, rejilla, columna = x)
        pistas.forEachIndexed { indice, numero ->
            val hueco = maxPistasCol - pistas.size + indice
            val texto = medidor.measure(numero.toString(), if (columnaHecha) estiloHecha else estilo)
            drawText(
                textLayoutResult = texto,
                topLeft = Offset(
                    origenX + x * paso + (paso - texto.size.width) / 2f,
                    hueco * ladoPista + (ladoPista - texto.size.height) / 2f,
                ),
            )
        }
    }
}

/** ¿Está ya resuelta esta fila o columna? Sirve para dar por hechas sus pistas. */
private fun lineaCompleta(
    puzzle: Puzzle,
    rejilla: List<Cell>,
    fila: Int = -1,
    columna: Int = -1,
): Boolean = when {
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
