package io.github.fherbreteau.matrix.json;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.BOOLEAN;

class JsonArrayTest {

    @Test
    void addGetAndSize() {
        var arr = new JsonArray();
        arr.add(JsonString.of("a"));
        arr.add(JsonNumber.of(1));
        assertThat(arr).extracting(JsonArray::size).isEqualTo(2);
        assertThat(arr).extracting(x -> x.get(0)).extracting(JsonValue::asString).isEqualTo("a");
        assertThat(arr).extracting(x -> x.get(1)).extracting(JsonValue::asDouble).isEqualTo(1.0);
        assertThat(arr).extracting(JsonValue::toJson).isEqualTo("[\"a\",1]");
    }

    @Test
    void arrayPredicates() {
        JsonValue arr = new JsonArray();
        assertThat(arr).extracting(JsonValue::isArray, BOOLEAN).isTrue();
        assertThat(arr).extracting(JsonValue::isObject, BOOLEAN).isFalse();
        assertThat(arr).extracting(JsonValue::asArray).isSameAs(arr);
    }
}
