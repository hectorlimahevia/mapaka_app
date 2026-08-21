# Mapaka — Documento global del proyecto

Versión: 1.0 · Estado: fase de diseño cerrada, pendiente de implementación
Este documento es el mapa del proyecto: qué es Mapaka, cómo está construido, por qué se tomó cada decisión, y dónde encontrar el detalle de cada pieza. Si solo puedes leer un archivo antes de ponerte a trabajar en el proyecto (tú, Code, o cualquier otra persona que se sume), es este.

---

## 1. Qué es Mapaka

Mapaka es una aplicación familiar para gestionar de forma centralizada la paga, el ahorro, las tareas domésticas, las recompensas y el tiempo de pantalla de los hijos de una unidad familiar. Nace como proyecto para uso doméstico propio, con la puerta abierta a comercializarse más adelante si funciona bien — esto último no es la prioridad actual, así que ninguna decisión tomada hasta ahora depende de monetización, tiendas de apps de pago, ni infraestructura de nivel empresarial.

Hay dos roles de usuario con permisos muy distintos:

- **PARENT** (adulto administrador): control total — crea y gestiona hijos, tareas, reglas de paga y de pantalla, aprueba o rechaza toda recompensa, consulta históricos.
- **CHILD** (hijo/a): rol de consulta y ejecución limitada — ve su saldo, sus tareas, sus objetivos de ahorro y su tiempo de pantalla; puede marcar tareas como hechas, pero nunca aprobar nada ni alterar saldos directamente.

Ambos roles entran con un **PIN numérico de 4 dígitos** — no hay contraseña alfanumérica en ningún punto de la app. Es una decisión deliberada de coste cero: una contraseña con recuperación por correo obligaría a dar de alta un servicio de envío de email solo para eso. Ver la sección 4 para el flujo completo de alta y recuperación.

Principio de negocio no negociable, heredado del documento funcional original: **ninguna acción que genere recompensa económica o de tiempo de pantalla es efectiva hasta que un adulto la valida.** Todo el sistema de ledgers (dinero y pantalla) está construido alrededor de esa regla.

---

## 2. Dónde está cada cosa (índice de archivos del proyecto)

| Archivo | Qué contiene |
|---|---|
| `Família+.pdf` | Especificación funcional y técnica original (84 páginas): visión de producto, roles, arquitectura de referencia, modelo de datos completo tabla por tabla, sistema de paga y de tiempo de pantalla. Sigue siendo la fuente de verdad para el modelo de datos y las reglas de negocio base — este documento global no la duplica, la resume y la conecta con todo lo decidido después. |
| `mapaka_documento_global.md` | Este documento — la vista de conjunto. |
| `mapaka_prompts_code.md` | 12 prompts secuenciales, listos para pegar en Code, que implementan el proyecto de principio a fin (bootstrap, sistema de diseño, base de datos, backend, navegación y autenticación, registro de familia y recuperación de PIN, pantallas, feature NFC, empaquetado Android, despliegue, verificación). |
| `mapaka-logo.svg` | El logo definitivo ("Cercles de família"), en SVG, listo para usar tal cual. |
| `mapaka_mockup.html` | Maqueta HTML interactiva y animada con el diseño aprobado: navegación CHILD/PARENT (móvil y escritorio), y el flujo completo de sesión NFC en la tableta compartida. Publicada también como artefacto propio ("Mapaka Maqueta") — Code debe reproducir fielmente su aspecto, no reinterpretarlo. |
| `mapaka_login_animacions.html` | Las 3 propuestas de animación de entrada del logo en el login; la elegida ("Muntatge en cascada") es la referencia de coreografía exacta para el Prompt 6. |

---

## 3. Identidad de marca

**Nombre:** Mapaka. Nombre inventado, elegido tras descartar varias rondas de candidatos (Famy, Mainada, Estalvia, Llavor, Fita, Arrel, Bri, Kesa, Niuet, Maina, Kukumi entre otros) por conflictos reales de marca, app o dominio ya existentes. Mapaka no tuvo ningún conflicto detectado en ninguna comprobación.

**Logo — "Cercles de família":** tres círculos superpuestos de tamaño y color distinto (adulto + dos hijos), sin figuración literal ni letras — la superposición de color es lo que transmite "unidad familiar".

```svg
<svg viewBox="0 0 200 200">
  <circle cx="82" cy="105" r="56" fill="#6C4DFF" opacity="0.88"/>
  <circle cx="138" cy="90" r="30" fill="#FF5D8F" opacity="0.88"/>
  <circle cx="128" cy="150" r="24" fill="#FFC93C" opacity="0.92"/>
</svg>
```

