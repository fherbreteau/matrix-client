package io.github.fherbreteau.matrix.json;

public final class JsonNumber implements JsonValue {

    private final double value;

    private JsonNumber(double value) {
        this.value = value;
    }

    public static JsonNumber of(double value) {
        return new JsonNumber(value);
    }

    @Override
    public boolean isNumber() {
        return true;
    }

    @Override
    public double asDouble() {
        return value;
    }

    public long asLong() {
        return (long) value;
    }

    @Override
    public String toJson() {
        if (value == Math.floor(value) && !Double.isInfinite(value)
                && Math.abs(value) < 9.007199254740992E15) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }
}
