# Repository Guidelines

## Project Structure & Module Organization

This repository is split into a Spring Boot backend and a Next.js frontend. Backend code lives in `backend/src/main/java/com/awscalculator/backend`, organized by feature packages such as `project`, `resource`, `calculation`, `recommendation`, `pricing`, `share`, and `ai`. Backend configuration and regional pricing JSON files are in `backend/src/main/resources`; tests are under `backend/src/test`.

Frontend code lives in `frontend/src`. App Router files are in `frontend/src/app`, calculator components are in `frontend/src/components/calculator`, and API helpers are in `frontend/src/lib`. Product notes are in `docs/`.

## Build, Test, and Development Commands

- `docker compose up -d`: starts the local PostgreSQL 16 database using credentials from `docker-compose.yml`.
- `cd backend && ./mvnw spring-boot:run`: runs the API on `http://localhost:8080`.
- `cd backend && ./mvnw test`: runs backend tests.
- `cd frontend && npm install`: installs frontend dependencies from `package-lock.json`.
- `cd frontend && npm run dev`: starts the Next.js dev server on `http://localhost:3000`.
- `cd frontend && npm run build`: creates a production frontend build.
- `cd frontend && npm run lint`: runs ESLint checks.

## Coding Style & Naming Conventions

Use Java 21 for backend changes. Keep Spring classes in feature packages, with conventional names such as `ProjectController`, `ProjectService`, `ProjectRepository`, and DTOs under `dto`. Prefer constructor injection.

Frontend code uses TypeScript, React 19, Next.js 16, and Tailwind CSS. Use PascalCase for React components, camelCase for functions and variables, and keep component-specific types close to the component unless shared.

## Testing Guidelines

Backend tests use Spring Boot and JUnit through Maven. Name test classes after the subject under test, for example `CalculationServiceTests`, and place them in the matching package under `backend/src/test/java`. Add focused service or controller tests when changing cost calculations, persistence, validation, or API responses.

No frontend test framework is currently configured. For UI changes, run `npm run lint` and `npm run build`.

## Commit & Pull Request Guidelines

The current history uses short imperative commit messages, for example `Add Spring Boot backend, multi-region AWS pricing, and full calculator UI`. Keep commits concise and outcome-focused.

Pull requests should include a short summary, tests run, linked issues if applicable, and screenshots for visible UI changes. Call out database, environment variable, pricing data, or API contract changes.

## Security & Configuration Tips

Local database defaults are intentionally development-only. Override `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `SERVER_PORT`, and `AI_EXPLAINER_*` values through environment variables for non-local environments. Do not commit secrets, generated build output, or local database volumes.
