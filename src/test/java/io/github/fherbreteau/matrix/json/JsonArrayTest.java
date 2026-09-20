package io.github.fherbreteau.matrix.json;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonArrayTest {

    @Test
    void addGetAndSize() {
        var arr = new JsonArray();
        arr.add(JsonString.of("a"));
        arr.add(JsonNumber.of(1));
        assertThat(arr.size()).isEqualTo(2);
        assertThat(arr.get(0).asString()).isEqualTo("a");
        assertThat(arr.get(1).asDouble()).isEqualTo(1);
        assertThat(arr.toJson()).isEqualTo("[\"a\",1]");
    }

    @Test
    void arrayPredicates() {
        JsonValue arr = new JsonArray();
        assertThat(arr.isArray()).isTrue();
        assertThat(arr.isObject()).isFalse();
        assertThat(arr.asArray()).isSameAs(arr);
    }
}
