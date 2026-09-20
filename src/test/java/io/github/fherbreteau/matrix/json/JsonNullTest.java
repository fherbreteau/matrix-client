package io.github.fherbreteau.matrix.json;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonNullTest {

    @Test
    void singletonAndSerialization() {
        assertThat(JsonParser.parse("null")).isSameAs(JsonNull.INSTANCE);
        assertThat(JsonNull.INSTANCE.toJson()).isEqualTo("null");
        assertThat(JsonNull.INSTANCE.isNull()).isTrue();
    }
}
