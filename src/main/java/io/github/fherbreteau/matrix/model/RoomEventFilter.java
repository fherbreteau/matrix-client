package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.List;

/**
 * A room event filter used by `/sync`, creation of server-side filters and room history. Unknown
 * fields remain settable through {@link Builder#rawRoomOption(String, JsonValue)}, providing
 * forward compatibility. Use the {@link Builder} via {@link #builder()}.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#filtering">Matrix
 *     specification</a>
 */
public final class RoomEventFilter implements FilterJson {

  private static final String LAZY_LOAD_MEMBERS = "lazy_load_members";
  private static final String INCLUDE_REDUNDANT_MEMBERS = "include_redundant_members";

  private final JsonObject options;

  private RoomEventFilter(JsonObject options) {
    this.options = options;
  }

  /**
   * Returns a new room-event-filter builder.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Returns the filter as a JSON object.
   *
   * @return the room event filter JSON
   */
  public JsonValue toJson() {
    return options;
  }

  /** Builder for {@link RoomEventFilter}. */
  public static final class Builder {

    private final JsonObject options = new JsonObject();

    private Builder() {}

    /**
     * Limits the timeline chunk size returned by sync.
     *
     * @param limit positive maximum event count
     * @return this builder for chaining
     * @throws IllegalArgumentException if the limit is negative
     */
    public Builder limit(int limit) {
      if (limit < 0) {
        throw new IllegalArgumentException("timeline filter limit must not be negative");
      }
      options.put("limit", limit);
      return this;
    }

    /**
     * Includes events sent by these users.
     *
     * @param senders user IDs
     * @return this builder for chaining
     */
    public Builder senders(List<UserId> senders) {
      options.put("senders", FilterJson.strings(senders.stream().map(UserId::value).toList()));
      return this;
    }

    /**
     * Excludes events sent by these users.
     *
     * @param senders user IDs
     * @return this builder for chaining
     */
    public Builder notSenders(List<UserId> senders) {
      options.put("not_senders", FilterJson.strings(senders.stream().map(UserId::value).toList()));
      return this;
    }

    /**
     * Includes events of these types. The {@code *} wildcard matches all event types.
     *
     * @param types event type names
     * @return this builder for chaining
     */
    public Builder types(List<String> types) {
      options.put("types", FilterJson.strings(types));
      return this;
    }

    /**
     * Excludes events of these types. The {@code *} wildcard matches all event types.
     *
     * @param types event type names
     * @return this builder for chaining
     */
    public Builder notTypes(List<String> types) {
      options.put("not_types", FilterJson.strings(types));
      return this;
    }

    /**
     * Includes events in these rooms.
     *
     * @param rooms room IDs
     * @return this builder for chaining
     */
    public Builder rooms(List<RoomId> rooms) {
      options.put("rooms", FilterJson.strings(rooms.stream().map(RoomId::value).toList()));
      return this;
    }

    /**
     * Excludes events from these rooms.
     *
     * @param rooms room IDs
     * @return this builder for chaining
     */
    public Builder notRooms(List<RoomId> rooms) {
      options.put("not_rooms", FilterJson.strings(rooms.stream().map(RoomId::value).toList()));
      return this;
    }

    /**
     * Includes or excludes events whose content contains a URL.
     *
     * @param containsUrl whether matching events should contain a URL
     * @return this builder for chaining
     */
    public Builder containsUrl(boolean containsUrl) {
      options.put("contains_url", containsUrl);
      return this;
    }

    /**
     * Enables lazy-loaded membership events for rooms in the timeline.
     *
     * @param enabled whether to enable lazy-loading
     * @return this builder for chaining
     */
    public Builder lazyLoadMembers(boolean enabled) {
      options.put(LAZY_LOAD_MEMBERS, enabled);
      return this;
    }

    /**
     * Includes redundant member events; only applies when lazy-loading is enabled.
     *
     * @param enabled whether to include redundant member events
     * @return this builder for chaining
     */
    public Builder includeRedundantMembers(boolean enabled) {
      options.put(INCLUDE_REDUNDANT_MEMBERS, enabled);
      return this;
    }

    /**
     * Passes through an additional field in the spec's RoomEventFilter.
     *
     * @param name the filter field name
     * @param value its JSON value
     * @return this builder for chaining
     */
    public Builder rawRoomOption(String name, JsonValue value) {
      options.put(name, value);
      return this;
    }

    /**
     * Builds the room event filter.
     *
     * @return the filter
     */
    public RoomEventFilter build() {
      if (options.has(INCLUDE_REDUNDANT_MEMBERS)
          && (!options.has(LAZY_LOAD_MEMBERS) || !options.get(LAZY_LOAD_MEMBERS).asBoolean())) {
        throw new IllegalArgumentException(
            "include_redundant_members requires lazy_load_members to be enabled");
      }
      return new RoomEventFilter(options);
    }
  }
}
