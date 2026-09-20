package io.github.fherbreteau.matrix.json;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonBooleanTest {

    @Test
    void singletons() {
        assertThat(JsonBoolean.of(true)).isSameAs(JsonBoolean.TRUE);
        assertThat(JsonBoolean.of(false)).isSameAs(JsonBoolean.FALSE);
    }

    @Test
    void predicatesAndSerialization() {
        assertThat(JsonBoolean.TRUE.isBoolean()).isTrue();
        assertThat(JsonBoolean.TRUE.asBoolean()).isTrue();
        assertThat(JsonBoolean.TRUE.toJson()).isEqualTo("true");
        assertThat(JsonBoolean.FALSE.toJson()).isEqualTo("false");
    }
}
