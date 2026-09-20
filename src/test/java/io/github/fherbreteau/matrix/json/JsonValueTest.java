package io.github.fherbreteau.matrix.json;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.InstanceOfAssertFactories.BOOLEAN;

class JsonValueTest {

    @Test
    void nullValuePredicates() {
        JsonValue value = JsonNull.INSTANCE;
        assertThat(value).extracting(JsonValue::isNull, BOOLEAN).isTrue();
        assertThat(value).extracting(JsonValue::isObject, BOOLEAN).isFalse();
        assertThat(value).extracting(JsonValue::isArray, BOOLEAN).isFalse();
        assertThat(value).extracting(JsonValue::isString, BOOLEAN).isFalse();
        assertThat(value).extracting(JsonValue::isNumber, BOOLEAN).isFalse();
        assertThat(value).extracting(JsonValue::isBoolean, BOOLEAN).isFalse();
    }

    @Test
    void nonNullValuePredicates() {
        JsonValue value = JsonBoolean.of(true);
        assertThat(value).extracting(JsonValue::isBoolean, BOOLEAN).isTrue();
        assertThat(value).extracting(JsonValue::isObject, BOOLEAN).isFalse();
        assertThat(value).extracting(JsonValue::isArray, BOOLEAN).isFalse();
        assertThat(value).extracting(JsonValue::isString, BOOLEAN).isFalse();
        assertThat(value).extracting(JsonValue::isNumber, BOOLEAN).isFalse();
        assertThat(value).extracting(JsonValue::isNull, BOOLEAN).isFalse();
    }

    @Test
    void defaultAccessorsThrow() {
        JsonValue value = JsonNull.INSTANCE;
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(value::asObject);
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(value::asArray);
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(value::asString);
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(value::asDouble);
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(value::asLong);
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(value::asBigDecimal);
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(value::asBoolean);
    }
}
