# Mapaka

**A full-stack family finance & chores app — kids earn, save and spend through a real transaction ledger; parents run it all from a PWA or a sideloaded Android app.**

![Vue 3](https://img.shields.io/badge/Vue-3.5-42b883?logo=vuedotjs&logoColor=white)
![TypeScript](https://img.shields.io/badge/TypeScript-6.0-3178c6?logo=typescript&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Neon-4169E1?logo=postgresql&logoColor=white)
![PWA](https://img.shields.io/badge/PWA-installable-5A0FC8)
![Capacitor](https://img.shields.io/badge/Capacitor-Android-119EFF?logo=capacitor&logoColor=white)
![i18n](https://img.shields.io/badge/i18n-ca%20%7C%20es%20%7C%20en-orange)

## Overview

Mapaka is a full-stack web app I designed and built end-to-end for my own family, replacing an ad-hoc spreadsheet with a real system for managing our children's allowance, chores, savings goals and screen time. It's in active daily use at home, running on a real production stack — PostgreSQL on Neon, Spring Boot on Render — installed as a PWA on iOS and as a sideloaded APK on Android, at zero infrastructure cost.

Beyond solving a real problem, it's an end-to-end case study in owning a product from functional spec through architecture decisions to a deployed, multi-platform app used by real people every day.

## Live demo

**[mapaka-backend.onrender.com](https://mapaka-backend.onrender.com)** — a separate "Demo Family" with fictional data, kept fully isolated from my own family's real usage.

| Role | Family name | Login |
|---|---|---|
| Parent | `Demo Family` | PIN `1234` (name: **Demo Parent**) |
| Child | `Demo Family` | PIN `1111` (name: **Alex**) |
| Child | `Demo Family` | PIN `2222` (name: **Sam**) |

The demo family already has a month's allowance generated and confirmed, a savings goal in progress with a donation, an approved chore showing the automatic reward split, and one pending chore waiting for approval — so there's real data to look at immediately, and a pending-approval flow to try as the parent.

Running on Render's free tier, so the backend sleeps after 15 minutes of inactivity and can take up to a minute to wake up on the first request — the app shows an explicit loading screen for that instead of a blank page (see [Deployment](#deployment)).

## Feature highlights

- **Two roles, one codebase.** PARENT (full control) and CHILD (age-appropriate, restricted) share the same authenticated app, with authorization enforced server-side at the endpoint level, not just hidden in the UI.
- **Ledger-first financial model.** No balance — money, savings, or screen time — is ever stored as a fixed number. Every available amount is always `SUM(transactions)` computed at read time. A deliberate architectural choice to guarantee auditability and eliminate an entire class of balance-drift bugs.
- **Chores with automatic reward splitting.** Recurring "responsibility" chores and one-off "extra" tasks, gated behind parent approval; the reward amount is split into spending/savings/goal allocations automatically at approval time, using each child's own percentage rule — never entered by hand.
- **Percentage-funded savings goals.** A goal reserves a slice of a child's future spending percentage and fills itself from every payout, plus optional one-off donations from relatives.
- **NFC-powered shared screen time.** A physical NFC tag on the family tablet starts/stops a session; elapsed time is split among the children who used it and debited from each one's monthly ledger — no daily bookkeeping, and negative balances are allowed and recovered on the next payout.
- **Full internationalization from day one.** Catalan as the source language, with complete Spanish and English translations and an automated key-parity check so no UI string can silently go untranslated in one locale.
- **Deployed for real, at zero cost.** Serverless Postgres on Neon's free tier, Spring Boot on Render's free tier, distributed as an installable PWA for iOS and a Capacitor-wrapped Android APK sideloaded outside the Play Store — a deliberate distribution strategy for cost and control, not a shortcut.

## Tech stack

| Layer | Stack |
|---|---|
| Backend | Java 21 · Spring Boot 4.1 · Spring Security (JWT) · Spring Data JPA · Flyway · PostgreSQL · Actuator |
| Frontend | Vue 3 (Composition API) · TypeScript · Vite · Pinia · Vue Router · vue-i18n · Chart.js |
| Mobile | Capacitor (Android APK) · vite-plugin-pwa (installable PWA for iOS/Android) |
| Infra | Neon (serverless PostgreSQL) · Render (Docker web service) · Docker Compose for local dev |
| Testing | Spring Boot integration tests against `embedded-postgres` (real DB, no mocks) |

## Architecture highlights

- **Domain-driven backend structure** — 17 feature packages (`auth`, `family`, `child`, `task`, `allowance`, `money`, `savings`, `screentime`, `settlement`, `notification`, `audit`, `security`, …) instead of a generic `controller/service/repository` split, so each domain owns its full vertical slice.
- **13 incremental Flyway migrations** reflecting real, iterative schema evolution — including two migrations added after the app was already in use, once real usage surfaced gaps in the original data model (per-child avatars, goal percentages, task penalties, a genuine monthly screen-time ledger replacing an earlier daily-reset design).
- **JWT auth with server-enforced role boundaries** — a CHILD account cannot reach a PARENT-only endpoint even by calling the API directly; it's not just a hidden UI element.
- **Integration tests over embedded Postgres** covering family registration, NFC sessions, and both roles' core screens end-to-end against a real (in-memory) database.
- **Single-artifact production deployment** — one Docker image serves both the REST API and the compiled Vue SPA from the same Spring Boot jar, so the whole app runs inside one free-tier Render instance with no separate static hosting.

## Project structure

```
backend/    Spring Boot 4.1 (Java 21) — REST API, JPA, Flyway migrations, JWT security
frontend/   Vue 3 + Vite + TypeScript — SPA/PWA, Capacitor Android wrapper
docs/       Brand assets
```

## Running it locally

```bash
docker compose up
```

Boots a local PostgreSQL, the backend (`dev` profile, port `8080`) and the Vite dev server (port `5173`).

## Deployment

Runs entirely on free tiers:

- **Database:** [Neon](https://neon.tech) — serverless PostgreSQL, auto-suspends and wakes on its own, no manual reactivation. Flyway migrations run automatically the first time the backend connects, so the schema needs no manual setup.
- **Backend + frontend:** a single [Render](https://render.com) web service (Docker), built from `Dockerfile.prod`, which compiles the Vue app and serves it as static resources from the same Spring Boot jar as the API. `render.yaml` at the repo root defines the service as a Render Blueprint, so it deploys straight from a GitHub connection with no manual service configuration beyond three environment variables (`DATABASE_URL`, `JWT_SECRET`, `FRONTEND_URL`).

One thing worth knowing if you spin up your own instance: Render's free plan sleeps the service after 15 minutes of inactivity and takes up to a minute to wake — the frontend shows an explicit loading state for that cold start rather than a blank screen.

## Mobile distribution

- **Android:** packaged with [Capacitor](https://capacitorjs.com), distributed as a directly-installable APK (sideload) — no Google Play review process or developer fee, and it unlocks native NFC tag read/write from inside the app.
- **iOS:** installed as a PWA straight from Safari ("Add to Home Screen") — no native build or Apple Developer account needed.

A deliberate choice to ship on both platforms without the cost and lead time of app store distribution, while keeping the codebase a single Vue 3 app.

## Development process

This was built from a complete functional specification, broken into an ordered sequence of self-contained implementation stages, each one reviewed against the running app before moving to the next — including several rounds of revisiting already-shipped screens once real household usage surfaced UX and data-model gaps (a per-child avatar system, percentage-based savings goals, and a monthly rather than daily screen-time model were all added this way). It reflects how I like to work: specify precisely, ship incrementally, and treat real usage feedback as part of the spec rather than an afterthought.

## Project status

**Implemented:** family registration & PIN-based auth for both roles, full i18n (ca/es/en) with automated key-parity checks, chores & rewards with per-child automatic splitting, allowance rules and monthly payout generation, savings goals with donations, NFC-based shared screen-time sessions, Android packaging, production deployment on Render.

**Next up:** CI pipeline, frontend unit tests.

## Author

**Héctor Lima Hevia**
[GitHub](https://github.com/hectorlimahevia) · [LinkedIn](https://www.linkedin.com/in/hectorjlima) · hectorlimahevia@gmail.com

---

*Personal project, built and actively used by my own family. See [`LICENSE`](LICENSE) — all rights reserved; feel free to browse and run the code to evaluate my work, but it isn't licensed for reuse or redistribution.*
