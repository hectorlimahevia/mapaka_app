# Mapaka

Aplicació familiar per gestionar de forma centralitzada la paga, l'estalvi, les tasques domèstiques, les recompenses i el temps de pantalla dels fills d'una unitat familiar.

Vegeu [`docs/mapaka_documento_global.md`](docs/mapaka_documento_global.md) per a la visió de conjunt del projecte (què és, arquitectura, identitat de marca, distribució) i [`docs/Família+.pdf`](docs/Família+.pdf) per a l'especificació funcional i el model de dades complet.

## Estructura del monorepo

```
backend/    Spring Boot 4.1 (Java 21) — API REST, JPA, Flyway, seguretat JWT
frontend/   Vue 3 + Vite + TypeScript — SPA / PWA
docs/       Especificació funcional, document global i prompts d'implementació
```

## Desenvolupament local

```bash
docker compose up
```

Aixeca PostgreSQL local, el backend (perfil `dev`, port 8080) i el frontend (servidor de desenvolupament Vite, port 5173).

## Producció

Un únic servei desplegat a Render fa de tot: `Dockerfile.prod` (a l'arrel del repositori) compila el frontend Vue i el copia com a recursos estàtics dins del mateix jar de Spring Boot, així que el backend serveix alhora l'API i l'aplicació web — sense cap hosting de frontend separat ni cost addicional (Prompt 13 de `docs/mapaka_prompts_code.md`).

### Base de dades (Neon)

1. Crea un compte a [neon.tech](https://neon.tech) (pla gratuït) i un projecte nou.
2. Al panell del projecte, copia la "Connection string" en format `postgres://usuari:contrasenya@host/basededades?sslmode=require` — aquest és el valor que s'ha de posar a la variable d'entorn `DATABASE_URL` de Render (secció següent). **No cal reescriure-la com a `jdbc:...`**: `DatabaseUrlEnvironmentPostProcessor` la converteix automàticament a l'arrencar, perquè el driver JDBC de PostgreSQL no accepta credencials incrustades a la URL tal com les dona Neon.

### Backend + frontend (Render)

1. Crea un compte a [render.com](https://render.com) i connecta-hi aquest repositori de GitHub.
2. "New +" → "Blueprint" → selecciona el repositori. Render detecta `render.yaml` a l'arrel i proposa crear-hi un servei web Docker (`mapaka-backend`) que fa servir `Dockerfile.prod`.
3. Al crear el servei, Render demanarà les variables marcades com a secretes al blueprint (mai queden escrites al repositori):
   - `DATABASE_URL` — la connection string de Neon del pas anterior, tal qual.
   - `JWT_SECRET` — una cadena aleatòria llarga, p. ex. `openssl rand -base64 48`.
   - `FRONTEND_URL` — origen permès per CORS. La mateixa app web servida des d'aquest backend no el necessita (same-origin); aquesta variable és per a l'APK Android/Capacitor o qualsevol altre frontend que hi truqui des d'un origen diferent — p. ex. `https://localhost`.
4. Desplega. El healthcheck de Render apunta a `/actuator/health` (ja inclòs des del Prompt 1) — Render no marca el servei com actiu fins que respon.
5. **El pla gratuït de Render "adorm" el servei als 15 minuts d'inactivitat i triga fins a un minut a despertar.** El frontend ja mostra un estat de càrrega ("Despertant Mapaka…") a qualsevol crida que trigui — no cal fer res més per a l'MVP.

Un cop desplegat, `docker-compose.prod.yml` (a l'arrel) reprodueix exactament el mateix `Dockerfile.prod` en local si cal depurar-ho sense passar per Render — les tres variables (`DATABASE_URL`, `JWT_SECRET`, `FRONTEND_URL`) s'han de proporcionar sempre des de l'entorn, mai hardcodejades al fitxer.

## Empaquetat mòbil

- **Android:** Capacitor (`frontend/android/`), distribuït com a APK d'instal·lació directa (sense Google Play). Vegeu [`frontend/android/README.md`](frontend/android/README.md) per generar la clau de signatura i l'APK.
- **iOS:** PWA instal·lada des de Safari (sense empaquetat natiu).

Detall complet a `docs/mapaka_documento_global.md` secció 5.
