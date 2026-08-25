# Mapaka — Prompts per a Code

Aquest document recull totes les decisions preses durant la fase de disseny (marca, color, tipografia, navegació, autenticació i registre, idioma, gestió de tasques i regles de paga, la funcionalitat NFC de temps de pantalla i l'estratègia de distribució multiplataforma) convertides en instruccions llestes perquè Code (l'agent de codi) implementi el projecte pas a pas.

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

**Navegació:** patró adaptatiu pel mateix component d'AppShell, no dos components separats. Per sota de `768px`, qualsevol rol veu una barra inferior d'ítems — CHILD: Inici, Tasques, Objectius, Pantalla (4). PARENT: Resum, Tasques, Aprovacions (amb badge de pendents), Fills, Configuració (5 — vegeu el Prompt 10, on s'afegeix "Tasques" per gestionar responsabilitats, extres i regles de paga, un forat real que la maqueta original no cobria). Per sobre de `768px`, el rol PARENT canvia a panell lateral fix amb els mateixos ítems; CHILD no té vista d'escriptori pròpia perquè no és el seu cas d'ús principal. Referència visual: els frames de `mapaka_mockup.html` — "Vista CHILD — mòbil", "Vista PARENT — mòbil" i "Vista PARENT — escriptori" — són la referència de color, tipografia i component de targeta/llista; el panell "Tasques" de PARENT no hi apareix (es va detectar el forat després de fer la maqueta) i s'ha de construir amb els mateixos components base del Prompt 2.

**Idioma de la interfície:** multilingüe des del principi — català (idioma base i font de veritat de totes les traduccions), amb castellà i anglès disponibles i seleccionables. Cap text visible a la interfície es pot escriure literalment dins d'un component: sempre a través del sistema d'i18n del Prompt 5. Els prompts i comentaris de codi poden ser en castellà/anglès, però cap text d'usuari final fora dels fitxers de traducció.

**Autenticació:** PIN numèric de 4 dígits per a **tots dos rols**, PARENT inclòs — no hi ha contrasenya alfanumèrica enlloc de l'aplicació. Es tria expressament per mantenir la infraestructura a cost zero: una contrasenya amb recuperació per correu exigiria donar d'alta un servei d'enviament d'email transaccional només per a aquest propòsit. El PIN es guarda sempre com a hash (mai en clar ni reversible) al mateix camp `password_hash` de la taula `users`, independentment del rol. Vegeu el Prompt 6 per al flux complet de creació de família, alta de perfils i recuperació de PIN.

**Animació d'entrada del login — "Muntatge en cascada":** cada cercle del logo arriba d'una direcció diferent (el primari des de sota, el secundari des de dalt-dreta, l'accent des de baix-esquerra) i encaixa amb un lleuger rebot (`cubic-bezier(.2,.9,.3,1.4)`), amb un petit retard esglaonat entre els tres (~140ms). El wordmark "Mapaka" apareix després, amb un col·lapse de `letter-spacing` combinat amb un fade-in. Implementació de referència exacta (keyframes CSS inclosos, llestos per portar a un component Vue): `mapaka_login_animacions.html`, proposta 2. Respecta `prefers-reduced-motion` (desactiva l'animació i mostra l'estat final directament).

**Maqueta de referència visual:** l'artefacte `mapaka-maqueta-animada` (HTML autocontingut) conté l'aparença i les transicions exactes aprovades — Code hauria de reproduir-ne fidelment els components, no reinterpretar-los.

**Estratègia de distribució (sense cost, sense App Store ni Google Play):** és una única aplicació (Vue 3 + Spring Boot), no tres — només canvia com arriba a cada dispositiu.

- **Android:** empaquetada amb Capacitor i distribuïda com a APK d'instal·lació directa (sideload), sense passar per Google Play. Això dona accés a un plugin NFC natiu real (lectura I escriptura activa de l'etiqueta des de dins de l'app), a més del patró passiu ja dissenyat.
- **iOS:** sense empaquetat natiu. S'instal·la com a PWA des de Safari (Compartir → Afegir a l'inici). El NFC es queda amb el patró passiu (l'etiqueta porta la URL gravada; el sistema operatiu la llegeix i obre la PWA) — a iOS no hi ha escaneig actiu possible des de la interfície.
- Motiu de la decisió: qualsevol via que acabi passant per Xcode (nativa o Capacitor) per publicar a l'App Store exigeix un macOS que el Mac disponible (2015) no pot executar amb les versions actuals d'Xcode. Es descarta expressament aquesta via mentre no hi hagi un Mac més recent o un servei de compilació al núvol (Codemagic, GitHub Actions amb runner macOS) — no és una limitació d'arquitectura, és una limitació d'eina de compilació.

**Base de dades i hosting:** PostgreSQL (no MySQL — el propi esquema ja fa servir UUID i ENUM natius, que encaixen millor amb Postgres). En desenvolupament es fa servir el PostgreSQL local del `docker-compose.yml` (Prompt 1), sense dependre de connexió a internet. En producció, la base de dades viu a **Neon** (pla gratuït: 0,5 GB, es reactiva sola en segons davant d'inactivitat, sense necessitat d'entrar a cap panell a reactivar-la manualment — al contrari que Supabase, que es va descartar per pausar-se als 7 dies i requerir reactivació manual). Neon exigeix connexió SSL (`sslmode=require`); el datasource de Spring Boot per a l'entorn de producció ha de llegir la cadena de connexió de Neon des d'una variable d'entorn, mai hardcodejada.

**Hosting del backend:** **Render** (pla gratuït, 750 hores d'instància al mes — cobreix un servei encès tot el mes). Es va descartar Railway perquè el seu pla gratuït real dura només 30 dies (crèdit inicial de 5$); passat aquest període caldria pagar el pla Hobby (5$/mes + consum), i no hi havia cap despesa prèvia que ho fes "gratis marginal". Es va descartar Fly.io perquè ja no ofereix pla gratuït per a comptes noves. Important: el servei gratuït de Render "s'adorm" als 15 minuts d'inactivitat i triga uns 60 segons a despertar-se amb la primera petició — cal preveure-ho a la interfície (per exemple, un estat de càrrega clar a la pantalla de la tauleta compartida en comptes de deixar-la en blanc mentre el backend arrenca).

**Temps de pantalla: model mensual, no diari.** Família+.pdf original preveia una generació diària (taules `screen_time_rules` / `daily_screen_balances`, secció 15). Es descarta aquest model: la família no fa servir la tauleta a diari, i el que es vol és que el temps de pantalla es comporti **exactament igual que els diners** — un ledger pur, sense cap valor fix guardat enlloc (coherent amb el principi no negociable de tot el projecte). Funcionament: cada fill té un camp `screen_minutes_monthly` (a `allowance_rules`, junt amb la resta de la seva regla de paga); `POST /api/allowances/generate` acredita aquest import una vegada al mes al Screen Time Ledger (`screen_time_transactions`, `source_type = 'MONTHLY_ALLOWANCE'`), exactament quan es genera la paga en diners. El saldo és sempre `SUM(screen_time_transactions)`, mai un camp guardat — es gasta amb les sessions NFC (`source_type = 'NFC_SESSION'`), es pot ajustar manualment amb una Bonificació/Penalització (`source_type = 'MANUAL'`), pot quedar negatiu (ja decidit), i **el que no es gasta un mes es queda acumulat pel següent** — com les dades mòbils d'una tarifa de telefonia, no com un pot que es buida cada dia. No cal implementar cap taula ni cap tasca programada de generació diària — si ja existeix del disseny original, es pot ignorar o eliminar.

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

**Afegit després (per al Prompt 10):** un component/utilitat `FormRow` per a parelles de camps curts i relacionats dins d'un formulari — `grid-template-columns: 1fr 1fr; gap: 16px` a partir de `768px`, una sola columna per sota. Reutilitza'l sempre que dos camps curts vagin junts, no només a Tasques.
```

---

## Prompt 3 — Base de dades: migracions Flyway

```
Genera les migracions Flyway per a totes les taules descrites a Família+.pdf (families, users, child_profiles, allowance_rules, monthly_allowances, money_transactions, savings_goals, reward_adjustments, monthly_settlements, audit_log — revisa el document sencer, no només les primeres seccions). **Excepció important:** no creïs `screen_time_rules` ni `daily_screen_balances` — es va decidir que el temps de pantalla funciona com un ledger mensual, igual que els diners, no amb generació diària (vegeu "Temps de pantalla: model mensual, no diari" al context de disseny). Afegeix en canvi `screen_minutes_monthly INTEGER NOT NULL DEFAULT 0` a `allowance_rules`, i reutilitza `screen_time_transactions` (o com s'anomeni al teu esquema) com a únic origen de veritat del saldo de minuts.

Afegeix a més les taules noves per a la funcionalitat de sessió NFC compartida, seguint el mateix estil i convencions (UUID PK, timestamps, restriccions):

- screen_tag: id, family_id (FK), token (VARCHAR únic — el que es grava a l'etiqueta NFC física), created_at, active.
- screen_session: id, screen_tag_id (FK), started_at, ended_at (nullable mentre està activa), elapsed_seconds, status (ENUM: ACTIVE, CLOSED).
- screen_session_participant: id, session_id (FK), child_id (FK), assigned_seconds, created_at.

Cada fila de screen_session_participant, en tancar-se, ha de generar una transacció al Screen Time Ledger existent (screen_time_transactions o l'equivalent que ja defineix el document) amb source_type = 'NFC_SESSION' i source_id apuntant a screen_session_participant.id, permetent saldo negatiu (sense restricció CHECK >= 0) tal com es va decidir.

Afegeix també a la taula `users`: `locale VARCHAR(2) NOT NULL DEFAULT 'ca' CHECK (locale IN ('ca','es','en'))` — preferència d'idioma de cada membre de la família (vegeu Prompt 5, internacionalització).
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
- PATCH /api/users/{id}/locale — actualitza la preferència d'idioma (`ca`/`es`/`en`) de l'usuari autenticat. Cap altre usuari pot canviar l'idioma d'un altre membre.
- GET /api/auth/me (ja definit a Família+.pdf) ha d'incloure el camp `locale` a la resposta, perquè el frontend apliqui l'idioma guardat just després del login sense haver de fer una crida addicional.

Cap missatge d'error ni de validació que generi el backend s'ha d'enviar mai com a text literal en cap idioma — sempre com a codi semàntic (per exemple `PIN_INVALID_LENGTH`, `RECOVERY_CODE_EXPIRED`, `INSUFFICIENT_PERMISSIONS`). És el frontend qui tradueix aquest codi al missatge visible amb el sistema d'i18n del Prompt 5. Documenta els codis d'error possibles a l'OpenAPI de cada endpoint.

Documenta tots els endpoints amb OpenAPI/Swagger, incloent exemples de request/response.
```

---

## Prompt 5 — Internacionalització (i18n): configuració base

```
Configura el sistema d'internacionalització abans de construir cap pantalla, perquè tots els prompts següents (login, registre, CHILD, PARENT, sessió NFC) ja escriguin els textos a través d'ell des del principi, en comptes d'haver-los d'extreure més tard.

- Llibreria: vue-i18n (Composition API, `legacy: false`), amb càrrega diferida (lazy) dels fitxers de cada idioma perquè el bundle inicial només inclogui l'idioma actiu.
- Estructura de fitxers: `frontend/src/i18n/locales/ca.json`, `es.json`, `en.json`. El català és el fitxer font — qualsevol clau nova es crea primer allà. Organitza les claus per pantalla/namespace, seguint exactament els noms ja establerts a la maqueta i als prompts: `common` (botons i etiquetes compartides), `nav`, `login`, `registre`, `inici`, `tasques`, `objectius`, `pantalla`, `resum`, `aprovacions`, `fills`, `config`, `nfc`, i `errors` (un mapa codi-de-backend → missatge, per als codis semàntics definits al Prompt 4 — mai el backend enviant text directament).
- Convenció de claus: semàntiques, no el text literal català (per exemple `nav.inici`, `login.pinLabel`, `tasques.rewardLabel`) — així una clau no s'ha de renombrar quan canviï la traducció.
- Afegeix un script de comprovació (per exemple a `package.json`, executable en local i pensat per a un futur pas de CI) que falli si `es.json` o `en.json` no tenen exactament el mateix conjunt de claus que `ca.json` — evita que quedin claus a mig traduir sense que ningú se n'adoni.
- Mai tradueixis automàticament dades introduïdes per la família (noms de tasques, d'objectius d'estalvi, de fills, de la família mateixa) — només es tradueix el "xassís" de l'aplicació (etiquetes, botons, missatges del sistema). El nom "Mapaka" tampoc es tradueix mai.

Detecció i persistència de l'idioma:

1. A l'obrir l'app sense sessió (login/registre): si hi ha un idioma guardat a `localStorage` (clau `mapaka-locale`), s'aplica. Si no n'hi ha, es detecta amb `navigator.language`, mapejant a `ca`/`es`/`en`; qualsevol altre idioma cau al català per defecte.
2. En completar el login, `GET /api/auth/me` (o la resposta del login) inclou el camp `locale` de l'usuari — s'aplica immediatament i es desa també a `localStorage`, perquè la següent vegada que s'obri l'app en aquest dispositiu ja hi hagi la llengua correcta abans fins i tot d'iniciar sessió.
3. Canviar d'idioma sempre actualitza `localStorage` a l'instant; si hi ha sessió activa, a més crida `PATCH /api/users/{id}/locale` perquè quedi guardat al perfil i es recuperi en iniciar sessió des d'un altre dispositiu.

Selector d'idioma (component `LanguageSwitcher`, reutilitzat arreu — CA / ES / EN):

- A la pantalla de login/registre: visible sense necessitat d'autenticar-se.
- Per a PARENT: dins de Configuració.
- Per a CHILD: com que la navegació CHILD no té pantalla de configuració pròpia, afegeix una icona petita (globus/bandera) a la capçalera de la pantalla Inici — l'únic punt d'accés per a aquest rol.

Formats numèrics i de data:

- Els imports monetaris **no** varien de format amb l'idioma: sempre coma decimal i símbol "€" darrere (`14,00 €`), independentment de si la interfície està en català, castellà o anglès — és la moneda pròpia de la família, no un valor que s'hagi d'adaptar culturalment. Manté'l com una utilitat `formatMoney()` compartida, no via `$n` de vue-i18n.
- Les dates (per exemple "Resums mensuals") sí que es formaten segons l'idioma actiu amb `Intl.DateTimeFormat`, perquè és només una etiqueta de lectura ("20 d'agost" / "20 de agosto" / "August 20").
```

---

## Prompt 6 — Frontend: shell de navegació i autenticació

```
Implementa el shell de navegació de Mapaka amb Vue Router:

- Login amb PIN numèric de 4 dígits per a tots dos rols (vegeu "Autenticació" al context de disseny). Per a CHILD, reprodueix el patró "selecciona el teu perfil" de la secció 7.2 de Família+.pdf: primer es tria l'avatar/nom dins la família, després es demana el PIN — mai un camp d'usuari en text lliure. Per a PARENT, un únic camp de PIN n'hi ha prou perquè el correu/usuari ja identifica la família.
- Pantalla de login amb l'animació d'entrada del logo descrita al context de disseny ("Muntatge en cascada").
- Guàrdies de ruta per rol: un CHILD mai pot accedir a rutes de PARENT ni viceversa.
- Component AppShell únic i reutilitzat pels dos rols (no dos components de navegació separats): per sota de 768px de viewport, renderitza sempre una barra inferior fixa, amb la llista d'ítems (etiqueta, icona, ruta) depenent només del rol i **sense assumir un nombre fix d'ítems** — CHILD en té 4 (Inici, Tasques, Objectius, Pantalla), PARENT en té 5 (Resum, Tasques, Aprovacions amb comptador de pendents en un badge sobre la icona, Fills, Configuració). L'amplada de l'indicador animat i de cada botó s'ha de calcular a partir de `items.length`, no d'una constant. Per sobre de 768px, el rol PARENT canvia a un panell lateral fix amb els mateixos ítems; el rol CHILD no té variant d'escriptori.
- L'ítem actiu de la navegació ha de portar un indicador animat (transform + transition, no display toggling brusc) que es desplaça entre posicions, reproduint el comportament de l'artefacte mapaka-maqueta-animada — inclosa la variant "Vista PARENT — mòbil", que fa servir exactament el mateix patró de barra inferior que CHILD, només canviant els ítems.
- Aplica l'atribut data-role a <html> just després de l'autenticació, perquè el sistema de disseny del Prompt 2 s'apliqui automàticament.
```

---

## Prompt 7 — Registre de família, alta de perfils i recuperació de PIN

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

## Prompt 8 — Frontend: pantalles CHILD

```
Implementa les 4 pantalles del rol CHILD, connectades als endpoints reals del backend (no dades fictícies):

- Inici: targeta de saldo disponible amb animació de count-up en carregar (usa requestAnimationFrame, no setInterval cru), xip d'estalvi, llista de moviments recents amb imports en tabular-nums i color segons signe (verd/vermell) més el signe +/- explícit com a reforç no dependent només del color.
- Tasques: llista de tasques amb estat visual DISPONIBLE / MARCADA COM A FETA (pendent d'aprovació) / APROVADA / RECHAZADA, seguint el flux exacte de la secció 2.1 del document. Marcar una tasca com a feta crida l'endpoint corresponent i mai modifica el saldo directament.
- Objectius: targetes d'objectius d'estalvi amb barra de progrés animada (width transition en entrar a la pantalla, no a la càrrega de l'app), **més un botó "Nou objectiu"** (nom + import a assolir) que crida `POST /api/children/{childId}/savings-goals` — sense aquest botó la pantalla és només de lectura i el fill no té manera de crear-ne un.
- Pantalla: **revisat** — el disseny original (anell "de 60 min assignats avui") assumia una generació diària que ja no existeix (vegeu "Temps de pantalla: model mensual, no diari" al context de disseny). Mostra en canvi el saldo acumulat del mes amb el mateix patró que la targeta de saldo de la pantalla Inici (import gran en tabular-nums, aquí en minuts o "Xh Ym"), amb una llista de moviments recents (paga mensual, sessions NFC, bonificacions/penalitzacions) igual que la llista de moviments de diners. Afegeix un enllaç/explicació de com utilitzar l'objecte NFC físic per iniciar una sessió a la tauleta compartida.

Reprodueix fidelment l'aparença de l'artefacte mapaka-maqueta-animada (colors, tipografia, espaiats, transicions) — no és un esborrany, és el disseny aprovat.
```

---

## Prompt 9 — Frontend: pantalles PARENT

```
Implementa les pantalles del rol PARENT connectades al backend real. Nota: la gestió de tasques es implementa al Prompt 10, no aquí. **Aquest prompt s'ha revisat després d'una primera implementació** (v1: paga mensual duplicada entre regla general i fill, ajust manual amb tres imports manuals, temps de pantalla amb reinici diari) — el que ve a continuació és la versió corregida i és la que val.

- Resum familiar: graella de targetes per fill amb saldo i activitat recent, llista de moviments agregada de tota la família. Afegeix un botó "Generar paga del mes" que crida `POST /api/allowances/generate` i genera **alhora** l'ingrés de diners i el de minuts de pantalla de tots els fills (mateix acte, mateix botó — vegeu més avall per què el temps de pantalla ara també és mensual). Mostra el resultat en un estat de confirmació (`POST /api/allowances/{id}/confirm`) o cancel·lació (`POST /api/allowances/{id}/cancel`) abans de donar-lo per definitiu. Afegeix també un enllaç "Resums mensuals" que llista els tancaments (`GET /api/settlements`) amb detall en clicar (`GET /api/settlements/{id}`).
- Aprovacions: llista de tasques/recompenses pendents amb accions Aprovar/Rebutjar. Aprovar crida l'endpoint que genera el MoneyTransaction/ScreenTimeTransaction corresponent; rebutjar només canvia l'estat, sense generar moviment. La fila desapareix amb una transició (opacity + max-height), no amb un salt brusc, i el comptador de pendents del menú lateral s'actualitza en temps real.
- **Fills:** gestió de perfil i edat per fill, més:
  - **Paga — interruptor "Paga personalitzada per a aquest fill"** (reutilitza el mateix component `switch` que ja existeix a Configuració). Per defecte, **desactivat**: es mostra un bloc de només lectura "Aplica la regla general: X € al mes (segons edat), Y% per gastar / Z% per estalviar" — el número surt de la regla general per franja d'edat que correspongui, mai un camp editable, perquè no hi hagi dos llocs on definir el mateix import (aquest era exactament el problema de la v1). Si s'activa l'interruptor, apareixen dos camps editables — **Paga mensual (€)** i **% per gastar** (la resta va a estalvi, mateix patró que ja hi havia) — que creen o actualitzen una fila a `allowance_rules` amb `child_id` establert, prevalent sobre la regla general (secció 8.2 de Família+.pdf).
  - **Temps de pantalla — "Minuts de pantalla per mes"**: un únic camp numèric editable, sempre visible (no hi ha regla general per a temps de pantalla, només per fill). Aquest número és el que `POST /api/allowances/generate` acredita cada mes al Screen Time Ledger del fill — **no es reinicia mai cada dia**. Funciona exactament igual que els diners: es genera un cop al mes, es gasta amb les sessions NFC, i el que no es gasta s'acumula pel mes següent (com les dades mòbils d'una companyia telefònica) — vegeu "Temps de pantalla: model mensual, no diari" al context de disseny per a la justificació completa d'aquest canvi de model.
  - **Bonificacions** (substitueix l'antic "Ajust manual" — nom més clar i formulari més senzill): formulari amb només tres camps, en aquest ordre: (1) selector **Tipus**: Bonificació / Penalització; (2) selector **Categoria**: Monetària / Temps de pantalla; (3) un únic camp **Valor** (€ o minuts segons la categoria triada) + un camp de text lliure per al motiu. **L'usuari mai introdueix per separat l'import a gastar i el d'estalviar** — si la categoria és Monetària, el backend reparteix automàticament el valor entre `money_amount` i `savings_amount` segons el percentatge actiu del fill (el personalitzat si en té, si no el de la regla general per edat) abans de cridar `POST /api/children/{id}/money-adjustments`; si la categoria és Temps de pantalla, el valor sencer és `screen_minutes` i crida `POST /api/children/{id}/screen-time/adjustments`. `adjustment_type` és `BONUS` si Tipus=Bonificació, `PENALTY` si Tipus=Penalització (secció 17 de Família+.pdf); el backend calcula el repartiment — el frontend no fa aquest càlcul, només l'ensenya com a previsualització abans d'enviar.
- Configuració: regles generals (aprovació obligatòria, notificacions, permisos de transferència disponible→estalvi) tal com apareix a la secció 8 del document, més un editor de **regles de paga generals per franja d'edat** (sense `child_id`): llista de franges (edat mínima, edat màxima, import mensual, % per gastar — la resta a estalvi) amb alta/edició/baixa via `POST`/`GET`/`PATCH`/`DELETE /api/allowance-rules`. Aquesta llista només s'usa per calcular el bloc de només lectura de Fills quan un fill no té paga personalitzada — mai apareix un camp "paga mensual" duplicat a cap altre lloc.

Inclou aquí també a la vista de "Aprovacions" els resultats de sessions NFC amb repartiment que hagin deixat algun fill en saldo negatiu, marcats visualment (no bloquegen res, és només informatiu per al pare/mare).
```

---

## Prompt 10 — Frontend i backend: gestió de tasques (PARENT)

```
Família+.pdf defineix tot el sistema de tasques (secció 12) i el seu CRUD (seccions 29-31), però ni la maqueta original ni el Prompt 9 van incloure mai una pantalla per crear-les — sense aquest prompt no hi ha manera d'introduir cap tasca, ni estàndard ni extra, des de la interfície. Implementa'l abans de continuar amb la sessió NFC o el desplegament.

Backend (si encara no existeix del Prompt 4):
- Confirma que estan implementats: POST/GET/PATCH/DELETE /api/tasks (amb filtres ?type=, ?active=, ?childId=), POST/GET/DELETE /api/tasks/{taskId}/assignments.
- `task_rewards` **només** guarda `money_amount` (import total en diners, sense repartir) i `screen_minutes` — **no hi ha camp `savings_amount` editable per l'usuari** (mateix principi que les Bonificacions del Prompt 9: mai es demana per separat quant va a gastar i quant a estalviar). El repartiment es calcula quan s'aprova la tasca, no quan es crea: `POST /api/task-completions/{id}/approve` reparteix `money_amount` entre `MoneyTransaction` (SPENDING) i `MoneyTransaction` (SAVINGS) segons el percentatge actiu **d'aquell fill en aquell moment** (la seva regla personalitzada si en té, si no la general per edat) — com que una tasca es pot assignar a diversos fills amb percentatges diferents, el repartiment final pot no ser idèntic per a tots encara que `money_amount` sigui el mateix.

Frontend — nova pantalla "Tasques" per a PARENT (cinquè ítem de navegació, vegeu el context de disseny):
- Llista de tasques existents, amb filtre visual per tipus: **Responsabilitat** (RESPONSIBILITY — hàbits, sense sorpresa, ex. "Fer el llit") i **Extra** (EXTRA — feina addicional puntual, ex. "Rentar el cotxe"). Cada fila mostra nom, tipus, recompensa (diners i/o minuts, el que tingui) i a quins fills està assignada.
- **Formulari "Nova tasca" / "Edita tasca" — maquetació en 3 passos dins la mateixa targeta** (es van comparar 3 opcions — una columna amb blocs, dues columnes a escriptori, i passos — es va triar aquesta): pestanyes "1. Bàsic", "2. Recompensa", "3. Assignació" amb un indicador animat que es desplaça entre elles, reproduint el mateix patró (transform + transition) que l'indicador de la navegació inferior.
  - **Pas 1, Bàsic:** nom, descripció, i tipus + recurrència aparellats en una mateixa fila a partir de 768px (`FormRow` del Prompt 2), una sola columna per sota.
  - **Pas 2, Recompensa:** només dos camps — **Diners (€)** i **Minuts de pantalla** (com a mínim un dels dos > 0). Sota els camps, mostra una previsualització de només lectura del repartiment ("Es repartirà en aprovar-la: X € gastar / Y € estalvi") calculada amb el percentatge del fill assignat (si n'hi ha més d'un amb percentatges diferents, mostra un rang o el del primer fill seleccionat amb un avís que pot variar per fill). El camp "Requereix aprovació" és el component `switch` ja existent a Configuració, no un checkbox del navegador ni un tercer desplegable.
  - **Pas 3, Assignació:** píndoles de selecció múltiple dels fills.
  - Botons "Cancel·la" (botó fantasma, discret — mai vermell sòlid: el vermell queda reservat a accions destructives reals) i "Desar" (primari) fixos sota les pestanyes, visibles independentment del pas actiu.
- Donar de baixa una tasca és `active = false`, mai un DELETE físic si ja té completions associades (mateix criteri que ja s'aplica a Fills a Família+.pdf).

Referència visual exacta: `mapaka_form_tasques.html`, "Opció 3 · Passos dins la mateixa targeta". Reutilitza els components base del Prompt 2 (BaseButton, BaseCard, inputs, `switch`) per a la resta de detalls.
```

---

## Prompt 11 — Feature: sessió NFC compartida (tauleta)

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

## Prompt 12 — Empaquetat Android amb Capacitor (APK d'instal·lació directa)

```
Afegeix Capacitor al projecte frontend existent, sense modificar el codi Vue 3 ja implementat:

1. `npm install @capacitor/core @capacitor/android` i `npx cap init` amb appId tipus `cat.mapaka.app` i appName "Mapaka".
2. `npx cap add android` per generar el projecte Android natiu; configura't perquè `npm run build` + `npx cap sync android` sigui el flux estàndard per portar la darrera versió web al projecte Android.
3. Instal·la el plugin de NFC (@capgo/capacitor-nfc o l'equivalent de Capawesome) i afegeix, NOMÉS a la pantalla de "tauleta compartida" i NOMÉS quan es detecti que s'executa dins de Capacitor (Capacitor.isNativePlatform()), un botó addicional "Escanejar ara" que faci una lectura activa de l'etiqueta i, opcionalment, una pantalla d'administració per a PARENT que permeti escriure/vincular una etiqueta NFC nova a la família directament des de l'app, sense eines externes. Aquesta millora és exclusiva d'Android — la pantalla ha de continuar funcionant igual de bé sense ella (patró passiu del Prompt 8).
4. Genera la configuració de firma (keystore) i documenta a un README el procés per generar l'APK signat des d'Android Studio (Build > Generate Signed Bundle/APK), i com instal·lar-lo per USB o per enllaç de descarga en un dispositiu amb "orígens desconeguts" activat.

No configuris res relacionat amb Google Play (aquest projecte no s'hi publicarà per ara) ni afegeixis cap dependència d'iOS/Capacitor — iOS es serveix directament com a PWA des del mateix backend, sense empaquetat natiu.
```

---

## Prompt 13 — Desplegament del backend a Render

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

## Prompt 15 — Ajustos i millores (ronda de revisió sobre l'app real)

```
Aquest prompt arriba després d'haver vist l'aplicació ja funcionant (Prompts 1-10 implementats). No és disseny des de zero: són correccions i ampliacions concretes sobre pantalles que ja existeixen. Referència visual de tot el que segueix: `mapaka_mockup_v2.html`.

### 1. Base de dades (noves migracions Flyway, afegides a les del Prompt 3)

- `child_profiles`: afegeix `avatar_icon VARCHAR(50) NULL` (clau d'una icona d'un set tancat predefinit; `NULL` vol dir "mostra la inicial del nom"). El camp de color ja existent (el que es tria en crear el fill) es manté, però restringeix els valors possibles a una paleta tancada d'uns 8-10 tons saturats (la mateixa a totes bandes: alta, backend i frontend) — **cap to blanc ni molt clar és una opció vàlida**, perquè la icona en blanc de sobre sempre hi tingui contrast. Valida-ho tant al DTO del backend com al selector del frontend.
- `savings_goals`: afegeix `allocation_percentage NUMERIC(5,2) NOT NULL DEFAULT 0` (punts percentuals que es resten directament del "per gastar" del fill mentre l'objectiu estigui actiu) i, si no existeix ja, un `status` amb almenys `ACTIVE` / `COMPLETED`.
- Taula nova `donations`: `id` (UUID PK), `savings_goal_id` (FK a savings_goals), `family_id` (FK), `donor_name VARCHAR(120) NULL`, `message VARCHAR(280) NULL`, `amount NUMERIC(10,2) NOT NULL CHECK (amount > 0)`, `created_by_user_id` (FK a users, sempre un PARENT), `created_at TIMESTAMP NOT NULL DEFAULT now()`.
- `tasks` (o la taula equivalent del Prompt 10): afegeix `penalty_money_amount NUMERIC(10,2) NOT NULL DEFAULT 0` i `penalty_screen_minutes INTEGER NOT NULL DEFAULT 0`, només rellevants quan `type = 'RESPONSIBILITY'`.
- `task_completions`: afegeix `completion_group_id UUID NULL` — totes les files creades juntes en una mateixa finalització col·laborativa (vegeu punt 5.2 més avall) comparteixen el mateix valor. Una finalització individual (sense col·laboradors) també en genera un, encara que el grup tingui una sola fila — simplifica l'aprovació perquè sempre es tracta un grup, mai un cas especial d'un sol element.
- No cal cap taula nova per a les alertes (punt 4): es calculen sempre a partir de dades ja existents, mai com a estat guardat — coherent amb el principi de ledger de tot el projecte.

### 2. Backend — repartiment de diners en N parts (canvi transversal important)

Fins ara el repartiment d'un import (paga mensual, Bonificacions, aprovació de recompensa de tasca) era sempre binari: gastar / estalvi. Amb els objectius per percentatge (punt 6.2) passa a ser en N parts. Crea un únic servei compartit, per exemple `MoneySplitCalculator.split(amount, child)`, i fes-lo servir des de `POST /api/allowances/generate`, des de l'endpoint de Bonificacions del Prompt 9, i des de `POST /api/task-completions/{id}/approve` del Prompt 10 — no repliquis aquesta lògica tres vegades.

Lògica del càlcul, per a un fill concret:
1. Parteix del seu percentatge de gastar vigent (personalitzat si en té, si no el general per edat) i de la resta com a estalvi.
2. Per cada `savings_goal` seva amb `status = 'ACTIVE'`, resta el seu `allocation_percentage` directament del percentatge de gastar (mai del d'estalvi).
3. Reparteix l'import: la part de gastar (ja reduïda) genera una `MoneyTransaction` de tipus SPENDING, la part d'estalvi una de tipus SAVINGS, i cada objectiu actiu genera la seva pròpia `MoneyTransaction` (o el tipus que faci servir el teu esquema per a contribucions a objectius) vinculada al seu `savings_goal_id`.
4. Després d'aplicar-ho, comprova si algun objectiu ha arribat al seu `target_amount` (sumant les seves contribucions més les seves donacions, punt 6.1) — si és així, marca'l `COMPLETED`. Un objectiu `COMPLETED` deixa d'restar percentatge en el següent càlcul: el seu marge torna sol a "gastar".

Validació obligatòria en crear o editar un objectiu (`POST`/`PATCH /api/children/{childId}/savings-goals`): la suma dels `allocation_percentage` de tots els objectius `ACTIVE` d'un fill mai pot superar el seu percentatge de gastar vigent. Retorna un codi semàntic (`GOAL_PERCENTAGE_EXCEEDS_AVAILABLE`) si es supera, mai un text literal.

**Implementació (Fase A):** el "tipus que faci servir el teu esquema per a contribucions a objectius" és un nou valor `GOAL` a l'enum `wallet_type` — separat de `SAVINGS` perquè el progrés d'un objectiu (punt 7.1, targeta híbrida) no es confongui mai amb l'estalvi general del fill; les consultes existents de `spendingBalance`/`savingsBalance` no necessiten cap canvi perquè ja filtren per `wallet_type` explícit. De pas es va corregir un bug preexistent (Prompt 8 original): el progrés d'un objectiu llegia el saldo compartit d'estalvi del fill en comptes del seu propi progrés — amb diversos objectius actius tots mostraven el mateix número. `MoneySplitCalculator.apply(...)` no només reparteix sinó que també crea els `MoneyTransaction` i marca l'objectiu `COMPLETED`, i retorna els imports reals de gastar/estalvi perquè `AllowanceGenerationService.confirm()` tanqui el `MonthlySettlement` amb el repartiment autoritatiu (recalculat en confirmar, no la previsió del DRAFT).

- `GET /api/families/current/alerts` — calcula i retorna la llista d'alertes actives (per ara només una: `MONTHLY_ALLOWANCE_PENDING`, activa des del dia 1 del mes si no hi ha cap `monthly_allowances`/`monthly_settlements` generat per aquest mes). Pensat perquè s'hi puguin afegir més tipus d'alerta en el futur sense canviar el contracte (una llista d'objectes `{type, params}`, mai text ja traduït).
- `POST /api/tasks/{taskId}/complete` — body `{ collaboratorChildIds: [] }` (buit si no hi ha ajuda). Crea una fila de `task_completions` per cada participant (qui truca l'endpoint + els col·laboradors), totes amb el mateix `completion_group_id` nou. Rebutja amb `TASK_ALREADY_CLAIMED` si algun dels participants (inclòs qui truca) ja té una finalització no rebutjada per a l'ocurrència actual d'aquesta tasca. Aquesta comprovació és el que bloqueja la tasca a l'instant per a la resta.
- `GET /api/tasks/{taskId}/completion-status` (o inclou-ho a la resposta de `GET /api/tasks`) — indica si la tasca ja està reclamada per aquesta ocurrència, per qui, i si el fill que consulta n'és un dels participants.
- `POST /api/task-completions/group/{completionGroupId}/approve` — aprova totes les files del grup en un sol acte. Reparteix `money_amount` i `screen_minutes` de `task_rewards` **a parts iguals entre els participants del grup**, i després passa la part de cadascú pel `MoneySplitCalculator` (punt 2) amb el seu propi percentatge.
- `GET /api/tasks/incomplete?asOf=` — llista tasques de tipus RESPONSIBILITY, per fill, la finestra de recurrència de les quals ja ha vençut sense cap finalització aprovada.
- `POST /api/tasks/{taskId}/children/{childId}/apply-penalty` — aplica manualment `penalty_money_amount`/`penalty_screen_minutes` de la tasca com un ajust negatiu (mateix mecanisme que una Penalització de Bonificacions, `source_type='TASK_PENALTY'`), passant la part monetària pel `MoneySplitCalculator`.
- `PATCH /api/children/{id}/avatar` — body `{ color, icon }` (icon pot ser `null`). Només el propi fill (o un PARENT de la seva família) el pot cridar.
- `POST /api/savings-goals/{id}/donations` — només PARENT. Crea la fila a `donations` i una `MoneyTransaction` vinculada 100% a aquest objectiu, sense passar pel `MoneySplitCalculator` — el dinater donat no toca mai gastar ni estalvi.
- Endpoint de moviments ja existent (Resum familiar / moviments del fill): afegeix paràmetres opcionals `from`, `to` i `childId` per filtrar per data i per fill, més paginació (`page`, `size`), per donar suport als filtres del punt 6.3.

### 4. Frontend — Alertes al Resum familiar (Opció A confirmada)

A la capçalera del panell PARENT (Resum familiar), afegeix una campaneta amb un `badge` de comptador (reutilitza `BadgeCounter` del Prompt 2) que obre un desplegable amb la llista d'alertes de `GET /api/families/current/alerts`. Amb una sola alerta activa avui ("Recorda generar la paga d'aquest mes"), un clic sobre la fila porta directament al botó "Generar paga del mes" ja existent al Resum. Component pensat per créixer: no assumeixis mai una sola alerta possible.

### 5. Frontend — Tasques (amplia el Prompt 10)

**5.1 Pas "Recompensa" — un switch per tipus.** Substitueix els dos camps sempre editables per un `switch` (el component ja existent) davant de cadascun: "Diners (€)" i "Minuts de pantalla". Apagat, el camp queda desactivat i el seu valor es força a 0; encès, el camp apareix editable. Cal que com a mínim un dels dos estigui encès per poder desar (mateixa validació que ja hi havia, ara reforçada visualment). Mateix tractament per al camp "Valor" de Bonificacions i pel camp de minuts del punt 5.4.

**5.2 Tasca Extra sense assignació — flux de col·laboració (assistent en 2 pantalles, Opció B).** Quan `type = EXTRA`, el pas "3. Assignació" del Prompt 10 desapareix — la tasca és visible per a tots els fills sense `child_id` fixat. Des de la pantalla de detall de la tasca (CHILD), el botó "Ho he fet!" obre:
  - **Pantalla 1 de 2:** pregunta a pantalla completa "Has rebut ajuda d'algun germà?" amb dos botons — "No, jo sol" (crida directament `POST /api/tasks/{id}/complete` amb `collaboratorChildIds: []`) i "Sí, m'han ajudat" (passa a la pantalla 2).
  - **Pantalla 2 de 2:** llista de germans en chips seleccionables (mateix component que els filtres de moviments del punt 6.3), amb un text d'ajuda que avança quant tocarà a cadascú (import i minuts totals ÷ nombre de participants, incloent-hi qui ha iniciat). Botó "Enrere" (fantasma) torna a la pantalla 1; "Confirmar" crida `POST /api/tasks/{id}/complete` amb els `collaboratorChildIds` triats.
  - Pantalla de confirmació breu ("Enviat per aprovació!") en tancar el flux.
  - Si un altre fill obre la mateixa tasca un cop reclamada: si no hi participa, mostra l'estat "Ja s'ha completat aquesta tasca" (amb qui se n'ha encarregat, si es coneix); si hi participa, mostra "Has col·laborat en aquesta tasca — esperant l'aprovació".
  - Al panell d'Aprovacions de PARENT, un grup de finalització amb diversos participants s'aprova o rebutja com una sola fila (una trucada a `POST /api/task-completions/group/{completionGroupId}/approve`), no una per fill.

**5.3 Responsabilitat — Penalització.** Al pas "2. Recompensa", quan `type = RESPONSIBILITY`, afegeix una subsecció "Penalització" amb el mateix patró de dos switches + camps (Diners / Minuts de pantalla) del punt 5.1, que guarda `penalty_money_amount`/`penalty_screen_minutes` a la tasca. Ambdós, tant la recompensa com la penalització, poden quedar a 0. Al panell PARENT (per exemple, a Aprovacions o a una nova pestanya "Tasques incompletes" dins de Tasques), llista el resultat de `GET /api/tasks/incomplete` amb un botó "Aplicar penalització" per fill que crida `POST /api/tasks/{taskId}/children/{childId}/apply-penalty` — mai s'aplica sola de forma automàtica.

**5.4 Selector de minuts sense restricció de múltiples de 5.** Canvia qualsevol input de minuts de pantalla (recompensa de tasca, Bonificacions, Fills) per un camp numèric lliure (`type="number"`, `step="1"`) amb xips d'accés ràpid opcionals (+15, +30, +60) al costat, en comptes de l'`step="5"` actual.

**Implementació (Fase B) — decisions no explícites aquí:**
- Cada `TaskCompletion` d'un grup guarda l'import **total** de la tasca (sense dividir) en completar-se, igual que ja feia el flux individual — `POST /api/task-completions/group/{completionGroupId}/approve` divideix aquest snapshot pel nombre de participants, en comptes de rellegir `task_rewards` en viu. Manté el mateix invariant que ja hi havia ("la recompensa mai canvia després de completar-se") en lloc d'introduir-ne un de nou només per al cas col·laboratiu.
- El bloqueig de "TASK_ALREADY_CLAIMED" i l'estat CLAIMED_BY_OTHERS es resolen amb una única consulta compartida (`RecurrenceWindow`, extreta de `TaskService`) reutilitzada també per `GET /api/tasks/incomplete` — evita triplicar el càlcul de "quina és la finestra de recurrència vigent".
- "Tasques incompletes" (5.3) es va construir com un quart botó de filtre dins la mateixa pantalla de Tasques (no una pestanya separada ni dins Aprovacions), reaprofitant el mateix `BaseCard`/`BaseButton` de la llista de tasques.
- Aplicar una penalització no té cap protecció d'idempotència al backend (no hi ha taula que recordi "ja penalitzat aquest període") — el frontend simplement treu la fila de la llista just després d'aplicar-la amb èxit; és una acció manual i deliberada del PARENT, no un procés automàtic que calgui protegir de duplicar-se sol.
- El comptador de pendents (`useApprovalsStore.decrement()`) accepta ara un nombre opcional de files a restar, perquè aprovar/rebutjar un grup col·laboratiu resol diverses files de cop, no una.

### 6. Frontend — CHILD (amplia el Prompt 8)

**6.1 Pantalla — ja sense solapament (Opció A).** Reorganitza la targeta de saldo de minuts perquè el número principal quedi centrat dins l'anell i el text secundari (per exemple "de 200 min aquest mes") vagi just a sota, fora de l'anell — mai superposat.

**6.2 Avatar amb inicial i edició (adelante).** Afegeix un component `ChildAvatar` (cercle de color amb la inicial del nom, o la icona triada si `avatar_icon` no és `null`) i col·loca'l a l'esquerra de la salutació personalitzada a totes les pantalles CHILD. A la barra de nav, el mateix avatar (més petit) porta superposada una insígnia de llapis; en tocar-la, obre un modal de personalització amb **pestanyes separades "Color" / "Icona"** (Opció 2 confirmada): la pestanya Color mostra la graella tancada de tons (sense blancs), la pestanya Icona mostra la graella d'icones de línia blanca. Desar crida `PATCH /api/children/{id}/avatar`.

**6.3 Minuts de pantalla setmanals (conversió automàtica, Opció 1 confirmada).** Al formulari de Fills (Prompt 9), el camp canvia d'etiqueta a "Minuts de pantalla per setmana". El valor que veu i edita el pare és `screen_minutes_monthly ÷ 4` (arrodonit); en desar, es torna a multiplicar per 4 i es guarda a `screen_minutes_monthly` de sempre — cap canvi de backend ni de calendari de generació, només la unitat que veu el pare.

**Implementació (Fase C) — decisions no explícites aquí:**
- `avatarColor`/`avatarIcon` viatgen dins la mateixa resposta de `POST /api/auth/login` i `POST /api/auth/refresh` (camps nous a `AuthResponse`) — evita una crida addicional només per pintar l'avatar just després d'entrar. `useAuthStore.setAvatar(...)` actualitza l'store a l'instant en desar des del modal, sense esperar un refresh de sessió.
- L'editor d'avatar es va penjar de la mateixa capçalera d'Inici que ja fa servir el selector d'idioma (mai una barra de nav de 5 ítems ni una pantalla "Perfil" nova) — coherent amb el precedent ja establert al Prompt 5: com que CHILD no té pantalla de Configuració pròpia, les icones d'utilitat hi viuen a Inici.
- Nou component compartit `ChildScreenHeader` (avatar + títol) reutilitzat a Tasques/Objectius/Pantalla per no repetir el mateix marcatge tres vegades.
- L'arreglo del solapament (6.1) substitueix el hack de `margin-top` negatiu per un contenidor `position: relative` amb el número centrat via `position: absolute` — elimina la causa real del solapament, no només el símptoma en una resolució concreta.
- La conversió setmanal (6.3) s'aplica també al bloc de només lectura de Fills, no només al formulari d'edició — mostrar "200 min/mes" a un lloc i "50 min/setmana" a un altre hauria estat inconsistent per al mateix ajust.
- Es va unificar la paleta de colors duplicada (Registre i Fills tenien cadascun el seu propi array de 5 colors, subconjunt incomplet dels 9 que ja validava el backend des de la Fase A) en una única constant `CHILD_COLORS` compartida.

**Implementació (Fase C, ajust posterior) — decisions no explícites aquí:**
- **Icones d'avatar:** substituïdes les 6 formes genèriques dibuixades a mà per 8 icones d'animals (`cat`, `dog`, `bird`, `fish`, `rabbit`, `horse`, `butterfly`, `cow`) de la llibreria [Phosphor Icons](https://phosphoricons.com) (pes "regular", contorn), extretes directament del paquet `@phosphor-icons/core` (MIT, gratuït). Es va descartar Font Awesome perquè l'estil "Classic Regular" (contorn) que es demanava original­ment només existeix a Font Awesome Pro (de pagament), cosa que trencaria el principi de cost zero ja establert per a Neon/Render. `AVATAR_ICON_VIEWBOX` (`0 0 256 256`) i `AVATAR_ICON_PATHS` es van reescriure amb els paths reals de Phosphor; el SVG de `ChildAvatar.vue` va passar de traç blanc (`stroke`) a farciment blanc (`fill`), consistent amb el format nadiu de Phosphor. `AvatarIconSet.VALID_ICONS` al backend es va actualitzar amb la mateixa llista.
- **Editor d'avatar + selector d'idioma + canvi de PIN, unificats i traslladats a la TopBar:** el precedent d'"Inici com a lloc de les icones d'utilitat" (vegeu nota anterior) es va revisar perquè l'avatar només era editable des de la pantalla Inici — a Tasques/Objectius/Pantalla només es veia un cercle de només lectura. Ara `TopBar.vue` (compartida amb PARENT mòbil) mostra, només per a rol CHILD, un botó d'avatar amb insígnia de llapis just abans del botó de sortida, que obre `ChildAccountModal.vue` — un modal nou de 3 seccions (Avatar / Idioma / PIN) que substitueix `AvatarEditorModal.vue` (eliminat). La secció Idioma incrusta el `LanguageSwitcher` ja existent; la secció Avatar reutilitza la lògica de pestanyes Color/Icona d'abans.
- **Canvi de PIN autoservei pel fill:** nou endpoint `PATCH /api/users/me/pin` (`ChangeOwnPinRequest{oldPin, newPin}`), obert a qualsevol usuari autenticat (`@PreAuthorize("isAuthenticated()")` a nivell de mètode, sobreescrivint el `hasRole('PARENT')` de classe de `UserController`, mateix patró que `updateLocale`). `UserManagementService.changeOwnPin(...)` valida el PIN antic (`INVALID_CURRENT_PIN` si no coincideix) abans d'aplicar el nou. **Es manté intacte, sense cap canvi,** el flux existent de `resetPin` (PARENT-only, sense validar PIN antic, pot resetejar el de qualsevol membre de la família) — el canvi de PIN autoservei és una via *addicional*, no un substitut, perquè un pare ha de poder seguir recuperant l'accés d'un fill encara que aquest hagi oblidat el PIN nou.
- De pas es van completar 8 claus `errors.*` que faltaven a `ca.json`/`es.json`/`en.json` des de les fases A-C (`GOAL_PERCENTAGE_EXCEEDS_AVAILABLE`, `SAVINGS_GOAL_NOT_FOUND`, `INVALID_CHILD_COLOR`, `INVALID_AVATAR_ICON`, `TASK_ALREADY_CLAIMED`, `INVALID_TASK_TYPE`, `INVALID_TASK_PENALTY`, `INVALID_CURRENT_PIN`) que fins ara queien silenciosament al missatge genèric `errors.GENERIC`.

### 7. Frontend — Resum familiar (amplia el Prompt 9)

**7.1 Targeta híbrida (confirmada).** Substitueix la targeta de cada fill/adult per: una fila de 3 mini-estadístiques (Total, Per gastar, Estalvi) i, a sota, una barra segmentada proporcional amb la mateixa informació més, si en té, el tram de cada objectiu actiu (`allocation_percentage`), amb la seva llegenda de colors a sota.

**7.2 Moviments amb filtres, no scroll infinit.** Per defecte mostra només els moviments dels últims 7 dies (o els últims 10, el que tingui menys), agrupats per data. Afegeix una barra de xips de període ("Aquesta setmana" / "Aquest mes" / "Personalitzat") i una fila de xips per fill (avatar de color de cada fill + "Tots"), que criden l'endpoint de moviments amb els nous paràmetres `from`/`to`/`childId` del punt 3. Un enllaç "Veure tots els moviments" porta a una pantalla pròpia amb paginació.

**7.3 Color del fill aplicat arreu.** Fes servir `avatar_color` de cada fill com a variable CSS (`--child-color`) a: el seu avatar (punts 6.2 i aquí), un accent (vora esquerra o similar, no un farciment complet, per no perdre contrast) a la seva targeta del Resum familiar i a la seva fitxa dins de Fills, i als xips de filtre del punt 7.2.

**7.4 Badge de tasca assignada a la nav del fill.** A la barra de nav de CHILD, l'ítem "Tasques" porta el mateix tractament visual que el `badge` d'Aprovacions pendents de PARENT (cercle amb número), comptant les tasques assignades al fill encara no marcades com a fetes. Color del badge: `avatar_color` del propi fill, amb una vora blanca fina per garantir contrast sigui quin sigui el to triat.

### 8. Frontend — Donacions i Objectius (amplia el Prompt 8, pantalla Objectius)

**8.1 Crear/editar un objectiu.** El formulari "Nou objectiu" (i la seva edició) demana: Nom de l'objectiu, Import a assolir (€), i Percentatge — amb una previsualització en viu (com a `mapaka_mockup_v2.html`, secció 5.2) que mostra com queda el repartiment de gastar/estalvi/objectiu, i un avís si el percentatge introduït supera el marge disponible (`GOAL_PERCENTAGE_EXCEEDS_AVAILABLE`). Editar un objectiu existent permet canviar tots dos valors amb la mateixa validació.

**8.2 Donacions.** A cada targeta d'objectiu (tant al Resum familiar de PARENT com a la pantalla Objectius del fill, en lectura per al fill) afegeix un botó "🎁 Donar" (només operable per PARENT) que obre un formulari amb Import (€), Nom del donant (opcional) i Missatge (opcional), i crida `POST /api/savings-goals/{id}/donations`. L'import donat es mostra al progrés de l'objectiu però mai es reparteix ni resta res del gastar/estalvi del fill.
```

---

## Prompt 16 — Verificació

```
Revisa tot el que s'ha implementat als prompts 1-15 contra Família+.pdf i contra aquest document:

1. Cap acció que generi recompensa (diner o temps) és efectiva sense passar per un estat d'aprovació o pel repartiment explícit de la sessió NFC.
2. Cap saldo es guarda com a valor fix — tot es calcula per suma de moviments (ledger), incloent el temps de pantalla assignat per sessions NFC.
3. Els rols CHILD no poden accedir a cap endpoint ni ruta reservada a PARENT.
4. Cap text visible a la interfície està escrit literalment dins d'un component — tot passa per `$t()`. Executa el script de paritat de claus entre `ca.json`, `es.json` i `en.json` i confirma que no hi ha cap clau que falti a cap dels tres. Prova manualment el canvi d'idioma des del selector en almenys dues pantalles (una CHILD, una PARENT) i confirma que persisteix després de recarregar i després de tancar sessió i tornar a entrar.
5. Els imports i minuts es mostren amb font-variant-numeric: tabular-nums, i els imports monetaris mantenen sempre el format `14,00 €` independentment de l'idioma actiu de la interfície.
6. La navegació canvia correctament de bottom-nav a sidebar segons breakpoint i rol.
7. Escriu tests d'integració per als tres endpoints nous de sessió NFC (tap, stop, assign), incloent el cas de repartiment amb saldo negatiu.
8. El PIN (de qualsevol rol) i el codi de recuperació de família mai apareixen en clar en logs, respostes d'error ni al codi font — sempre hashejats. El codi de recuperació només es mostra un cop, a la resposta de POST /api/families/register.
9. Escriu tests d'integració per al flux de registre (POST /api/families/register), l'alta d'un fill amb PIN, i el flux de recuperació (POST /api/auth/recover) incloent el cas del codi ja consumit.
10. Cap endpoint del backend retorna mai un missatge d'error com a text literal — sempre un codi semàntic que el frontend tradueix.
11. Un PARENT pot, sense sortir de la interfície: crear una tasca de tipus Responsabilitat i una d'Extra amb recompensa i assignar-les a un fill; definir una regla de paga general per franja d'edat; generar la paga del mes i veure-la reflectida com a MoneyTransaction al moviment del fill. Si algun d'aquests passos no es pot fer sense escriure SQL a mà, el prompt corresponent no s'ha acabat.
12. Un CHILD pot crear un nou objectiu d'estalvi des de la pantalla Objectius, no només veure els que ja existeixen.
13. A Fills, amb l'interruptor de paga personalitzada desactivat, l'import mostrat és de només lectura i coincideix amb la regla general que correspongui per l'edat del fill — no hi ha cap camp "paga mensual" editable duplicat en cap altre lloc de la pantalla.
14. Generar la paga del mes crea moviments de diners **i** de temps de pantalla a la vegada, per a tots els fills. Cap procés genera temps de pantalla diàriament, i el saldo de minuts d'un fill que no fa servir la tauleta un mes es manté acumulat al mes següent (no es reinicia ni es perd).
15. Una Bonificació o Penalització de categoria Monetària reparteix automàticament l'import introduït entre gastar i estalviar segons el percentatge actiu del fill — l'usuari mai introdueix aquests dos imports per separat. Una de categoria Temps de pantalla només demana minuts.
16. Al formulari de Tasques, els selectors de tipus i recurrència apareixen en una mateixa fila a partir de 768px d'amplada de viewport, dins el pas "Bàsic".
17. El formulari de "Nova tasca" no té cap camp d'estalvi: la recompensa d'una tasca només demana un import total en diners i uns minuts de pantalla. El repartiment entre gastar i estalviar es calcula en aprovar la tasca (no en crear-la), segons el percentatge de cada fill assignat en aquell moment.
18. La campaneta d'alertes de Resum familiar mostra "Recorda generar la paga" només quan encara no s'ha generat aquest mes, i desapareix sola en cridar "Generar paga del mes" — no hi ha cap taula ni camp nou que guardi l'alerta com a estat.
19. Al pas "Recompensa" de Tasques (i al formulari de Bonificacions), cada tipus d'import (diners / minuts de pantalla) té el seu propi interruptor; amb l'interruptor apagat el camp queda desactivat i el seu valor és 0. No es pot desar amb els dos interruptors apagats alhora.
20. Una tasca Extra no té cap fill assignat en crear-la. En marcar-la com a feta, si el fill indica que ha rebut ajuda, pot triar els germans col·laboradors i la recompensa es reparteix a parts iguals entre tots (inclòs qui l'ha iniciada) abans de passar pel repartiment individual de cadascú. Un cop reclamada (encara que el pare no l'hagi aprovat), cap altre fill no participant la pot tornar a marcar com a feta.
21. Una tasca de tipus Responsabilitat permet deixar la recompensa a 0 i, per separat, configurar una penalització (diners i/o minuts) que mai s'aplica sola — només quan el pare l'activa manualment des de la llista de tasques incompletes.
22. Cap selector de minuts de pantalla de tota l'app està limitat a múltiples de 5 — accepta qualsevol nombre enter.
23. La pantalla Pantalla de CHILD no té cap text superposat a l'anell de progrés. Cada fill mostra el seu avatar (inicial o icona triada) al costat de la salutació a totes les pantalles CHILD, i pot personalitzar-ne el color (mai blanc ni un to molt clar) i la icona des de la barra de nav.
24. El camp de minuts de pantalla de Fills es mostra i s'edita en termes setmanals, però segueix generant-se un cop al mes (multiplicat per 4) exactament com abans — no hi ha cap tasca programada nova.
25. Les targetes del Resum familiar (adults i fills) mostren alhora el total, el disponible per gastar i l'estalvi — mai només un d'aquests tres imports.
26. La llista de moviments del Resum familiar no creix sense límit: mostra per defecte només el període més recent i permet filtrar per setmana, mes o hijo concret, amb accés a la llista completa paginada.
27. El color triat per a cada fill es veu aplicat de manera consistent al seu avatar, a la seva targeta del Resum, a la seva fitxa de Fills i als xips de filtre — sense necessitat d'anar-lo repetint manualment enlloc.
28. L'ítem "Tasques" de la nau de CHILD mostra un comptador quan té tasques assignades pendents de marcar com a fetes, amb el mateix tractament visual que el badge d'Aprovacions de PARENT.
29. Un PARENT pot registrar una donació sobre un objectiu concret d'un fill, i l'import donat s'afegeix al progrés de l'objectiu sense passar mai pel repartiment de gastar/estalvi del fill.
30. Un objectiu d'estalvi nou demana import i percentatge, la suma de percentatges de tots els objectius actius d'un fill mai supera el seu percentatge de gastar vigent, i en completar-se un objectiu el seu percentatge deixa de restar-se automàticament sense intervenció del pare.

Informa de qualsevol incoherència trobada abans de continuar.
```
