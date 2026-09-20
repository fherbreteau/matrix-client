package io.github.fherbreteau.matrix.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.fherbreteau.matrix.error.DiscoveryException;
import io.github.fherbreteau.matrix.json.JsonArray;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * The Matrix spec versions and features supported by a homeserver, as
 * returned by {@code /_matrix/client/versions}. Unknown fields (for example
 * {@code unstable_features}) are preserved for forward compatibility.
 */
public final class MatrixVersions {

    private final List<String> versions;
    private final Map<String, JsonValue> fields;

    private MatrixVersions(List<String> versions, Map<String, JsonValue> fields) {
        this.versions = List.copyOf(versions);
        this.fields = Map.copyOf(fields);
    }

    /**
     * Validates and parses a {@code /versions} response body.
     *
     * @throws DiscoveryException if the body is not a JSON object or does
     *         not contain a {@code versions} array of strings
     */
    public static MatrixVersions from(JsonValue body) {
        if (body == null || !body.isObject()) {
            throw new DiscoveryException("Versions response must be a JSON object");
        }
        JsonObject obj = body.asObject();
        JsonValue versionsValue = obj.get("versions");
        if (versionsValue == null || !versionsValue.isArray()) {
            throw new DiscoveryException("Versions response must contain a versions array");
        }
        JsonArray versionsArray = versionsValue.asArray();
        var versions = new ArrayList<String>(versionsArray.size());
        for (int i = 0; i < versionsArray.size(); i++) {
            JsonValue version = versionsArray.get(i);
            if (version == null || !version.isString()) {
                throw new DiscoveryException("Versions array must contain only strings");
            }
            versions.add(version.asString());
        }
        var fields = new LinkedHashMap<String, JsonValue>();
        for (Map.Entry<String, JsonValue> entry : obj.entrySet()) {
            if (!"versions".equals(entry.getKey())) {
                fields.put(entry.getKey(), entry.getValue());
            }
        }
        return new MatrixVersions(versions, fields);
    }

    /**
     * Returns the Matrix spec versions supported by the homeserver.
     */
    public List<String> getVersions() {
        return versions;
    }

    /**
     * Returns whether the homeserver supports the given Matrix spec version.
     */
    public boolean supports(String version) {
        return versions.contains(version);
    }

    /**
     * Returns the additional fields of the response (such as
     * {@code unstable_features}), beyond the {@code versions} array.
     */
    public Map<String, JsonValue> getFields() {
        return fields;
    }
}
