package io.github.fherbreteau.matrix.json;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonObjectTest {

    @Test
    void putAndGetRoundTrip() {
        var obj = new JsonObject();
        obj.put("a", JsonString.of("1"));
        obj.put("b", JsonNumber.of(2));
        assertThat(obj.get("a").asString()).isEqualTo("1");
        assertThat(obj.get("b").asDouble()).isEqualTo(2);
        assertThat(obj.has("a")).isTrue();
        assertThat(obj.has("c")).isFalse();
        assertThat(obj.names()).hasSize(2);
        assertThat(obj.toJson()).isEqualTo("{\"a\":\"1\",\"b\":2}");
    }

    @Test
    void objectPredicates() {
        JsonValue obj = new JsonObject();
        assertThat(obj.isObject()).isTrue();
        assertThat(obj.isArray()).isFalse();
        assertThat(obj.asObject()).isSameAs(obj);
    }

    @Test
    void nestedObjectSerialization() {
        var obj = new JsonObject(new java.util.LinkedHashMap<>());
        obj.put("null", JsonNull.INSTANCE);
        obj.put("bool", JsonBoolean.of(false));
        assertThat(obj.get("missing")).isNull();
        assertThat(obj.get("bool").isBoolean()).isTrue();
        assertThat(obj.get("bool").asBoolean()).isFalse();
        assertThat(obj.get("null").isNull()).isTrue();
        assertThat(obj.toJson()).isEqualTo("{\"null\":null,\"bool\":false}");
    }
}
