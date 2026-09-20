package io.github.fherbreteau.matrix.json;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class JsonParserTest {

    @Test
    void parsesObject() {
        JsonValue value = JsonParser.parse("{\"a\":1,\"b\":[true,null,\"x\"]}");
        assertThat(value.isObject()).isTrue();
        assertThat(value.asObject().get("a").asDouble()).isEqualTo(1.0);
        assertThat(value.asObject().get("b").asArray().get(2).asString()).isEqualTo("x");
    }

    @Test
    void parsesEscapes() {
        JsonValue value = JsonParser.parse("\"line\\n\\u00e9\\\"q\\\"\"");
        assertThat(value.asString()).isEqualTo("line\né\"q\"");
    }

    @Test
    void roundTrips() {
        var obj = new JsonObject()
                .put("name", "alice")
                .put("count", JsonNumber.of(3))
                .put("nested", new JsonObject().put("ok", JsonBoolean.of(true)));
        JsonValue reparsed = JsonParser.parse(obj.toJson());
        assertThat(reparsed.asObject().get("name").asString()).isEqualTo("alice");
        assertThat(reparsed.asObject().get("nested").asObject().get("ok").asBoolean()).isTrue();
        assertThat(obj.toJson()).isEqualTo("{\"name\":\"alice\",\"count\":3,\"nested\":{\"ok\":true}}");
    }

    @Test
    void rejectsMalformedInput() {
        String[] inputs = {
                "{",
                "{\"a\":}",
                "{} trailing",
                "nul",
                "",
                "[",
                "[1,]",
                "{\"a\"}",
                "{\"a\":1",
                "[1",
                "\"unterminated",
                "\"bad\\x\"",
                "\"bad\\",
                "\"bad\\uZZZZ\"",
                "\"bad\\u0",
                "tru",
                "-",
                "01x",
                "{\"a\":1,",
                "{\"a\" 1}"
        };
        for (String input : inputs) {
            assertThatIllegalArgumentException().as("input: %s", input).isThrownBy(() -> JsonParser.parse(input));
        }
    }
}
