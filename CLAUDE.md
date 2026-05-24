# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Summary

"Who Votes Like You" — a web application that shows users which politicians and parties vote similarly to them, based on parliamentary voting records. The backend is Kotlin/Spring Boot; the frontend is Vue 3 + Pinia + TypeScript.

## Build & Test Commands

```shell
# Run all tests (Kotest)
./gradlew test

# Build without running tests
./gradlew build -x test

# Run a single module's tests
./gradlew :jobs:service:test
```

**Toolchain**: Gradle 9.5.0, Kotlin 2.3.0, Java 25 (virtual threads preferred over coroutines). Root `build.gradle.kts` only declares the Kotest plugin; individual modules configure themselves via convention plugins in `buildSrc/`.

## Architecture Overview

The codebase follows a clean architecture with API interfaces separated from implementations:

```
who-votes-like-you
├── models/                          ← Shared domain models (no module deps)
│   ├── jobs, tenants, votes, vsmetadata
├── storage/
│   ├── api/                         ← Storage interfaces & cache contracts
│   └── in-memory/                   ← In-memory implementations
├── vote-fetcher/service             ← VoteFetcher service (fetches from parliament APIs)
├── vote-viewer/service              ← VoteViewer service (renders user's voting profile)
├── jobs/                            ← Background job management (api + service)
└── voting-tenant/                   ← Multi-parliament plugin system
    ├── plugin-api/                  ← Plugin interfaces
    ├── service/                     ← Plugin registry
    └── implementations/polish-sejm  ← Polish Sejm implementation (~25 files: fetchers, REST client, data models)
```

**Key patterns:**
- `*:api` modules contain interfaces; `*:service` or `implementations/*` modules contain concrete implementations.
- `models/` has no dependencies on other project modules — it's the shared domain layer.
- The voting-tenant system is plugin-based: each parliament (e.g., Polish Sejm) is a separate implementation module that plugs into the core via plugin-api interfaces.

## Technology Stack

- **Backend**: Kotlin with Spring Boot (prefer Java virtual threads over coroutines)
- **Frontend**: Vue 3 + Pinia + TypeScript (Vite + Vitest)
- **Database**: Neo4j (graph-based voting patterns and similarities)
- **Build**: Gradle 9.5.0 with Kotlin DSL, Java 25 toolchain

## Vote Viewer Frontend Commands

The frontend lives in `vote-viewer/frontend/` and is managed by Vite. Run commands from that directory:

```shell
# Start the dev server (hot reload)
cd vote-viewer/frontend && npm run dev

# Build for production (type-checks first, then bundles)
npm run build

# Preview the production build locally
npm run preview

# Format/fix code with Prettier
npm run format

# Check formatting without modifying files
npm run format-check

# Run frontend tests with Vitest
npm test
```

**Toolchain**: Gradle 9.5.0, Kotlin 2.3.0, Java 25 (virtual threads preferred over coroutines). Root `build.gradle.kts` only declares the Kotest plugin; individual modules configure themselves via convention plugins in `buildSrc/`.

## Testing Conventions

- **Behavior tests** → Kotest `BehaviorSpec` DSL (reference: `VoteFetchingTest.kt`)
- **Input/output assertions** → Kotest `FunSpec`
- **Mocking** → MockK
- **Run all tests**: `./gradlew test`

## Test Writing Guidelines (MANDATORY)

