# Contributing to matrix-client

🎉 **First off, thanks for taking the time to contribute!** 🎉

The following is a set of guidelines for contributing to matrix-client. These are mostly guidelines, not rules. Use your best judgment, and feel free to propose changes to this document in a pull request.

## 📋 Table of Contents

- [How Can I Contribute?](#thinking-how-can-i-contribute)
- [Getting Started](#rocket-getting-started)
- [Development Setup](#computer-development-setup)
- [Pull Request Process](#git-pull-request-process)
- [Coding Standards](#memo-coding-standards)
- [Commit Message Guidelines](#writing_hand-commit-message-guidelines)
- [Testing](#test_tube-testing)
- [Community](#people_hugging-community)

## 🤔 How Can I Contribute?

### Reporting Bugs

- **Use GitHub Issues**: [Open a new issue](https://github.com/fherbreteau/matrix-client/issues)
- **Include details**:
  - Version of matrix-client
  - Java version
  - Operating system
  - Steps to reproduce
  - Expected vs actual behavior

### Suggesting Enhancements

- **Use GitHub Issues**: [Start a discussion](https://github.com/fherbreteau/matrix-client/issues)
- **Provide context**:
  - Use case
  - Why this enhancement would be useful
  - Potential implementation ideas

### Pull Requests

- **Fork the repository**
- **Create a feature branch**: `git checkout -b feature/your-feature`
- **Commit your changes**: `git commit -am 'Add some feature'`
- **Push to the branch**: `git push origin feature/your-feature`
- **Open a pull request**

## 🚀 Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork**:
   ```bash
   git clone https://github.com/your-username/matrix-client.git
   cd matrix-client
   ```
3. **Set up upstream**:
   ```bash
   git remote add upstream https://github.com/fherbreteau/matrix-client.git
   ```

## 💻 Development Setup

### Prerequisites

- **Java 25 (latest LTS)**
- **Maven 3.8+**
- **Git**

### Build the Project

```bash
# Clean build with tests
mvn clean verify

# Compile only (skips tests)
mvn compile

# Generate Javadoc
mvn javadoc:javadoc
```

### Development Workflow

```bash
# Pull latest changes from upstream
git pull upstream main

# Create a new feature branch
git checkout -b feature/your-feature

# Make your changes
# ...

# Build and test
mvn clean verify

# Commit your changes
git commit -am 'Add some feature'

# Push to your fork
git push origin feature/your-feature
```

## 🔄 Pull Request Process

1. **Ensure tests pass**: `mvn test`
2. **Update documentation** if needed
3. **Follow coding standards** (see below)
4. **Write good commit messages** (see below)
5. **Open a pull request** with:
   - Clear title and description
   - Reference to related issues
6. **Address review feedback** promptly

## 📝 Coding Standards

- **Follow the Matrix specification** (<https://spec.matrix.org/latest/client-server-api/>):
  always check the latest published version before implementing or changing an
  endpoint — verify request/response field names, required fields,
  deprecations, rate-limiting and error codes, and record intentional
  deviations in the Javadoc
- **Link to the specification in Javadoc**: every new endpoint method on
  `MatrixClient` and every new model class must carry
  `@see <a href="https://spec.matrix.org/latest/client-server-api/<section>">Matrix specification</a>`
  pointing at the section it implements or models

- **Follow the Google Java Style Guide** — formatting is enforced by
  `google-java-format` (Spotify `fmt-maven-plugin`): run `mvn fmt:format` before committing;
  `mvn verify` fails on unformatted code (2-space indent, 4-space continuation
  indent, 100-column limit) and on Checkstyle `google_checks.xml` violations
  (import order, whitespace, naming, Javadoc on public members with mandatory `@param`/`@return`/`@throws` tags in order)
- **Use meaningful names**: `homeserverUrl` not `url2`, `matrixClient` not `mc`
- **Keep methods small**: Single responsibility principle
- **Add Javadoc**: For public classes and methods (doclint runs in CI)
- **Prefer immutability**: Value types should be immutable; use records and sealed interfaces where they fit
- **No comments in code** unless explicitly requested

### Module System

- Production code must compile against the **JDK only** — never add third-party runtime dependencies
- Test-scoped dependencies (JUnit 5) are allowed
- Keep `module-info.java` in sync with new packages (exported vs internal)

## ✍️ Commit Message Guidelines

### Format

```
<type>(<scope>): <subject>
<BLANK LINE>
<body>
<BLANK LINE>
<footer>
```

### Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Formatting changes
- `refactor`: Code changes that neither fix bugs nor add features
- `perf`: Performance improvements
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

### Example

```
feat(endpoint): Add join-room endpoint

Adds MatrixClient.join(roomId) using the /_matrix/client/join
endpoint with server-name fallback support.

Closes #42
```

## 🧪 Testing

### Running Tests

```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=JsonParserTest
```

### Writing Tests

- **Use JUnit 5** with **AssertJ** for fluent assertions
- **Prefer idiomatic AssertJ** over chained getter assertions:
  - `assertThat(x).extracting(X::getter)` instead of `assertThat(x.getter())`
  - `asInstanceOf(type(...))` or `InstanceOfAssertFactories` instead of manual casts
  - `hasMessage` / `hasCause` on exceptions instead of `getMessage()` / `getCause()`
  - typed factories (`BOOLEAN`, `STRING`, `list`, `map`, `optional`, `collection`) when extracting
  - `singleElement()` on single-item collections, `hasValue()` on `AtomicReference`s
  - never use `org.junit.jupiter.api.Assertions`
- **Never hit the network**: inject a stub `HttpTransport` via
  `MatrixClient.builder(...).transport(...)` instead
- **Test edge cases**: malformed JSON, HTTP errors, empty responses
- **Keep tests isolated**
- **Use descriptive names**: `serverErrorMapsToMatrixException` not `test2`

## 👥 Community

- **GitHub Issues**: For bugs and feature requests
- **Matrix Room**: `#matrix-client:matrix.org`

### Maintainers

- **François Herbreteau**: [@fherbreteau](https://github.com/fherbreteau)
- **Contributors**: See the [contributors graph](https://github.com/fherbreteau/matrix-client/graphs/contributors)

## 🙏 Thanks!

Your contributions make this project better. Whether it's fixing bugs, adding features, improving documentation, or helping others, every contribution is valuable.

**Happy coding!** 🚀
