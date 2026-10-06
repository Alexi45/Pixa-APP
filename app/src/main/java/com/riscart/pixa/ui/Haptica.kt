package com.riscart.pixa.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import com.riscart.pixa.juego.Efecto

/**
 * El tacto del juego.
 *
 * Colocar una pieza, cerrar una línea y meter la pata tienen que **notarse
 * distinto** en la mano. Es lo que convierte tocar casillas en colocar azulejos,
 * y es de lo poco que de verdad engancha sin tener que prometerle nada a nadie.
 *
 * Se usa la háptica del sistema, no el vibrador a pelo: así respeta los ajustes
 * del usuario y se siente como el resto del móvil.
 */
@Composable
fun recordarHaptica(): (Efecto) -> Unit {
    val vista = LocalView.current
    return remember(vista) {
        { efecto ->
            val constante = when (efecto) {
                Efecto.PIEZA, Efecto.MARCA -> HapticFeedbackConstants.CLOCK_TICK
                Efecto.LINEA -> confirmar()
                Efecto.VICTORIA -> confirmar()
                Efecto.FALLO -> rechazar()
                Efecto.NADA -> null
            }
            if (constante != null) vista.performHapticFeedback(constante)
        }
    }
}

private fun confirmar(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        HapticFeedbackConstants.CONFIRM
    } else {
        HapticFeedbackConstants.VIRTUAL_KEY
    }

private fun rechazar(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        HapticFeedbackConstants.REJECT
    } else {
        HapticFeedbackConstants.LONG_PRESS
    }
