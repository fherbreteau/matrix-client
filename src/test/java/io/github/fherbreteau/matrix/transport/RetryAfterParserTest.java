package io.github.fherbreteau.matrix.transport;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;

class RetryAfterParserTest {

  @Test
  void parsesDeltaSeconds() {
    assertThat(RetryAfterParser.parse("7")).isEqualTo(7000L);
  }

  @Test
  void parsesFutureHttpDate() {
    String retryAt =
        DateTimeFormatter.RFC_1123_DATE_TIME.format(
            Instant.now().plusSeconds(30).atZone(ZoneOffset.UTC));
    assertThat(RetryAfterParser.parse(retryAt)).isBetween(0L, 30_000L);
  }

  @Test
  void returnsZeroForPastHttpDate() {
    String retryAt =
        DateTimeFormatter.RFC_1123_DATE_TIME.format(
            Instant.now().minusSeconds(30).atZone(ZoneOffset.UTC));
    assertThat(RetryAfterParser.parse(retryAt)).isZero();
  }

  @Test
  void rejectsBlankMalformedAndOverflowValues() {
    assertThat(RetryAfterParser.parse(null)).isNull();
    assertThat(RetryAfterParser.parse(" ")).isNull();
    assertThat(RetryAfterParser.parse("not a date")).isNull();
    assertThat(RetryAfterParser.parse("9223372036854775807")).isNull();
  }
}
