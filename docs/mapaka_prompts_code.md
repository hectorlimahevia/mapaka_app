# Mapaka — Prompts per a Code

Aquest document recull totes les decisions preses durant la fase de disseny (marca, color, tipografia, navegació, autenticació i registre, la funcionalitat NFC de temps de pantalla i l'estratègia de distribució multiplataforma) convertides en instruccions llestes perquè Code (l'agent de codi) implementi el projecte pas a pas.

## Com fer servir aquest document

No enganxis tots els prompts d'un cop. Cada bloc és una unitat de treball coherent i pensada per executar-se **en ordre**, revisant el resultat abans de passar al següent — igual que faríeu amb tasques d'un sprint. Aquesta és una pràctica general en prompting per a codi: un prompt petit i ben delimitat és més fàcil de verificar i de corregir que un prompt gegant que ho demana tot alhora, i si alguna cosa surt malament saps exactament en quin punt buscar l'error.

Cada prompt assumeix que Code té accés al repositori i al document `Família+.pdf` (l'especificació funcional i tècnica original: arquitectura Vue 3 + Spring Boot + PostgreSQL, rols PARENT/CHILD, ledgers de diner i de pantalla, model de base de dades). Els prompts d'aquí **no repeteixen** aquesta informació — hi referencien perquè Code la llegeixi, en lloc de duplicar-la i arriscar-se que quedi desactualitzada.

---

## Context de disseny (referència per a tots els prompts)

**Marca:** Mapaka — nom inventat, verificat sense conflictes de marca ni de domini.

**Design tokens (CSS custom properties):**

```css
:root {
  --primary: #6C4DFF;
  --secondary: #FF5D8F;
  --accent: #FFC93C;
  --bg: #FFFDF7;
  --text: #2A2145;
  --muted: #8A84AD;
  --success: #2ECC71;
  --error: #FF5252;
  --warning: #F5A623;

  --primary-adult: #4B3AA6;
  --secondary-adult: #C9486B;
  --accent-adult: #D9A72E;
  --bg-adult: #F6F5F9;

  --font-heading: "Baloo 2", sans-serif;
  --font-body: "Nunito Sans", sans-serif;
}
[data-role="parent"] { --primary: var(--primary-adult); --secondary: var(--secondary-adult); --accent: var(--accent-adult); --bg: var(--bg-adult); }
```

**Logo — "Cercles de família":** tres cercles superposats de mida i color diferents (adult + dos fills), sense figuració literal ni lletres — la superposició de color és el que transmet "unitat familiar". Aprovat tal qual, sense variacions de color:

```svg
<svg viewBox="0 0 200 200">
  <circle cx="82" cy="105" r="56" fill="#6C4DFF" opacity="0.88"/>
  <circle cx="138" cy="90" r="30" fill="#FF5D8F" opacity="0.88"/>
  <circle cx="128" cy="150" r="24" fill="#FFC93C" opacity="0.92"/>
</svg>
```

Fes servir aquest SVG tal qual (sense redibuixar-lo) per a: favicon i icones de PWA (genera els talls de mida 192×192 i 512×512 exigits pel manifest.json a partir d'aquest mateix viewBox), la marca del panell lateral de PARENT, i la pantalla de login/splash. No el combinis amb cap altre símbol ni lletra — és una marca autosuficient, pensada per anar sempre acompanyada del wordmark "Mapaka" en Baloo 2 quan hi hagi espai, i sola quan no n'hi hagi (favicon, icona d'app).

Regla no negociable: qualsevol xifra monetària o de minuts ha de portar `font-variant-numeric: tabular-nums`, i el pes tipogràfic dels imports destacats ha de ser 800/900 (Nunito Sans compensa amb pes el que li falta de "seriositat" respecte a una font més neutra).

**Navegació:** patró adaptatiu pel mateix component d'AppShell, no dos components separats. Per sota de `768px`, qualsevol rol veu una barra inferior de 4 ítems — CHILD: Inici, Tasques, Objectius, Pantalla; PARENT: Resum, Aprovacions (amb badge de pendents), Fills, Configuració. Per sobre de `768px`, el rol PARENT canvia a panell lateral fix (Resum familiar, Aprovacions, Fills, Configuració); CHILD no té vista d'escriptori pròpia perquè no és el seu cas d'ús principal. Referència visual exacta: els tres frames de l'artefacte mapaka-maqueta-animada / `mapaka_mockup.html` — "Vista CHILD — mòbil", "Vista PARENT — mòbil" i "Vista PARENT — escriptori".

**Idioma de la interfície:** català, en tots els textos visibles (etiquetes, botons, missatges). Els prompts i comentaris de codi poden ser en castellà/anglès, però cap text d'usuari final.

**Autenticació:** PIN numèric de 4 dígits per a **tots dos rols**, PARENT inclòs — no hi ha contrasenya alfanumèrica enlloc de l'aplicació. Es tria expressament per mantenir la infraestructura a cost zero: una contrasenya amb recuperació per correu exigiria donar d'alta un servei d'enviament d'email transaccional només per a aquest propòsit. El PIN es guarda sempre com a hash (mai en clar ni reversible) al mateix camp `password_hash` de la taula `users`, independentment del rol. Vegeu el Prompt 6 per al flux complet de creació de família, alta de perfils i recuperació de PIN.

**Animació d'entrada del login — "Muntatge en cascada":** cada cercle del logo arriba d'una direcció diferent (el primari des de sota, el secundari des de dalt-dreta, l'accent des de baix-esquerra) i encaixa amb un lleuger rebot (`cubic-bezier(.2,.9,.3,1.4)`), amb un petit retard esglaonat entre els tres (~140ms). El wordmark "Mapaka" apareix després, amb un col·lapse de `letter-spacing` combinat amb un fade-in. Implementació de referència exacta (keyframes CSS inclosos, llestos per portar a un component Vue): `mapaka_login_animacions.html`, proposta 2. Respecta `prefers-reduced-motion` (desactiva l'animació i mostra l'estat final directament).

**Maqueta de referència visual:** l'artefacte `mapaka-maqueta-animada` (HTML autocontingut) conté l'aparença i les transicions exactes aprovades — Code hauria de reproduir-ne fidelment els components, no reinterpretar-los.

**Estratègia de distribució (sense cost, sense App Store ni Google Play):** és una única aplicació (Vue 3 + Spring Boot), no tres — només canvia com arriba a cada dispositiu.

- **Android:** empaquetada amb Capacitor i distribuïda com a APK d'instal·lació directa (sideload), sense passar per Google Play. Això dona accés a un plugin NFC natiu real (lectura I escriptura activa de l'etiqueta des de dins de l'app), a més del patró passiu ja dissenyat.
- **iOS:** sense empaquetat natiu. S'instal·la com a PWA des de Safari (Compartir → Afegir a l'inici). El NFC es queda amb el patró passiu (l'etiqueta porta la URL gravada; el sistema operatiu la llegeix i obre la PWA) — a iOS no hi ha escaneig actiu possible des de la interfície.
- Motiu de la decisió: qualsevol via que acabi passant per Xcode (nativa o Capacitor) per publicar a l'App Store exigeix un macOS que el Mac disponible (2015) no pot executar amb les versions actuals d'Xcode. Es descarta expressament aquesta via mentre no hi hagi un Mac més recent o un servei de compilació al núvol (Codemagic, GitHub Actions amb runner macOS) — no és una limitació d'arquitectura, és una limitació d'eina de compilació.

**Base de dades i hosting:** PostgreSQL (no MySQL — el propi esquema ja fa servir UUID i ENUM natius, que encaixen millor amb Postgres). En desenvolupament es fa servir el PostgreSQL local del `docker-compose.yml` (Prompt 1), sense dependre de connexió a internet. En producció, la base de dades viu a **Neon** (pla gratuït: 0,5 GB, es reactiva sola en segons davant d'inactivitat, sense necessitat d'entrar a cap panell a reactivar-la manualment — al contrari que Supabase, que es va descartar per pausar-se als 7 dies i requerir reactivació manual). Neon exigeix connexió SSL (`sslmode=require`); el datasource de Spring Boot per a l'entorn de producció ha de llegir la cadena de connexió de Neon des d'una variable d'entorn, mai hardcodejada.

**Hosting del backend:** **Render** (pla gratuït, 750 hores d'instància al mes — cobreix un servei encès tot el mes). Es va descartar Railway perquè el seu pla gratuït real dura només 30 dies (crèdit inicial de 5$); passat aquest període caldria pagar el pla Hobby (5$/mes + consum), i no hi havia cap despesa prèvia que ho fes "gratis marginal". Es va descartar Fly.io perquè ja no ofereix pla gratuït per a comptes noves. Important: el servei gratuït de Render "s'adorm" als 15 minuts d'inactivitat i triga uns 60 segons a despertar-se amb la primera petició — cal preveure-ho a la interfície (per exemple, un estat de càrrega clar a la pantalla de la tauleta compartida en comptes de deixar-la en blanc mentre el backend arrenca).

---

## Prompt 1 — Bootstrap del repositori

```
Crea l'estructura de monorepo descrita a la secció 5 de Família+.pdf (backend/, frontend/, docs/, docker-compose.yml, docker-compose.prod.yml). Renombra qualsevol referència textual a "Família+" per "Mapaka" en README, package.json, pom.xml i configuració Docker — el nom del producte ha canviat, l'arquitectura no.

Backend: Java 21, Spring Boot 4.1.x, Spring Web, Spring Data JPA, Spring Security, Bean Validation, Flyway, PostgreSQL Driver, Spring Boot Actuator, OpenAPI/Swagger, tal com especifica la secció 4 del document.

Frontend: Vue 3 + Vite + TypeScript + Vue Router + Pinia + Axios + Chart.js o Apache ECharts, PWA opcional activada des del principi (manifest.json amb el nom "Mapaka").

No implementis encara cap pantalla ni entitat de negoci en aquest pas — només l'esquelet, la configuració de build, i que `docker-compose up` aixequi backend + frontend + PostgreSQL sense errors.

El `docker-compose.yml` (desenvolupament) inclou el seu propi contenidor PostgreSQL local. El `docker-compose.prod.yml` NO ha d'incloure contenidor de base de dades — en producció es connecta a Neon (veure "Base de dades i hosting" al context de disseny) mitjançant una variable d'entorn `DATABASE_URL` amb `sslmode=require`, mai amb credencials hardcodejades al fitxer.
```

---

## Prompt 2 — Sistema de disseny (tokens + components base)

```
Implementa el sistema de disseny de Mapaka al frontend Vue 3:

1. Crea un fitxer de tokens CSS (variables custom properties) amb els valors del bloc "Context de disseny" d'aquest document, incloent la variant [data-role="parent"] per als tons desaturats.
2. Carrega les tipografies Baloo 2 (heading) i Nunito Sans (body) via Google Fonts amb preconnect, i aplica `font-size-adjust: from-font` al body per evitar salts visuals durant la càrrega.
3. Crea components base reutilitzables amb aquests tokens: BaseButton (variant primary/accent/danger), BaseCard, AmountDisplay (força font-variant-numeric: tabular-nums i pes 800+), BadgeCounter.
4. Configura l'atribut data-role a l'element <html> segons el rol de l'usuari autenticat (PARENT o CHILD), perquè tota la resta de l'app hereti automàticament la paleta correcta sense haver de repetir lògica de color a cada component.

Fes servir clamp() amb unitats fluïdes per als títols (h1, h2), seguint el patró: font-size: clamp(mínim, preferit, màxim), amb el màxim no superior a 2.5× el mínim per motius d'accessibilitat.
```

---

## Prompt 3 — Base de dades: migracions Flyway

```
Genera les migracions Flyway per a totes les taules descrites a Família+.pdf (families, users, child_profiles, allowance_rules, monthly_allowances, money_transactions, savings_goals, screen_time_rules, daily_screen_balances, reward_adjustments, monthly_settlements, audit_log — revisa el document sencer, no només les primeres seccions).

Afegeix a més les taules noves per a la funcionalitat de sessió NFC compartida, seguint el mateix estil i convencions (UUID PK, timestamps, restriccions):

- screen_tag: id, family_id (FK), token (VARCHAR únic — el que es grava a l'etiqueta NFC física), created_at, active.
- screen_session: id, screen_tag_id (FK), started_at, ended_at (nullable mentre està activa), elapsed_seconds, status (ENUM: ACTIVE, CLOSED).
- screen_session_participant: id, session_id (FK), child_id (FK), assigned_seconds, created_at.

Cada fila de screen_session_participant, en tancar-se, ha de generar una transacció al Screen Time Ledger existent (screen_time_transactions o l'equivalent que ja defineix el document) amb source_type = 'NFC_SESSION' i source_id apuntant a screen_session_participant.id, permetent saldo negatiu (sense restricció CHECK >= 0) tal com es va decidir.
```

---

## Prompt 4 — Backend: entitats, endpoints REST i seguretat per rol

```
Implementa les entitats JPA i els repositoris per a totes les taules del Prompt 3. Implementa Spring Security amb JWT: els PARENT tenen accés complet, els CHILD només poden llegir el seu propi estat i marcar tasques com a fetes (mai aprovar-les ni alterar saldos), tal com detalla la secció 3 (ROLES) de Família+.pdf.

Endpoints REST necessaris per a la sessió NFC:

- POST /api/screen-tags/{token}/tap — rep el toc de l'etiqueta (o el botó "Simular toc"). Si no hi ha sessió ACTIVE per a aquest screen_tag_id, en crea una i la retorna. Si ja n'hi ha una ACTIVE, la tanca (status=CLOSED, ended_at=now, elapsed_seconds calculat) i la retorna igualment — el mateix toc serveix per iniciar i per aturar.
- POST /api/screen-sessions/{id}/stop — alternativa explícita al segon toc, per al botó "Aturar" de la interfície.
- POST /api/screen-sessions/{id}/assign — rep una llista de child_id seleccionats. Reparteix elapsed_seconds entre ells a parts iguals (arrodoniment: el residu de segons s'assigna al primer child_id de la llista, per no perdre precisió), crea els screen_session_participant corresponents, i genera les transaccions de consum al ledger, permetent saldo negatiu.
- GET /api/families/{id}/children — llista de fills per emplenar el selector "Qui ha jugat?".

Documenta tots els endpoints amb OpenAPI/Swagger, incloent exemples de request/response.
```

---

## Prompt 5 — Frontend: shell de navegació i autenticació

```
Implementa el shell de navegació de Mapaka amb Vue Router:

- Login amb PIN numèric de 4 dígits per a tots dos rols (vegeu "Autenticació" al context de disseny). Per a CHILD, reprodueix el patró "selecciona el teu perfil" de la secció 7.2 de Família+.pdf: primer es tria l'avatar/nom dins la família, després es demana el PIN — mai un camp d'usuari en text lliure. Per a PARENT, un únic camp de PIN n'hi ha prou perquè el correu/usuari ja identifica la família.
- Pantalla de login amb l'animació d'entrada del logo descrita al context de disseny ("Muntatge en cascada").
- Guàrdies de ruta per rol: un CHILD mai pot accedir a rutes de PARENT ni viceversa.
- Component AppShell únic i reutilitzat pels dos rols (no dos components de navegació separats): per sota de 768px de viewport, renderitza sempre una barra inferior fixa de 4 ítems, amb la llista d'ítems (etiqueta, icona, ruta) depenent només del rol — CHILD: Inici, Tasques, Objectius, Pantalla; PARENT: Resum, Aprovacions (amb comptador de pendents en un badge sobre la icona), Fills, Configuració. Per sobre de 768px, el rol PARENT canvia a un panell lateral fix amb els mateixos 4 ítems; el rol CHILD no té variant d'escriptori.
- L'ítem actiu de la navegació ha de portar un indicador animat (transform + transition, no display toggling brusc) que es desplaça entre posicions, reproduint el comportament de l'artefacte mapaka-maqueta-animada — inclosa la variant "Vista PARENT — mòbil", que fa servir exactament el mateix patró de barra inferior que CHILD, només canviant els ítems.
- Aplica l'atribut data-role a <html> just després de l'autenticació, perquè el sistema de disseny del Prompt 2 s'apliqui automàticament.
```

---

## Prompt 6 — Registre de família, alta de perfils i recuperació de PIN

```
Família+.pdf defineix POST /api/families i POST /api/children però no connecta un flux real d'alta ni cap mecanisme de recuperació — implementa'l des de zero seguint aquestes regles:

Backend:
- POST /api/families/register — endpoint públic (sense autenticació prèvia): rep el nom de la família i les dades del primer PARENT (nom, PIN de 4 dígits). Crea la família i el primer usuari PARENT en una única transacció. Genera també un codi de recuperació d'un sol ús (per exemple 8 caràcters alfanumèrics), el desa hashejat a un nou camp `families.recovery_code_hash`, i el retorna en clar **només en aquesta resposta** — no es torna a poder consultar mai més.
- POST /api/families/current/parents — afegir un PARENT addicional. Requereix estar autenticat com a PARENT de la mateixa família.
- POST /api/children ja existeix a Família+.pdf; assegura't que accepta el PIN de 4 dígits en l'alta i el hasheja igual que un password.
- PATCH /api/users/{id}/pin — reseteja el PIN d'un altre membre de la família. Requereix estar autenticat com a PARENT de la mateixa família (un PARENT pot resetejar el PIN de qualsevol CHILD o d'un altre PARENT).
- POST /api/auth/recover — rep el codi de recuperació d'un sol ús i, si coincideix amb el hash de la família, retorna un token temporal de curta durada (per exemple 10 minuts) que només permet cridar PATCH /api/users/{id}/pin sobre el primer PARENT de la família, per definir-hi un PIN nou. El codi es consumeix: un cop fet servir, `recovery_code_hash` es posa a NULL i cal generar-ne un de nou des de Configuració.
- Nou camp a la migració Flyway del Prompt 3: `families.recovery_code_hash VARCHAR(255) NULL`, `families.recovery_code_generated_at TIMESTAMP NULL`.
- Mai registris el PIN ni el codi de recuperació en clar als logs (mateixa regla que ja aplica a `password` a la secció de seguretat de Família+.pdf).

Frontend — assistent d'alta en 4 passos (pantalla pública, abans del login):
1. Nom de la família.
2. Dades del primer PARENT: nom + PIN de 4 dígits (input numèric, doble entrada per confirmar).
3. Afegir fills: nom, edat, color/avatar, PIN de 4 dígits per a cadascun — es poden afegir unL rere l'altre amb un botó "Afegir un altre fill"; el pas es pot saltar i fer-ho més tard des de Configuració.
4. Pantalla final: mostra el codi de recuperació una única vegada, amb un avís clar de "apunta'l en un lloc seguro, no es tornarà a mostrar" i un botó de còpia al porta-retalls. Sense aquest pas no es pot continuar (checkbox "l'he desat").

Frontend — recuperació de PIN (accessible des del login):
- Si la família té més d'un PARENT: l'enllaç "Has oblidat el PIN?" explica que un altre pare/mare l'ha de resetejar des de Configuració → Fills i pares, i no ofereix cap altre camí.
- Si no n'hi ha prou (un sol PARENT, o vol recuperar-lo sense l'altre present): formulari per introduir el codi de recuperació, que crida POST /api/auth/recover i, si és vàlid, porta directament a la pantalla de definir un PIN nou.

Reutilitza els components base del Prompt 2 (inputs, botons) i l'animació de login del context de disseny per a la pantalla final de l'assistent.
```

---

## Prompt 7 — Frontend: pantalles CHILD

```
Implementa les 4 pantalles del rol CHILD, connectades als endpoints reals del backend (no dades fictícies):

- Inici: targeta de saldo disponible amb animació de count-up en carregar (usa requestAnimationFrame, no setInterval cru), xip d'estalvi, llista de moviments recents amb imports en tabular-nums i color segons signe (verd/vermell) més el signe +/- explícit com a reforç no dependent només del color.
- Tasques: llista de tasques amb estat visual DISPONIBLE / MARCADA COM A FETA (pendent d'aprovació) / APROVADA / RECHAZADA, seguint el flux exacte de la secció 2.1 del document. Marcar una tasca com a feta crida l'endpoint corresponent i mai modifica el saldo directament.
- Objectius: targetes d'objectius d'estalvi amb barra de progrés animada (width transition en entrar a la pantalla, no a la càrrega de l'app).
- Pantalla: mostra el saldo de minuts disponibles (anell SVG animat com a l'artefacte de referència) i un enllaç/explicació de com utilitzar l'objecte NFC físic per iniciar una sessió a la tauleta compartida.

Reprodueix fidelment l'aparença de l'artefacte mapaka-maqueta-animada (colors, tipografia, espaiats, transicions) — no és un esborrany, és el disseny aprovat.
```

---

## Prompt 8 — Frontend: pantalles PARENT

```
Implementa les pantalles del rol PARENT connectades al backend real:

- Resum familiar: graella de targetes per fill amb saldo i activitat recent, llista de moviments agregada de tota la família.
- Aprovacions: llista de tasques/recompenses pendents amb accions Aprovar/Rebutjar. Aprovar crida l'endpoint que genera el MoneyTransaction/ScreenTimeTransaction corresponent; rebutjar només canvia l'estat, sense generar moviment. La fila desapareix amb una transició (opacity + max-height), no amb un salt brusc, i el comptador de pendents del menú lateral s'actualitza en temps real.
- Fills: gestió de perfil, edat i regles de paga/pantalla per fill.
- Configuració: regles generals (aprovació obligatòria, notificacions, permisos de transferència disponible→estalvi) tal com apareix a la secció 8 del document.

Inclou aquí també la vista de "Aprovacions" els resultats de sessions NFC amb repartiment que hagin deixat algun fill en saldo negatiu, marcats visualment (no bloquegen res, és només informatiu per al pare/mare).
```

---

## Prompt 9 — Feature: sessió NFC compartida (tauleta)

```
Implementa la pantalla de "tauleta compartida" per a la sessió NFC de temps de pantalla, com una ruta pública dins de l'app (accessible via la URL gravada a l'etiqueta física, sense necessitar login individual del fill — identifica la família pel token de l'etiqueta):

1. Estat inicial: missatge "Toca l'objecte Mapaka" + botó alternatiu "Iniciar temps" (per si l'etiqueta falla o el dispositiu no la suporta).
2. Estat actiu: cronòmetre en viu (mm:ss) que NO està lligat a cap fill encara, botó "Aturar".
3. Estat "Qui ha jugat?": selector múltiple amb els fills de la família (avatar + nom), crida a POST /api/screen-sessions/{id}/assign en confirmar.
4. Estat resultat: mostra els minuts assignats a cadascú, amb avís visible (no bloquejant) si algú queda en saldo negatiu, explicant que es recupera amb la propera paga de temps.

Important: aquesta pantalla NO ha d'utilitzar la Web NFC API (navigator.nfc) per llegir l'etiqueta — no funciona a iOS ni a escriptori, i és la que garanteix que el flux bàsic funcioni igual de bé en qualsevol dispositiu. L'etiqueta física porta gravada directament la URL d'aquesta pantalla; és el sistema operatiu del dispositiu qui la llegeix i obre el navegador (o la PWA instal·lada, o l'app Android). El codi només necessita gestionar bé què passa quan algú arriba a aquesta URL (identificar la família pel token a la ruta, i decidir si inicia o tanca sessió segons si ja n'hi ha una ACTIVE). Aquest flux és el que ha de funcionar igual a totes les plataformes — no en depenguis per a l'MVP.

Reprodueix l'aparença i les transicions del bloc "Tauleta compartida" de l'artefacte mapaka-maqueta-animada.
```

---

## Prompt 10 — Empaquetat Android amb Capacitor (APK d'instal·lació directa)

```
Afegeix Capacitor al projecte frontend existent, sense modificar el codi Vue 3 ja implementat:

1. `npm install @capacitor/core @capacitor/android` i `npx cap init` amb appId tipus `cat.mapaka.app` i appName "Mapaka".
2. `npx cap add android` per generar el projecte Android natiu; configura't perquè `npm run build` + `npx cap sync android` sigui el flux estàndard per portar la darrera versió web al projecte Android.
3. Instal·la el plugin de NFC (@capgo/capacitor-nfc o l'equivalent de Capawesome) i afegeix, NOMÉS a la pantalla de "tauleta compartida" i NOMÉS quan es detecti que s'executa dins de Capacitor (Capacitor.isNativePlatform()), un botó addicional "Escanejar ara" que faci una lectura activa de l'etiqueta i, opcionalment, una pantalla d'administració per a PARENT que permeti escriure/vincular una etiqueta NFC nova a la família directament des de l'app, sense eines externes. Aquesta millora és exclusiva d'Android — la pantalla ha de continuar funcionant igual de bé sense ella (patró passiu del Prompt 8).
4. Genera la configuració de firma (keystore) i documenta a un README el procés per generar l'APK signat des d'Android Studio (Build > Generate Signed Bundle/APK), i com instal·lar-lo per USB o per enllaç de descarga en un dispositiu amb "orígens desconeguts" activat.

No configuris res relacionat amb Google Play (aquest projecte no s'hi publicarà per ara) ni afegeixis cap dependència d'iOS/Capacitor — iOS es serveix directament com a PWA des del mateix backend, sense empaquetat natiu.
```

---

## Prompt 11 — Desplegament del backend a Render

```
Prepara el backend Spring Boot per desplegar-se a Render (pla gratuït):

1. Crea un `render.yaml` (Render Blueprint) al backend/ que defineixi un servei web tipus Docker, apuntant al Dockerfile ja existent (secció 5 de Família+.pdf).
2. Configura el healthcheck de Render perquè apunti a l'endpoint de Spring Boot Actuator (`/actuator/health`), ja inclòs des del Prompt 1.
3. Totes les credencials (connexió a Neon, secret JWT, etc.) s'han de llegir de variables d'entorn definides al panell de Render — mai hardcodejades ni al `render.yaml` ni al codi.
4. Important: el pla gratuït de Render "adorm" el servei als 15 minuts d'inactivitat i triga fins a un minut a despertar. Afegeix un estat de càrrega explícit al frontend (Vue 3) per a qualsevol crida a l'API que trigui més de 2-3 segons — especialment a la pantalla de "tauleta compartida" (Prompt 8), on un fill podria trobar-se la pantalla en blanc si el backend està "adormit" just quan toca l'objecte NFC. Un missatge del tipus "Despertant Mapaka…" amb una animació lleugera és suficient; no cal cap solució més complexa per a l'MVP.
5. Documenta al README els passos per connectar el repositori a Render i les variables d'entorn necessàries.

No configuris res relacionat amb Railway ni Fly.io — es van descartar per no ser gratuïts de forma continuada.
```

---

## Prompt 12 — Verificació

```
Revisa tot el que s'ha implementat als prompts 1-11 contra Família+.pdf i contra aquest document:

1. Cap acció que generi recompensa (diner o temps) és efectiva sense passar per un estat d'aprovació o pel repartiment explícit de la sessió NFC.
2. Cap saldo es guarda com a valor fix — tot es calcula per suma de moviments (ledger), incloent el temps de pantalla assignat per sessions NFC.
3. Els rols CHILD no poden accedir a cap endpoint ni ruta reservada a PARENT.
4. Tots els textos visibles a la interfície estan en català.
5. Els imports i minuts es mostren amb font-variant-numeric: tabular-nums.
6. La navegació canvia correctament de bottom-nav a sidebar segons breakpoint i rol.
7. Escriu tests d'integració per als tres endpoints nous de sessió NFC (tap, stop, assign), incloent el cas de repartiment amb saldo negatiu.
8. El PIN (de qualsevol rol) i el codi de recuperació de família mai apareixen en clar en logs, respostes d'error ni al codi font — sempre hashejats. El codi de recuperació només es mostra un cop, a la resposta de POST /api/families/register.
9. Escriu tests d'integració per al flux de registre (POST /api/families/register), l'alta d'un fill amb PIN, i el flux de recuperació (POST /api/auth/recover) incloent el cas del codi ja consumit.

Informa de qualsevol incoherència trobada abans de continuar.
```
