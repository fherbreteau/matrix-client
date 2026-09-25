package io.github.fherbreteau.matrix.endpoint;

import io.github.fherbreteau.matrix.error.MatrixServerException;
import io.github.fherbreteau.matrix.error.RateLimitedException;
import io.github.fherbreteau.matrix.model.MatrixFilter;
import io.github.fherbreteau.matrix.model.SyncResponse;
import io.github.fherbreteau.matrix.transport.TransportInterruptedException;
import io.github.fherbreteau.matrix.transport.TransportTimeoutException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Optional cancellable long-poll sync loop. A successful response is delivered to the listener; the
 * client's configured token store persists its {@code next_batch} before the next iteration. Retry
 * delays are configurable and rate-limit responses honour their {@code Retry-After} delay.
 *
 * <p>The loop runs on a dedicated daemon thread. Call {@link #close()} to cancel it and interrupt
 * any in-flight long poll.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#syncing">Matrix specification</a>
 */
public final class SyncLoop implements AutoCloseable {

  private final MatrixClient client;
  private final long timeoutMs;
  private final MatrixFilter filter;
  private final Consumer<SyncResponse> listener;
  private final long initialRetryDelayMs;
  private final long maxRetryDelayMs;
  private final AtomicBoolean running = new AtomicBoolean();
  private final AtomicBoolean started = new AtomicBoolean();
  private final AtomicBoolean closed = new AtomicBoolean();
  private final AtomicReference<Thread> worker = new AtomicReference<>();
  private long retryDelayMs;

  /**
   * Creates a sync loop with exponential retry from 500 ms to 30 seconds.
   *
   * @param client authenticated client to sync with
   * @param timeoutMs long-poll timeout in milliseconds
   * @param filter optional typed sync filter
   * @param listener called after each successfully parsed response
   */
  public SyncLoop(
      MatrixClient client, long timeoutMs, MatrixFilter filter, Consumer<SyncResponse> listener) {
    this(client, timeoutMs, filter, listener, 500, 30_000);
  }

  /**
   * Creates a sync loop with configurable retry delays.
   *
   * @param client authenticated client to sync with
   * @param timeoutMs long-poll timeout in milliseconds
   * @param filter optional typed sync filter
   * @param listener called after each successfully parsed response
   * @param initialRetryDelayMs delay before the first retryable failure retry
   * @param maxRetryDelayMs maximum exponential retry delay
   */
  public SyncLoop(
      MatrixClient client,
      long timeoutMs,
      MatrixFilter filter,
      Consumer<SyncResponse> listener,
      long initialRetryDelayMs,
      long maxRetryDelayMs) {
    this.client = Objects.requireNonNull(client, "client");
    this.timeoutMs = timeoutMs;
    this.filter = filter;
    this.listener = Objects.requireNonNull(listener, "listener");
    if (initialRetryDelayMs < 0 || maxRetryDelayMs < initialRetryDelayMs) {
      throw new IllegalArgumentException("retry delays must satisfy 0 <= initial <= max");
    }
    this.initialRetryDelayMs = initialRetryDelayMs;
    this.maxRetryDelayMs = maxRetryDelayMs;
  }

  /**
   * Starts the loop if it has not already been started.
   *
   * @return this loop
   * @throws IllegalStateException if this loop was already started or closed
   */
  public synchronized SyncLoop start() {
    if (closed.get() || !started.compareAndSet(false, true)) {
      throw new IllegalStateException("sync loop already started or closed");
    }
    running.set(true);
    Thread thread = new Thread(this::run, "matrix-sync-loop");
    thread.setDaemon(true);
    worker.set(thread);
    thread.start();
    return this;
  }

  /**
   * Returns whether the loop is currently running.
   *
   * @return whether the loop is running
   */
  public boolean isRunning() {
    return running.get();
  }

  private void run() {
    try {
      runLoop();
    } finally {
      running.set(false);
    }
  }

  private void runLoop() {
    retryDelayMs = initialRetryDelayMs;
    while (running.get()) {
      try {
        processSyncResponse();
      } catch (TransportInterruptedException _) {
        Thread.currentThread().interrupt();
        running.set(false);
      } catch (TransportTimeoutException _) {
        retryAfter(retryDelayMs);
      } catch (MatrixServerException exception) {
        handleServerException(exception);
      } catch (RuntimeException exception) {
        running.set(false);
        throw exception;
      }
    }
  }

  private void processSyncResponse() {
    SyncResponse response = client.syncWithFilter(timeoutMs, filter);
    listener.accept(response);
    retryDelayMs = initialRetryDelayMs;
  }

  private void handleServerException(MatrixServerException exception) {
    if (!isRetryableSyncFailure(exception)) {
      running.set(false);
      return;
    }
    retryAfterDelay(exception);
  }

  private void retryAfterDelay(MatrixServerException exception) {
    long delay = retryDelayMs;
    if (exception instanceof RateLimitedException rateLimited
        && rateLimited.getRetryAfterMs() != null) {
      delay = Math.max(0, rateLimited.getRetryAfterMs());
    }
    retryAfter(delay);
  }

  private void retryAfter(long delay) {
    if (pause(delay)) {
      retryDelayMs = nextDelay(retryDelayMs);
    } else {
      running.set(false);
    }
  }

  private static boolean isRetryableSyncFailure(MatrixServerException exception) {
    int status = exception.getStatusCode();
    return status == 408 || status == 429 || status >= 500;
  }

  private long nextDelay(long current) {
    return current == 0 ? Math.min(1, maxRetryDelayMs) : Math.min(current * 2, maxRetryDelayMs);
  }

  private boolean pause(long delayMs) {
    try {
      Thread.sleep(delayMs);
      return running.get();
    } catch (InterruptedException _) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  boolean awaitTermination(Duration timeout) throws InterruptedException {
    Thread thread = worker.get();
    if (thread == null) {
      return false;
    }
    thread.join(timeout.toMillis());
    return !thread.isAlive();
  }

  /** Stops the loop and interrupts an in-flight sync request or backoff wait. */
  @Override
  public synchronized void close() {
    closed.set(true);
    running.set(false);
    Thread thread = worker.get();
    if (thread != null) {
      thread.interrupt();
    }
  }
}
