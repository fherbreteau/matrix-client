package io.github.fherbreteau.matrix.json;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonParserTest {

    @Test
    void parsesObject() {
        JsonValue value = JsonParser.parse("{\"a\":1,\"b\":[true,null,\"x\"]}");
        assertTrue(value.isObject());
        assertEquals(1.0, value.asObject().get("a").asDouble());
        assertEquals("x", value.asObject().get("b").asArray().get(2).asString());
    }

    @Test
    void parsesEscapes() {
        JsonValue value = JsonParser.parse("\"line\\n\\u00e9\\\"q\\\"\"");
        assertEquals("line\né\"q\"", value.asString());
    }

    @Test
    void roundTrips() {
        var obj = new JsonObject()
                .put("name", "alice")
                .put("count", JsonNumber.of(3))
                .put("nested", new JsonObject().put("ok", JsonBoolean.of(true)));
        JsonValue reparsed = JsonParser.parse(obj.toJson());
        assertEquals("alice", reparsed.asObject().get("name").asString());
        assertTrue(reparsed.asObject().get("nested").asObject().get("ok").asBoolean());
        assertEquals("{\"name\":\"alice\",\"count\":3,\"nested\":{\"ok\":true}}", obj.toJson());
    }

    @Test
    void rejectsMalformedInput() {
        assertThrows(IllegalArgumentException.class, () -> JsonParser.parse("{"));
        assertThrows(IllegalArgumentException.class, () -> JsonParser.parse("{\"a\":}"));
        assertThrows(IllegalArgumentException.class, () -> JsonParser.parse("{} trailing"));
        assertThrows(IllegalArgumentException.class, () -> JsonParser.parse("nul"));
    }
}
