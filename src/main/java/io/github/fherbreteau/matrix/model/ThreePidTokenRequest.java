package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.Map;

/**
 * Parameters for email- and MSISDN-based third-party identifier or password-reset token requests.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3registeremailrequesttoken">Matrix
 *     specification</a>
 */
public final class ThreePidTokenRequest {

  private final JsonObject body;

  private ThreePidTokenRequest(JsonObject body) {
    this.body = body;
  }

  /**
   * Returns a builder for a third-party identifier token request.
   *
   * @return a new token request builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Serializes this request.
   *
   * @return the request body
   */
  public JsonValue toJson() {
    return body;
  }

  /** Builder for third-party identifier token parameters. */
  public static final class Builder {
    private final JsonObject body = new JsonObject();

    /**
     * Sets the client secret.
     *
     * @param value the client secret
     * @return this builder
     */
    public Builder clientSecret(String value) {
      return putIfPresent("client_secret", value);
    }

    /**
     * Sets the email address.
     *
     * @param value the email address
     * @return this builder
     */
    public Builder email(String value) {
      return putIfPresent("email", value);
    }

    /**
     * Sets the phone country code.
     *
     * @param value the phone country code
     * @return this builder
     */
    public Builder country(String value) {
      return putIfPresent("country", value);
    }

    /**
     * Sets the phone number.
     *
     * @param value the phone number
     * @return this builder
     */
    public Builder phoneNumber(String value) {
      return putIfPresent("phone_number", value);
    }

    /**
     * Sets the send-attempt counter.
     *
     * @param value the send-attempt counter
     * @return this builder
     */
    public Builder sendAttempt(long value) {
      body.put("send_attempt", value);
      return this;
    }

    /**
     * Sets an identity server where supported by the endpoint.
     *
     * @param value the identity server
     * @return this builder
     */
    public Builder identityServer(String value) {
      return putIfPresent("id_server", value);
    }

    /**
     * Sets an identity-server access token where supported by the endpoint.
     *
     * @param value the identity-server access token
     * @return this builder
     */
    public Builder identityAccessToken(String value) {
      return putIfPresent("id_access_token", value);
    }

    /**
     * Sets a post-validation redirect.
     *
     * @param value the redirect URL
     * @return this builder
     */
    public Builder nextLink(String value) {
      return putIfPresent("next_link", value);
    }

    /**
     * Adds a request extension or UI-auth field.
     *
     * @param name the field name
     * @param value the field value
     * @return this builder
     */
    public Builder put(String name, JsonValue value) {
      body.put(name, value);
      return this;
    }

    /**
     * Adds or replaces arbitrary request fields.
     *
     * @param values the fields to add
     * @return this builder
     */
    public Builder putAll(Map<String, JsonValue> values) {
      values.forEach(body::put);
      return this;
    }

    /**
     * Builds the request.
     *
     * @return the token request
     */
    public ThreePidTokenRequest build() {
      return new ThreePidTokenRequest(body);
    }

    private Builder putIfPresent(String name, String value) {
      if (value != null) {
        body.put(name, value);
      }
      return this;
    }
  }
}
