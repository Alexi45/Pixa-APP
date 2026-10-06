package com.riscart.pixa.ui

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.riscart.pixa.datos.PartidaGuardada
import com.riscart.pixa.datos.Progreso
import com.riscart.pixa.engine.Catalogo
import com.riscart.pixa.juego.formatearTiempo

/** Los tres tamaños jugables. */
data class Dificultad(val tamano: Int, val nombre: String, val descripcion: String)

val DIFICULTADES = listOf(
    Dificultad(5, "Fácil", "5 × 5 · un café"),
    Dificultad(10, "Normal", "10 × 10 · un rato"),
    Dificultad(15, "Difícil", "15 × 15 · en serio"),
)

/** El ancho máximo del contenido: en una tablet, una columna de 2.000 px no se lee. */
private val ANCHO_MAXIMO = 560.dp

@Composable
fun PantallaInicio(
    completados: Int,
    racha: Int,
    pistas: Int,
    diarioHecho: Boolean,
    descubiertos: Set<String>,
    guardada: PartidaGuardada?,
    sabeJugar: Boolean,
    mejorTiempo: (Int) -> Int?,
    onEntendido: () -> Unit,
    onSeguir: (PartidaGuardada) -> Unit,
    onDescartar: () -> Unit,
    onJugarDiario: () -> Unit,
    onJugar: (Dificultad) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = coloresTablero()
    var explicando by remember { mutableStateOf(!sabeJugar) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = ANCHO_MAXIMO).padding(horizontal = 22.dp)) {
                Spacer(Modifier.height(36.dp))
                Text("Pixa", style = MaterialTheme.typography.displayLarge)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Nonogramas: coloca las piezas siguiendo los números",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
            }

            // La cenefa va de pared a pared, no recortada al ancho del contenido.
            Cenefa(colores)

            Column(Modifier.widthIn(max = ANCHO_MAXIMO)) {
                Column(Modifier.padding(horizontal = 22.dp)) {
                    Spacer(Modifier.height(20.dp))

                    if (guardada != null) {
                        TarjetaSeguir(guardada, onSeguir = { onSeguir(guardada) }, onDescartar)
                        Spacer(Modifier.height(10.dp))
                    }

                    TarjetaDiaria(racha = racha, hecho = diarioHecho, onJugar = onJugarDiario)

                    Spacer(Modifier.height(22.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("Jugar", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${descubiertos.size} de ${Catalogo.todos.size} dibujos",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 3.dp),
                        )
                    }
                    Spacer(Modifier.height(10.dp))

                    DIFICULTADES.forEach { dificultad ->
                        TarjetaDificultad(
                            dificultad = dificultad,
                            mejor = mejorTiempo(dificultad.tamano),
                            descubiertos = descubiertos,
                            colores = colores,
                            onClick = { onJugar(dificultad) },
                        )
                        Spacer(Modifier.height(10.dp))
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Estadistica("Resueltos", completados.toString(), Modifier.weight(1f))
                        Estadistica(
                            "Racha",
                            if (racha > 0) "$racha días" else "—",
                            Modifier.weight(1f),
                        )
                        Estadistica("Pistas", pistas.toString(), Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = "¿Cómo se juega?",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { explicando = true }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    )

                    Spacer(Modifier.height(32.dp))
                }
            }
        }

        if (explicando) {
            ComoSeJuega(
                colores = colores,
                onCerrar = {
                    explicando = false
                    onEntendido()
                },
            )
        }
    }
}

/** La tira de olambrillas que remata la cabecera. */
@Composable
private fun Cenefa(colores: ColoresTablero) {
    val primario = MaterialTheme.colorScheme.primary
    val secundario = MaterialTheme.colorScheme.secondary
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp),
    ) {
        dibujarCenefa(junta = colores.junta, primero = primario, segundo = secundario)
    }
}

@Composable
private fun TarjetaSeguir(
    partida: PartidaGuardada,
    onSeguir: () -> Unit,
    onDescartar: () -> Unit,
) {
    val puestas = partida.celdas.count { it == '1' }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .fondoDePieza(
                color = MaterialTheme.colorScheme.secondary,
                radio = 16.dp,
                relieve = 0.75f,
            )
            .clickable(onClick = onSeguir)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Seguir con «${partida.titulo}»",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "$puestas piezas puestas · ${formatearTiempo(partida.segundos)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.78f),
            )
        }
        Text(
            text = "✕",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.7f),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onDescartar)
                .padding(10.dp),
        )
    }
}

@Composable
private fun TarjetaDiaria(racha: Int, hecho: Boolean, onJugar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            // Sin destello: en una pieza tan grande el reflejo se estira y
            // parece un botón de cristal de 2008, no cerámica.
            .fondoDePieza(
                color = MaterialTheme.colorScheme.primary,
                radio = 20.dp,
                relieve = 0.85f,
            )
            .clickable(onClick = onJugar)
            .padding(22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Puzzle del día",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = when {
                    hecho && racha > 1 -> "Hecho ✓ · llevas $racha días seguidos"
                    hecho -> "Hecho ✓ · vuelve mañana"
                    racha > 0 -> "Mantén tu racha de $racha días"
                    else -> "Uno nuevo cada día, igual para todos"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.80f),
            )
        }
        Text(
            text = if (hecho) "✓" else "▶",
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 24.sp,
        )
    }
}

