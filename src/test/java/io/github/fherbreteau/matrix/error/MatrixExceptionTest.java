package io.github.fherbreteau.matrix.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.BOOLEAN;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import io.github.fherbreteau.matrix.json.JsonParser;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MatrixExceptionTest {

  @Test
  void carriesErrcodeAndMessage() {
    MatrixException exception = new MatrixException("M_LIMIT_EXCEEDED", "Too many requests");
    assertThat(exception).extracting(MatrixException::getErrcode).isEqualTo("M_LIMIT_EXCEEDED");
    assertThat(exception).extracting(MatrixException::getMessage).isEqualTo("Too many requests");
  }

  @Test
  void carriesCause() {
    var cause = new RuntimeException("root");
    MatrixException exception = new MatrixException("M_UNKNOWN", "failed", cause);
    assertThat(exception).extracting(MatrixException::getCause).isSameAs(cause);
  }

  @Test
  void parsesMatrixErrorBody() {
    var exception =
        MatrixServerException.fromResponse(
            429, JsonParser.parse("{\"errcode\":\"M_LIMIT_EXCEEDED\",\"error\":\"Rate limited\"}"));
    assertThat(exception).isInstanceOf(RateLimitedException.class);
    assertThat(exception).extracting(MatrixServerException::getStatusCode).isEqualTo(429);
    assertThat(exception)
        .extracting(MatrixServerException::getErrcode)
        .isEqualTo("M_LIMIT_EXCEEDED");
    assertThat(exception).extracting(MatrixServerException::getMessage).isEqualTo("Rate limited");
    assertThat(exception).extracting(MatrixServerException::isRetryable, BOOLEAN).isTrue();
  }

  @Test
  void fallsBackOnNonJsonBody() {
    var exception = MatrixServerException.fromResponse(502, null);
    assertThat(exception).extracting(MatrixServerException::getStatusCode).isEqualTo(502);
    assertThat(exception).extracting(MatrixServerException::getErrcode).isEqualTo("M_UNRECOGNIZED");
    assertThat(exception).extracting(MatrixServerException::getMessage).isEqualTo("HTTP 502");
    assertThat(exception).extracting(MatrixServerException::isRetryable, BOOLEAN).isTrue();
  }

  @Test
  void fallsBackOnNonObjectBody() {
    var exception = MatrixServerException.fromResponse(500, JsonParser.parse("[]"));
    assertThat(exception).extracting(MatrixServerException::getErrcode).isEqualTo("M_UNRECOGNIZED");
    assertThat(exception).extracting(MatrixServerException::getMessage).isEqualTo("HTTP 500");
  }

  @Test
  void fallsBackOnPartialBody() {
    var exception =
        MatrixServerException.fromResponse(400, JsonParser.parse("{\"error\":\"bad\"}"));
    assertThat(exception).extracting(MatrixServerException::getErrcode).isEqualTo("M_UNRECOGNIZED");
    assertThat(exception).extracting(MatrixServerException::getMessage).isEqualTo("bad");
  }

  @Test
  void baseExceptionWithoutErrcode() {
    MatrixException exception = new MatrixServerException(400, null, "oops");
    assertThat(exception).extracting(MatrixException::getErrcode).isNull();
    assertThat(exception).extracting(MatrixException::getMessage).isEqualTo("oops");
  }

  @Test
  void retainsUnknownFields() {
    var exception =
        MatrixServerException.fromResponse(
            400,
            JsonParser.parse(
                """
                {"errcode":"M_INVALID_PARAM","error":"bad param","retry_after_ms":2000,"soft_fail":true}
                """));
    assertThat(exception.getFields())
        .containsEntry("retry_after_ms", JsonParser.parse("2000"))
        .containsEntry("soft_fail", JsonParser.parse("true"))
        .doesNotContainKeys("errcode", "error");
  }

  @Test
  void retryableStatuses() {
    for (int status : new int[] {408, 429, 500, 502, 503, 504}) {
      assertThat(MatrixServerException.fromResponse(status, null))
          .extracting(MatrixServerException::isRetryable, BOOLEAN)
          .as("status %d", status)
          .isTrue();
    }
    for (int status : new int[] {400, 401, 403, 404, 405, 501}) {
      assertThat(MatrixServerException.fromResponse(status, null))
          .extracting(MatrixServerException::isRetryable, BOOLEAN)
          .as("status %d", status)
          .isFalse();
    }
  }

  @Test
  void rateLimitedCarriesRetryAfter() {
    var exception =
        MatrixServerException.fromResponse(
            429,
            JsonParser.parse("{\"errcode\":\"M_LIMIT_EXCEEDED\",\"error\":\"Too many\"}"),
            Map.of("retry-after", "12"));
    assertThat(exception)
        .isInstanceOf(RateLimitedException.class)
        .asInstanceOf(type(RateLimitedException.class))
        .extracting(
            RateLimitedException::getRetryAfterMs,
            RateLimitedException::isRetryable,
            RateLimitedException::getStatusCode)
        .containsExactly(12000L, true, 429);
  }

  @Test
  void rateLimitedWithoutRetryAfter() {
    var exception = MatrixServerException.fromResponse(429, JsonParser.parse("{}"), Map.of());
    assertThat(exception)
        .isInstanceOf(RateLimitedException.class)
        .asInstanceOf(type(RateLimitedException.class))
        .extracting(RateLimitedException::getRetryAfterMs)
        .isNull();
  }

  @Test
  void rateLimitedWithNonNumericRetryAfter() {
    var exception =
        MatrixServerException.fromResponse(
            429, JsonParser.parse("{}"), Map.of("retry-after", "Wed, 21 Oct 2026 07:28:00 GMT"));
    assertThat(exception)
        .isInstanceOf(RateLimitedException.class)
        .asInstanceOf(type(RateLimitedException.class))
        .extracting(RateLimitedException::getRetryAfterMs)
        .isNull();
  }

  @Test
  void fourHundredAndTwentyNineWithoutHeaders() {
    var exception = MatrixServerException.fromResponse(429, JsonParser.parse("{}"));
    assertThat(exception)
        .isInstanceOf(RateLimitedException.class)
        .asInstanceOf(type(RateLimitedException.class))
        .extracting(RateLimitedException::getRetryAfterMs)
        .isNull();
  }

  @Test
  void constructorComputesRetryability() {
    var retryable = new MatrixServerException(503, "M_UNAVAILABLE", "down");
    var nonRetryable = new MatrixServerException(404, "M_NOT_FOUND", "gone");
    var explicit =
        new MatrixServerException(
            200, "M_CUSTOM", "weird", Map.of("extra", JsonParser.parse("1")), true);
    assertThat(retryable).extracting(MatrixServerException::isRetryable, BOOLEAN).isTrue();
    assertThat(nonRetryable).extracting(MatrixServerException::isRetryable, BOOLEAN).isFalse();
    assertThat(explicit).extracting(MatrixServerException::isRetryable, BOOLEAN).isTrue();
    assertThat(explicit)
        .extracting(MatrixServerException::getFields, map(String.class, JsonValue.class))
        .containsKey("extra");
    assertThat(explicit).extracting(MatrixServerException::getStatusCode).isEqualTo(200);
  }
}
