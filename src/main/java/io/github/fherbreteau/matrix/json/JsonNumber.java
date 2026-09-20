package io.github.fherbreteau.matrix.json;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A JSON number. Integral values are preserved exactly; the backing {@link BigDecimal} keeps full
 * precision for arbitrary-size values.
 */
public final class JsonNumber implements JsonValue {

  private final BigDecimal value;

  private JsonNumber(BigDecimal value) {
    this.value = value;
  }

  /** Creates a JSON number from a long. */
  public static JsonNumber of(long value) {
    return new JsonNumber(BigDecimal.valueOf(value));
  }

  /** Creates a JSON number from a double. */
  public static JsonNumber of(double value) {
    return new JsonNumber(BigDecimal.valueOf(value));
  }

  /** Creates a JSON number from an arbitrary-precision decimal. */
  public static JsonNumber of(BigDecimal value) {
    return new JsonNumber(Objects.requireNonNull(value, "value"));
  }

  @Override
  public boolean isNumber() {
    return true;
  }

  @Override
  public double asDouble() {
    return value.doubleValue();
  }

  /** Returns this number as a {@code long}, truncating fractional values. */
  public long asLong() {
    return value.longValue();
  }

  /** Returns this number as a {@link BigDecimal} preserving full precision. */
  @Override
  public BigDecimal asBigDecimal() {
    return value;
  }

  public boolean isIntegral() {
    return value.scale() <= 0 || value.stripTrailingZeros().scale() <= 0;
  }

  @Override
  public String toJson() {
    var normalized = value.stripTrailingZeros();
    if (normalized.scale() > 0 || normalized.scale() < -40) {
      return normalized.toString();
    }
    return normalized.toPlainString();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof JsonNumber other)) {
      return false;
    }
    return value.compareTo(other.value) == 0;
  }

  @Override
  public int hashCode() {
    return value.stripTrailingZeros().hashCode();
  }

  @Override
  public String toString() {
    return "JsonNumber[" + toJson() + "]";
  }
}
