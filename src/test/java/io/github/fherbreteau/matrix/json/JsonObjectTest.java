package io.github.fherbreteau.matrix.json;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonObjectTest {

    @Test
    void putAndGetRoundTrip() {
        var obj = new JsonObject();
        obj.put("a", JsonString.of("1"));
        obj.put("b", JsonNumber.of(2));
        obj.put("long", 3L);
        obj.put("double", 2.5);
        obj.put("flag", false);
        assertThat(obj.get("a").asString()).isEqualTo("1");
        assertThat(obj.get("b").asDouble()).isEqualTo(2);
        assertThat(obj.get("long").asLong()).isEqualTo(3L);
        assertThat(obj.get("double").asDouble()).isEqualTo(2.5);
        assertThat(obj.get("flag").asBoolean()).isFalse();
        assertThat(obj.has("a")).isTrue();
        assertThat(obj.has("c")).isFalse();
        assertThat(obj.names()).hasSize(5);
        assertThat(obj.toJson()).isEqualTo("{\"a\":\"1\",\"b\":2,\"long\":3,\"double\":2.5,\"flag\":false}");
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
