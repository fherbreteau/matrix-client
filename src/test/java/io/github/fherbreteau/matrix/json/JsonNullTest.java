package io.github.fherbreteau.matrix.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.BOOLEAN;

import org.junit.jupiter.api.Test;

class JsonNullTest {

  @Test
  void singletonAndSerialization() {
    assertThat(JsonParser.parse("null")).isSameAs(JsonNull.INSTANCE);
    assertThat(JsonNull.INSTANCE).extracting(JsonValue::toJson).isEqualTo("null");
    assertThat(JsonNull.INSTANCE).extracting(JsonValue::isNull, BOOLEAN).isTrue();
  }
}
