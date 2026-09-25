package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.List;

/**
 * A Matrix event filter. Fields not represented by typed accessors remain settable via {@link
 * Builder#rawOption(String, JsonValue)}, allowing spec and homeserver extensions to be passed
 * through. Use the {@link Builder} via {@link #builder()}.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#filtering">Matrix
 *     specification</a>
 */
public final class EventFilter implements FilterJson {

  private static final String LAZY_LOAD_MEMBERS = "lazy_load_members";
  private static final String INCLUDE_REDUNDANT_MEMBERS = "include_redundant_members";
  private static final String TYPES = "types";
  private static final String NOT_TYPES = "not_types";
  private static final String SENDERS = "senders";
  private static final String NOT_SENDERS = "not_senders";
  private static final String ROOMS = "rooms";
  private static final String NOT_ROOMS = "not_rooms";
  private static final String CONTAINS_URL = "contains_url";
  private static final String LIMIT = "limit";

  private final JsonObject options;

  private EventFilter(JsonObject options) {
    this.options = options;
  }

  /**
   * Returns a new event-filter builder.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Returns this filter as a JSON object.
   *
   * @return the event-filter JSON object
   */
  public JsonValue toJson() {
    return options;
  }

  /** Builder for {@link EventFilter}. */
  public static final class Builder {

    private final JsonObject options = new JsonObject();

    private Builder() {}

    /**
     * Includes these event types. The wildcard {@code *} matches every type.
     *
     * @param types event type names
     * @return this builder for chaining
     */
    public Builder types(List<String> types) {
      options.put(TYPES, FilterJson.strings(types));
      return this;
    }

    /**
     * Excludes these event types. The wildcard {@code *} matches every type.
     *
     * @param types event type names
     * @return this builder for chaining
     */
    public Builder notTypes(List<String> types) {
      options.put(NOT_TYPES, FilterJson.strings(types));
      return this;
    }

    /**
     * Includes events sent by these users.
     *
     * @param senders user IDs
     * @return this builder for chaining
     */
    public Builder senders(List<UserId> senders) {
      options.put(SENDERS, FilterJson.strings(senders.stream().map(UserId::value).toList()));
      return this;
    }

    /**
     * Excludes events sent by these users.
     *
     * @param senders user IDs
     * @return this builder for chaining
     */
    public Builder notSenders(List<UserId> senders) {
      options.put(NOT_SENDERS, FilterJson.strings(senders.stream().map(UserId::value).toList()));
      return this;
    }

    /**
     * Includes events in these rooms.
     *
     * @param rooms room IDs
     * @return this builder for chaining
     */
    public Builder rooms(List<RoomId> rooms) {
      options.put(ROOMS, FilterJson.strings(rooms.stream().map(RoomId::value).toList()));
      return this;
    }

    /**
     * Excludes events in these rooms.
     *
     * @param rooms room IDs
     * @return this builder for chaining
     */
    public Builder notRooms(List<RoomId> rooms) {
      options.put(NOT_ROOMS, FilterJson.strings(rooms.stream().map(RoomId::value).toList()));
      return this;
    }

    /**
     * Filters to events whose content contains a URL, or excludes them when false.
     *
     * @param containsUrl whether to include events containing URLs
     * @return this builder for chaining
     */
    public Builder containsUrl(boolean containsUrl) {
      options.put(CONTAINS_URL, containsUrl);
      return this;
    }

    /**
     * Limits the number of events returned in an event collection.
     *
     * @param limit maximum number of events; must be positive
     * @return this builder for chaining
     * @throws IllegalArgumentException if the limit is not positive
     */
    public Builder limit(int limit) {
      if (limit <= 0) {
        throw new IllegalArgumentException("filter limit must be positive");
      }
      options.put(LIMIT, limit);
      return this;
    }

    /**
     * Enables lazy-loading room members during sync.
     *
     * @param enabled whether to lazy-load members
     * @return this builder for chaining
     */
    public Builder lazyLoadMembers(boolean enabled) {
      options.put(LAZY_LOAD_MEMBERS, enabled);
      return this;
    }

    /**
     * Includes redundant member events when lazy-loading is enabled.
     *
     * @param enabled whether to include redundant member events
     * @return this builder for chaining
     */
    public Builder includeRedundantMembers(boolean enabled) {
      options.put(INCLUDE_REDUNDANT_MEMBERS, enabled);
      return this;
    }

    /**
     * Passes through an additional filter option without interpreting it.
     *
     * @param name spec-defined or extension filter field
     * @param value JSON value for the field
     * @return this builder for chaining
     */
    public Builder rawOption(String name, JsonValue value) {
      options.put(name, value);
      return this;
    }

    /**
     * Builds the event filter.
     *
     * @return the event filter
     */
    public EventFilter build() {
      if (options.has(INCLUDE_REDUNDANT_MEMBERS)
          && (!options.has(LAZY_LOAD_MEMBERS) || !options.get(LAZY_LOAD_MEMBERS).asBoolean())) {
        throw new IllegalArgumentException(
            "include_redundant_members requires lazy_load_members to be enabled");
      }
      return new EventFilter(options);
    }
  }
}
