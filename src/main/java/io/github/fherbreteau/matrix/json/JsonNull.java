package io.github.fherbreteau.matrix.json;

public final class JsonNull implements JsonValue {

    public static final JsonNull INSTANCE = new JsonNull();

    private JsonNull() {
    }

    @Override
    public boolean isNull() {
        return true;
    }

    @Override
    public String toJson() {
        return "null";
    }
}
