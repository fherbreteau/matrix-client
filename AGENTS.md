# AGENTS.md

## Project Overview

`matrix-client` is a lightweight Java client library for the [Matrix](https://matrix.org/)
protocol with **no runtime third-party dependencies**. All production code relies
exclusively on the JDK (including `java.net.http.HttpClient` for HTTP).

- **Java**: 25 (latest LTS; `maven.compiler.release=25`)
- **Module system**: Java Platform Module System — module `io.github.fherbreteau.matrix`
  is declared in `src/main/java/module-info.java` and exports exactly the public packages.
- **Build**: Maven (see `pom.xml`).

## Project Documentation

The following documentation files live at the root of the repository:

- **`README.md`** — Project introduction, dependency policy, build commands, and usage example
- **`CHANGELOG.md`** — Release history and changes per version
- **`CONTRIBUTING.md`** — Guidelines for contributing to the project
- **`SECURITY.md`** — Security policy and vulnerability reporting

## Build & Test Commands

```bash
# Full clean build with tests
mvn clean verify

# Compile only (skips tests)
mvn compile

# Run tests only
mvn test

# Run a single test class
mvn test -Dtest=JsonParserTest

# Generate Javadoc
mvn javadoc:javadoc
```

## CI Requirements

All of the following must pass before committing:

1. **Java**: `mvn clean verify` — compiles with `--release 25`, runs Checkstyle (0 violations), the unit tests,
   JaCoCo coverage (≥80% instructions, 0 missed methods/classes), and builds the modular jar.
2. **Javadoc**: `mvn javadoc:javadoc` — must complete without errors (doclint is
   configured as `all,-missing`).
3. **CI**: GitHub Actions (`.github/workflows/ci.yml`) builds on Java 25.

### Coverage Rule (≥80%)

Code coverage must remain **above 80%**. The JaCoCo check in `mvn verify`
enforces ≥80% instructions coverage with **0 missed methods and 0 missed
classes** at the bundle level — never land code that drops coverage below the
gate; add tests in the same change instead. SonarCloud analyzes the project
(`sonar` profile, `mvn -Psonar verify sonar:sonar` in CI with `SONAR_TOKEN`).

## Module System Rules

- Every package under `src/main/java` must either be **exported** in
  `module-info.java` (public API) or remain non-exported (internal).
- The module must only `requires` JDK modules; **never add third-party runtime
  dependencies** (see the dependency policy in `README.md`).
- Test code runs on the classpath (not modularized) and may use JUnit 5.
- Test assertions use the AssertJ fluent style — never `org.junit.jupiter.api.Assertions`.

## Architecture

| Package | Description |
|---|---|
| `io.github.fherbreteau.matrix.transport` | HTTP layer: `HttpTransport` abstraction + `JdkHttpTransport` (default, `java.net.http.HttpClient`) |
| `io.github.fherbreteau.matrix.json` | Minimal JSON parser/serializer (`JsonParser`, sealed `JsonValue` hierarchy) |
| `io.github.fherbreteau.matrix.model` | Matrix data models (`Room`, `RoomEvent`) |
| `io.github.fherbreteau.matrix.endpoint` | `MatrixClient`, the homeserver API entry point (builder pattern) |
| `io.github.fherbreteau.matrix.error` | `MatrixException` base and `MatrixServerException` for HTTP errors |

### Key Patterns

- **Transport abstraction**: all homeserver calls go through `HttpTransport`;
  tests inject a stub transport via `MatrixClient.builder(...).transport(...)`
  instead of hitting the network.
- **JSON**: `JsonValue` is a sealed interface (object / array / string / number /
  boolean / null); parsing is a hand-written recursive-descent parser in `JsonParser`.
- **Error mapping**: non-2xx responses are converted to `MatrixServerException`
  carrying the HTTP status and the Matrix `errcode`/`error` fields.
- **Immutability**: value types (`Room`, `RoomEvent`, JSON types) are immutable or
  build-once; records are preferred for simple data carriers.

## Code Style

- Checkstyle (`checkstyle.xml`) enforces no trailing whitespace, LF line endings at EOF, no tabs, `FinalClass` rule (all classes with private constructors must be `final`), ordered imports (`java` group first, separated), `EmptyLineSeparator` between methods, `UnusedImports`, `WhitespaceAround`.
- No comments in code unless explicitly requested.
- Public classes and methods carry Javadoc (doclint runs in CI).
- Prefer records and sealed interfaces where they fit.
- Keep the dependency policy: production code must compile against the JDK only.

## Dependencies

- **Java 25 (latest LTS)**, **Maven 3.8+**
- **JUnit Jupiter 5.10.2**, **AssertJ 3.27.7** (test scope only)

Keep this section in sync with `pom.xml` when dependencies are bumped.

## Pull Requests

Before creating a pull request — and before pushing any update to an existing one — update `CHANGELOG.md`:

1. Locate the `## [Unreleased]` section at the top of `CHANGELOG.md`.
2. Add a single bullet describing the **main purpose** of the PR (not a per-commit log): `- **Bold summary**: what changed and why (#123).` End the bullet with the PR number in parentheses — at creation time the number is unknown, so append it in the first update after the PR is created.
3. Place it under the matching category header (`🚀 Features`, `🐛 Fixes`, `🛡️ Security & Hardening`, `🧹 Refactoring`, `📚 Documentation`, `🔧 Build System`, `🧪 Testing`, `📁 Project Structure`), creating the category if it does not exist yet.
4. When updating an existing PR, refine its existing entry instead of adding a duplicate.
5. Never modify released sections (`## [X.Y.Z] - date`); they are frozen once published.
