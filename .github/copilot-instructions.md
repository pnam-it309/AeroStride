# Copilot Instructions for AeroStride

## Build, Test, and Lint Commands

### Backend (Spring Boot)
- **Build:**
  - `cd BE && ./gradlew build`
- **Test (all):**
  - `cd BE && ./gradlew test`
- **Test (single):**
  - Use Gradle's `--tests` flag, e.g. `./gradlew test --tests com.example.be.core.admin.AdminServiceTest`

### Frontend (Vue 3 + Vite)
- **Install dependencies:**
  - `cd FE && npm ci`
- **Dev server:**
  - `cd FE && npm run dev`
- **Build:**
  - `cd FE && npm run build`
- **Lint:**
  - `cd FE && npm run lint` (ESLint)
  - `cd FE && npm run format` (Prettier)
- **Test (all):**
  - `cd FE && npm run test`
- **Test (UI):**
  - `cd FE && npm run test:ui`
- **Test (single):**
  - `npx vitest run tests/path/to/file.test.js`

## High-Level Architecture

### Backend
- **Layered Spring Boot:**
  - `core/`: Business logic, split by domain (admin, staff, customer)
  - `entity/`: JPA entities
  - `infrastructure/`: Config, security, constants, exceptions
  - `repository/`: JPA repositories
  - `utils/`: Utility classes
  - `src/main/resources/db/migration/`: Flyway DB migrations
- **API routes** defined in `RoutesConstant.java`.
- **Error handling:** Use `RestApiException` for global JSON error responses.
- **DB changes:** Only via new migration files, not direct DB edits.

### Frontend
- **Vue 3 + Vite + Pinia:**
  - `src/components/`: Shared/base UI components
  - `src/pages/`: Role-based pages (admin, shop, auth, errors)
  - `src/services/`: Axios API services
  - `src/stores/`: Pinia state management
  - `src/constants/`: Route/API constants
  - `src/router/`: Vue Router config
  - `src/utils/`: Utility functions
- **API endpoints** managed in `services/` and `constants/`.
- **Add new page:** Create `.vue` in `pages/` and register in `router/index.js`.
- **Update API:** Edit endpoints in `services/`.

## Key Conventions
- **Backend:**
  - New APIs: Define route in `RoutesConstant.java`, implement in `core/`, add repo if needed.
  - DB: All schema changes via Flyway migrations.
  - Errors: Throw `RestApiException` for consistent error handling.
- **Frontend:**
  - Use composables for reusable logic (`src/composables/`).
  - Shared components in `components/shared/` and `components/base/`.
  - All API calls via `services/`.
  - State via Pinia stores.
- **Docker:**
  - Use `docker/docker-compose.yml` for local/dev multi-service setup (backend, frontend, redis).
  - Build images with provided Dockerfiles in `docker/`.

---

For more details, see `docs/backend_doc.md` and `docs/frontend_doc.md`.
