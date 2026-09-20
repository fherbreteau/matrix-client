package io.github.fherbreteau.matrix.json;

import java.util.ArrayList;
import java.util.List;

/**
 * A JSON array.
 */
public final class JsonArray implements JsonValue {

    private final List<JsonValue> values;

    public JsonArray() {
        this(new ArrayList<>());
    }

    public JsonArray(List<JsonValue> values) {
        this.values = values;
    }

    @Override
    public boolean isArray() {
        return true;
    }

    @Override
    public JsonArray asArray() {
        return this;
    }

    public JsonValue get(int index) {
        return values.get(index);
    }

    public JsonArray add(JsonValue value) {
        values.add(value);
        return this;
    }

    public int size() {
        return values.size();
    }

    @Override
    public String toJson() {
        var sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(values.get(i).toJson());
        }
        return sb.append(']').toString();
    }
}
