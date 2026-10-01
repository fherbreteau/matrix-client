package io.github.fherbreteau.matrix.transport;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

final class RetryAfterParser {

  private RetryAfterParser() {}

  static Long parse(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String stripped = value.strip();
    try {
      return Duration.ofSeconds(Long.parseLong(stripped)).toMillis();
    } catch (NumberFormatException _) {
      try {
        TemporalAccessor parsed = DateTimeFormatter.RFC_1123_DATE_TIME.parse(stripped);
        Instant retryAt = Instant.from(parsed).atOffset(ZoneOffset.UTC).toInstant();
        return Math.max(0, Duration.between(Instant.now(), retryAt).toMillis());
      } catch (DateTimeException | ArithmeticException _) {
        return null;
      }
    } catch (ArithmeticException _) {
      return null;
    }
  }
}
