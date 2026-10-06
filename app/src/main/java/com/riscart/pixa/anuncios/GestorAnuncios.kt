package com.riscart.pixa.anuncios

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Los anuncios, en un solo sitio.
 *
 * Dos reglas que se respetan aquí y que marcan la diferencia entre un juego que
 * la gente conserva y uno que desinstala:
 *
 *  1. El juego NUNCA se bloquea por culpa de un anuncio. Si no carga o falla, la
 *     recompensa se da igual. Perder un céntimo es mejor que perder al jugador.
 *  2. El intersticial no sale cada partida, sino cada [PARTIDAS_ENTRE_ANUNCIOS].
 *     Y jamás en mitad de un puzzle: solo al terminarlo.
 *
 * Los identificadores de abajo son los OFICIALES DE PRUEBA de Google: funcionan
 * sin cuenta y muestran anuncios de test. Al publicar hay que cambiarlos por los
 * tuyos (ver README) — y ojo: usar los de prueba en producción no genera ingresos,
 * y pulsar tus propios anuncios reales puede costarte la cuenta de AdMob.
 */
class GestorAnuncios(private val actividad: Activity) {

    private var recompensado: RewardedAd? = null
    private var intersticial: InterstitialAd? = null
    private var partidasTerminadas = 0

    /**
     * `MobileAds.initialize` hace E/S de red, así que va fuera del hilo principal
     * (si no, se nota un tirón al abrir la app). Las cargas de anuncios, en cambio,
     * tienen que volver al hilo de interfaz.
     */
    fun iniciar() {
        Thread {
            MobileAds.initialize(actividad) {
                actividad.runOnUiThread {
                    cargarRecompensado()
                    cargarIntersticial()
                }
            }
        }.start()
    }

    // ───────── Anuncio recompensado: pistas extra ─────────

    private fun cargarRecompensado() {
        RewardedAd.load(
            actividad,
            ID_RECOMPENSADO,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(anuncio: RewardedAd) {
                    recompensado = anuncio
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Recompensado no disponible: ${error.message}")
                    recompensado = null
                }
            },
        )
    }

    val hayRecompensado: Boolean get() = recompensado != null

    /**
     * Muestra el anuncio y luego entrega la recompensa.
     * Si no hay anuncio cargado, se entrega igualmente.
     */
    fun mostrarRecompensado(onRecompensa: () -> Unit) {
        val anuncio = recompensado
        if (anuncio == null) {
            onRecompensa()
            cargarRecompensado()
            return
        }

        anuncio.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                recompensado = null
                cargarRecompensado()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                recompensado = null
                cargarRecompensado()
                onRecompensa()
            }
        }

        anuncio.show(actividad) { onRecompensa() }
    }

    // ───────── Intersticial: al terminar un puzzle ─────────

    private fun cargarIntersticial() {
        InterstitialAd.load(
            actividad,
            ID_INTERSTICIAL,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(anuncio: InterstitialAd) {
                    intersticial = anuncio
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Intersticial no disponible: ${error.message}")
                    intersticial = null
                }
            },
        )
    }

    /** Se llama al salir de una partida terminada. Nunca durante el puzzle. */
    fun alTerminarPartida(onContinuar: () -> Unit) {
        partidasTerminadas++
        val toca = partidasTerminadas % PARTIDAS_ENTRE_ANUNCIOS == 0
        val anuncio = intersticial

        if (!toca || anuncio == null) {
            onContinuar()
            if (anuncio == null) cargarIntersticial()
            return
        }

        anuncio.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                intersticial = null
                cargarIntersticial()
                onContinuar()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                intersticial = null
                cargarIntersticial()
                onContinuar()
            }
        }
        anuncio.show(actividad)
    }

    companion object {
        private const val TAG = "Anuncios"

        /** Cada cuántas partidas terminadas aparece un intersticial. */
        private const val PARTIDAS_ENTRE_ANUNCIOS = 3

        /** Pistas que da ver un anuncio recompensado. */
        const val PISTAS_POR_ANUNCIO = 2

        // IDs de PRUEBA de Google. Cambiar por los reales antes de publicar.
        private const val ID_RECOMPENSADO = "ca-app-pub-3940256099942544/5224354917"
        private const val ID_INTERSTICIAL = "ca-app-pub-3940256099942544/1033173712"
    }
}
