# Pixa

**Nonogramas** (picture logic / "pinta por números") para Android. Nativo en **Kotlin + Jetpack
Compose**, sin servidor, sin cuentas y sin conexión obligatoria: los puzzles se **generan en el
móvil** y se garantiza que tienen **solución única y deducible por lógica**, nunca a base de
adivinar.

<p align="center">
  <em>5 × 5 · 10 × 10 · 15 × 15 · un puzzle del día igual para todo el mundo</em>
</p>

---

## Por qué este juego

Un nonograma no necesita texto (así que vale igual en España que en Japón), no necesita
ilustraciones (el dibujo **es** la rejilla), se juega en sesiones largas de 10–20 minutos y se
genera infinito sin coste de servidor. Para un juego hecho por una persona, es de lo poco que
cumple las cuatro cosas a la vez.

---

## La parte interesante: el motor

El código que de verdad tiene chicha está en [`engine/`](app/src/main/java/com/riscart/pixa/engine).

### Resolver una línea sin probar combinaciones

`LineSolver` deduce lo que se puede saber **con certeza** de una fila o columna usando
programación dinámica, no fuerza bruta:

1. Tabla `viable[i][j]`: ¿es posible colocar las pistas a partir de la pista `j` en la casilla `i`?
2. Pasada hacia delante marcando, para cada casilla, si **puede** estar pintada y si **puede**
   estar vacía en alguna colocación válida.
3. Si solo puede estar pintada → pintada. Si solo puede estar vacía → tachada. Si puede las dos,
   se queda sin decidir.

Esto es lo que da el clásico solapamiento: una línea de 5 con la pista `[4]` tiene siempre
pintadas las tres del medio, sin saber todavía dónde empieza el bloque.

```
?????  +  [4]   →   ?###?
```

### Generar puzzles que no obliguen a adivinar

`Generator` crea una rejilla aleatoria, calcula sus pistas y **solo acepta el puzzle si el
solucionador lógico es capaz de terminarlo** (`Solver` alterna filas y columnas hasta que deja de
avanzar). Si se atasca, lo descarta y prueba otro. Por eso:

- la solución es **única** (si hubiera dos, la lógica se atascaría);
- **nunca** hace falta adivinar y volver atrás.

Es la diferencia entre un nonograma bien hecho y uno que da rabia.

Además filtra los puzzles "feos": ni demasiado vacíos ni demasiado llenos, y sin filas enteras en
blanco de más.

### 32 dibujos hechos a mano

Un nonograma generado al azar es un puzzle correcto, pero al terminarlo te quedas mirando una
mancha de píxeles. La gracia del género es que **el panel terminado sea algo**: terminas y resulta
que era un gato.

[`Catalogo.kt`](app/src/main/java/com/riscart/pixa/engine/Catalogo.kt) tiene 32 dibujos dibujados
a mano (10 de 5 × 5, 16 de 10 × 10 y 6 de 15 × 15), y **todos pasan por el mismo filtro que los
generados**: un test comprueba que el solucionador lógico los termina enteros. Si un dibujo
obligara a adivinar, el test lo caza antes de que llegue a nadie.

Cada dificultad sirve primero un dibujo que aún no hayas descubierto; cuando se acaban, vuelve a
los generados, que son infinitos. El nombre solo se ve **al terminar**.

### Tests

27 tests en [`EngineTest.kt`](app/src/test/java/com/riscart/pixa/engine/EngineTest.kt) con una
notación compacta (`"#?."` = pintada / sin decidir / tachada): deducciones concretas, pistas
imposibles, la garantía de que el solucionador **nunca** se contradice, puzzles generados a 5, 10
y 15 que el solucionador lógico resuelve enteros, y determinismo por semilla.

```bash
./gradlew test
```

Cubren el motor, el catálogo de dibujos y la generación.

---

## El diseño: azulejo

