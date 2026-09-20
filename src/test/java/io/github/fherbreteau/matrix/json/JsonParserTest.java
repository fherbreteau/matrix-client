package io.github.fherbreteau.matrix.json;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void parsesSurrogatePairs() {
        assertThat(JsonParser.parse("\"\\uD83D\\uDE00\"").asString()).isEqualTo("\uD83D\uDE00");
        assertThat(JsonParser.parse("\"hi \\uD83D\\uDE00!\"").asString()).isEqualTo("hi \uD83D\uDE00!");
        assertThat(JsonParser.parse("\"\\u00E9\"").asString()).isEqualTo("é");
    }

    @Test
    void rejectsUnpairedSurrogates() {
        assertThatThrownBy(() -> JsonParser.parse("\"\\uD83D\""))
                .isInstanceOf(JsonParseException.class)
                .hasMessageContaining("Unpaired high surrogate");
        assertThatThrownBy(() -> JsonParser.parse("\"\\uDE00\""))
                .isInstanceOf(JsonParseException.class)
                .hasMessageContaining("Unpaired low surrogate");
        assertThatThrownBy(() -> JsonParser.parse("\"\\uD83Dx\""))
                .isInstanceOf(JsonParseException.class)
                .hasMessageContaining("Unpaired high surrogate");
        assertThatThrownBy(() -> JsonParser.parse("\"\\uD83D\\u0041\""))
                .isInstanceOf(JsonParseException.class)
                .hasMessageContaining("Unpaired high surrogate");
    }

    @Test
    void parsesStrictNumbers() {
        assertThat(JsonParser.parse("42").toJson()).isEqualTo("42");
        assertThat(JsonParser.parse("-7").toJson()).isEqualTo("-7");
        assertThat(JsonParser.parse("1.5").asDouble()).isEqualTo(1.5);
        assertThat(JsonParser.parse("1e3").asDouble()).isEqualTo(1000.0);
        assertThat(JsonParser.parse("2.5E-3").asDouble()).isEqualTo(0.0025);
        assertThat(JsonParser.parse("-1.25e+2").asDouble()).isEqualTo(-125.0);
        assertThat(JsonParser.parse("0.10").toJson()).isEqualTo("0.1");
        assertThat(JsonParser.parse("-0").toJson()).isEqualTo("0");
    }

    @Test
    void preservesLargeIntegralValues() {
        JsonValue value = JsonParser.parse("123456789012345678901234567890");
        var number = (JsonNumber) value;
        assertThat(number.isNumber()).isTrue();
        assertThat(number.asBigDecimal()).isEqualTo(new BigDecimal("123456789012345678901234567890"));
        assertThat(number.toJson()).isEqualTo("123456789012345678901234567890");
    }

    @Test
    void rejectsMalformedNumbers() {
        String[] inputs = {
                "01",
                "1.",
                ".5",
                "+1",
                "1e",
                "1e+",
                "1.2.3",
                "1e3e4",
                "0x1f",
                "1-2",
                "Infinity",
                "NaN",
                "- 1",
                "1.5e",
                "--1"
        };
        for (String input : inputs) {
            assertThatIllegalArgumentException().as("input: %s", input).isThrownBy(() -> JsonParser.parse(input));
        }
    }

    @Test
    void reportsPositionOfError() {
        assertThatThrownBy(() -> JsonParser.parse("{\"a\": tru }"))
                .isInstanceOf(JsonParseException.class)
                .hasMessageContaining("position 6")
                .satisfies(e -> assertThat(((JsonParseException) e).getPosition()).isEqualTo(6));
    }

    @Test
    void rejectsNullInput() {
        assertThatThrownBy(() -> JsonParser.parse(null))
                .isInstanceOf(JsonParseException.class)
                .hasMessageContaining("null");
    }

    @Test
    void acceptsAllWhitespaceVariants() {
        JsonValue value = JsonParser.parse(" \t\r\n{ \"a\" : [ 1 , 2 ] }\t\n ");
        assertThat(value.asObject().get("a").asArray().size()).isEqualTo(2);
    }

    @Test
    void matrixNestedPayloadRoundTrips() {
        var payload = new JsonObject()
                .put("event_id", "$abc:matrix.org")
                .put("sender", "@alice:matrix.org")
                .put("type", "m.room.message")
                .put("content", new JsonObject()
                        .put("msgtype", "m.text")
                        .put("body", "Hello \"world\"\nnew\tline")
                        .put("formatted_body", "<b>é</b> \uD83D\uDE00"))
                .put("unsigned", new JsonObject()
                        .put("age", 42)
                        .put("transaction_id", "m123.4"))
                .put("unknown_future_field", new JsonObject()
                        .put("nested_unknown", new JsonArray()
                                .add(new JsonObject().put("x", 1.5))
                                .add(JsonNull.INSTANCE)));
        String serialized = payload.toJson();
        JsonValue reparsed = JsonParser.parse(serialized);
        assertThat(reparsed.toJson()).isEqualTo(serialized);
        var content = reparsed.asObject().get("content").asObject();
        assertThat(content.get("body").asString()).isEqualTo("Hello \"world\"\nnew\tline");
        assertThat(content.get("formatted_body").asString()).isEqualTo("<b>é</b> \uD83D\uDE00");
        var unknown = reparsed.asObject().get("unknown_future_field").asObject();
        assertThat(unknown.get("nested_unknown").asArray().size()).isEqualTo(2);
        assertThat(unknown.get("nested_unknown").asArray().get(0).asObject().get("x").asDouble()).isEqualTo(1.5);
        assertThat(unknown.get("nested_unknown").asArray().get(1).isNull()).isTrue();
    }

    @Test
    void unknownFieldsAreRetained() {
        JsonValue value = JsonParser.parse("""
                {"type":"m.room.message","content":{"msgtype":"m.text","body":"hi"},"org.matrix.custom":true}
                """);
        var obj = value.asObject();
        assertThat(obj.names()).containsExactlyInAnyOrder("type", "content", "org.matrix.custom");
        assertThat(obj.get("org.matrix.custom").asBoolean()).isTrue();
        assertThat(obj.getOrDefault("missing", JsonNull.INSTANCE)).isSameAs(JsonNull.INSTANCE);
        assertThat(obj.size()).isEqualTo(3);
        assertThat(obj.entrySet()).hasSize(3);
    }

    @Test
    void objectKeepsInsertionOrderOnSerialization() {
        var obj = new JsonObject(new LinkedHashMap<>());
        obj.put("b", 1);
        obj.put("a", 2);
        assertThat(obj.toJson()).isEqualTo("{\"b\":1,\"a\":2}");
        assertThat(JsonParser.parse("{\"z\":1,\"a\":2}").toJson()).isEqualTo("{\"z\":1,\"a\":2}");
    }

    @Test
    void serializesControlCharactersAndUnicodeSafely() {
        var payload = new JsonObject().put("emoji", "🙂").put("del", "\u007f");
        assertThat(payload.get("emoji").asString()).isEqualTo("🙂");
        assertThat(payload.get("del").asString()).isEqualTo("\u007f");
        assertThat(JsonParser.parse(payload.toJson()).asObject().get("del").asString()).isEqualTo("\u007f");
    }
}
