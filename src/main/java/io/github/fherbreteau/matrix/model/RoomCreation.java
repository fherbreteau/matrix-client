package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonArray;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.ArrayList;
import java.util.List;

/**
 * The parameters of a room creation request. Unknown homeserver options can still be provided
 * through the raw {@code creation_content}, and all optional fields are omitted from the request
 * body when unset. Use the {@link Builder} via {@link #builder()}.
 */
public final class RoomCreation {

  private final String visibility;
  private final String roomAliasName;
  private final String name;
  private final String topic;
  private final List<UserId> invites;
  private final List<Invite3pid> invites3pid;
  private final String roomVersion;
  private final String preset;
  private final boolean direct;
  private final List<JsonValue> initialState;
  private final CreationContent creationContent;
  private final JsonValue powerLevelContentOverride;

  private RoomCreation(Builder builder) {
    this.visibility = builder.visibility;
    this.roomAliasName = builder.roomAliasName;
    this.name = builder.name;
    this.topic = builder.topic;
    this.invites = List.copyOf(builder.invites);
    this.invites3pid = List.copyOf(builder.invites3pid);
    this.roomVersion = builder.roomVersion;
    this.preset = builder.preset;
    this.direct = builder.direct;
    this.initialState = List.copyOf(builder.initialState);
    this.creationContent = builder.creationContent;
    this.powerLevelContentOverride = builder.powerLevelContentOverride;
  }

  /**
   * Returns a new {@link Builder}.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Serializes the parameters as a create-room request body, omitting unset fields.
   *
   * @return the create-room request body
   */
  public JsonValue toJson() {
    JsonObject body = new JsonObject();
    if (creationContent != null) {
      body.put("creation_content", creationContent.toJson());
    }
    if (!initialState.isEmpty()) {
      JsonArray initialStateArray = new JsonArray();
      for (JsonValue state : initialState) {
        initialStateArray.add(state);
      }
      body.put("initial_state", initialStateArray);
    }
    if (!invites.isEmpty()) {
      JsonArray inviteArray = new JsonArray();
      for (UserId invite : invites) {
        inviteArray.add(new JsonObject().put("user_id", invite.value()));
      }
      body.put("invite", inviteArray);
    }
    if (!invites3pid.isEmpty()) {
      JsonArray invite3PidArray = new JsonArray();
      for (Invite3pid invite3pid : invites3pid) {
        invite3PidArray.add(invite3pid.toJson());
      }
      body.put("invite_3pid", invite3PidArray);
    }
    if (direct) {
      body.put("is_direct", true);
    }
    putIfPresent(body, "name", name);
    if (powerLevelContentOverride != null) {
      body.put("power_level_content_override", powerLevelContentOverride);
    }
    putIfPresent(body, "preset", preset);
    putIfPresent(body, "room_alias_name", roomAliasName);
    putIfPresent(body, "room_version", roomVersion);
    putIfPresent(body, "topic", topic);
    putIfPresent(body, "visibility", visibility);
    return body;
  }

  private static void putIfPresent(JsonObject body, String name, String value) {
    if (value != null) {
      body.put(name, value);
    }
  }

  /** Builder for {@link RoomCreation}. */
  public static final class Builder {

    private String visibility;
    private String roomAliasName;
    private String name;
    private String topic;
    private List<UserId> invites = new ArrayList<>();
    private List<Invite3pid> invites3pid = new ArrayList<>();
    private String roomVersion;
    private String preset;
    private boolean direct;
    private List<JsonValue> initialState = new ArrayList<>();
    private CreationContent creationContent;
    private JsonValue powerLevelContentOverride;

    private Builder() {}

    /**
     * Sets the visibility of the room in the directory: {@code public} or {@code private}.
     *
     * @param visibility the visibility in the directory
     * @return this builder for chaining
     */
    public Builder visibility(String visibility) {
      this.visibility = visibility;
      return this;
    }

    /**
     * Sets the localpart of the desired room alias in the directory.
     *
     * @param roomAliasName the localpart of the desired alias
     * @return this builder for chaining
     */
    public Builder roomAliasName(String roomAliasName) {
      this.roomAliasName = roomAliasName;
      return this;
    }

    /**
     * Sets the {@code m.room.name} shown to users.
     *
     * @param name the room name
     * @return this builder for chaining
     */
    public Builder name(String name) {
      this.name = name;
      return this;
    }

    /**
     * Sets the {@code m.room.topic} shown to users.
     *
     * @param topic the room topic
     * @return this builder for chaining
     */
    public Builder topic(String topic) {
      this.topic = topic;
      return this;
    }

    /**
     * Sets the users to invite to the room at creation time.
     *
     * @param invites the users to invite
     * @return this builder for chaining
     */
    public Builder invites(List<UserId> invites) {
      this.invites = invites;
      return this;
    }

    /**
     * Sets the third-party identities to invite to the room at creation time, as resolved through
     * an identity server.
     *
     * @param invites3pid the third-party invites
     * @return this builder for chaining
     */
    public Builder invites3pid(List<Invite3pid> invites3pid) {
      this.invites3pid = invites3pid;
      return this;
    }

    /**
     * Sets the desired Matrix room version.
     *
     * @param roomVersion the desired room version
     * @return this builder for chaining
     */
    public Builder roomVersion(String roomVersion) {
      this.roomVersion = roomVersion;
      return this;
    }

    /**
     * Sets the preset shaping the initial state of the room: {@code private_chat}, {@code
     * trusted_private_chat} or {@code public_chat}.
     *
     * @param preset the room preset
     * @return this builder for chaining
     */
    public Builder preset(String preset) {
      this.preset = preset;
      return this;
    }

    /**
     * Marks the room as a direct message room for invited users.
     *
     * @param direct whether the room is a direct message room
     * @return this builder for chaining
     */
    public Builder direct(boolean direct) {
      this.direct = direct;
      return this;
    }

    /**
     * Sets the {@code initial_state} events sent with the room creation, letting the client
     * override the default state event content.
     *
     * @param initialState the initial state events
     * @return this builder for chaining
     */
    public Builder initialState(List<JsonValue> initialState) {
      this.initialState = initialState;
      return this;
    }

    /**
     * Sets the {@code creation_content} overriding default keys of the {@code m.room.create} event,
     * such as the creator, additional creators, federation and room type.
     *
     * @param creationContent the creation content
     * @return this builder for chaining
     */
    public Builder creationContent(CreationContent creationContent) {
      this.creationContent = creationContent;
      return this;
    }

    /**
     * Sets the {@code power_level_content_override} applied on top of the default power levels.
     *
     * @param powerLevelContentOverride the power levels override
     * @return this builder for chaining
     */
    public Builder powerLevelContentOverride(JsonValue powerLevelContentOverride) {
      this.powerLevelContentOverride = powerLevelContentOverride;
      return this;
    }

    /**
     * Builds the room creation parameters.
     *
     * @return the room creation parameters
     */
    public RoomCreation build() {
      return new RoomCreation(this);
    }
  }
}
