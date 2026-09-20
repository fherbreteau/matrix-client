package io.github.fherbreteau.matrix.json;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class JsonValueTest {

    @Test
    void nullValuePredicates() {
        JsonValue value = JsonNull.INSTANCE;
        assertThat(value.isNull()).isTrue();
        assertThat(value.isObject()).isFalse();
        assertThat(value.isArray()).isFalse();
        assertThat(value.isString()).isFalse();
        assertThat(value.isNumber()).isFalse();
        assertThat(value.isBoolean()).isFalse();
    }

    @Test
    void nonNullValuePredicates() {
        JsonValue value = JsonBoolean.of(true);
        assertThat(value.isBoolean()).isTrue();
        assertThat(value.isObject()).isFalse();
        assertThat(value.isArray()).isFalse();
        assertThat(value.isString()).isFalse();
        assertThat(value.isNumber()).isFalse();
        assertThat(value.isNull()).isFalse();
    }

    @Test
    void defaultAccessorsThrow() {
        JsonValue value = JsonNull.INSTANCE;
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(value::asObject);
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(value::asArray);
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(value::asString);
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(value::asDouble);
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(value::asBoolean);
    }
}
