package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonArray;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonString;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.ArrayList;
import java.util.List;

/**
 * The {@code creation_content} of a room creation request: the keys of the {@code m.room.create}
 * event, such as the creator, the additional creators (room version 11), the federation behavior
 * and the room type. Use the {@link Builder} via {@link #builder()}.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/creation">Matrix specification</a>
 */
public final class CreationContent {

  private final List<UserId> additionalCreators;
  private final UserId creator;
  private final boolean federate;
  private final String type;

  private CreationContent(Builder builder) {
    this.additionalCreators = List.copyOf(builder.additionalCreators);
    this.creator = builder.creator;
    this.federate = builder.federate;
    this.type = builder.type;
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
   * Serializes the creation content as a JSON object, omitting unset fields.
   *
   * @return the creation content object
   */
  public JsonValue toJson() {
    JsonObject body = new JsonObject();
    if (!additionalCreators.isEmpty()) {
      JsonArray creatorArray = new JsonArray();
      for (UserId additionalCreator : additionalCreators) {
        creatorArray.add(JsonString.of(additionalCreator.value()));
      }
      body.put("additional_creators", creatorArray);
    }
    if (creator != null) {
      body.put("creator", creator.value());
    }
    if (!federate) {
      body.put("m.federate", false);
    }
    putIfPresent(body, "type", type);
    return body;
  }

  private static void putIfPresent(JsonObject body, String name, String value) {
    if (value != null) {
      body.put(name, value);
    }
  }

  /** Builder for {@link CreationContent}. */
  public static final class Builder {

    private List<UserId> additionalCreators = new ArrayList<>();
    private UserId creator;
    private boolean federate = true;
    private String type;

    private Builder() {}

    /**
     * Sets the additional creators of the room, allowed by room version 11 in addition to the
     * {@code m.room.create} sender.
     *
     * @param additionalCreators the users to mark as additional creators
     * @return this builder for chaining
     */
    public Builder additionalCreators(List<UserId> additionalCreators) {
      this.additionalCreators = additionalCreators;
      return this;
    }

    /**
     * Sets the {@code creator} key of the {@code m.room.create} event, overriding the creating
     * user.
     *
     * @param creator the user to declare as creator
     * @return this builder for chaining
     */
    public Builder creator(UserId creator) {
      this.creator = creator;
      return this;
    }

    /**
     * Sets whether users on other homeservers can join the room. Defaults to {@code true}; setting
     * {@code false} adds {@code "m.federate": false} to the creation content.
     *
     * @param federate whether the room can be federated
     * @return this builder for chaining
     */
    public Builder federate(boolean federate) {
      this.federate = federate;
      return this;
    }

    /**
     * Sets the room type, such as {@code m.space}; left out for regular message rooms.
     *
     * @param type the room type
     * @return this builder for chaining
     */
    public Builder type(String type) {
      this.type = type;
      return this;
    }

    /**
     * Builds the creation content.
     *
     * @return the creation content
     */
    public CreationContent build() {
      return new CreationContent(this);
    }
  }
}
