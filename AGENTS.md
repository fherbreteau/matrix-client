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

## Specification Conformance Rule

The Matrix Client-Server API is a living specification
(<https://spec.matrix.org/latest/client-server-api/>). **Always verify
implementations against the latest published version of the
specification** — fetch the relevant endpoint section from
<https://spec.matrix.org/latest/> before implementing or changing an
endpoint, and check request/response field names, required fields,
deprecations, rate-limiting and error codes against it.

- Every endpoint method must match the current spec for the endpoint it
  wraps (path, parameters, body fields, response fields).
- Removed/deprecated endpoints or fields must not be used (e.g. the
  pre-v1.16 `/profile/{userId}/displayname` paths).
- After implementing, re-read the spec section and diff it against the
  code; record any intentional deviation in the Javadoc.
- **Specification links in Javadoc are mandatory**: every endpoint method
  on `MatrixClient` and every model class must carry
  `@see <a href="https://spec.matrix.org/latest/client-server-api/<section>">Matrix specification</a>`
  pointing at the exact spec section it implements or models (e.g.
  `login`, `put-roomsroomidsendeventtypetxnid`, `get-roomsroomidmessages`).
  Add the link when creating the method or class — never leave a new
  public member without it. Generic accessors reference the closest
  relevant section (API standards, login, server discovery).
- This check applies to new features, bug fixes and refactors alike.

## CI Requirements

- **Optional sync API**: sync support stays opt-in. Use the injectable `SyncTokenStore` for `next_batch` persistence; never reinterpret sync tokens. Long-poll, cancellation and retry/backoff behavior must follow the latest Matrix sync/error guidance and remain configurable and tested.



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
- **AssertJ conventions**: prefer idiomatic AssertJ over chained getter
  assertions — `assertThat(x).extracting(X::getter)` instead of
  `assertThat(x.getter())`, `asInstanceOf(type(...))` (or
  `InstanceOfAssertFactories`) instead of manual casts, `hasMessage` /
  `hasCause` on exceptions instead of `getMessage()`/`getCause()`, and
  typed factories (`BOOLEAN`, `STRING`, `list`, `map`, `optional`,
  `collection`) when extracting. Use `singleElement()` on collections,
  `hasValue()` on `AtomicReference`s, and never JUnit assertions.

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

- **Google Java Style** (https://google.github.io/styleguide/javaguide.html) is the enforced style.
- Formatting is enforced by `google-java-format` through the Spotify `fmt-maven-plugin` — run `mvn fmt:format` after editing, `mvn verify` fails on unformatted code (2-space indentation, 4-space continuation indent, 100-column limit, unused-import removal, import sorting).
- Checkstyle runs the official `google_checks.xml` ruleset (severity error): no trailing whitespace, LF at EOF, no tabs, line length 100, Google import order (static imports first, then third-party, alphabetized in a single group, blank line separated), `EmptyLineSeparator`, `UnusedImports`, `WhitespaceAround`, naming rules, and Javadoc requirements: public classes and members documented **including `@param` for every parameter, `@return` for every non-void method, valid `@param`/`@return`/`@throws` order, and a description on every tag** — `mvn verify` fails on missing or misordered tags.
- **Import preference**: when using a class for the first time, import it instead of
  using its fully qualified name inline — unless the simple name conflicts with
  another class of the same name already imported from a different package.
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
