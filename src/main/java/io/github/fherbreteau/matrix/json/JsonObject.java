package io.github.fherbreteau.matrix.json;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class JsonObject implements JsonValue {

    private final Map<String, JsonValue> values;

    public JsonObject() {
        this(new LinkedHashMap<>());
    }

    public JsonObject(Map<String, JsonValue> values) {
        this.values = values;
    }

    @Override
    public boolean isObject() {
        return true;
    }

    @Override
    public JsonObject asObject() {
        return this;
    }

    public JsonValue get(String name) {
        return values.get(name);
    }

    public JsonObject put(String name, JsonValue value) {
        values.put(name, value);
        return this;
    }

    public JsonObject put(String name, String value) {
        values.put(name, JsonString.of(value));
        return this;
    }

    public boolean has(String name) {
        return values.containsKey(name);
    }

    public Set<String> names() {
        return values.keySet();
    }

    @Override
    public String toJson() {
        var sb = new StringBuilder("{");
        var first = true;
        for (var entry : values.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append(JsonString.of(entry.getKey()).toJson()).append(':');
            sb.append(entry.getValue().toJson());
        }
        return sb.append('}').toString();
    }
}
