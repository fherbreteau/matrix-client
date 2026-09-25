package io.github.fherbreteau.matrix.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.fherbreteau.matrix.error.MatrixServerException;
import io.github.fherbreteau.matrix.model.MatrixFilter;
import io.github.fherbreteau.matrix.model.PasswordCredentials;
import io.github.fherbreteau.matrix.model.RoomEventFilter;
import io.github.fherbreteau.matrix.model.SyncTokenStore;
import io.github.fherbreteau.matrix.transport.HttpTransport;
import io.github.fherbreteau.matrix.transport.TransportInterruptedException;
import io.github.fherbreteau.matrix.transport.TransportTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class SyncLoopTest {

  private static final String LOGIN_OK =
      "{\"user_id\":\"@sync:test.org\",\"access_token\":\"sync-token\",\"device_id\":\"SYNC\"}";
  private static final String SYNC_RESPONSE = "{\"next_batch\":\"n1\",\"rooms\":{}}";

  @Test
  void loopDeliversSuccessfulSyncAndPersistsNextBatch() throws Exception {
    var responses = new AtomicInteger();
    var received = new CountDownLatch(1);
    var token = new AtomicReference<String>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                request ->
                    request.url().endsWith("/login")
                        ? new HttpTransport.Response(200, LOGIN_OK)
                        : responseForFirstThenInterrupt(responses))
            .syncTokenStore(new AtomicTokenStore(token))
            .build();
    client.login(new PasswordCredentials("@sync:test.org", "secret"));
    try (var syncLoop = new SyncLoop(client, 0, null, response -> received.countDown(), 10, 20)) {
      syncLoop.start();
      assertThat(received.await(2, TimeUnit.SECONDS)).isTrue();
      syncLoop.close();
      assertThat(syncLoop.isRunning()).isFalse();
    }
    assertThat(token.get()).isEqualTo("n1");
  }

  @Test
  void loopRetriesRateLimitedErrorsWithRetryAfter() throws Exception {
    var attempts = new AtomicInteger();
    var delivered = new CountDownLatch(1);
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                request -> {
                  if (request.url().endsWith("/login")) {
                    return new HttpTransport.Response(200, LOGIN_OK);
                  }
                  if (attempts.getAndIncrement() == 0) {
                    return new HttpTransport.Response(
                        429,
                        Map.of("retry-after", "0"),
                        "{\"errcode\":\"M_LIMIT_EXCEEDED\",\"error\":\"slow down\"}",
                        0L);
                  }
                  return new HttpTransport.Response(200, SYNC_RESPONSE);
                })
            .build();
    client.login(new PasswordCredentials("@sync:test.org", "secret"));
    try (var syncLoop = new SyncLoop(client, 0, null, response -> delivered.countDown(), 0, 1)) {
      syncLoop.start();
      assertThat(delivered.await(2, TimeUnit.SECONDS)).isTrue();
      assertThat(syncLoop.isRunning()).isTrue();
    }
    assertThat(attempts.get()).isGreaterThanOrEqualTo(2);
  }

  @Test
  void defaultConstructorUsesDefaultRetryBackoff() throws Exception {
    var attempts = new AtomicInteger();
    var delivered = new CountDownLatch(1);
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                request -> {
                  if (request.url().endsWith("/login")) {
                    return new HttpTransport.Response(200, LOGIN_OK);
                  }
                  if (attempts.getAndIncrement() == 0) {
                    return new HttpTransport.Response(
                        429,
                        Map.of("retry-after", "0"),
                        "{\"errcode\":\"M_LIMIT_EXCEEDED\",\"error\":\"slow down\"}",
                        0L);
                  }
                  return new HttpTransport.Response(200, SYNC_RESPONSE);
                })
            .build();
    client.login(new PasswordCredentials("@sync:test.org", "secret"));
    try (var syncLoop = new SyncLoop(client, 0, null, response -> delivered.countDown())) {
      syncLoop.start();
      assertThat(delivered.await(2, TimeUnit.SECONDS)).isTrue();
      assertThat(syncLoop.isRunning()).isTrue();
    }
    assertThat(attempts.get()).isGreaterThanOrEqualTo(2);
  }

  @Test
  void loopRetriesTransportTimeouts() throws Exception {
    var attempts = new AtomicInteger();
    var delivered = new CountDownLatch(1);
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                request -> {
                  if (request.url().endsWith("/login")) {
                    return new HttpTransport.Response(200, LOGIN_OK);
                  }
                  if (attempts.getAndIncrement() == 0) {
                    throw new TransportTimeoutException(
                        "timeout", new HttpTimeoutException("timeout"));
                  }
                  return new HttpTransport.Response(200, SYNC_RESPONSE);
                })
            .build();
    client.login(new PasswordCredentials("@sync:test.org", "secret"));
    try (var syncLoop = new SyncLoop(client, 0, null, response -> delivered.countDown(), 0, 1)) {
      syncLoop.start();
      assertThat(delivered.await(2, TimeUnit.SECONDS)).isTrue();
      assertThat(syncLoop.isRunning()).isTrue();
    }
    assertThat(attempts.get()).isGreaterThanOrEqualTo(2);
  }

  @Test
  void retryDelayDoublesUntilItSaturatesAtConfiguredMaximum() {
    try (var loop =
        new SyncLoop(
            MatrixClient.builder("https://matrix.example.org").build(),
            0,
            null,
            response -> {},
            5,
            40)) {
      assertThat(loop.nextDelay(0)).isEqualTo(1);
      assertThat(loop.nextDelay(5)).isEqualTo(10);
      assertThat(loop.nextDelay(20)).isEqualTo(40);
      assertThat(loop.nextDelay(Long.MAX_VALUE)).isEqualTo(40);
    }
  }

  @Test
  void loopStopsOnNonRetryableErrors() throws Exception {
    var attempts = new AtomicInteger();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                request -> {
                  if (request.url().endsWith("/login")) {
                    return new HttpTransport.Response(200, LOGIN_OK);
                  }
                  attempts.incrementAndGet();
                  return new HttpTransport.Response(
                      403, "{\"errcode\":\"M_FORBIDDEN\",\"error\":\"denied\"}");
                })
            .build();
    client.login(new PasswordCredentials("@sync:test.org", "secret"));
    try (var loop = new SyncLoop(client, 0, null, response -> {}, 10, 20)) {
      loop.start();
      assertThat(loop.awaitTermination(Duration.ofSeconds(2))).isTrue();
      assertThat(loop.isRunning()).isFalse();
    }
    assertThat(attempts.get()).isEqualTo(1);
  }

  @Test
  void listenerFailureStopsTheLoopAndSignalsTermination() throws Exception {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                request ->
                    new HttpTransport.Response(
                        200, request.url().endsWith("/login") ? LOGIN_OK : SYNC_RESPONSE))
            .build();
    client.login(new PasswordCredentials("@sync:test.org", "secret"));
    var delivered = new CountDownLatch(1);
    try (var loop =
        new SyncLoop(
            client,
            0,
            null,
            syncResponse -> {
              delivered.countDown();
              throw new IllegalStateException("listener failed");
            })) {
      loop.start();
      assertThat(delivered.await(2, TimeUnit.SECONDS)).isTrue();
      assertThat(loop.awaitTermination(Duration.ofSeconds(2))).isTrue();
      assertThat(loop.isRunning()).isFalse();
    }
  }

  @Test
  void cancellationInterruptsSyncLoop() throws Exception {
    var entered = new CountDownLatch(1);
    var requests = new AtomicInteger();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                request -> {
                  if (request.url().endsWith("/login")) {
                    return new HttpTransport.Response(200, LOGIN_OK);
                  }
                  entered.countDown();
                  try {
                    new CountDownLatch(1).await(20, TimeUnit.SECONDS);
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new TransportInterruptedException("interrupted", e);
                  }
                  requests.incrementAndGet();
                  return new HttpTransport.Response(200, SYNC_RESPONSE);
                })
            .build();
    client.login(new PasswordCredentials("@sync:test.org", "secret"));
    try (var loop = new SyncLoop(client, 20_000, null, response -> {}, 10, 20)) {
      loop.start();
      assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();
      loop.close();
      assertThat(loop.isRunning()).isFalse();
      assertThat(requests.get()).isZero();
    }
  }

  @Test
  void loopAcceptsValidRetryConfiguration() {
    MatrixClient client = MatrixClient.builder("https://matrix.example.org").build();
    assertThat(new SyncLoop(client, 10, null, response -> {})).isNotNull();
    assertThatThrownBy(() -> new SyncLoop(client, 10, null, response -> {}, -1, 10))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new SyncLoop(client, 10, null, response -> {}, 50, 10))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void loopCanUseATypedFilterAndPreservesOpaqueTokens() throws Exception {
    var requests = new ArrayList<HttpTransport.Request>();
    var responses =
        new ArrayDeque<>(List.of(SYNC_RESPONSE, "{\"next_batch\":\"n2\",\"rooms\":{}}"));
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                request -> {
                  if (request.url().endsWith("/login")) {
                    return new HttpTransport.Response(200, LOGIN_OK);
                  }
                  requests.add(request);
                  if (request.url().contains("since=n2")) {
                    throw new MatrixServerException(500, "M_UNKNOWN", "stop");
                  }
                  return new HttpTransport.Response(200, responses.remove());
                })
            .build();
    client.login(new PasswordCredentials("@sync:test.org", "secret"));
    CountDownLatch delivered = new CountDownLatch(2);
    MatrixFilter filter =
        MatrixFilter.builder()
            .roomTimeline(RoomEventFilter.builder().types(List.of("m.room.message")).build())
            .build();
    try (var syncLoop = new SyncLoop(client, 0, filter, response -> delivered.countDown(), 0, 1)) {
      syncLoop.start();
      assertThat(delivered.await(2, TimeUnit.SECONDS)).isTrue();
      assertThat(syncLoop.isRunning()).isTrue();
    }
    assertThat(requests.get(0).url()).contains("filter=%7B");
    assertThat(requests.get(1).url()).contains("since=n1");
  }

  private static HttpTransport.Response responseForFirstThenInterrupt(AtomicInteger responses) {
    if (responses.getAndIncrement() == 0) {
      return new HttpTransport.Response(200, SYNC_RESPONSE);
    }
    throw new TransportInterruptedException("done", new InterruptedException("done"));
  }

  private static final class AtomicTokenStore implements SyncTokenStore {
    private final AtomicReference<String> token;

    private AtomicTokenStore(AtomicReference<String> token) {
      this.token = token;
    }

    @Override
    public Optional<String> current() {
      return Optional.ofNullable(token.get());
    }

    @Override
    public void save(String value) {
      token.set(value);
    }

    @Override
    public void clear() {
      token.set(null);
    }
  }
}
