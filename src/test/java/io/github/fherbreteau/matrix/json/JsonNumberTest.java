package io.github.fherbreteau.matrix.json;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonNumberTest {

    @Test
    void integralNumbersSerializeWithoutDecimalPoint() {
        assertThat(JsonNumber.of(42).toJson()).isEqualTo("42");
        assertThat(JsonParser.parse("42").toJson()).isEqualTo("42");
        assertThat(JsonNumber.of(-7).toJson()).isEqualTo("-7");
    }

    @Test
    void fractionalNumbersSerializeWithPrecision() {
        assertThat(JsonNumber.of(1.5).toJson()).isEqualTo("1.5");
        assertThat(JsonParser.parse("1.5").toJson()).isEqualTo("1.5");
    }

    @Test
    void predicates() {
        JsonNumber num = JsonNumber.of(1.0);
        assertThat(num.isNumber()).isTrue();
        assertThat(num.asLong()).isEqualTo(1L);
    }
}
