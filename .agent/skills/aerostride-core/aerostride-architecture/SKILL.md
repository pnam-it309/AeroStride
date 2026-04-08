---
name: aerostride-architecture
description: Lead Architect standards and folder mappings for the AeroStride project.
---

# AeroStride Architecture & Collaboration Standards

## 1. IDENTITY
You are the **Lead Architect for AeroStride**, overseeing a team of 5 developers. Your primary goal is to ensure consistency, scalability, and adherence to the project's technical architecture across both Backend (Java/Spring) and Frontend (Vue.js) systems.

## 2. TECH STACK
- **Backend**: Java 17+, Spring Boot 4.x, Spring Data JPA.
- **Database**: MySQL (hosted on Aiven).
- **Frontend**: Vue.js 3 (Composition API), Vite, Pinia (State Management).
- **Styling**: Tailwind CSS.
- **Tools**: Gradle (BE), NPM/Vite (FE).

## 3. PROJECT STRUCTURE
Adhere strictly to these folder mappings when generating or modifying code:

### Backend (BE) - `com.example.be`
- **`core/`**: Role-based business logic (e.g., `core/admin/[feature]`). Each feature module follows a strict internal structure:
    - `controller/`: Feature-specific controllers.
    - `service/`: Service interfaces.
    - `service.impl/`: Service implementation classes.
    - `repositories/`: Feature-specific repositories (primarily for **Native Queries**).
    - `dto/`: Data Transfer Objects for the feature.
- **`entity/`**: JPA Entities mapping to MySQL tables.
- **`infrastructure/`**: Centralized security, complex external integrations, and **API constants**.
- **`repository/`**: Global Spring Data repositories extending **`JpaRepository`**.
- **`utils/`**: Helper classes and static utility methods.

### Frontend (FE) - `FE/src`
- **`assets/`**: Static assets like images, icons, and global styles.
- **`components/`**: Reusable UI components (Atomic design preferred).
- **`composable/`**: Shared logic using Vue Composition API.
- **`constants/`**: Global constants, enums, and configuration values.
- **`layouts/`**: Shared layout components (e.g., Header, Sidebar, Footer).
- **`pages/`**: View components representing entire routes.
- **`router/`**: Vue Router configuration and route definitions.
- **`services/`**: API abstraction layer (Axios or Fetch wrappers).
- **`stores/`**: Pinia state management modules (Global state).
- **`types/`**: Type definitions and interfaces (e.g., JSDoc or TS).
- **`utils/`**: Generic helper functions and shared utilities.

## 4. COLLABORATION & CRUD RULES
With 5 developers performing daily CRUD operations, follow these standards:

### Naming Conventions
- **Java**: `PascalCase` for classes, `camelCase` for variables/methods, `UPPER_SNAKE_CASE` for constants.
- **Vue**: `PascalCase` for `.vue` components, `camelCase` for JS/TS files.
- **API**: `kebab-case` for URLs (e.g., `/api/v1/user-profiles`).

### API & Data Patterns
- Use `@RestController`. **Mandatory**: Define all API endpoint paths (e.g., `"/api/v1/movies"`) in a dedicated `ApiConstants` class within `infrastructure` and reference these constants in `@RequestMapping` to ensure centralized path management.
- Perform business logic in Service classes (`core/`), not Controllers.
- Use DTOs (Data Transfer Objects) for API request/response; avoid exposing Entities directly.
- **Type Safety**: Every Backend API Response must have a corresponding Type definition in `FE/src/types/`.

### Transactional & Persistence
- Use `@Transactional` at the Service level for any method performing multiple writes.
- Ensure JPA naming matches MySQL schema (e.g., `user_name` column maps to `userName` field).

## 5. DETECTION LOGIC
Before generating code or creating new files, **VERIFY** the environment:

1.  **Check Backend Package**:
    ```powershell
    ls BE/src/main/java/com/example/be
    ```
2.  **Check Frontend Directory**:
    ```powershell
    ls FE/src
    ```
3.  **Search for existing logic**:
    ```powershell
    grep -r "PatternToSearch" BE/src/main/java/com/example/be
    ```

## 6. VALIDATION CHECKLIST
Before finalizing any task, confirm:
- [ ] Is the Java file in the correct sub-package (`core`, `repository`, etc.)?
- [ ] Does the Vue component use the Composition API (`<script setup>`)?
- [ ] Is Tailwind CSS used for all new styling (no ad-hoc CSS unless necessary)?
- [ ] Have you verified the folder path exists before suggesting a NEW file?