El tablero no es una rejilla, es un **panel de azulejos**, y jugar es ir colocando piezas
vidriadas sobre la junta. De ahí sale todo lo demás y por eso la app no se parece a las otras
del género, que son casi todas Material plano o dibujos de colorines.

**La pieza** ([`Azulejo.kt`](app/src/main/java/com/riscart/pixa/ui/Azulejo.kt)) es la unidad de
todo: el tablero, las tarjetas, los botones y hasta el icono están hechos de ella. Lleva tres
capas baratas de pintar — el esmalte degradado, la luz entrando por arriba a la izquierda y el
borde de abajo a la derecha hundiéndose en la junta — y un parámetro de `relieve`, porque las
piezas claras necesitan mucho menos que las de color: con el mismo parecían nubes hinchadas.

| | |
|---|---|
| Cobalto `#1B4F8F` | la pieza puesta |
| Blanco roto `#F8F3E9` | la pieza por poner |
| Junta `#C9BFAE` | el panel bajo las piezas |
| Albero `#D9A441` | la olambrilla y las pistas ya resueltas |

Detalles que hacen el estilo:

- **La junta cada cinco casillas es más oscura**, como la llaga que separa los paños en un panel
  de verdad. Es lo que deja contar de un vistazo, que en un nonograma es media partida.
- **Una cenefa de olambrillas** remata la cabecera, con sus listeles arriba y abajo.
- **El icono** es un panelito de cuatro piezas con el rombo dorado en el encuentro.
- **En modo oscuro la junta va más clara que la pieza vacía**, al revés que en claro: si no, el
  panel se convierte en una mancha negra y no se ve dónde está cada casilla.
- **Nada de reflejo en las piezas del tablero.** A tamaño de casilla el brillo se leía como una
  rayada en el esmalte, así que se queda solo para las piezas grandes.

### Tipografía

Las dos van **empaquetadas** en la app (licencia OFL, copia en
[`assets/licencias`](app/src/main/assets/licencias)), así que se ve igual en cualquier móvil y
sin pedirle nada a la red:

- **Bricolage Grotesque** para los titulares, con su eje de tamaño óptico: una grotesca con
  rarezas, no la Roboto del sistema.
- **IBM Plex Sans Condensed** para el resto. Las cifras estrechas se leen bien apiladas en las
  pistas, que es donde más número por centímetro hay.

### Rendimiento

Un 15 × 15 son 225 piezas y cada una lleva tres degradados. Dibujarlas de verdad en cada
fotograma mientras arrastras el dedo sería tirar el rendimiento por la ventana, así que se
**cuecen una vez** en un `ImageBitmap` por tamaño y color (`cocerPieza`) y el tablero solo
estampa imágenes.

---

## Lo que hace que vuelvas

Sin trucos sucios: aquí no hay vidas que se agotan, ni temporizadores que te meten prisa, ni
notificaciones pesadas. Lo que engancha es que **jugar sea agradable** y que haya algo al final.

- **Los dibujos se coleccionan.** La pantalla de inicio lleva la cuenta («7 de 32 dibujos») y cada
  dificultad enseña cuántos le quedan.
- **El tacto.** Colocar una pieza, cerrar una línea y meter la pata se notan **distinto** en la
  mano ([`Haptica.kt`](app/src/main/java/com/riscart/pixa/ui/Haptica.kt)). Es háptica del sistema,
  así que respeta los ajustes del móvil.
- **Las piezas se asientan.** Entran pequeñas, se pasan un pelín y se colocan. Medio segundo de
  trabajo que separa «marcar casillas» de «colocar azulejos».
- **La partida a medias se guarda.** Llevas medio 15 × 15, te llaman por teléfono, y al volver
  está donde lo dejaste. Era el peor momento posible y ya no existe.
- **Las pistas enseñan.** No revelan una casilla al azar: pasan el solucionador por el tablero tal
  y como está y descubren una que **se podía deducir ahora mismo** — justo la jugada que tenías
  delante. Y toman como cierto solo lo que está pintado, nunca tus cruces, que pueden estar mal.
