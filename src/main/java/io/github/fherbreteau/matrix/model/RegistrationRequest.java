package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.Map;

/**
 * Parameters for registering a Matrix user. Unknown and UI-auth stage parameters can be supplied
 * through {@link Builder#put(String, JsonValue)}.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3register">Matrix
 *     specification</a>
 */
public final class RegistrationRequest {

  private final JsonObject body;

  private RegistrationRequest(JsonObject body) {
    this.body = body;
  }

  /**
   * Returns a builder for a registration request.
   *
   * @return a new registration builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Serializes this request body.
   *
   * @return the request body
   */
  public JsonValue toJson() {
    return body;
  }

  /** Builder for registration parameters. */
  public static final class Builder {
    private final JsonObject body = new JsonObject();

    /**
     * Sets the requested username.
     *
     * @param username the requested username
     * @return this builder
     */
    public Builder username(String username) {
      return putIfPresent("username", username);
    }

    /**
     * Sets the password for the new account.
     *
     * @param password the account password
     * @return this builder
     */
    public Builder password(String password) {
      return putIfPresent("password", password);
    }

    /**
     * Sets the client-generated device identifier.
     *
     * @param deviceId the client-generated device identifier
     * @return this builder
     */
    public Builder deviceId(String deviceId) {
      return putIfPresent("device_id", deviceId);
    }

    /**
     * Sets the initial device display name.
     *
     * @param displayName the initial device display name
     * @return this builder
     */
    public Builder initialDeviceDisplayName(String displayName) {
      return putIfPresent("initial_device_display_name", displayName);
    }

    /**
     * Inhibits login and token creation after account registration.
     *
     * @param inhibitLogin whether to inhibit login
     * @return this builder
     */
    public Builder inhibitLogin(boolean inhibitLogin) {
      body.put("inhibit_login", inhibitLogin);
      return this;
    }

    /**
     * Requests a refresh token with the access token.
     *
     * @param requestRefreshToken whether to request a refresh token
     * @return this builder
     */
    public Builder requestRefreshToken(boolean requestRefreshToken) {
      body.put("refresh_token", requestRefreshToken);
      return this;
    }

    /**
     * Adds or replaces the UI-auth response or another registration field.
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
     * Adds or replaces registration fields.
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
     * @return the registration request
     */
    public RegistrationRequest build() {
      return new RegistrationRequest(body);
    }

    private Builder putIfPresent(String name, String value) {
      if (value != null) {
        body.put(name, value);
      }
      return this;
    }
  }
}
