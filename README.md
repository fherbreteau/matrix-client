# matrix-client

A lightweight Matrix client library for the JVM with **no runtime third-party dependencies**.

## Requirements

- **Java 25 (latest LTS; the only required runtime is the JDK itself)**
- Maven 3.8+ to build (or use the standard Maven wrapper once installed).

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
# Format-independent clean build with tests
mvn clean verify

# Compile only
mvn compile

# Run unit tests
mvn test

# Generate Javadoc
mvn javadoc:javadoc
```

## Typed event content

`RoomEvent` preserves the full event envelope and raw JSON, including fields this library does not
currently model. The registry provides typed views for common message, membership, room name/topic,
power-level and canonical-alias fields; each typed value also exposes its complete raw content for
standard or future fields not represented as accessors. The registry is intentionally not exhaustive
of Matrix event types. Unknown types and malformed known content return `UnknownEventContent` with
the raw content intact, while registered application parsers are available for custom event types.

```java
import io.github.fherbreteau.matrix.model.EventRegistry;
import io.github.fherbreteau.matrix.model.MessageEventContent;
import io.github.fherbreteau.matrix.model.RoomEvent;

RoomEvent event = RoomEvent.from(response);
var typed = event.withTypedContent(new EventRegistry());
if (typed.content() instanceof MessageEventContent message) {
    System.out.println(message.body());
}
```

## Persistence

Session and sync-token state remain in-memory by default. Applications can opt into file-backed
stores without adding a database dependency:

```java
import io.github.fherbreteau.matrix.endpoint.MatrixClient;
import io.github.fherbreteau.matrix.model.FileSessionStore;
import io.github.fherbreteau.matrix.model.FileSyncTokenStore;
import io.github.fherbreteau.matrix.model.FileTransactionIdStore;
import java.nio.file.Path;

MatrixClient client = MatrixClient.builder("https://matrix.example.org")
    .sessionStore(new FileSessionStore(Path.of("state/session.json")))
    .syncTokenStore(new FileSyncTokenStore(Path.of("state/sync.json")))
    .transactionIdStore(new FileTransactionIdStore(Path.of("state/transactions.json")))
    .build();
```

File stores atomically replace data files when supported by the filesystem and restrict POSIX data
files to owner read/write permissions. Atomic moves do not guarantee durability through sudden power
loss. Session files contain bearer and refresh tokens in plaintext. Session and sync stores
synchronize access per store instance; share an instance between threads. Transaction-ID stores also
coordinate processes targeting the same path with a lock file. `sendMessageEventWithKey` reuses a
persisted transaction ID for the same caller-provided logical operation key; use a distinct stable
key for each operation. Media metadata is optional and application-defined through
`MediaMetadataStore`; it does not cache media bytes.

## Minimal example

```java
import io.github.fherbreteau.matrix.endpoint.MatrixClient;
import io.github.fherbreteau.matrix.json.JsonValue;

MatrixClient client = MatrixClient.builder("https://matrix.example.org").build();
JsonValue versions = client.getVersions();
System.out.println(versions.toJson());
```

## Minimal example
