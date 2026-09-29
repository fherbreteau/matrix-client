package io.github.fherbreteau.matrix.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.fherbreteau.matrix.error.MatrixServerException;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.model.RequestAttempt;
import io.github.fherbreteau.matrix.model.RetryPolicy;
import io.github.fherbreteau.matrix.transport.HttpTransport;
import io.github.fherbreteau.matrix.transport.TransportInterruptedException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class RequestReliabilityTest {

  @Test
  void retriesIdempotentReadWithBoundedBackoffAndStableCorrelation() {
    var attempts = new AtomicInteger();
    var delays = new ArrayList<Duration>();
    var events = new ArrayList<RequestAttempt>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .retryPolicy(new RetryPolicy(2, 10, 40, 100))
            .requestObserver(events::add)
            .retrySleeper(delays::add)
            .transport(
                request -> {
                  if (attempts.getAndIncrement() < 2) {
                    return new HttpTransport.Response(
                        503, "{\"errcode\":\"M_UNAVAILABLE\",\"error\":\"later\"}");
                  }
                  return new HttpTransport.Response(200, "{}");
                })
            .build();

    client.get("_matrix/client/v3/profile/%40alice:example.org?access_token=hidden");

    assertThat(attempts.get()).isEqualTo(3);
    assertThat(delays).containsExactly(Duration.ofMillis(10), Duration.ofMillis(20));
    assertThat(events).extracting(RequestAttempt::number).containsExactly(1, 2, 3);
    assertThat(events)
        .extracting(RequestAttempt::outcome)
        .containsExactly(
            RequestAttempt.Outcome.RETRYING,
            RequestAttempt.Outcome.RETRYING,
            RequestAttempt.Outcome.SUCCEEDED);
    assertThat(events)
        .extracting(RequestAttempt::correlationId)
        .containsOnly(events.getFirst().correlationId());
    assertThat(events)
        .extracting(RequestAttempt::endpoint)
        .containsOnly("/_matrix/client/v3/profile/*");
  }

  @Test
  void observerRedactsMediaServerAndMediaIdentifiers() {
    var events = new ArrayList<RequestAttempt>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .retryPolicy(RetryPolicy.disabled())
            .requestObserver(events::add)
            .transport(request -> new HttpTransport.Response(200, "{}"))
            .build();
    client.get("_matrix/client/v1/media/download/private-server/private-media-id");
    assertThat(events.getFirst().endpoint()).isEqualTo("/_matrix/client/v1/media/download/*/*");
  }

  @Test
  void doesNotRetryPostWithoutPerCallOptIn() {
    var attempts = new AtomicInteger();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .retryPolicy(new RetryPolicy(2, 0, 0, 100))
            .transport(
                request -> {
                  attempts.incrementAndGet();
                  return new HttpTransport.Response(
                      503, "{\"errcode\":\"M_UNAVAILABLE\",\"error\":\"later\"}");
                })
            .build();

    assertThatThrownBy(() -> client.post("_matrix/client/v3/custom", new JsonObject()))
        .isInstanceOf(MatrixServerException.class);
    assertThat(attempts.get()).isOne();
  }

  @Test
  void retriesPostOnlyWhenCallerDeclaresIdempotency() {
    var attempts = new AtomicInteger();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .retryPolicy(new RetryPolicy(1, 0, 0, 100))
            .retrySleeper(_ -> {})
            .transport(
                request -> {
                  if (attempts.getAndIncrement() == 0) {
                    return new HttpTransport.Response(
                        503, "{\"errcode\":\"M_UNAVAILABLE\",\"error\":\"later\"}");
                  }
                  return new HttpTransport.Response(200, "{}");
                })
            .build();

    client.request("POST", "_matrix/client/v3/custom", new JsonObject(), true);

    assertThat(attempts.get()).isEqualTo(2);
  }

  @Test
  void rateLimitUsesRetryAfterAndCapsTheDelay() {
    var attempts = new AtomicInteger();
    var delays = new ArrayList<Duration>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .retryPolicy(new RetryPolicy(1, 10, 10, 50))
            .retrySleeper(delays::add)
            .transport(
                request -> {
                  if (attempts.getAndIncrement() == 0) {
                    return new HttpTransport.Response(
                        429,
                        Map.of("retry-after", "100"),
                        "{\"errcode\":\"M_LIMIT_EXCEEDED\",\"error\":\"slow down\"}",
                        100L);
                  }
                  return new HttpTransport.Response(200, "{}");
                })
            .build();

    client.get("_matrix/client/v3/versions");

    assertThat(delays).containsExactly(Duration.ofMillis(50));
    assertThat(attempts.get()).isEqualTo(2);
  }

  @Test
  void interruptionStopsRetryAndRestoresInterruptFlag() throws Exception {
    var retryScheduled = new CountDownLatch(1);
    var attempts = new AtomicInteger();
    var failure = new AtomicReference<Throwable>();
    var interrupted = new AtomicReference<Boolean>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .retryPolicy(new RetryPolicy(1, 1_000, 1_000, 1_000))
            .requestObserver(
                event -> {
                  if (event.outcome() == RequestAttempt.Outcome.RETRYING) {
                    retryScheduled.countDown();
                  }
                })
            .transport(
                request -> {
                  attempts.incrementAndGet();
                  return new HttpTransport.Response(
                      503, "{\"errcode\":\"M_UNAVAILABLE\",\"error\":\"later\"}");
                })
            .build();
    Thread thread =
        new Thread(
            () -> {
              try {
                client.get("_matrix/client/v3/versions");
              } catch (Throwable exception) {
                failure.set(exception);
                interrupted.set(Thread.currentThread().isInterrupted());
              }
            });

    thread.start();
    assertThat(retryScheduled.await(2, TimeUnit.SECONDS)).isTrue();
    thread.interrupt();
    thread.join(2_000);

    assertThat(thread.isAlive()).isFalse();
    assertThat(attempts.get()).isOne();
    assertThat(failure.get()).isInstanceOf(TransportInterruptedException.class);
    assertThat(interrupted).hasValue(true);
  }

  @Test
  void genericRequestUsesDefaultReplaySafety() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .retryPolicy(RetryPolicy.disabled())
            .transport(request -> new HttpTransport.Response(200, "{}"))
            .build();
    assertThat(client.request("GET", "_matrix/client/v3/versions", null).isObject()).isTrue();
  }

  @Test
  void observerFailureIsPropagated() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .retryPolicy(RetryPolicy.disabled())
            .requestObserver(
                event -> {
                  throw new IllegalStateException("observer failed");
                })
            .transport(request -> new HttpTransport.Response(200, "{}"))
            .build();

    assertThatThrownBy(() -> client.get("_matrix/client/v3/versions"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("observer failed");
  }
}
