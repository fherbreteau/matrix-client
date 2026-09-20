package io.github.fherbreteau.matrix.json;

import java.math.BigDecimal;

/**
 * A minimal JSON value. Implemented by the object, array, string, number,
 * boolean and null representations produced by {@link JsonParser}.
 */
public sealed interface JsonValue permits JsonObject, JsonArray, JsonString, JsonNumber, JsonBoolean, JsonNull {

    default boolean isObject() {
        return false;
    }

    default boolean isArray() {
        return false;
    }

    default boolean isString() {
        return false;
    }

    default boolean isNumber() {
        return false;
    }

    default boolean isBoolean() {
        return false;
    }

    default boolean isNull() {
        return false;
    }

    default JsonObject asObject() {
        throw new UnsupportedOperationException("Not a JSON object");
    }

    default JsonArray asArray() {
        throw new UnsupportedOperationException("Not a JSON array");
    }

    default String asString() {
        throw new UnsupportedOperationException("Not a JSON string");
    }

    default double asDouble() {
        throw new UnsupportedOperationException("Not a JSON number");
    }

    default long asLong() {
        throw new UnsupportedOperationException("Not a JSON number");
    }

    default BigDecimal asBigDecimal() {
        throw new UnsupportedOperationException("Not a JSON number");
    }

    default boolean asBoolean() {
        throw new UnsupportedOperationException("Not a JSON boolean");
    }

    /**
     * Serializes this value back to a JSON string.
     */
    String toJson();
}
