package io.github.fherbreteau.matrix.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.BOOLEAN;

import org.junit.jupiter.api.Test;

class JsonBooleanTest {

    @Test
    void singletons() {
        assertThat(JsonBoolean.of(true)).isSameAs(JsonBoolean.TRUE);
        assertThat(JsonBoolean.of(false)).isSameAs(JsonBoolean.FALSE);
    }

    @Test
    void predicatesAndSerialization() {
        assertThat(JsonBoolean.TRUE).extracting(JsonValue::isBoolean, BOOLEAN).isTrue();
        assertThat(JsonBoolean.TRUE).extracting(JsonValue::asBoolean, BOOLEAN).isTrue();
        assertThat(JsonBoolean.TRUE).extracting(JsonValue::toJson).isEqualTo("true");
        assertThat(JsonBoolean.FALSE).extracting(JsonValue::toJson).isEqualTo("false");
    }
}
