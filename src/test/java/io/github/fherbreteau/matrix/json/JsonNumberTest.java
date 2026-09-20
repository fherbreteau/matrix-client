package io.github.fherbreteau.matrix.json;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.InstanceOfAssertFactories.BOOLEAN;

class JsonNumberTest {

    @Test
    void integralNumbersSerializeWithoutDecimalPoint() {
        assertThat(JsonNumber.of(42)).extracting(JsonValue::toJson).isEqualTo("42");
        assertThat(JsonParser.parse("42")).extracting(JsonValue::toJson).isEqualTo("42");
        assertThat(JsonNumber.of(-7)).extracting(JsonValue::toJson).isEqualTo("-7");
    }

    @Test
    void fractionalNumbersSerializeWithPrecision() {
        assertThat(JsonNumber.of(1.5)).extracting(JsonValue::toJson).isEqualTo("1.5");
        assertThat(JsonParser.parse("1.5")).extracting(JsonValue::toJson).isEqualTo("1.5");
    }

    @Test
    void predicates() {
        JsonNumber num = JsonNumber.of(1.0);
        assertThat(num).extracting(JsonValue::isNumber, BOOLEAN).isTrue();
        assertThat(num).extracting(JsonValue::asLong).isEqualTo(1L);
        assertThat(num).extracting(JsonNumber::isIntegral, BOOLEAN).isTrue();
        assertThat(JsonNumber.of(1.5)).extracting(JsonNumber::isIntegral, BOOLEAN).isFalse();
    }

    @Test
    void preservesBigDecimalPrecision() {
        var big = JsonNumber.of(new BigDecimal("9007199254740993"));
        assertThat(big).extracting(JsonValue::asLong).isEqualTo(9007199254740993L);
        assertThat(big).extracting(JsonValue::asDouble).isEqualTo(9007199254740992.0);
        assertThat(big).extracting(JsonValue::toJson).isEqualTo("9007199254740993");
        assertThat(JsonNumber.of(new BigDecimal("1.25"))).extracting(JsonValue::asBigDecimal).isEqualTo(new BigDecimal("1.25"));
    }

    @Test
    void scientificNotationRoundTrips() {
        assertThat(JsonParser.parse("1e100")).extracting(JsonValue::asBigDecimal).isEqualTo(new BigDecimal("1e+100"));
        assertThat(JsonParser.parse("1e100")).extracting(JsonValue::toJson).isEqualTo("1E+100");
        assertThat(JsonParser.parse("123.456e-10")).extracting(JsonValue::asDouble).isEqualTo(123.456e-10);
    }

    @Test
    void equalityComparesNumericValue() {
        assertThat(JsonNumber.of(1)).isEqualTo(JsonNumber.of(1L));
        assertThat(JsonNumber.of(1.0)).isEqualTo(JsonNumber.of(new BigDecimal("1.00")));
        assertThat(JsonNumber.of(1)).isNotEqualTo(JsonNumber.of(2));
        assertThat(JsonNumber.of(1)).isNotEqualTo("1");
        assertThat(JsonNumber.of(1)).hasSameHashCodeAs(JsonNumber.of(new BigDecimal("1.0")));
        assertThat(JsonNumber.of(1.5)).hasToString("JsonNumber[1.5]");
    }

    @Test
    void rejectsNullBigDecimal() {
        assertThatNullPointerException().isThrownBy(() -> JsonNumber.of((BigDecimal) null));
    }

    @Test
    void serializesHugeIntegralWithoutScientificNotation() {
        var number = JsonNumber.of(new BigDecimal("123456789012345678901234567890"));
        assertThat(number).extracting(JsonValue::toJson).isEqualTo("123456789012345678901234567890");
    }
}
