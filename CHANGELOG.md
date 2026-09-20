# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### 🚀 Features

- **Project Bootstrap**: Created the initial Maven project structure for a lightweight Matrix client with no runtime third-party dependencies — Java 25 (latest LTS) with Java Platform Module System (`io.github.fherbreteau.matrix`), HTTP transport built on `java.net.http.HttpClient`, a minimal JSON parser/serializer, Matrix models, endpoints, error types, unit tests, and CI (#1)

### 📚 Documentation

- **Project Documentation**: Added `README.md` (dependency policy, build commands, minimal example), `AGENTS.md`, `CHANGELOG.md`, `CONTRIBUTING.md`, and `SECURITY.md` (#1)

### 🔧 Build System

- **Build Enforcement Parity with vodozemac-java**: The Maven build now enforces the same quality gates as vodozemac-java — JaCoCo coverage check (≥80% instructions, 0 missed classes/methods) failing the build, SonarCloud integration (`sonar` profile, project key `fherbreteau_matrix-client`, JaCoCo XML report wired in), Maven/Java version enforcement (Maven ≥ 3.6.3, Java 25) via maven-enforcer, a `release` profile rejecting snapshot dependencies, and versions-maven-plugin with snapshot/alpha/beta filtering; test suite extended to reach full class/method coverage

- **Java 25 LTS**: The project always targets the latest LTS — `maven.compiler.release` set to 25 and CI builds on Java 25

- **Checkstyle Enforcement**: The Maven build now runs the same Checkstyle ruleset as vodozemac-java (`checkstyle.xml` + empty `checkstyle.suppression.xml`, Checkstyle 14.1.0 via maven-checkstyle-plugin 3.6.0) at the `validate` phase with 0 violations required — imports ordered with the `java` group first, no trailing whitespace, tabs, unused imports, naming and whitespace rules

### 🧪 Testing

- **AssertJ Migration**: All test assertions migrated from JUnit 5 assertions to the AssertJ fluent style (`assertThat`/`assertThatExceptionOfType`), with AssertJ 3.27.7 added as a test-scoped dependency

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
