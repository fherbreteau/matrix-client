# matrix-client

A lightweight Matrix client library for the JVM with **no runtime third-party dependencies**.

## Requirements

- Java 17 or newer (the only required runtime is the JDK itself).
- Maven 3.8+ to build.

## Dependency policy

- **Runtime:** production code must depend only on classes provided by the JDK
  (e.g. `java.net.http.HttpClient`, `java.util`).
- **Test:** JUnit 5 is allowed as a test-scoped dependency only.
- **Build tools:** the Maven plugins listed in `pom.xml` are build-time only and
  never leak into the produced artifact.

## Module system

The project uses the Java Platform Module System (`src/main/java/module-info.java`,
module `io.github.fherbreteau.matrix`) and exports the following packages:

| Package | Purpose |
| --- | --- |
| `io.github.fherbreteau.matrix.transport` | HTTP layer built on `java.net.http.HttpClient` |
| `io.github.fherbreteau.matrix.json` | Minimal JSON parser/serializer |
| `io.github.fherbreteau.matrix.model` | Matrix data models |
| `io.github.fherbreteau.matrix.endpoint` | Homeserver API endpoints / client entry point |
| `io.github.fherbreteau.matrix.error` | Error types |

## Building

```sh
# Clean build with tests
mvn clean verify

# Compile only
mvn compile

# Run unit tests
mvn test

# Generate Javadoc
mvn javadoc:javadoc
```

## Minimal example

```java
import io.github.fherbreteau.matrix.endpoint.MatrixClient;
import io.github.fherbreteau.matrix.json.JsonValue;

MatrixClient client = MatrixClient.builder("https://matrix.example.org").build();
JsonValue versions = client.getVersions();
System.out.println(versions.toJson());
```

## CI

GitHub Actions compiles the project and runs the test suite on Java 17 and 21.
