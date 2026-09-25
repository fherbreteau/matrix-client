package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.List;

/**
 * A saved `/sync` filter definition. Filter sections not exposed as typed builders remain available
 * through raw JSON and are preserved by the Matrix API. Use the {@link Builder} via {@link
 * #builder()}.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#filtering">Matrix
 *     specification</a>
 */
public final class MatrixFilter {

  private final JsonObject options;

  private MatrixFilter(JsonObject options) {
    this.options = options;
  }

  /**
   * Parses a saved filter definition returned by the homeserver.
   *
   * @param value the parsed filter definition
   * @return the filter, preserving unmodeled options
   */
  public static MatrixFilter from(JsonValue value) {
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException("filter definition must be a JSON object");
    }
    return new MatrixFilter(value.asObject());
  }

  /**
   * Returns a new matrix-filter builder.
   *
   * @return a new filter builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Returns this filter definition as JSON.
   *
   * @return the filter definition JSON
   */
  public JsonValue toJson() {
    return options;
  }

  /** Builder for {@link MatrixFilter}. */
  public static final class Builder {

    private final JsonObject options = new JsonObject();

    private Builder() {}

    /**
     * Sets which event fields a server should include; absent means all fields.
     *
     * @param fields the event fields to include
     * @return this builder for chaining
     */
    public Builder eventFields(List<String> fields) {
      options.put("event_fields", FilterJson.strings(fields));
      return this;
    }

    /**
     * Selects client or federation event format; absent defaults to client.
     *
     * @param eventFormat the event format to return
     * @return this builder for chaining
     */
    public Builder eventFormat(EventFormat eventFormat) {
      options.put("event_format", eventFormat.value());
      return this;
    }

    /**
     * Sets the presence-event filter.
     *
     * @param filter the filter to apply
     * @return this builder for chaining
     */
    public Builder presence(EventFilter filter) {
      options.put("presence", filter.toJson());
      return this;
    }

    /**
     * Sets the global account-data filter.
     *
     * @param filter the filter to apply
     * @return this builder for chaining
     */
    public Builder accountData(EventFilter filter) {
      options.put("account_data", filter.toJson());
      return this;
    }

    /**
     * Sets the room filter.
     *
     * @param filter the room sections and event filters
     * @return this builder for chaining
     */
    public Builder room(JsonValue filter) {
      options.put("room", filter);
      return this;
    }

    /**
     * Sets the joined-room timeline filter.
     *
     * @param filter the room-event filter
     * @return this builder for chaining
     */
    public Builder roomTimeline(RoomEventFilter filter) {
      JsonObject room =
          options.get("room") != null ? options.get("room").asObject() : new JsonObject();
      room.put("timeline", filter.toJson());
      options.put("room", room);
      return this;
    }

    /**
     * Sets the joined-room state filter.
     *
     * @param filter the room-event filter
     * @return this builder for chaining
     */
    public Builder roomState(RoomEventFilter filter) {
      JsonObject room =
          options.get("room") != null ? options.get("room").asObject() : new JsonObject();
      room.put("state", filter.toJson());
      options.put("room", room);
      return this;
    }

    /**
     * Sets the joined-room ephemeral-event filter.
     *
     * @param filter the room-event filter
     * @return this builder for chaining
     */
    public Builder roomEphemeral(EventFilter filter) {
      JsonObject room =
          options.get("room") != null ? options.get("room").asObject() : new JsonObject();
      room.put("ephemeral", filter.toJson());
      options.put("room", room);
      return this;
    }

    /**
     * Sets the joined-room account-data filter.
     *
     * @param filter the account-data event filter
     * @return this builder for chaining
     */
    public Builder roomAccountData(EventFilter filter) {
      JsonObject room =
          options.get("room") != null ? options.get("room").asObject() : new JsonObject();
      room.put("account_data", filter.toJson());
      options.put("room", room);
      return this;
    }

    /**
     * Passes through another top-level filter field from the specification or an extension.
     *
     * @param name filter field name
     * @param value JSON value for the field
     * @return this builder for chaining
     */
    public Builder rawOption(String name, JsonValue value) {
      options.put(name, value);
      return this;
    }

    /**
     * Builds the filter.
     *
     * @return the filter definition
     */
    public MatrixFilter build() {
      return new MatrixFilter(options);
    }
  }
}
