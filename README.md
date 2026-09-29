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
| `io.github.fherbreteau.matrix.model` | Core Matrix identifiers, room, sync, and response models |
| `io.github.fherbreteau.matrix.model.events` | Typed event content models and registry |
| `io.github.fherbreteau.matrix.store` | Injectable persistence interfaces |
| `io.github.fherbreteau.matrix.store.file` | File-backed persistence implementations |
| `io.github.fherbreteau.matrix.store.memory` | In-memory persistence implementations |
| `io.github.fherbreteau.matrix.retry` | Retry policy and request-observation API |
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

`RoomEvent` preserves the complete event envelope and raw JSON. The registry provides typed views
for standard message content and media metadata, member-event fields and third-party invites, room
name/topic (including `m.topic` text representations), all power-level fields/maps, and canonical
aliases. Message content is a sealed hierarchy selected by `msgtype` (`Text`, `Image`, `Location`,
and related variants); media variants expose a non-null source that is either a plaintext URL or an
encrypted-file descriptor. Unknown message types use an `Unknown` subtype that retains common fields
and raw JSON. Typed content retains its original JSON for extension fields. The event registry is
intentionally not an exhaustive Matrix event catalog: unregistered event types and malformed known
content return `UnknownEventContent`, preserving the raw content. Applications can register parsers
for custom event types.

```java
import io.github.fherbreteau.matrix.model.events.EventRegistry;
import io.github.fherbreteau.matrix.model.events.MessageEventContent;
import io.github.fherbreteau.matrix.model.RoomEvent;

RoomEvent event = RoomEvent.from(response);
var typed = event.withTypedContent(new EventRegistry());
if (typed.content() instanceof MessageEventContent message) {
    System.out.println(message.body());
}
```

## Request reliability

Idempotent HTTP methods use bounded retries by default: two retries with exponential backoff starting at
250 ms and capped at 2 seconds. `Retry-After` is honored up to 30 seconds. POST is not retried unless a
call explicitly declares it safe to repeat. Transaction-ID message sends and redactions are explicitly
replay-safe. Interrupting a calling thread cancels the retry wait and the request is not retried. Sync
continues to use its separate opt-in `SyncLoop` policy.

```java
import io.github.fherbreteau.matrix.endpoint.MatrixClient;
import io.github.fherbreteau.matrix.retry.RetryPolicy;

MatrixClient client = MatrixClient.builder("https://matrix.example.org")
    .retryPolicy(RetryPolicy.defaults())
    .requestObserver(attempt -> metrics.record(attempt.method(), attempt.statusCode()))
    .build();
```

Attempt observations include a correlation ID, sanitized endpoint, status, duration, retry delay and
outcome; they never include request headers, body or query values. Observer implementations should
return quickly and must not throw. Disable automatic retries with `RetryPolicy.disabled()`.

## Persistence

Session and sync-token state remain in-memory by default. Applications can opt into file-backed
stores without adding a database dependency:

```java
import io.github.fherbreteau.matrix.endpoint.MatrixClient;
import io.github.fherbreteau.matrix.store.file.FileSessionStore;
import io.github.fherbreteau.matrix.store.file.FileSyncTokenStore;
import io.github.fherbreteau.matrix.store.file.FileTransactionIdStore;
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