**Paleta — "Joc en família":** tonos vivos (púrpura/coral/amarillo) para transmitir juego de cara al hijo, con una variante desaturada de los mismos tonos para la vista del adulto (transmite control sin cambiar de marca). Colores semánticos (verde=ingreso, rojo=gasto, ámbar=pendiente) fijos en ambas variantes, nunca reutilizados para otra cosa.

```css
:root {
  --primary: #6C4DFF; --secondary: #FF5D8F; --accent: #FFC93C;
  --bg: #FFFDF7; --text: #2A2145; --muted: #8A84AD;
  --success: #2ECC71; --error: #FF5252; --warning: #F5A623;
  --primary-adult: #4B3AA6; --secondary-adult: #C9486B; --accent-adult: #D9A72E; --bg-adult: #F6F5F9;
}
```

**Tipografía:** Baloo 2 (redondeada, juguetona) para titulares y botones; Nunito Sans (más neutra, con cifras tabulares) para cuerpo de texto y, sobre todo, para cualquier cantidad de dinero o minutos — la seriedad que le falta a una fuente redondeada se recupera con peso tipográfico alto (800/900) en los importes, no cambiando de fuente.

**Navegación:** un único componente de AppShell adaptativo, no dos separados. Por debajo de 768px, cualquier rol ve una barra inferior de 4 ítems — CHILD: Inici, Tasques, Objectius, Pantalla; PARENT: Resum, Aprovacions (con contador de pendientes), Fills, Configuració. Por encima de 768px, el rol PARENT cambia a panel lateral fijo con los mismos 4 ítems; CHILD no tiene variante de escritorio. La vista "PARENT en móvil" reutiliza literalmente el mismo patrón de barra inferior que CHILD — solo cambian los ítems — para no duplicar trabajo de implementación ni introducir un tercer patrón de navegación.

**Idioma de la interfaz:** catalán, en todo texto visible al usuario final.

**Animación de login — "Muntatge en cascada":** cada círculo del logo llega de una dirección distinta y encaja con un ligero rebote, con un pequeño retardo escalonado entre los tres; el wordmark "Mapaka" aparece después con un colapso de espaciado entre letras. Elegida entre 3 propuestas — la referencia visual exacta (keyframes CSS) está en `mapaka_login_animacions.html`.

---

## 4. Registro de familia y recuperación de PIN

El documento funcional original (`Família+.pdf`) nunca llegó a definir esto: solo describe el login con una cuenta ya existente, sin un flujo de alta ni de recuperación de acceso. Se ha diseñado desde cero, con estas reglas:

- **Alta:** un asistente público de 4 pasos — nombre de la familia, datos del primer PARENT (nombre + PIN), alta de los hijos (nombre, edad, avatar, PIN de cada uno — opcional en este paso, se puede completar después), y una pantalla final que muestra un **código de recuperación de un solo uso**, visible una única vez, que hay que guardar fuera de la app (en papel, como el PUK de una SIM).
- **Padres adicionales:** se añaden desde dentro de la app (Configuració), no en el alta pública — requiere estar ya autenticado como PARENT de esa familia.
- **PIN olvidado, con más de un PARENT:** lo resetea el otro padre/madre desde Configuració → Fills i pares. No hay otro camino — evita depender de correo.
- **PIN olvidado, con un solo PARENT:** se introduce el código de recuperación generado en el alta; si es válido, permite definir un PIN nuevo y el código queda consumido (hay que generar uno nuevo después).

Detalle técnico completo (endpoints, campos nuevos en la tabla `families`, tests requeridos) en el Prompt 6 de `mapaka_prompts_code.md`.

---

## 5. Arquitectura técnica

```
Browser / PWA
     │ HTTPS
     ▼
Frontend Vue 3 (Vite, TypeScript, Vue Router, Pinia, Axios)
     │ REST / JSON
     ▼
Backend Spring Boot (Java 21, Spring Security JWT, Spring Data JPA, Flyway)
     │
     ▼
PostgreSQL (Neon en producción, contenedor local en desarrollo)
```

Monorepo con `backend/`, `frontend/`, `docs/`, `docker-compose.yml` (desarrollo) y `docker-compose.prod.yml` — estructura heredada de `Família+.pdf` sección 5, sin cambios salvo el renombrado de "Família+" a "Mapaka".

El modelo de datos completo (families, users, child_profiles, allowance_rules, money_transactions, savings_goals, screen_time_rules, etc.) está documentado tabla por tabla en `Família+.pdf` secciones 6-11. A eso se le añaden tres tablas nuevas para la funcionalidad NFC (ver sección 6 de este documento): `screen_tag`, `screen_session`, `screen_session_participant`.

---

## 6. Cómo se distribuye la app (sin coste, sin tiendas de apps)

Es una única aplicación — no hay tres versiones distintas del código, solo cambia cómo llega a cada dispositivo:

