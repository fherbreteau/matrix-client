package io.github.fherbreteau.matrix.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.BOOLEAN;

import org.junit.jupiter.api.Test;

class JsonStringTest {

    @Test
    void ofAndPredicates() {
        JsonString str = JsonString.of("hello");
        assertThat(str).extracting(JsonValue::isString, BOOLEAN).isTrue();
        assertThat(str).extracting(JsonValue::asString).isEqualTo("hello");
        assertThat(str).extracting(JsonValue::toJson).isEqualTo("\"hello\"");
    }

    @Test
    void escapesControlCharacters() {
        var str = JsonString.of("a\"b\\c\nd\re\tf\bg\u0000h");
        assertThat(str).extracting(JsonValue::toJson).isEqualTo("\"a\\\"b\\\\c\\nd\\re\\tf\\bg\\u0000h\"");
        assertThat(JsonParser.parse(str.toJson()))
                .extracting(JsonValue::asString)
                .isEqualTo("a\"b\\c\nd\re\tf\bg\u0000h");
    }

    @Test
    void roundTripsForwardSlash() {
        assertThat(JsonParser.parse("\"a\\/b\"")).extracting(JsonValue::asString).isEqualTo("a/b");
    }
}