@Composable
private fun TarjetaDificultad(
    dificultad: Dificultad,
    mejor: Int?,
    descubiertos: Set<String>,
    colores: ColoresTablero,
    onClick: () -> Unit,
) {
    val delTamano = Catalogo.porLado(dificultad.tamano)
    val encontrados = delTamano.count {
        Progreso.clave(dificultad.tamano, it.nombre) in descubiertos
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .fondoDePieza(
                color = MaterialTheme.colorScheme.surface,
                radio = 16.dp,
                relieve = 0.3f,
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiniPanel(dificultad.tamano, colores)
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                dificultad.nombre,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // Dos líneas: con la letra del sistema al 150 % en una sola se corta.
            Text(
                text = "${dificultad.descripcion} · $encontrados de ${delTamano.size} dibujos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (mejor != null) {
            Spacer(Modifier.size(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Mejor",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    formatearTiempo(mejor),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** Un panelito de muestra que insinúa el tamaño de la dificultad. */
@Composable
private fun MiniPanel(tamano: Int, colores: ColoresTablero) {
    val lado = when (tamano) {
        5 -> 2
        10 -> 3
        else -> 4
    }
    val patron = remember(lado) {
        val semilla = lado * 37
        List(lado * lado) { (it * semilla + it / lado) % 3 != 0 }
    }
    Canvas(Modifier.size(42.dp)) {
        val junta = size.width * 0.055f
        val paso = (size.width - junta) / lado
        val radio = paso * 0.16f
        drawRoundRect(
            color = colores.junta,
            cornerRadius = CornerRadius(radio * 2f, radio * 2f),
        )
        for (fila in 0 until lado) {
            for (columna in 0 until lado) {
                val puesta = patron[fila * lado + columna]
                dibujarPieza(
                    x = junta / 2f + columna * paso,
                    y = junta / 2f + fila * paso,
                    lado = paso - junta,
                    color = if (puesta) colores.piezaPuesta else colores.piezaVacia,
                    radio = radio,
                    destello = false,
                    relieve = if (puesta) 1f else 0.5f,
                )
            }
        }
    }
}

@Composable
private fun Estadistica(etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .fondoDePieza(
                color = MaterialTheme.colorScheme.surface,
                radio = 14.dp,
                relieve = 0.3f,
            )
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            valor,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
        Text(
            etiqueta,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Las reglas, la primera vez.
 *
 * Mucha gente se baja un nonograma sin saber qué es y lo desinstala a los dos
 * minutos. Explicarlo en cuatro líneas, con un ejemplo dibujado, es de lo más
 * barato que se puede hacer por la retención del primer día.
 */
@Composable
private fun ComoSeJuega(colores: ColoresTablero, onCerrar: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onCerrar),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(22.dp))
                .fondoDePieza(
                    color = MaterialTheme.colorScheme.surface,
                    radio = 22.dp,
                    relieve = 0.3f,
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Cómo se juega", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(14.dp))
            EjemploFila(colores)
            Spacer(Modifier.height(14.dp))
            Regla("Los números de cada fila y columna dicen cuántas piezas seguidas van ahí.")
            Regla("«2 1» significa dos piezas juntas, un hueco por lo menos, y otra pieza.")
            Regla("Si te equivocas al colocar, la casilla se tacha sola y cuenta un fallo.")
            Regla("Usa Tachar para marcar lo que seguro que está vacío: ayuda muchísimo.")
            Spacer(Modifier.height(18.dp))
            PiezaBoton(
                texto = "Entendido",
                color = MaterialTheme.colorScheme.primary,
                colorTexto = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.fillMaxWidth(),
                onClick = onCerrar,
            )
        }
    }
}

@Composable
private fun EjemploFila(colores: ColoresTablero) {
    val patron = listOf(true, true, false, false, true)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "2 1",
            style = MaterialTheme.typography.titleMedium,
            color = colores.textoPista,
        )
        Spacer(Modifier.size(10.dp))
        Canvas(
            Modifier
                .height(44.dp)
                .width(44.dp * 5),
        ) {
            val paso = size.height
            val junta = paso * 0.08f
            drawRoundRect(
                color = colores.junta,
                cornerRadius = CornerRadius(paso * 0.2f, paso * 0.2f),
            )
            patron.forEachIndexed { i, puesta ->
                dibujarPieza(
                    x = i * paso + junta / 2f,
                    y = junta / 2f,
                    lado = paso - junta,
                    color = if (puesta) colores.piezaPuesta else colores.piezaVacia,
                    radio = paso * 0.12f,
                    destello = false,
                    relieve = if (puesta) 1f else 0.5f,
                )
            }
        }
    }
}

@Composable
private fun Regla(texto: String) {
    Row(Modifier.padding(vertical = 4.dp)) {
        Text(
            "·",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            texto,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
