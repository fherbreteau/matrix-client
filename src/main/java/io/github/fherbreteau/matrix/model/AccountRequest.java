package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.Map;

/**
 * Flexible request body for account operations that use User-Interactive Authentication. Unknown
 * and stage-specific fields are preserved to support homeserver extensions.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#user-interactive-authentication-api">Matrix
 *     specification</a>
 */
public final class AccountRequest {

  private final JsonObject body;

  private AccountRequest(JsonObject body) {
    this.body = body;
  }

  /**
   * Returns a builder for an account operation request.
   *
   * @return a new account request builder
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

  /** Builder for account-operation request fields. */
  public static final class Builder {
    private final JsonObject body = new JsonObject();

    /**
     * Adds the UI-auth response or a stage-specific field.
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
     * Adds or replaces arbitrary account-operation fields.
     *
     * @param values the fields to add
     * @return this builder
     */
    public Builder putAll(Map<String, JsonValue> values) {
      values.forEach(body::put);
      return this;
    }

    /**
     * Sets the new password.
     *
     * @param password the new password
     * @return this builder
     */
    public Builder newPassword(String password) {
      return putIfPresent("new_password", password);
    }

    /**
     * Sets whether other devices should be logged out.
     *
     * @param logoutDevices whether to log out other devices
     * @return this builder
     */
    public Builder logoutDevices(boolean logoutDevices) {
      body.put("logout_devices", logoutDevices);
      return this;
    }

    /**
     * Sets whether account data should be erased during deactivation.
     *
     * @param erase whether to erase account data
     * @return this builder
     */
    public Builder erase(boolean erase) {
      body.put("erase", erase);
      return this;
    }

    /**
     * Sets an optional identity server.
     *
     * @param identityServer the identity server
     * @return this builder
     */
    public Builder identityServer(String identityServer) {
      return putIfPresent("id_server", identityServer);
    }

    /**
     * Sets the client secret used for third-party identifier operations.
     *
     * @param clientSecret the client secret
     * @return this builder
     */
    public Builder clientSecret(String clientSecret) {
      return putIfPresent("client_secret", clientSecret);
    }

    /**
     * Sets the third-party identifier session ID.
     *
     * @param sessionId the session identifier
     * @return this builder
     */
    public Builder sessionId(String sessionId) {
      return putIfPresent("sid", sessionId);
    }

    /**
     * Sets the identity-server access token.
     *
     * @param accessToken the identity-server access token
     * @return this builder
     */
    public Builder identityAccessToken(String accessToken) {
      return putIfPresent("id_access_token", accessToken);
    }

    /**
     * Sets the address for a third-party identifier.
     *
     * @param address the identifier address
     * @return this builder
     */
    public Builder address(String address) {
      return putIfPresent("address", address);
    }

    /**
     * Sets the third-party identifier medium, such as {@code email} or {@code msisdn}.
     *
     * @param medium the identifier medium
     * @return this builder
     */
    public Builder medium(String medium) {
      return putIfPresent("medium", medium);
    }

    /**
     * Builds the request.
     *
     * @return the account request
     */
    public AccountRequest build() {
      return new AccountRequest(body);
    }

    private Builder putIfPresent(String name, String value) {
      if (value != null) {
        body.put(name, value);
      }
      return this;
    }
  }
}