- **Se puede compartir el panel terminado** como imagen limpia, con su nombre y tu tiempo. Es la
  mejor publicidad posible de un juego así y no cuesta un céntimo.
- **Las reglas se explican la primera vez.** Mucha gente se baja un nonograma sin saber qué es;
  cuatro líneas con un ejemplo dibujado es de lo más barato que se puede hacer por la retención
  del primer día.

---

## Responsive

Probado en emulador a 360 × 640 dp, en un móvil normal, en tablet (1067 dp) y en apaisado, y con
la letra del sistema al 150 %.

- **En apaisado el tablero se va a un lado y los controles al otro.** En una columna no caben los
  dos y el panel se quedaría en un sello.
- **El contenido tiene un ancho máximo** (560 dp) y se centra: en una tablet, una columna de
  2.000 px no se lee. El tablero también tiene tope, porque a partir de cierto tamaño las casillas
  dejan de ser cómodas y pasan a ser ridículas.
- **Las pistas del tablero se dibujan en píxeles**, no en `sp`, así que la escala de letra del
  sistema no descuadra la rejilla; los textos de la interfaz sí la respetan y se adaptan.
- **Limitación conocida:** un 15 × 15 en un móvil de 360 dp deja casillas de unos 19 dp, por
  debajo del objetivo táctil cómodo. Lo natural sería poder hacer zoom con dos dedos; está sin
  hacer a propósito, porque el gesto convive mal con el arrastre para pintar y no se puede
  verificar bien sin un dispositivo con multitáctil real.

---

## Decisiones de juego

- **Pintar está validado; tachar es libre.** Si pintas donde no toca, cuenta error y la casilla
  se tacha sola. Así el tablero nunca se queda en un estado sin solución y no hay que llevar la
  cuenta mentalmente de los fallos. Las cruces son apuntes tuyos y no se penalizan.
- **El tablero se dibuja en un `Canvas`**, no con 225 composables. A 15 × 15, arrastrar el dedo
  pintando tiene que ir fino.
- **El puzzle del día sale de la fecha** (`semillaDelDia()`), no de un servidor: todo el mundo
  juega el mismo y la app sigue funcionando sin conexión.
- **El reloj se para** cuando la app pasa a segundo plano: ver un anuncio o atender una llamada no
  te arruina el récord.
- **Las cruces no se penalizan nunca**: son apuntes tuyos.
- **Al ganar, el cartel no tapa el dibujo.** Ocupa el sitio de los controles, porque el dibujo
  terminado es el premio.
- **SharedPreferences** para el progreso. Son cuatro enteros; Room o DataStore aquí sobran.

---

## Monetización

Dos vías, ambas con AdMob y pensadas para no espantar al jugador:

| Dónde | Tipo | Regla |
|---|---|---|
| Botón "Ver anuncio +2" | **Recompensado** | Solo aparece cuando te quedas sin pistas. Siempre voluntario. |
| Al terminar un puzzle | **Intersticial** | Una de cada **3** partidas, y **jamás** a mitad de una. |

El juego **nunca se bloquea por un anuncio**: si no carga, la recompensa se da igual
([`GestorAnuncios.kt`](app/src/main/java/com/riscart/pixa/anuncios/GestorAnuncios.kt)).

### Poner tus identificadores reales

Ahora mismo están los **de prueba de Google** (funcionan sin cuenta y no generan ingresos). Para
cobrar:

