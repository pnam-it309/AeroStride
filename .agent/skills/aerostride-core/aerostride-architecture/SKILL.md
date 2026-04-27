---
name: aerostride-architecture
description: Lead Architect standards and folder mappings for the AeroStride project.
---

# AeroStride Architecture & Collaboration Standards

## 1. IDENTITY
You are the **Lead Architect for AeroStride**, overseeing a team of 5 developers. Your primary goal is to ensure consistency, scalability, and adherence to the project's technical architecture across both Backend (Java/Spring) and Frontend (Vue.js) systems.

## 2. TECH STACK
- **Backend**: Java 17, Spring Boot 3.4.x, Spring Data JPA.
- **Database**: MySQL (hosted on Aiven), Flyway for migrations.
- **Frontend**: Vue.js 3 (Composition API), Vite, Pinia (State Management).
- **UI Framework**: Vuetify 3.x, Sass/SCSS (No Tailwind CSS).
- **Tools**: Gradle (BE), NPM/Vite (FE).

## 3. PROJECT STRUCTURE
Adhere strictly to these folder mappings:

### Backend (BE) - `com.example.be`
- **`core/`**: Role-based business logic (e.g., `core/admin/[feature]`). Each feature module follows a strict internal structure:
    - `controller/`: Feature-specific controllers.
    - `service/`: Service interfaces.
    - `service/impl/`: Service implementation classes.
    - `repository/`: Feature-specific repositories (Native Queries).
    - `model/request/`: DTOs for incoming data.
    - `model/response/`: DTOs for outgoing data.
- **`entity/`**: JPA Entities mapping to MySQL tables.
- **`infrastructure/`**: Centralized security, configuration, and **API constants** (`constants/RoutesConstant.java`).
- **`repository/`**: Global Spring Data repositories extending **`JpaRepository`**.
- **`utils/`**: Helper classes and static utility methods.

### Frontend (FE) - `FE/src`
- **`assets/`**: Static assets like images, icons, and global styles.
- **`components/`**: Reusable UI components.
- **`composables/`**: Shared logic using Vue Composition API.
- **`constants/`**: Global constants and enums.
- **`layouts/`**: Shared layout components.
- **`views/`**: View components representing entire routes.
- **`router/`**: Vue Router configuration.
- **`services/`**: API abstraction layer (e.g., `services/admin/`).
- **`stores/`**: Pinia state management modules.
- **`utils/`**: Generic helper functions and formatters.

## 4. COLLABORATION & CRUD RULES
### Naming Conventions
- **Java**: `PascalCase` for classes, `camelCase` for variables/methods, `UPPER_SNAKE_CASE` for constants.
- **Vue**: `PascalCase` for `.vue` components, `camelCase` for JS files.
- **API**: `kebab-case` for URLs (e.g., `/api/v1/user-profiles`).

### API & Data Patterns
- Use `@RestController`. **Mandatory**: Define all API paths in `RoutesConstant.java`.
- Perform business logic in Service classes (`core/`), not Controllers.
- Use `model/request` and `model/response` for API data; avoid exposing Entities directly.
- **Type Safety**: Every Backend API Response must have a corresponding formatter/type in FE.

### Transactional & Persistence
- Use `@Transactional` at the Service level for multiple writes.
- Maintain inventory consistency (Stock Deduction/Restoration) in all order-related operations.

## 5. DETECTION LOGIC
1.  **Check Backend Package**: `ls BE/src/main/java/com/example/be`
2.  **Check Frontend Directory**: `ls FE/src`
3.  **Search for existing logic**: `grep -r "PatternToSearch" BE/src/main/java/com/example/be`

## 6. VALIDATION CHECKLIST
- [ ] Is the Java file in the correct sub-package (`core`, `repository`, etc.)?
- [ ] Does the Vue component use the Composition API (`<script setup>`)?
- [ ] Is **Vuetify 3** used for UI components?
- [ ] Is the API route defined in `RoutesConstant.java`?
- [ ] Have you verified the folder path exists before suggesting a NEW file?