- **Android:** empaquetada con Capacitor (envuelve la misma build de Vue 3 en un proyecto Android nativo) y distribuida como APK de instalación directa — se pasa el archivo al dispositivo y se instala activando "orígenes desconocidos", sin pasar por Google Play. Esto da acceso a un plugin NFC nativo real: lectura activa desde un botón dentro de la app, y escritura de etiquetas nuevas sin herramientas externas.
- **iOS:** sin empaquetado nativo. Se instala como PWA desde Safari (Compartir → Añadir a inicio). El NFC en iOS usa un patrón pasivo: la etiqueta física lleva grabada una URL, y es el propio sistema operativo quien la detecta y abre la PWA — no hay escaneo activo posible desde la interfaz en iOS.

**Por qué esta combinación y no una app nativa o una publicada en las tiendas:** el Mac disponible (MacBook Pro de 2015) no puede ejecutar una versión de Xcode lo bastante reciente como para compilar o publicar en la App Store bajo los requisitos actuales de Apple — esto afecta por igual a una app 100% nativa y a una empaquetada con Capacitor, así que no es un argumento a favor de ir nativo. Se descartó también ir 100% nativo (Swift + Kotlin) porque hubiera significado escribir y mantener la interfaz dos veces, sin reutilizar nada del frontend Vue 3 ya diseñado.

---

## 7. La funcionalidad de tiempo de pantalla por NFC

Un objeto físico impreso en 3D, compartido por la familia, con una etiqueta NFC embebida (tipo NTAG213, sin batería ni electrónica). El flujo:

1. Tocar el objeto contra la tableta (o pulsar "Iniciar temps" si el dispositivo no lee NFC) arranca un cronómetro que **no está ligado a ningún hijo todavía**.
2. Tocar de nuevo, o pulsar "Aturar", detiene el cronómetro.
3. Aparece "Qui ha jugat?": selección múltiple de los hijos de la familia.
4. El tiempo transcurrido se reparte a partes iguales entre los seleccionados, generando una transacción de consumo en el Screen Time Ledger de cada uno — permitiendo saldo negativo, que se recupera con la siguiente paga o bonificación de tiempo, igual que ya funciona el ledger de dinero.

Este flujo funciona igual en cualquier dispositivo (Android, iOS, escritorio) porque no depende de la API activa Web NFC del navegador (sin soporte real fuera de Chrome/Android) sino de que el sistema operativo lea la URL grabada en la etiqueta — el escaneo activo desde dentro de la app es una mejora exclusiva de la versión Android empaquetada con Capacitor, no un requisito para que la función baseline funcione.

---

## 8. Infraestructura y hosting (coste total: 0 €)

| Pieza | Dónde vive | Por qué |
|---|---|---|
| Base de datos | **Neon** (PostgreSQL, plan gratuito) | Se descartó Supabase por su límite de 2 proyectos gratuitos y porque se pausa a los 7 días de inactividad requiriendo reactivación manual desde el panel — Neon se autorreactiva en segundos, sin intervención. Tampoco hacía falta ninguna de las funciones extra de Supabase (Auth, Storage, API autogenerada), porque el backend propio de Spring Boot ya las cubre todas. |
| Backend | **Render** (plan gratuito, servicio web Docker) | Se descartó Railway porque su nivel gratuito real dura solo 30 días; después exige el plan Hobby (5 $/mes). Se descartó Fly.io porque ya no ofrece plan gratuito para cuentas nuevas. Render sí ofrece 750 horas de instancia al mes sin coste — suficiente para un servicio encendido todo el mes. Contrapartida: el servicio se duerme a los 15 minutos de inactividad y tarda ~1 minuto en despertar; el frontend debe mostrar un estado de carga claro para no dejar al usuario ante una pantalla en blanco durante ese arranque. |
| Frontend | Servido por el mismo backend / distribuido como APK (Android) o accedido como PWA (iOS) | Sin coste de hosting adicional — no hay una CDN ni un hosting de frontend separado necesario para el volumen de este proyecto. |

---

## 9. Estado del proyecto y próximos pasos

Todo lo anterior está decidido y documentado. El siguiente paso es la implementación: abrir `mapaka_prompts_code.md` y ejecutar los 12 prompts en orden con Code, revisando el resultado de cada uno antes de pasar al siguiente. Ese documento contiene las instrucciones técnicas exactas; este documento es el que explica el porqué de cada una, para volver a él cuando haga falta recordar una decisión o poner a alguien nuevo en contexto.

Decisiones explícitamente pendientes o fuera de alcance por ahora: publicación en App Store / Google Play (descartada mientras no cambien las prioridades), monetización o modelo comercial (fuera de alcance actual).