1. **Always verify against complete objects — never assert every field separately.**

   ❌ Don't write:
   ```kotlin
   val result = functionUnderTest()
   result.textField shouldBe "value1"
   result.numberField shouldBe 123
   result.list.size shouldBe 2
   result.list[0].value shouldBe 1
   result.list[1].value shouldBe 2
   ```

   ✅ Write:
   ```kotlin
   val result = functionUnderTest()
   result shouldBe ResultType(
       textField = "value1",
       numberField = 123,
       list = listOf(NestedType(value = 1), NestedType(value = 2)),
   )
   ```

   Pick the matcher based on whether fields are deterministic:

   **Determinate objects (standard `equals` works):**
   - Object: `actual shouldBe expected`
   - List (order matters): `actual shouldContainExactly expected`
   - List (order irrelevant): `actual shouldContainExactlyInAnyOrder expected`

   **Non-deterministic fields or custom matchers needed** (e.g. random UUIDs, timestamps with tolerance, floating-point deltas):
   - Object: `shouldBeEqualUsingFields { ... }` — from `io.kotest.matchers.equality.*`
   - List (order matters): `shouldContainExactlyUsingFields { ... }` — from `wvly.utilities.test`
   - List (order irrelevant): `shouldContainExactlyInAnyOrderUsingFields { ... }` — from `wvly.utilities.test`

   **Note:** the two `*UsingFields` collection variants (`shouldContainExactlyUsingFields`, `shouldContainExactlyInAnyOrderUsingFields`) are custom project functions in `wvly.utilities.test`, not standard Kotest matchers. They require the dependency `testImplementation(project(":utilities:test-utilities"))`.

   Within a `*UsingFields` block, use these two knobs:
   - **`this.overrideMatchers`** — apply tolerance or custom matchers to specific fields (e.g. timestamps, doubles).
   - **`this.excludedProperties`** — skip truly non-deterministic fields entirely (e.g. random UUIDs).

   Example:
   ```kotlin
   actual shouldBeEqualUsingFields {
       this.overrideMatchers = mapOf(
           ActualType::timestamp to matchInstantsWithTolerance(1.seconds),
           ActualType::ratio to matchDoublesWithTolerance(0.1),
       )
       this.excludedProperties = listOf(ActualType::id)
       expected
   }
   ```

   **DRY reuse:** if the same field-level comparison is repeated across multiple tests, extract it into an infix helper function (at the bottom of the test file, or in a shared fixtures class). See point 3 for how this applies to `JobStatus`.
2. **Consolidate assertions on the same data into a single Then/And clause.** Never fetch the same cache/list twice in adjacent And sections just to check different fields. Fetch once, assign to a local variable, then assert against the full expected object from that single reference. See: `VoteImportTest.kt` for the canonical pattern.

   The rule is about asserting **one object** across multiple Then/And clauses — not about having separate Then blocks for different objects. It is fine to have one `Then("job finishes") { ... }` followed by another `Then("produces 2 votes") { ... }` when each block asserts a different data source (e.g., JobStatus vs VoteCache).

3. **Verify JobStatus after every processing call using shouldMatch.** When a test calls `plugin.startFetch()`, `plugin.startVoteProcessing()`, or any plugin lifecycle method, it MUST verify the resulting JobStatus using one of these in order of preference:
   1. Full `shouldMatch` (preferred) — verifies state, name, statusMessage, and step tree with timestamp tolerance via `JobTestingUtilities.kt`. Use helper functions to build expected JobStatus objects — follow the pattern in `VoteImportTest.kt` and `VoteFetchingTest.kt`.
   2. State check only (minimum acceptable): `viewer.getJobStatus(jobId).state shouldBe JobState.FINISHED`

   Never verify only the job `name` field without checking `state`. A PENDING or FAILED job could still have a matching name if not checked explicitly.

4. **No comments in tests.** Descriptions in Kotest DSL should be self-descriptive enough that no inline comments are needed. Remove all `// Verify ...`, section comment separators (`// ---`), and similar. If a test description is unclear, improve the description rather than adding a comment. Never leave empty Then blocks — every block must contain at least one assertion against a complete object.

## Important Notes

- **Excluded from build**: `vote-analyzer/` (stub, no src/) and `vote-storage-old/` (legacy Neo4j storage in Java+Kotlin, excluded from root `settings.gradle.kts`) are not part of the active build.
- **Architecture docs** are in `doc/` (12 sections covering introduction through glossary). The building block view (`doc/05_building_block_view/`) has design boards that illustrate component relationships.
