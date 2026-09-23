package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * The receipts and fully-read marker to set on a room, as accepted by {@code POST
 * /_matrix/client/v3/rooms/{roomId}/read_markers}. All fields are optional and omitted from the
 * request body when unset. Use the {@link Builder} via {@link #builder()}.
 */
public final class ReadMarkers {

  private final String fullyRead;
  private final String read;
  private final String readPrivate;

  private ReadMarkers(Builder builder) {
    this.fullyRead = builder.fullyRead;
    this.read = builder.read;
    this.readPrivate = builder.readPrivate;
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
   * Serializes the markers as the read-markers request body, omitting unset fields.
   *
   * @return the read-markers request body
   */
  public JsonValue toJson() {
    JsonObject body = new JsonObject();
    if (fullyRead != null) {
      body.put("m.fully_read", fullyRead);
    }
    if (read != null) {
      body.put("m.read", read);
    }
    if (readPrivate != null) {
      body.put("m.read.private", readPrivate);
    }
    return body;
  }

  /** Builder for {@link ReadMarkers}. */
  public static final class Builder {

    private String fullyRead;
    private String read;
    private String readPrivate;

    private Builder() {}

    /**
     * Sets the event identifier the {@code m.fully_read} marker points at.
     *
     * @param fullyRead the event identifier the marker points at
     * @return this builder for chaining
     */
    public Builder fullyRead(String fullyRead) {
      this.fullyRead = fullyRead;
      return this;
    }

    /**
     * Sets the event identifier of the public read receipt.
     *
     * @param read the event identifier of the public read receipt
     * @return this builder for chaining
     */
    public Builder read(String read) {
      this.read = read;
      return this;
    }

    /**
     * Sets the event identifier of the private read receipt.
     *
     * @param readPrivate the event identifier of the private read receipt
     * @return this builder for chaining
     */
    public Builder readPrivate(String readPrivate) {
      this.readPrivate = readPrivate;
      return this;
    }

    /**
     * Builds the read markers.
     *
     * @return the read markers
     */
    public ReadMarkers build() {
      return new ReadMarkers(this);
    }
  }
}
