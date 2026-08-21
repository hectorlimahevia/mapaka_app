# Mapaka — Android (Capacitor)

Aquesta carpeta és el projecte Android natiu generat per Capacitor. **No s'edita directament** el contingut web (`android/app/src/main/assets/public`) — sempre es regenera des de `frontend/`.

## Flux de treball estàndard

Cada cop que hi hagi canvis al codi Vue 3 que calgui provar o empaquetar a Android:

```bash
npm run build
npx cap sync android
```

`cap sync` copia l'última build (`dist/`) dins del projecte Android i actualitza els plugins natius (com `@capgo/capacitor-nfc`). No cal tornar a fer `npx cap add android`.

## Requisits per compilar (no inclosos en aquest repositori)

- **Android Studio** (inclou el SDK d'Android i Gradle).
- **JDK 21** (ja necessari per al backend).

> Aquesta màquina de desenvolupament no té l'SDK d'Android instal·lat — el projecte s'ha generat i verificat pel que fa a estructura i configuració, però la compilació real de l'APK i les proves amb lector NFC físic només es poden fer des d'un equip amb Android Studio i, idealment, un dispositiu Android real (l'emulador no té lector NFC).

## Obrir el projecte

Android Studio → **Open** → selecciona la carpeta `frontend/android`. Deixa que Gradle sincronitzi la primera vegada (pot trigar uns minuts).

## Generar la clau de signatura (una sola vegada)

Una APK signada necessita una clau pròpia. **Genera-la tu mateix i guarda-la fora del repositori** — perdre-la vol dir no poder tornar a publicar una actualització de l'app amb el mateix `appId` (`cat.mapaka.app`) mai més. Aquest repositori ja ignora `*.keystore` i `*.jks` (`.gitignore` arrel) perquè mai s'hi pugi per accident.

```bash
keytool -genkeypair -v -keystore mapaka-release.keystore -alias mapaka -keyalg RSA -keysize 2048 -validity 10000
```

Et demanarà una contrasenya per al keystore i una altra per a la clau (poden coincidir) — anota-les en un gestor de contrasenyes, no en cap fitxer del projecte. Guarda `mapaka-release.keystore` en un lloc seguir (per exemple, el mateix gestor de contrasenyes o un disc extern), **no dins de `frontend/android`**.

## Generar l'APK signada

1. A Android Studio: **Build → Generate Signed Bundle / APK…**
2. Selecciona **APK** (no *Android App Bundle* — no publiquem a Google Play, i una APK és més senzilla d'instal·lar directament).
3. Selecciona el fitxer `mapaka-release.keystore` generat al pas anterior i introdueix les contrasenyes.
4. Selecciona la variant **release**.
5. Android Studio deixa l'APK resultant a `android/app/release/app-release.apk`.

## Instal·lar-la en un dispositiu

**Per USB (amb el dispositiu connectat i la depuració USB activada):**

```bash
adb install android/app/release/app-release.apk
```

**Per enllaç de descàrrega** (sense cable): puja `app-release.apk` a qualsevol lloc que el dispositiu pugui obrir (un Drive, un servidor propi, etc.), descarrega-la des del mòbil i obre-la.

En qualsevol dels dos casos, el dispositiu Android demanarà activar **"Permetre instal·lació d'aplicacions d'origen desconegut"** per a l'app des de la qual s'instal·la (el navegador o el gestor de fitxers) — és normal, ja que no es distribueix via Google Play.

## El que NO fa aquest projecte (deliberadament)

- **No hi ha publicació a Google Play.** No hi ha cap fitxer de configuració relacionat (`play-services.json`, credencials de Play Console, etc.) — la distribució és sempre APK directa.
- **No hi ha cap dependència d'iOS.** iOS s'instal·la com a PWA des de Safari, sense empaquetat natiu — vegeu `docs/mapaka_documento_global.md` secció 5.

## Funcionalitat NFC exclusiva d'Android

La pantalla de sessió compartida (`/screen/:token`) funciona igual de bé sense app instal·lada (patró passiu: l'etiqueta porta la URL gravada i el sistema operatiu l'obre). Quan es detecta que l'app corre dins de Capacitor natiu (`Capacitor.isNativePlatform()`), apareixen dues funcionalitats addicionals:

- Un botó **"Escanejar ara"** a la pròpia pantalla de sessió, per fer una lectura NFC activa sense dependre que el sistema operatiu obri la URL automàticament.
- Una pantalla d'administració (**Configuració → Etiquetes NFC**) perquè el PARENT registri etiquetes noves i les escrigui físicament des de l'app, sense eines externes.

Cap d'aquestes dues funcions és necessària per al funcionament bàsic — són millores exclusives de la variant Android.
