# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A hands-on exercise (one of several sibling copies under `03-hands-on/`, one per AI tool) implementing the task in `prompt_1.md`: a weather panel where the user types a city (or uses browser geolocation) and sees the current weather. Data comes from Open-Meteo (Geocoding + Forecast APIs, no key). **The frontend must only talk to the backend**, never to Open-Meteo directly. UI text, error messages and code comments are in Portuguese (PT-BR).

- `backend/` — Spring Boot 4.0.6, Java 25, Maven wrapper. Base package `com.cezar.dev.airquality` (project name "airquality" is a leftover from the template; it's a weather app).
- `frontend/` — Angular 21 standalone components with signals, SSR enabled (`@angular/ssr`, routes prerendered), Tailwind 4 via PostCSS, Vitest for unit tests.
- `compose.yaml` — dev environment for both services.

## Commands

Backend (host has Java 25; no global `mvn`, use the wrapper):

```bash
cd backend
./mvnw test                                        # all tests
./mvnw test -Dtest=WeatherControllerTests          # one class
./mvnw test -Dtest=WeatherControllerTests#unknownCityIs404   # one method
./mvnw package
```

Frontend:

```bash
cd frontend
npx ng test                       # Vitest, all specs
npx ng test --include src/app/app.spec.ts
npx ng build
npx prettier --check "src/**/*.{ts,html}"   # printWidth 100, single quotes
```

Running the app: per the global VPS rules, don't use `ng serve`/`mvn spring-boot:run` on the host — use the `sandbox` skill. `compose.yaml` is the source of truth for how the two services start (`mvn spring-boot:run` in `maven:3.9-eclipse-temurin-25`; `npm ci` + `ng serve -c template` in `node:22`), with code bind-mounted so edits hot-reload. `VERSION` (default `01`) parametrizes the compose project name, Traefik router names and URL prefix so several copies can run side by side.

## Architecture

Request path: browser → `frontend` `WeatherService` (`src/app/weather/weather.service.ts`) calls the **relative** URL `api/weather` → resolved against `<base href>` (`/<VERSION>/`) → Traefik routes `/<VERSION>/api/*` to the backend and strips that prefix → backend sees `GET /weather`. Consequences:
- Backend controllers are mapped without any `/api` prefix (`/weather`, `/healthcheck`).
- The `template` configuration in `angular.json` sets `baseHref` and `allowedHosts`; the compose command rewrites `baseHref` in `angular.json` with `sed` on every start to match `VERSION` (so that file may show local diffs). Missing `allowedHosts` → 403 "Blocked request" from the dev server.
- Spring uses `server.forward-headers-strategy=framework` (set via `SPRING_APPLICATION_JSON` in compose) because it sits behind the proxy.

Backend (`backend/src/main/java/com/cezar/dev/airquality/`):
- `controllers/WeatherController` — `GET /weather?city=...` or `?lat=..&lon=..`; validates input (400 via `ResponseStatusException`), and has local `@ExceptionHandler`s mapping `CityNotFoundException` → 404 and `WeatherUnavailableException` → 502, both with body `{ "message": ... }`.
- `services/WeatherService` — the only class calling Open-Meteo, via two `RestClient`s (JDK HTTP client, 3s connect / 5s read timeouts). Base URLs overridable by `openmeteo.geocoding-url` / `openmeteo.forecast-url`. Upstream JSON is mapped through private nested records into the normalized `dto/WeatherResponse` (`location`, `current`, `units`), so the frontend never sees the Open-Meteo shape. Geocoding uses `count=1`, `language=pt`. Coordinate lookups return `location.name == null` (Open-Meteo has no reverse geocoding).
- `services/WeatherCodes` — WMO weather code → PT-BR description + emoji icon (day/night aware).

Frontend (`frontend/src/app/weather/`):
- `weather.model.ts` mirrors the backend `WeatherResponse` record — keep the two in sync.
- `weather-panel.ts` holds all UI state in signals; uses a `requestId` counter to drop stale responses; guards `navigator` access because the component also renders on the server (SSR); maps HTTP status (400/404/502) to user messages.

Tests:
- Backend `WeatherControllerTests` uses `@SpringBootTest` + MockMvc with `WeatherService` replaced by `@MockitoBean` — no network calls.
- Frontend `app.spec.ts` drives the real form and uses `HttpTestingController`, matching on URL `api/weather` and the displayed PT-BR strings, so renaming text in the template breaks these tests.
