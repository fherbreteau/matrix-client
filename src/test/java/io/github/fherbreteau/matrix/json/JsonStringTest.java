package io.github.fherbreteau.matrix.json;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonStringTest {

    @Test
    void ofAndPredicates() {
        JsonString str = JsonString.of("hello");
        assertThat(str.isString()).isTrue();
        assertThat(str.asString()).isEqualTo("hello");
        assertThat(str.toJson()).isEqualTo("\"hello\"");
    }

    @Test
    void escapesControlCharacters() {
        var str = JsonString.of("a\"b\\c\nd\re\tf\bg\u0000h");
        assertThat(str.toJson()).isEqualTo("\"a\\\"b\\\\c\\nd\\re\\tf\\bg\\u0000h\"");
        assertThat(JsonParser.parse(str.toJson()).asString()).isEqualTo("a\"b\\c\nd\re\tf\bg\u0000h");
    }

    @Test
    void roundTripsForwardSlash() {
        assertThat(JsonParser.parse("\"a\\/b\"").asString()).isEqualTo("a/b");
    }
}
