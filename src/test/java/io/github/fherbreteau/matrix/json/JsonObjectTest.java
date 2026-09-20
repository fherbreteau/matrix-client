package io.github.fherbreteau.matrix.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.BOOLEAN;
import static org.assertj.core.api.InstanceOfAssertFactories.collection;

import org.junit.jupiter.api.Test;

class JsonObjectTest {

    @Test
    void putAndGetRoundTrip() {
        var obj = new JsonObject();
        obj.put("a", JsonString.of("1"));
        obj.put("b", JsonNumber.of(2));
        obj.put("long", 3L);
        obj.put("double", 2.5);
        obj.put("flag", false);
        assertThat(obj).extracting(x -> x.get("a")).extracting(JsonValue::asString).isEqualTo("1");
        assertThat(obj).extracting(x -> x.get("b")).extracting(JsonValue::asDouble).isEqualTo(2.0);
        assertThat(obj).extracting(x -> x.get("long")).extracting(JsonValue::asLong).isEqualTo(3L);
        assertThat(obj).extracting(x -> x.get("double")).extracting(JsonValue::asDouble).isEqualTo(2.5);
        assertThat(obj).extracting(x -> x.get("flag")).extracting(JsonValue::asBoolean, BOOLEAN).isFalse();
        assertThat(obj).extracting(x -> x.has("a"), BOOLEAN).isTrue();
        assertThat(obj).extracting(x -> x.has("c"), BOOLEAN).isFalse();
        assertThat(obj).extracting(JsonObject::names, collection(String.class)).hasSize(5);
        assertThat(obj).extracting(JsonValue::toJson).isEqualTo("{\"a\":\"1\",\"b\":2,\"long\":3,\"double\":2.5,\"flag\":false}");
    }

    @Test
    void objectPredicates() {
        JsonValue obj = new JsonObject();
        assertThat(obj).extracting(JsonValue::isObject, BOOLEAN).isTrue();
        assertThat(obj).extracting(JsonValue::isArray, BOOLEAN).isFalse();
        assertThat(obj).extracting(JsonValue::asObject).isSameAs(obj);
    }

    @Test
    void nestedObjectSerialization() {
        var obj = new JsonObject(new java.util.LinkedHashMap<>());
        obj.put("null", JsonNull.INSTANCE);
        obj.put("bool", JsonBoolean.of(false));
        assertThat(obj).extracting(x -> x.get("missing")).isNull();
        assertThat(obj).extracting(x -> x.get("bool")).extracting(JsonValue::isBoolean, BOOLEAN).isTrue();
        assertThat(obj).extracting(x -> x.get("bool")).extracting(JsonValue::asBoolean, BOOLEAN).isFalse();
        assertThat(obj).extracting(x -> x.get("null")).extracting(JsonValue::isNull, BOOLEAN).isTrue();
        assertThat(obj).extracting(JsonValue::toJson).isEqualTo("{\"null\":null,\"bool\":false}");
    }
}
