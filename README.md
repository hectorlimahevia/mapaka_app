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

`docker-compose.prod.yml` desplega només el backend (sense contenidor de base de dades — es connecta a Neon via la variable d'entorn `DATABASE_URL`, mai amb credencials al fitxer).

## Empaquetat mòbil

- **Android:** Capacitor, distribuït com a APK d'instal·lació directa (sense Google Play).
- **iOS:** PWA instal·lada des de Safari (sense empaquetat natiu).

Detall complet a `docs/mapaka_documento_global.md` secció 5.
