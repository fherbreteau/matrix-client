# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### 🚀 Features

- **HTTP Transport and Matrix Error Model**: Hardened the transport layer per issue #3 — `JdkHttpTransport` supports GET/POST/PUT/DELETE with UTF-8 JSON bodies and configurable connect/request timeouts, redirect following and proxy via `HttpTransportConfig` (bearer access-token authentication that is never logged: `Request.toString()` redacts the `Authorization` header); transport failures map to typed exceptions (`TransportException` base, `TransportTimeoutException`, `TransportInterruptedException`, `UncheckedTransportException`); the Matrix error model now retains unknown error-body fields (`getFields`), exposes retryability (`isRetryable()` for 408/429/500/502/503/504), surfaces `Retry-After` (`Response.retryAfterMs`), and a `RateLimitedException` for HTTP 429; tests use real JDK `HttpServer` fixtures plus stubbed `HttpClient`s (#18)

- **Dependency-Free JSON Parser and Serializer**: Hardened the `io.github.fherbreteau.matrix.json` layer to fully implement issue #2 — RFC 8259-compliant strict parsing of objects, arrays, strings, numbers, booleans and null with exact numeric preservation via `BigDecimal` (`JsonNumber.asBigDecimal`, integral values beyond `double` precision stay exact), Unicode surrogate-pair handling in `\uXXXX` escapes with unpaired surrogates rejected, dedicated `JsonParseException` carrying the position of the offending character, deterministic order-preserving serialization, and richer object access for unknown Matrix fields (`getOrDefault`, `size`, `entrySet`, typed `put` overloads); comprehensive new tests cover valid and invalid JSON and Matrix-style nested payload round-trips (#17)

- **Project Bootstrap**: Created the initial Maven project structure for a lightweight Matrix client with no runtime third-party dependencies — Java 25 (latest LTS) with Java Platform Module System (`io.github.fherbreteau.matrix`), HTTP transport built on `java.net.http.HttpClient`, a minimal JSON parser/serializer, Matrix models, endpoints, error types, unit tests, and CI (#1, #16)

### 📚 Documentation

- **Project Documentation**: Added `README.md` (dependency policy, build commands, minimal example), `AGENTS.md`, `CHANGELOG.md`, `CONTRIBUTING.md`, and `SECURITY.md` (#1, #16)

### 🔧 Build System

- **Build Enforcement Parity with vodozemac-java**: The Maven build now enforces the same quality gates as vodozemac-java — JaCoCo coverage check (≥80% instructions, 0 missed classes/methods) failing the build, SonarCloud integration (`sonar` profile, project key `fherbreteau_matrix-client`, JaCoCo XML report wired in), Maven/Java version enforcement (Maven ≥ 3.6.3, Java 25) via maven-enforcer, a `release` profile rejecting snapshot dependencies, and versions-maven-plugin with snapshot/alpha/beta filtering; test suite extended to reach full class/method coverage (#16)

- **Java 25 LTS**: The project always targets the latest LTS — `maven.compiler.release` set to 25 and CI builds on Java 25 (#16)

- **Checkstyle Enforcement**: The Maven build now runs the same Checkstyle ruleset as vodozemac-java (`checkstyle.xml` + empty `checkstyle.suppression.xml`, Checkstyle 14.1.0 via maven-checkstyle-plugin 3.6.0) at the `validate` phase with 0 violations required — imports ordered with the `java` group first, no trailing whitespace, tabs, unused imports, naming and whitespace rules (#16)

### 🧪 Testing

- **AssertJ Migration**: All test assertions migrated from JUnit 5 assertions to the AssertJ fluent style (`assertThat`/`assertThatExceptionOfType`), with AssertJ 3.27.7 added as a test-scoped dependency (#16)

## 🤝 Contributing to Changelog

When making changes, please:

1. Add entries to the **Unreleased** section
2. Follow the existing format and categories
3. Be concise but descriptive
4. Reference related issues/PRs when possible
5. Update version and date when releasing

## 📬 Contact

For questions about this changelog or versioning:

- **GitHub Issues**: [fherbreteau/matrix-client/issues](https://github.com/fherbreteau/matrix-client/issues)
- **Email**: fherbreteau@gmail.com
- **Matrix**: @fherbreteau:matrix.org

---

**Last Updated**: 2026-09-20
**Maintainer**: François Herbreteau