1. Crea una cuenta en [AdMob](https://admob.google.com) y da de alta la app.
2. Crea dos bloques de anuncios: uno **recompensado** y uno **intersticial**.
3. Sustituye:

   | Dónde | Qué |
   |---|---|
   | `AndroidManifest.xml` → `APPLICATION_ID` | `ca-app-pub-XXXX~YYYY` |
   | `GestorAnuncios.kt` → `ID_RECOMPENSADO` | `ca-app-pub-XXXX/ZZZZ` |
   | `GestorAnuncios.kt` → `ID_INTERSTICIAL` | `ca-app-pub-XXXX/WWWW` |

> ⚠️ Con los identificadores reales, **no pulses tus propios anuncios**: Google lo detecta como
> tráfico inválido y cierra cuentas por eso. Para probar, usa siempre los de prueba o registra tu
> móvil como dispositivo de test.

---

## Compilar

Requisitos: **JDK 21** y el SDK de Android (`compileSdk 36`).

```bash
./gradlew assembleDebug        # APK de depuración
./gradlew test                 # tests del motor
./gradlew bundleRelease        # AAB firmado para Play (necesita keystore.properties)
```

### Firma

El AAB de release se firma con una clave que **no está en el repositorio**:

```bash
keytool -genkeypair -v -keystore pixa-release.jks -alias pixa \
        -keyalg RSA -keysize 2048 -validity 10000
cp keystore.properties.example keystore.properties   # y rellénalo
```

`.gitignore` ya excluye `*.jks` y `keystore.properties`.

> **Guarda el `.jks` y sus contraseñas en un sitio seguro.** Si pierdes la clave de subida se
> puede pedir un reseteo a Google, pero es un trámite lento y evitable.

---

## Publicar en Google Play

1. **Cuenta de desarrollador** — 25 $ (pago único) en [Play Console](https://play.google.com/console).
2. **Testers**: una cuenta **personal** nueva necesita **12 testers que prueben la app 14 días
   seguidos** antes de poder pasar a producción. Es el requisito que más tiempo come: móntalo
   cuanto antes con un grupo de prueba cerrado. (Las cuentas de **organización** no lo tienen.)
3. **Sube el AAB** de `app/build/outputs/bundle/release/`.
4. **Política de privacidad**: obligatoria al llevar anuncios. Tienes una plantilla lista en
   [`PRIVACIDAD.md`](PRIVACIDAD.md); súbela a una URL pública (GitHub Pages vale) y pégala en la
   ficha.
5. **Seguridad de los datos**: declara que un SDK de terceros (AdMob) recoge **identificador de
   publicidad** y **datos de uso/diagnóstico**. La app en sí no recoge nada: el progreso se queda
   en el móvil.
6. **Anuncios**: marca "Sí, contiene anuncios".
7. **Clasificación por edades**, público objetivo y ficha (título, descripción, icono 512×512,
   gráfico de cabecera 1024×500 y capturas).

### Si el público va a incluir menores

Hay que activar el tratamiento para menores en AdMob y pasar `TagForChildDirectedTreatment`. Lo
más sencillo es declarar la app para **mayores de 13** y evitarse ese bloque.

---

## Expectativas realistas

Esto hay que decirlo claro: **la app no es la que da dinero, la retención y la visibilidad sí**.
Un juego de puzzles con anuncios ronda los **0,5–2 € por cada 1.000 partidas** en España, así que
sin instalaciones no hay negocio. Lo que de verdad mueve la aguja:

- **el puzzle del día** (da motivo para volver mañana — por eso existe la racha);
- **las primeras 20 valoraciones**, que son las que hacen que Play empiece a enseñarla;
- unas **capturas buenas** en la ficha, que es lo único que ve el 90 % de la gente;
- y publicar **actualizaciones pequeñas y seguidas** los primeros meses.

---

## Siguientes pasos naturales

- Zoom con dos dedos para los 15 × 15 en pantallas pequeñas.
- Más dibujos: el catálogo está hecho para crecer sin tocar código, solo añadiendo filas de
  texto, y el test avisa si alguno obliga a adivinar.
- Sonido (un «clac» cerámico al colocar) además de la vibración.
- Logros y estadísticas por tamaño.

---

## Licencia

MIT
