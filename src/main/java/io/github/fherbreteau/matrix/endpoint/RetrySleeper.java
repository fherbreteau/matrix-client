package io.github.fherbreteau.matrix.endpoint;

import java.time.Duration;

@FunctionalInterface
interface RetrySleeper {

  void sleep(Duration delay) throws InterruptedException;

  static RetrySleeper threadSleeper() {
    return delay -> {
      long millis = delay.toMillis();
      int nanos = (int) delay.minusMillis(millis).toNanos();
      Thread.sleep(millis, nanos);
    };
  }
}
