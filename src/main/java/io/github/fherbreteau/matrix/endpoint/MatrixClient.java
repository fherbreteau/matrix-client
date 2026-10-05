package io.github.fherbreteau.matrix.endpoint;

import io.github.fherbreteau.matrix.error.AuthenticationException;
import io.github.fherbreteau.matrix.error.DiscoveryException;
import io.github.fherbreteau.matrix.error.MatrixServerException;
import io.github.fherbreteau.matrix.error.RateLimitedException;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonParser;
import io.github.fherbreteau.matrix.json.JsonValue;
import io.github.fherbreteau.matrix.model.AccountOperationResponse;
import io.github.fherbreteau.matrix.model.AccountRequest;
import io.github.fherbreteau.matrix.model.AuthMetadata;
import io.github.fherbreteau.matrix.model.AuthenticationApi;
import io.github.fherbreteau.matrix.model.ContentReport;
import io.github.fherbreteau.matrix.model.Credentials;
import io.github.fherbreteau.matrix.model.DeleteDevicesRequest;
import io.github.fherbreteau.matrix.model.Device;
import io.github.fherbreteau.matrix.model.DeviceId;
import io.github.fherbreteau.matrix.model.DeviceSigningUploadRequest;
import io.github.fherbreteau.matrix.model.DeviceSigningUploadResult;
import io.github.fherbreteau.matrix.model.DeviceUpdateRequest;
import io.github.fherbreteau.matrix.model.DevicesResponse;
import io.github.fherbreteau.matrix.model.Direction;
import io.github.fherbreteau.matrix.model.EncryptionRequest;
import io.github.fherbreteau.matrix.model.EventId;
import io.github.fherbreteau.matrix.model.JoinedMembers;
import io.github.fherbreteau.matrix.model.KeyChangesResponse;
import io.github.fherbreteau.matrix.model.KeySignaturesUploadRequest;
import io.github.fherbreteau.matrix.model.KeySignaturesUploadResponse;
import io.github.fherbreteau.matrix.model.KeysClaimRequest;
import io.github.fherbreteau.matrix.model.KeysClaimResponse;
import io.github.fherbreteau.matrix.model.KeysQueryRequest;
import io.github.fherbreteau.matrix.model.KeysQueryResponse;
import io.github.fherbreteau.matrix.model.KeysUploadRequest;
import io.github.fherbreteau.matrix.model.KeysUploadResponse;
import io.github.fherbreteau.matrix.model.MatrixFilter;
import io.github.fherbreteau.matrix.model.MatrixVersions;
import io.github.fherbreteau.matrix.model.MediaDownload;
import io.github.fherbreteau.matrix.model.MediaUploadReservation;
import io.github.fherbreteau.matrix.model.MessageBody;
import io.github.fherbreteau.matrix.model.MutualRoomsResponse;
import io.github.fherbreteau.matrix.model.MxcUri;
import io.github.fherbreteau.matrix.model.PasswordCredentials;
import io.github.fherbreteau.matrix.model.Presence;
import io.github.fherbreteau.matrix.model.PresenceStatus;
import io.github.fherbreteau.matrix.model.PublicRoomsResponse;
import io.github.fherbreteau.matrix.model.ReadMarkers;
import io.github.fherbreteau.matrix.model.RegistrationAvailability;
import io.github.fherbreteau.matrix.model.RegistrationRequest;
import io.github.fherbreteau.matrix.model.RegistrationResponse;
import io.github.fherbreteau.matrix.model.RegistrationTokenValidity;
import io.github.fherbreteau.matrix.model.RelationsOptions;
import io.github.fherbreteau.matrix.model.RelationsResponse;
import io.github.fherbreteau.matrix.model.RoomAlias;
import io.github.fherbreteau.matrix.model.RoomAliasResolution;
import io.github.fherbreteau.matrix.model.RoomCreation;
import io.github.fherbreteau.matrix.model.RoomEvent;
import io.github.fherbreteau.matrix.model.RoomId;
import io.github.fherbreteau.matrix.model.RoomKeyBackupInfo;
import io.github.fherbreteau.matrix.model.RoomKeyBackupKeysResponse;
import io.github.fherbreteau.matrix.model.RoomKeyBackupVersion;
import io.github.fherbreteau.matrix.model.RoomKeyBackupVersionRequest;
import io.github.fherbreteau.matrix.model.RoomKeyBackupWriteResponse;
import io.github.fherbreteau.matrix.model.RoomMessagesPage;
import io.github.fherbreteau.matrix.model.RoomSummary;
import io.github.fherbreteau.matrix.model.RoomTag;
import io.github.fherbreteau.matrix.model.RoomTags;
import io.github.fherbreteau.matrix.model.SearchRequest;
import io.github.fherbreteau.matrix.model.SearchResponse;
import io.github.fherbreteau.matrix.model.Session;
import io.github.fherbreteau.matrix.model.SpaceHierarchyOptions;
import io.github.fherbreteau.matrix.model.SpaceHierarchyResponse;
import io.github.fherbreteau.matrix.model.SyncOptions;
import io.github.fherbreteau.matrix.model.SyncResponse;
import io.github.fherbreteau.matrix.model.ThirdPartyLocations;
import io.github.fherbreteau.matrix.model.ThirdPartyProtocol;
import io.github.fherbreteau.matrix.model.ThirdPartyProtocols;
import io.github.fherbreteau.matrix.model.ThirdPartyUsers;
import io.github.fherbreteau.matrix.model.ThreadsOptions;
import io.github.fherbreteau.matrix.model.ThreadsResponse;
import io.github.fherbreteau.matrix.model.ThreePidResponse;
import io.github.fherbreteau.matrix.model.ThreePidTokenRequest;
import io.github.fherbreteau.matrix.model.ThreePidTokenResponse;
import io.github.fherbreteau.matrix.model.ThumbnailMethod;
import io.github.fherbreteau.matrix.model.UrlPreview;
import io.github.fherbreteau.matrix.model.UserDirectorySearchRequest;
import io.github.fherbreteau.matrix.model.UserDirectorySearchResponse;
import io.github.fherbreteau.matrix.model.UserId;
import io.github.fherbreteau.matrix.model.UserProfile;
import io.github.fherbreteau.matrix.model.WhoamiResponse;
import io.github.fherbreteau.matrix.retry.RequestAttempt;
import io.github.fherbreteau.matrix.retry.RequestObserver;
import io.github.fherbreteau.matrix.retry.RetryPolicy;
import io.github.fherbreteau.matrix.store.SessionStore;
import io.github.fherbreteau.matrix.store.SyncTokenStore;
import io.github.fherbreteau.matrix.store.TransactionIdStore;
import io.github.fherbreteau.matrix.transport.BinaryResponse;
import io.github.fherbreteau.matrix.transport.HttpTransport;
import io.github.fherbreteau.matrix.transport.HttpTransport.Request;
import io.github.fherbreteau.matrix.transport.JdkMediaTransport;
import io.github.fherbreteau.matrix.transport.MediaSizeLimitException;
import io.github.fherbreteau.matrix.transport.MediaTransport;
import io.github.fherbreteau.matrix.transport.MediaTransport.BinaryRequest;
import io.github.fherbreteau.matrix.transport.MediaTransport.StreamingBinaryRequest;
import io.github.fherbreteau.matrix.transport.TransportInterruptedException;
import io.github.fherbreteau.matrix.transport.TransportTimeoutException;
import io.github.fherbreteau.matrix.transport.UncheckedTransportException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * Entry point for talking to a Matrix homeserver.
 *
 * <p>Minimal example:
 *
 * <pre>{@code
 * MatrixClient client = MatrixClient.builder("https://matrix.example.org").build();
 * JsonValue versions = client.getVersions();
 * }</pre>
 *
 * <p>With homeserver discovery ({@code /.well-known/matrix/client}) and capability validation at
 * build time:
 *
 * <pre>{@code
 * MatrixClient client = MatrixClient.builder("https://matrix.example.org")
 *         .discover()
 *         .build();
 * }</pre>
 */
public final class MatrixClient {

  private static final String BEARER_PREFIX = "Bearer ";
  private static final String SYNC_URI = "_matrix/client/v3/sync";
  private static final String M_MISSING_TOKEN = "M_MISSING_TOKEN";
  private static final String NO_SESSION_MESSAGE = "No authenticated session";
  private static final String USER_PATH = "_matrix/client/v3/user/";
  private static final String ACCOUNT_DATA_PATH = "/account_data/"; // NOSONAR
  private static final String USER_ID_FIELD = "user_id";
  private static final String REASON_FIELD = "reason";
  private static final String EVENT_ID_FIELD = "event_id";
  private static final String CHUNK_FIELD = "chunk";
  private static final String CONTENT_TYPE_HEADER = "content-type";
  private static final String CONTENT_DISPOSITION_HEADER = "content-disposition";
  private static final String ROOMS_PATH = "_matrix/client/v3/rooms/";
  private static final String CLIENT_V1_ROOMS_PATH = "_matrix/client/v1/rooms/";
  private static final String DEVICES_PATH = "_matrix/client/v3/devices/";
  private static final String HTTP_DELETE = "DELETE";
  private static final String DIR_QUERY_PARAM = "&dir=";
  private static final String LIMIT_QUERY_PARAM = "&limit=";
  private static final String DIRECTORY_PATH = "_matrix/client/v3/directory/room/";
  private static final String PROFILE_PATH = "_matrix/client/v3/profile/";
  private static final String USERS_PATH = "_matrix/client/v3/users/";
  private static final String USER_ROOMS_SEGMENT = "/rooms/";
  private static final String THIRD_PARTY_PATH = "_matrix/client/v3/thirdparty/";
  private static final String KEYS_PATH = "_matrix/client/v3/keys/";
  private static final String ROOM_KEYS_PATH = "_matrix/client/v3/room_keys/";
  private static final String ROOM_KEY_VERSION_PATH = ROOM_KEYS_PATH + "version";
  private static final char URL_PATH_SEPARATOR = '/';
  private static final String ROOM_KEY_VERSION_ID_PATH = ROOM_KEY_VERSION_PATH + URL_PATH_SEPARATOR;
  private static final String MUTUAL_ROOMS_PATH = "_matrix/client/v1/mutual_rooms";
  private static final String ROOM_ID_FIELD = "room_id";
  private static final String FILE_TYPE = "application/octet-stream";

  private final HttpTransport transport;
  private final String homeserverUrl;
  private final MatrixVersions versions;
  private final DiscoveredHomeserver discovery;
  private final SessionStore sessionStore;
  private final MediaTransport mediaTransport;
  private final long maxMediaUploadBytes;
  private final SyncTokenStore syncTokenStore;
  private final TransactionIdStore transactionIdStore;
  private final RetryPolicy retryPolicy;
  private final RequestObserver requestObserver;
  private final RetrySleeper retrySleeper;

  private MatrixClient(Builder builder) {
    this.transport = builder.transport;
    this.mediaTransport = builder.mediaTransport;
    this.maxMediaUploadBytes =
        builder.maxMediaUploadBytes > 0
            ? builder.maxMediaUploadBytes
            : builder.mediaTransport instanceof JdkMediaTransport jdkMediaTransport
                ? jdkMediaTransport.maxUploadBytes()
                : 0;
    this.sessionStore = builder.sessionStore;
    this.syncTokenStore = builder.syncTokenStore;
    this.transactionIdStore = builder.transactionIdStore;
    this.retryPolicy = builder.retryPolicy;
    this.requestObserver = builder.requestObserver;
    this.retrySleeper = builder.retrySleeper;
    if (builder.discover) {
      this.discovery = HomeserverDiscovery.discover(builder.transport, builder.homeserverUrl);
      if (discovery.outcome() == DiscoveryOutcome.FAIL_PROMPT
          || discovery.outcome() == DiscoveryOutcome.FAIL_ERROR) {
        throw new DiscoveryException(discovery.failureReason());
      }
      this.homeserverUrl = discovery.homeserverUrl();
    } else {
      this.discovery = null;
      this.homeserverUrl = builder.homeserverUrl;
    }
    this.versions =
        builder.validateVersions
            ? MatrixVersions.from(
                fetch(builder.transport, homeserverUrl, "_matrix/client/versions"))
            : null;
  }

  /**
   * Returns a builder for a client pointing at the given homeserver URL.
   *
   * @param homeserverUrl the base URL of the homeserver
   * @return a builder for a client pointing at the homeserver
   * @see <a href="https://spec.matrix.org/latest/client-server-api/#login">Matrix specification</a>
   */
  public static Builder builder(String homeserverUrl) {
    return new Builder(homeserverUrl);
  }

  /**
   * Returns the transport used to reach the homeserver.
   *
   * @return the transport used to reach the homeserver
   * @see <a href="https://spec.matrix.org/latest/client-server-api/#api-standards">Matrix
   *     specification</a>
   */
  public HttpTransport getTransport() {
    return transport;
  }

  /**
   * Returns the base URL of the homeserver this client talks to.
   *
   * @return the base URL of the homeserver
   * @see <a href="https://spec.matrix.org/latest/client-server-api/#server-discovery">Matrix
   *     specification</a>
   */
  public String getHomeserverUrl() {
    return homeserverUrl;
  }

  /**
   * Returns the result of the discovery performed at build time, or {@code null} when discovery was
   * not requested.
   *
   * @return the discovery result, or {@code null} when discovery was not requested
   * @see <a href="https://spec.matrix.org/latest/client-server-api/#server-discovery">Matrix
   *     specification</a>
   */
  public DiscoveredHomeserver getDiscovery() {
    return discovery;
  }

  /**
   * Returns the validated capabilities of the homeserver when version validation was requested at
   * build time, or {@code null} otherwise.
   *
   * @return the validated capabilities, or {@code null} when not validated
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientversions">Matrix
   *     specification</a>
   */
  public MatrixVersions getCapabilities() {
    return versions;
  }

  /**
   * Retrieves the homeserver's supported Matrix spec versions.
   *
   * @return the parsed {@code /versions} response
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientversions">Matrix
   *     specification</a>
   */
  public JsonValue getVersions() {
    return get("_matrix/client/versions");
  }

  /**
   * Retrieves and validates the homeserver's supported Matrix spec versions. Unknown fields (such
   * as {@code unstable_features}) are preserved on the returned {@link MatrixVersions}.
   *
   * @return the validated spec versions and features of the homeserver
   * @throws io.github.fherbreteau.matrix.error.DiscoveryException if the response is malformed
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientversions">Matrix
   *     specification</a>
   */
  public MatrixVersions getSupportedVersions() {
    return MatrixVersions.from(getVersions());
  }

  /**
   * Logs in with the given credentials, stores the resulting session in the session store and
   * returns it.
   *
   * @param credentials the login credentials
   * @return the authenticated session
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if the credentials are
   *     invalid or the account cannot log in
   * @see <a href="https://spec.matrix.org/latest/client-server-api/#login">Matrix specification</a>
   */
  public Session login(Credentials credentials) {
    return login(credentials, null);
  }

  /**
   * Logs in with the given credentials, stores the resulting session in the session store and
   * returns it.
   *
   * @param credentials the login credentials
   * @param requestRefreshToken whether to request a refreshable token
   * @return the authenticated session
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if the credentials are
   *     invalid or the account cannot log in
   * @see <a href="https://spec.matrix.org/latest/client-server-api/#login">Matrix specification</a>
   */
  public Session login(Credentials credentials, boolean requestRefreshToken) {
    return login(credentials, null, requestRefreshToken);
  }

  /**
   * Logs in with the given credentials and an optional device display name, stores the resulting
   * session in the session store and returns it.
   *
   * @param credentials the login credentials
   * @param deviceDisplayName the optional device display name
   * @return the authenticated session
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if the credentials are
   *     invalid or the account cannot log in
   * @see <a href="https://spec.matrix.org/latest/client-server-api/#login">Matrix specification</a>
   */
  public Session login(Credentials credentials, String deviceDisplayName) {
    return login(credentials, deviceDisplayName, false);
  }

  /**
   * Logs in with the given credentials, an optional device display name and an optional request for
   * a refreshable token; stores the resulting session in the session store and returns it.
   *
   * @param credentials the login credentials
   * @param deviceDisplayName the optional device display name
   * @param requestRefreshToken whether to request a refreshable token
   * @return the authenticated session
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if the credentials are
   *     invalid or the account cannot log in
   * @see <a href="https://spec.matrix.org/latest/client-server-api/#login">Matrix specification</a>
   */
  public Session login(
      Credentials credentials, String deviceDisplayName, boolean requestRefreshToken) {
    var body = credentials.toJson().asObject();
    if (deviceDisplayName != null) {
      body.put("initial_device_display_name", deviceDisplayName);
    }
    if (requestRefreshToken) {
      body.put("refresh_token", true);
    }
    try {
      Session session =
          Session.from(post("_matrix/client/v3/login", body), AuthenticationApi.LEGACY);
      sessionStore.save(session);
      return session;
    } catch (RateLimitedException e) {
      throw e;
    } catch (MatrixServerException e) {
      String message = e.getMessage();
      if (credentials instanceof PasswordCredentials(String identifier, String password)) {
        message = message.replace(password, "***");
        message = message.replace(identifier, "***");
      }
      throw new AuthenticationException(e.getErrcode(), message);
    }
  }

  /**
   * Checks whether a username is available for registration.
   *
   * @param username the desired username
   * @return the availability and raw response fields
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3registeravailable">Matrix
   *     specification</a>
   */
  public RegistrationAvailability isUsernameAvailable(String username) {
    return RegistrationAvailability.from(
        get(
            appendQuery(
                "_matrix/client/v3/register/available",
                new JsonObject().put("username", username))));
  }

  /**
   * Checks whether a registration token is currently valid.
   *
   * @param token the registration token
   * @return the validity and raw response fields
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1registermloginregistration_tokenvalidity">Matrix
   *     specification</a>
   */
  public RegistrationTokenValidity isRegistrationTokenValid(String token) {
    return RegistrationTokenValidity.from(
        get(
            appendQuery(
                "_matrix/client/v1/register/m.login.registration_token/validity",
                new JsonObject().put("token", token))));
  }

  /**
   * Registers a user. The request may be repeated with additional UI-auth fields after a {@link
   * MatrixServerException} challenge.
   *
   * @param registration the registration parameters, including any UI-auth response
   * @return the registered user and optional login credentials
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3register">Matrix
   *     specification</a>
   */
  public RegistrationResponse register(RegistrationRequest registration) {
    return register(registration, null);
  }

  /**
   * Registers a user with the specified account kind.
   *
   * @param registration the registration parameters, including any UI-auth response
   * @param kind account kind, either {@code user} or {@code guest}, or {@code null} for the default
   * @return the registered user and optional login credentials
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3register">Matrix
   *     specification</a>
   */
  public RegistrationResponse register(RegistrationRequest registration, String kind) {
    String path = "_matrix/client/v3/register";
    if (kind != null) {
      if (!"user".equals(kind) && !"guest".equals(kind)) {
        throw new IllegalArgumentException("kind must be user or guest");
      }
      path = appendQuery(path, new JsonObject().put("kind", kind));
    }
    RegistrationResponse response = RegistrationResponse.from(post(path, registration.toJson()));
    if (response.accessToken() != null) {
      sessionStore.save(Session.from(response.raw(), AuthenticationApi.LEGACY));
    }
    return response;
  }

  /**
   * Retrieves OAuth authentication and account-management metadata.
   *
   * @return the metadata, preserving unknown fields
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1auth_metadata">Matrix
   *     specification</a>
   */
  public AuthMetadata getAuthMetadata() {
    return AuthMetadata.from(get("_matrix/client/v1/auth_metadata"));
  }

  /**
   * Returns the OAuth account-management URL when advertised by the homeserver.
   *
   * @return the account-management URL, or {@code null} if it is not advertised
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1auth_metadata">Matrix
   *     specification</a>
   */
  public String getAccountManagementUri() {
    JsonValue value = getAuthMetadata().get("account_management_uri");
    return value != null && value.isString() ? value.asString() : null;
  }

  /**
   * Changes the current account password using User-Interactive Authentication.
   *
   * @param request the new password and UI-auth response fields
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3accountpassword">Matrix
   *     specification</a>
   */
  public void changePassword(AccountRequest request) {
    ensureLegacyAccountApi();
    authenticatedUiAuth("POST", "_matrix/client/v3/account/password", request.toJson());
  }

  /**
   * Deactivates the current account using User-Interactive Authentication.
   *
   * @param request the deactivation and UI-auth fields
   * @return the identity-server unbind result and raw response
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3accountdeactivate">Matrix
   *     specification</a>
   */
  public AccountOperationResponse deactivateAccount(AccountRequest request) {
    ensureLegacyAccountApi();
    AccountOperationResponse response =
        AccountOperationResponse.from(
            authenticatedUiAuth("POST", "_matrix/client/v3/account/deactivate", request.toJson()));
    sessionStore.clear();
    syncTokenStore.clear();
    return response;
  }

  /**
   * Returns third-party identifiers associated with the current account.
   *
   * @return the associated identifiers and raw fields
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3account3pid">Matrix
   *     specification</a>
   */
  public ThreePidResponse getThreePids() {
    ensureLegacyAccountApi();
    return ThreePidResponse.from(authenticated("GET", "_matrix/client/v3/account/3pid", null));
  }

  /**
   * Adds a previously validated third-party identifier to the current account.
   *
   * @param request the identifier-verification session and UI-auth fields
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3account3pidadd">Matrix
   *     specification</a>
   */
  public void addThreePid(AccountRequest request) {
    ensureLegacyAccountApi();
    authenticatedUiAuth("POST", "_matrix/client/v3/account/3pid/add", request.toJson());
  }

  /**
   * Binds an identifier to an identity server.
   *
   * @param request the identifier-verification and identity-server fields
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3account3pidbind">Matrix
   *     specification</a>
   */
  public void bindThreePid(AccountRequest request) {
    ensureLegacyAccountApi();
    authenticated("POST", "_matrix/client/v3/account/3pid/bind", request.toJson());
  }

  /**
   * Removes a third-party identifier from the current account.
   *
   * @param request the identifier and optional identity-server fields
   * @return the identity-server unbind result and raw response
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3account3piddelete">Matrix
   *     specification</a>
   */
  public AccountOperationResponse deleteThreePid(AccountRequest request) {
    ensureLegacyAccountApi();
    return AccountOperationResponse.from(
        authenticated("POST", "_matrix/client/v3/account/3pid/delete", request.toJson()));
  }

  /**
   * Removes an identifier binding from an identity server without removing its homeserver
   * association.
   *
   * @param request the identifier and optional identity-server fields
   * @return the identity-server unbind result and raw response
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3account3pidunbind">Matrix
   *     specification</a>
   */
  public AccountOperationResponse unbindThreePid(AccountRequest request) {
    ensureLegacyAccountApi();
    return AccountOperationResponse.from(
        authenticated("POST", "_matrix/client/v3/account/3pid/unbind", request.toJson()));
  }

  /**
   * Requests an email verification token for registration.
   *
   * @param request the email token request parameters
   * @return the session ID and optional submit URL
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3registeremailrequesttoken">Matrix
   *     specification</a>
   */
  public ThreePidTokenResponse requestRegistrationEmailToken(ThreePidTokenRequest request) {
    if (sessionStore.current().filter(Session::usesOauth).isPresent()) {
      throw new UnsupportedOperationException(
          "Registration token requests require the legacy authentication API");
    }
    return ThreePidTokenResponse.from(
        post("_matrix/client/v3/register/email/requestToken", request.toJson()));
  }

  /**
   * Requests an MSISDN verification token for registration.
   *
   * @param request the phone token request parameters
   * @return the session ID and optional submit URL
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3registermsisdnrequesttoken">Matrix
   *     specification</a>
   */
  public ThreePidTokenResponse requestRegistrationMsisdnToken(ThreePidTokenRequest request) {
    if (sessionStore.current().filter(Session::usesOauth).isPresent()) {
      throw new UnsupportedOperationException(
          "Registration token requests require the legacy authentication API");
    }
    return ThreePidTokenResponse.from(
        post("_matrix/client/v3/register/msisdn/requestToken", request.toJson()));
  }

  /**
   * Requests a token to add an email address to the current account.
   *
   * @param request the email token request parameters
   * @return the session ID and optional submit URL
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3account3pidemailrequesttoken">Matrix
   *     specification</a>
   */
  public ThreePidTokenResponse requestThreePidEmailToken(ThreePidTokenRequest request) {
    ensureLegacyAccountApi();
    return ThreePidTokenResponse.from(
        post("_matrix/client/v3/account/3pid/email/requestToken", request.toJson()));
  }

  /**
   * Requests a token to add an MSISDN to the current account.
   *
   * @param request the phone token request parameters
   * @return the session ID and optional submit URL
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3account3pidmsisdnrequesttoken">Matrix
   *     specification</a>
   */
  public ThreePidTokenResponse requestThreePidMsisdnToken(ThreePidTokenRequest request) {
    ensureLegacyAccountApi();
    return ThreePidTokenResponse.from(
        post("_matrix/client/v3/account/3pid/msisdn/requestToken", request.toJson()));
  }

  /**
   * Requests an email token to reset the current account password.
   *
   * @param request the email token request parameters
   * @return the session ID and optional submit URL
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3accountpasswordemailrequesttoken">Matrix
   *     specification</a>
   */
  public ThreePidTokenResponse requestPasswordResetEmailToken(ThreePidTokenRequest request) {
    ensureLegacyAccountApi();
    return ThreePidTokenResponse.from(
        post("_matrix/client/v3/account/password/email/requestToken", request.toJson()));
  }

  /**
   * Requests an MSISDN token to reset the current account password.
   *
   * @param request the phone token request parameters
   * @return the session ID and optional submit URL
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3accountpasswordmsisdnrequesttoken">Matrix
   *     specification</a>
   */
  public ThreePidTokenResponse requestPasswordResetMsisdnToken(ThreePidTokenRequest request) {
    ensureLegacyAccountApi();
    return ThreePidTokenResponse.from(
        post("_matrix/client/v3/account/password/msisdn/requestToken", request.toJson()));
  }

  /**
   * Lists the devices registered for the current user.
   *
   * @return the devices and the raw response
   * @throws AuthenticationException if there is no session or the token is invalid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3devices">Matrix
   *     specification</a>
   */
  public DevicesResponse getDevices() {
    return DevicesResponse.from(authenticated("GET", "_matrix/client/v3/devices", null));
  }

  /**
   * Retrieves a device registered for the current user.
   *
   * @param deviceId the device identifier
   * @return the device and raw response
   * @throws AuthenticationException if there is no session or the token is invalid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3devicesdeviceid">Matrix
   *     specification</a>
   */
  public Device getDevice(DeviceId deviceId) {
    return Device.from(authenticated("GET", DEVICES_PATH + encode(deviceId.value()), null));
  }

  /**
   * Updates an existing device's optional metadata.
   *
   * @param deviceId the device identifier
   * @param update the fields to update
   * @throws AuthenticationException if there is no session or the token is invalid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#put_matrixclientv3devicesdeviceid">Matrix
   *     specification</a>
   */
  public void updateDevice(DeviceId deviceId, DeviceUpdateRequest update) {
    authenticated("PUT", DEVICES_PATH + encode(deviceId.value()), update.toJson());
  }

  /**
   * Deletes one device using User-Interactive Authentication when required. OAuth-issued sessions
   * must instead direct the user to the account-management UI.
   *
   * @param deviceId the device identifier
   * @throws AuthenticationException if there is no session or the token is invalid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#delete_matrixclientv3devicesdeviceid">Matrix
   *     specification</a>
   */
  public void deleteDevice(DeviceId deviceId) {
    deleteDevice(deviceId, null);
  }

  /**
   * Deletes one device, including the authentication response for a UI-auth retry when needed.
   *
   * @param deviceId the device identifier
   * @param auth the optional UI-auth response
   * @throws AuthenticationException if there is no session or the token is invalid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#delete_matrixclientv3devicesdeviceid">Matrix
   *     specification</a>
   */
  public void deleteDevice(DeviceId deviceId, JsonValue auth) {
    ensureDeviceDeletionSupportedBySession();
    JsonObject body = auth == null ? null : new JsonObject().put("auth", auth);
    authenticatedUiAuth(HTTP_DELETE, DEVICES_PATH + encode(deviceId.value()), body);
  }

  /**
   * Deletes multiple devices using User-Interactive Authentication when required.
   *
   * @param request the device IDs and optional UI-auth response
   * @throws AuthenticationException if there is no session or the token is invalid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3delete_devices">Matrix
   *     specification</a>
   */
  public void deleteDevices(DeleteDevicesRequest request) {
    ensureDeviceDeletionSupportedBySession();
    authenticatedUiAuth("POST", "_matrix/client/v3/delete_devices", request.toJson());
  }

  /**
   * Publishes device identity, one-time, and fallback keys.
   *
   * @param request raw upload payload
   * @return counts of unclaimed one-time keys by algorithm
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     its token is invalid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3keysupload">Matrix
   *     specification</a>
   */
  public KeysUploadResponse uploadKeys(KeysUploadRequest request) {
    return KeysUploadResponse.from(authenticated("POST", KEYS_PATH + "upload", request.toJson()));
  }

  /**
   * Queries device and cross-signing keys for users and devices.
   *
   * @param request user-to-device query and optional remote timeout
   * @return raw key maps, failures, and extension fields
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     its token is invalid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3keysquery">Matrix
   *     specification</a>
   */
  public KeysQueryResponse queryKeys(KeysQueryRequest request) {
    return KeysQueryResponse.from(authenticated("POST", KEYS_PATH + "query", request.toJson()));
  }

  /**
   * Claims one-time or fallback keys for devices.
   *
   * @param request user/device/algorithm claims and optional remote timeout
   * @return claimed keys and remote failures without transforming key material
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     its token is invalid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3keysclaim">Matrix
   *     specification</a>
   */
  public KeysClaimResponse claimKeys(KeysClaimRequest request) {
    return KeysClaimResponse.from(authenticated("POST", KEYS_PATH + "claim", request.toJson()));
  }

  /**
   * Gets users whose device identity keys changed between two sync tokens.
   *
   * @param from earlier sync token
   * @param to later sync token
   * @return users whose keys changed or who left
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     its token is invalid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3keyschanges">Matrix
   *     specification</a>
   */
  public KeyChangesResponse getKeyChanges(String from, String to) {
    JsonObject query = new JsonObject().put("from", from).put("to", to);
    return KeyChangesResponse.from(
        authenticated("GET", appendQuery(KEYS_PATH + "changes", query), null));
  }

  /**
   * Uploads cross-signing keys. UIA challenges are surfaced through thrown Matrix server errors.
   *
   * @param request raw cross-signing keys and optional UIA response
   * @return successful upload response
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     its token is invalid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3keysdevice_signingupload">Matrix
   *     specification</a>
   */
  public DeviceSigningUploadResult uploadDeviceSigningKeys(DeviceSigningUploadRequest request) {
    return DeviceSigningUploadResult.from(
        authenticated("POST", KEYS_PATH + "device_signing/upload", request.toJson()));
  }

  /**
   * Retries cross-signing key upload with explicit UIA fields.
   *
   * @param request raw cross-signing keys and UIA response
   * @return successful upload response
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     its token is invalid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3keysdevice_signingupload">Matrix
   *     specification</a>
   */
  public DeviceSigningUploadResult uploadDeviceSigningKeysWithAuth(
      DeviceSigningUploadRequest request) {
    return DeviceSigningUploadResult.from(
        authenticatedUiAuth("POST", KEYS_PATH + "device_signing/upload", request.toJson()));
  }

  /**
   * Uploads signatures for devices and cross-signing keys.
   *
   * @param request nested user/key/signature JSON map
   * @return per-key failures and the raw response
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     its token is invalid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3keyssignaturesupload">Matrix
   *     specification</a>
   */
  public KeySignaturesUploadResponse uploadKeySignatures(KeySignaturesUploadRequest request) {
    return KeySignaturesUploadResponse.from(
        authenticated("POST", KEYS_PATH + "signatures/upload", request.toJson()));
  }

  /**
   * Creates a backup-version operation.
   *
   * @return current backup version metadata
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     its token is invalid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3room_keysversion">Matrix
   *     specification</a>
   */
  public RoomKeyBackupInfo getRoomKeyBackupVersion() {
    String path = ROOM_KEY_VERSION_PATH;
    return RoomKeyBackupInfo.from(authenticated("GET", path, null));
  }

  /**
   * Creates a room-key backup version.
   *
   * @param request algorithm and algorithm-specific authentication data
   * @return opaque backup version identifier
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     its token is invalid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3room_keysversion">Matrix
   *     specification</a>
   */
  public RoomKeyBackupVersion createRoomKeyBackupVersion(RoomKeyBackupVersionRequest request) {
    return RoomKeyBackupVersion.from(
        authenticated("POST", ROOM_KEY_VERSION_PATH, request.toJson()));
  }

  /**
   * Retrieves one specific room-key backup version.
   *
   * @param versionId opaque backup version identifier
   * @return backup metadata
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     its token is invalid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3room_keysversionversion">Matrix
   *     specification</a>
   */
  public RoomKeyBackupInfo getRoomKeyBackupVersionById(String versionId) {
    return RoomKeyBackupInfo.from(
        authenticated("GET", ROOM_KEY_VERSION_ID_PATH + encode(versionId), null));
  }

  /**
   * Updates authentication data of a specific backup version.
   *
   * @param versionId opaque backup version identifier
   * @param request backup algorithm and authentication data, optionally including matching version
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     its token is invalid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#put_matrixclientv3room_keysversionversion">Matrix
   *     specification</a>
   */
  public void updateRoomKeyBackupVersion(String versionId, RoomKeyBackupVersionRequest request) {
    if (request.version() != null && !request.version().equals(versionId)) {
      throw new IllegalArgumentException("backup body version must match path version");
    }
    JsonObject body = request.toJson();
    body.put("version", versionId);
    authenticated("PUT", ROOM_KEY_VERSION_ID_PATH + encode(versionId), body);
  }

  /**
   * Deletes a specific room-key backup version.
   *
   * @param versionId opaque backup version identifier
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     its token is invalid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#delete_matrixclientv3room_keysversionversion">Matrix
   *     specification</a>
   */
  public void deleteRoomKeyBackupVersion(String versionId) {
    authenticated(HTTP_DELETE, ROOM_KEY_VERSION_ID_PATH + encode(versionId), null);
  }

  /**
   * Retrieves all backed-up room sessions for a version.
   *
   * @param versionId opaque backup version identifier
   * @return raw rooms map and complete response
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     its token is invalid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3room_keyskeys">Matrix
   *     specification</a>
   */
  public RoomKeyBackupKeysResponse getRoomKeyBackup(String versionId) {
    return RoomKeyBackupKeysResponse.from(
        authenticated("GET", roomKeysPath(versionId, null, null), null));
  }

  /**
   * Uploads room-key sessions for all rooms into a backup version.
   *
   * @param versionId opaque backup version identifier
   * @param request raw room/session key JSON
   * @return current key count and etag
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     its token is invalid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#put_matrixclientv3room_keyskeys">Matrix
   *     specification</a>
   */
  public RoomKeyBackupWriteResponse uploadRoomKeyBackup(
      String versionId, EncryptionRequest request) {
    return RoomKeyBackupWriteResponse.from(
        authenticated("PUT", roomKeysPath(versionId, null, null), request.toJson()));
  }

  /**
   * Deletes all backed-up keys in a version.
   *
   * @param versionId opaque backup version identifier
   * @return current key count and etag
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     its token is invalid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#delete_matrixclientv3room_keyskeys">Matrix
   *     specification</a>
   */
  public RoomKeyBackupWriteResponse deleteRoomKeyBackup(String versionId) {
    return RoomKeyBackupWriteResponse.from(
        authenticated(HTTP_DELETE, roomKeysPath(versionId, null, null), null));
  }

  /**
   * Retrieves backed-up sessions for a single room.
   *
   * @param versionId opaque backup version identifier
   * @param roomId room identifier
   * @return raw sessions map and complete response
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     its token is invalid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3room_keyskeysroomid">Matrix
   *     specification</a>
   */
  public RoomKeyBackupKeysResponse getRoomKeyBackupForRoom(String versionId, RoomId roomId) {
    return RoomKeyBackupKeysResponse.from(
        authenticated("GET", roomKeysPath(versionId, roomId, null), null));
  }

  /**
   * Uploads backed-up sessions for a single room.
   *
   * @param versionId opaque backup version identifier
   * @param roomId room identifier
   * @param request raw session-map JSON
   * @return current key count and etag
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     its token is invalid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#put_matrixclientv3room_keyskeysroomid">Matrix
   *     specification</a>
   */
  public RoomKeyBackupWriteResponse uploadRoomKeyBackupForRoom(
      String versionId, RoomId roomId, EncryptionRequest request) {
    return RoomKeyBackupWriteResponse.from(
        authenticated("PUT", roomKeysPath(versionId, roomId, null), request.toJson()));
  }

  /**
   * Deletes backed-up sessions for one room.
   *
   * @param versionId opaque backup version identifier
   * @param roomId room identifier
   * @return current key count and etag
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     its token is invalid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#delete_matrixclientv3room_keyskeysroomid">Matrix
   *     specification</a>
   */
  public RoomKeyBackupWriteResponse deleteRoomKeyBackupForRoom(String versionId, RoomId roomId) {
    return RoomKeyBackupWriteResponse.from(
        authenticated(HTTP_DELETE, roomKeysPath(versionId, roomId, null), null));
  }

  /**
   * Retrieves one backed-up Megolm session.
   *
   * @param versionId opaque backup version identifier
   * @param roomId room identifier
   * @param sessionId opaque session identifier
   * @return validated session key data and complete response
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     its token is invalid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3room_keyskeysroomidsessionid">Matrix
   *     specification</a>
   */
  public RoomKeyBackupKeysResponse getRoomKeyBackupSession(
      String versionId, RoomId roomId, String sessionId) {
    return RoomKeyBackupKeysResponse.from(
        authenticated("GET", roomKeysPath(versionId, roomId, sessionId), null));
  }

  /**
   * Uploads one backed-up Megolm session.
   *
   * @param versionId opaque backup version identifier
   * @param roomId room identifier
   * @param sessionId opaque session identifier
   * @param request raw session key JSON object
   * @return current key count and etag
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     its token is invalid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#put_matrixclientv3room_keyskeysroomidsessionid">Matrix
   *     specification</a>
   */
  public RoomKeyBackupWriteResponse uploadRoomKeyBackupSession(
      String versionId, RoomId roomId, String sessionId, EncryptionRequest request) {
    return RoomKeyBackupWriteResponse.from(
        authenticated("PUT", roomKeysPath(versionId, roomId, sessionId), request.toJson()));
  }

  /**
   * Deletes one backed-up Megolm session.
   *
   * @param versionId opaque backup version identifier
   * @param roomId room identifier
   * @param sessionId opaque session identifier
   * @return current key count and etag
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     its token is invalid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#delete_matrixclientv3room_keyskeysroomidsessionid">Matrix
   *     specification</a>
   */
  public RoomKeyBackupWriteResponse deleteRoomKeyBackupSession(
      String versionId, RoomId roomId, String sessionId) {
    return RoomKeyBackupWriteResponse.from(
        authenticated(HTTP_DELETE, roomKeysPath(versionId, roomId, sessionId), null));
  }

  private static String roomKeysPath(String version, RoomId roomId, String sessionId) {
    StringBuilder path = new StringBuilder(ROOM_KEYS_PATH).append("keys");
    if (roomId != null) {
      path.append('/').append(encode(roomId.value()));
      if (sessionId != null) {
        path.append('/').append(encode(sessionId));
      }
    }
    path.append("?version=").append(encode(version));
    return path.toString();
  }

  /**
   * Returns whether the authenticated session was obtained through the OAuth API.
   *
   * @return {@code true} when the current session is OAuth-issued
   * @throws AuthenticationException if there is no authenticated session
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#authentication-api-discovery">Matrix
   *     specification</a>
   */
  public boolean usesOauthSession() {
    return sessionStore
        .current()
        .orElseThrow(() -> new AuthenticationException(M_MISSING_TOKEN, NO_SESSION_MESSAGE))
        .usesOauth();
  }

  /**
   * Returns OAuth authentication metadata, or empty when the homeserver does not support OAuth. A
   * 404 response with {@code M_UNRECOGNIZED} means OAuth is unsupported.
   *
   * @return OAuth metadata when supported
   * @throws MatrixServerException if metadata retrieval fails for another reason
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1auth_metadata">Matrix
   *     specification</a>
   */
  public Optional<AuthMetadata> findAuthMetadata() {
    try {
      return Optional.of(getAuthMetadata());
    } catch (MatrixServerException exception) {
      if (exception.getStatusCode() == 404 && "M_UNRECOGNIZED".equals(exception.getErrcode())) {
        return Optional.empty();
      }
      throw exception;
    }
  }

  /**
   * Returns the current authenticated session, if any.
   *
   * @return the current authenticated session, if any
   * @see <a href="https://spec.matrix.org/latest/client-server-api/#login">Matrix specification</a>
   */
  public Optional<Session> getSession() {
    return sessionStore.current();
  }

  /**
   * Renews the current session with its refresh token, replacing the stored session.
   *
   * @return the refreshed session
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session, the
   *     session is not refreshable, or the refresh token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#refreshing-access-tokens">Matrix
   *     specification</a>
   */
  public Session refresh() {
    Session session =
        sessionStore
            .current()
            .orElseThrow(() -> new AuthenticationException(M_MISSING_TOKEN, NO_SESSION_MESSAGE));
    if (!session.isRefreshable()) {
      throw new AuthenticationException(M_MISSING_TOKEN, "Session is not refreshable");
    }
    try {
      Session refreshed =
          Session.fromRefresh(
              session,
              post(
                  "_matrix/client/v3/refresh",
                  new JsonObject().put("refresh_token", session.refreshToken())));
      sessionStore.save(refreshed);
      return refreshed;
    } catch (RateLimitedException e) {
      throw e;
    } catch (MatrixServerException e) {
      throw new AuthenticationException(e.getErrcode(), e.getMessage());
    }
  }

  /**
   * Invalidates the current access token server-side and clears the stored session.
   *
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3logout">Matrix
   *     specification</a>
   */
  public void logout() {
    authenticated("POST", "_matrix/client/v3/logout", null);
    sessionStore.clear();
    syncTokenStore.clear();
  }

  /**
   * Invalidates every access token issued for this user and clears the stored session.
   *
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3logout">Matrix
   *     specification</a>
   */
  public void logoutAll() {
    authenticated("POST", "_matrix/client/v3/logout/all", null);
    sessionStore.clear();
    syncTokenStore.clear();
  }

  /**
   * Creates a room with default settings and returns its identifier.
   *
   * @return the identifier of the created room
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3createroom">Matrix
   *     specification</a>
   */
  public RoomId createRoom() {
    return createRoom(RoomCreation.builder().build());
  }

  /**
   * Creates a room with the given parameters and returns its identifier.
   *
   * @param creation the room creation parameters
   * @return the identifier of the created room
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3createroom">Matrix
   *     specification</a>
   */
  public RoomId createRoom(RoomCreation creation) {
    return RoomId.of(
        authenticated("POST", "_matrix/client/v3/createRoom", creation.toJson())
            .asObject()
            .get(ROOM_ID_FIELD)
            .asString());
  }

  /**
   * Joins a room by its identifier and returns the joined room.
   *
   * @param roomId the room to join
   * @return the joined room identifier
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a href="https://spec.matrix.org/latest/client-server-api/#joining-rooms">Matrix
   *     specification</a>
   */
  public RoomId joinRoom(RoomId roomId) {
    return joinRoom(roomId.value());
  }

  /**
   * Joins a room by one of its aliases and returns the joined room.
   *
   * @param roomAlias the room alias to join
   * @return the joined room identifier
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a href="https://spec.matrix.org/latest/client-server-api/#joining-rooms">Matrix
   *     specification</a>
   */
  public RoomId joinRoom(RoomAlias roomAlias) {
    return joinRoom(roomAlias.value());
  }

  private RoomId joinRoom(String roomAliasOrId) {
    return RoomId.of(
        authenticated("POST", "_matrix/client/v3/join/" + encode(roomAliasOrId), new JsonObject())
            .asObject()
            .get(ROOM_ID_FIELD)
            .asString());
  }

  /**
   * Leaves a room.
   *
   * @param roomId the room to leave
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a href="https://spec.matrix.org/latest/client-server-api/#leaving-rooms">Matrix
   *     specification</a>
   */
  public void leaveRoom(RoomId roomId) {
    authenticated("POST", ROOMS_PATH + encode(roomId.value()) + "/leave", new JsonObject());
  }

  /**
   * Invites a user to a room.
   *
   * @param roomId the room to invite the user to
   * @param userId the user to invite
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3roomsroomidinvite">Matrix
   *     specification</a>
   */
  public void invite(RoomId roomId, UserId userId) {
    authenticated(
        "POST",
        ROOMS_PATH + encode(roomId.value()) + "/invite",
        new JsonObject().put(USER_ID_FIELD, userId.value()));
  }

  /**
   * Retrieves the full state of a room. Unknown event types are preserved.
   *
   * @param roomId the room whose state to retrieve
   * @return the full room state, preserving unknown event types
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3roomsroomidstateeventtypestatekey">Matrix
   *     specification</a>
   */
  public List<RoomEvent> getRoomState(RoomId roomId) {
    JsonValue response = authenticated("GET", ROOMS_PATH + encode(roomId.value()) + "/state", null);
    var events = new ArrayList<RoomEvent>();
    if (response.isArray()) {
      for (int i = 0; i < response.asArray().size(); i++) {
        events.add(RoomEvent.from(response.asArray().get(i)));
      }
    }
    return events;
  }

  /**
   * Retrieves the members currently joined to a room.
   *
   * @param roomId the room whose members to retrieve
   * @return the joined members with their display names
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3roomsroomidjoined_members">Matrix
   *     specification</a>
   */
  public JoinedMembers getJoinedMembers(RoomId roomId) {
    return JoinedMembers.from(
        authenticated("GET", ROOMS_PATH + encode(roomId.value()) + "/joined_members", null));
  }

  /**
   * Retrieves the {@code m.room.name} state of a room, if set.
   *
   * @param roomId the room whose name to retrieve
   * @return the room name, or empty when unset
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3roomsroomidstateeventtypestatekey">Matrix
   *     specification</a>
   */
  public Optional<String> getRoomName(RoomId roomId) {
    return getRoomStateField(roomId, "m.room.name", "name");
  }

  /**
   * Retrieves the {@code m.room.canonical_alias} state of a room, if set.
   *
   * @param roomId the room whose canonical alias to retrieve
   * @return the canonical alias, or empty when unset
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3roomsroomidstateeventtypestatekey">Matrix
   *     specification</a>
   */
  public Optional<RoomAlias> getCanonicalAlias(RoomId roomId) {
    return getRoomStateField(roomId, "m.room.canonical_alias", "alias").map(RoomAlias::of);
  }

  /**
   * Resolves a room alias to its room identifier and candidate servers.
   *
   * @param roomAlias the alias to resolve
   * @return the room identifier and candidate servers
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3directoryroomroomalias">Matrix
   *     specification</a>
   */
  public RoomAliasResolution resolveRoomAlias(RoomAlias roomAlias) {
    return RoomAliasResolution.from(get(DIRECTORY_PATH + encode(roomAlias.value())));
  }

  /**
   * Removes a user from a room; the user must not be invited by themselves.
   *
   * @param roomId the room to remove the user from
   * @param userId the user to remove
   * @param reason the optional reason for the removal
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3roomsroomidkick">Matrix
   *     specification</a>
   */
  public void kick(RoomId roomId, UserId userId, String reason) {
    var body = new JsonObject().put(USER_ID_FIELD, userId.value());
    if (reason != null) {
      body.put(REASON_FIELD, reason);
    }
    authenticated("POST", ROOMS_PATH + encode(roomId.value()) + "/kick", body);
  }

  /**
   * Bans a user from a room, preventing them from joining it.
   *
   * @param roomId the room to ban the user from
   * @param userId the user to ban
   * @param reason the optional reason for the ban
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3roomsroomidban">Matrix
   *     specification</a>
   */
  public void ban(RoomId roomId, UserId userId, String reason) {
    var body = new JsonObject().put(USER_ID_FIELD, userId.value());
    if (reason != null) {
      body.put(REASON_FIELD, reason);
    }
    authenticated("POST", ROOMS_PATH + encode(roomId.value()) + "/ban", body);
  }

  /**
   * Removes a previous ban on a user so they can join the room again.
   *
   * @param roomId the room to unban the user from
   * @param userId the user to unban
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3roomsroomidunban">Matrix
   *     specification</a>
   */
  public void unban(RoomId roomId, UserId userId) {
    authenticated(
        "POST",
        ROOMS_PATH + encode(roomId.value()) + "/unban",
        new JsonObject().put(USER_ID_FIELD, userId.value()));
  }

  /**
   * Forgets a room the user has left; the room is removed from the room list.
   *
   * @param roomId the room to forget
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3roomsroomidforget">Matrix
   *     specification</a>
   */
  public void forget(RoomId roomId) {
    authenticated("POST", ROOMS_PATH + encode(roomId.value()) + "/forget", new JsonObject());
  }

  /**
   * Retrieves the membership events of a room.
   *
   * @param roomId the room whose members to retrieve
   * @return the membership events of the room
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3roomsroomidmembers">Matrix
   *     specification</a>
   */
  public List<RoomEvent> getMembers(RoomId roomId) {
    JsonValue response =
        authenticated("GET", ROOMS_PATH + encode(roomId.value()) + "/members", null);
    var members = new ArrayList<RoomEvent>();
    JsonValue chunkValue = response.asObject().get(CHUNK_FIELD);
    if (chunkValue != null && chunkValue.isArray()) {
      var chunk = chunkValue.asArray();
      for (int i = 0; i < chunk.size(); i++) {
        members.add(RoomEvent.from(chunk.get(i)));
      }
    }
    return members;
  }

  /**
   * Retrieves the identifiers of all rooms the current user is joined to.
   *
   * @return the joined room identifiers
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3joined_rooms">Matrix
   *     specification</a>
   */
  public List<RoomId> getJoinedRooms() {
    JsonValue response = authenticated("GET", "_matrix/client/v3/joined_rooms", null);
    var rooms = new ArrayList<RoomId>();
    JsonValue joined = response.asObject().get("joined_rooms");
    if (joined != null && joined.isArray()) {
      var joinedArray = joined.asArray();
      for (int i = 0; i < joinedArray.size(); i++) {
        rooms.add(RoomId.of(joinedArray.get(i).asString()));
      }
    }
    return rooms;
  }

  /**
   * Sends a plain-text message to a room with a generated transaction identifier.
   *
   * @param roomId the room to send the message to
   * @param text the plain-text body of the message
   * @return the created event identifier
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#put_matrixclientv3roomsroomidsendeventtypetxnid">Matrix
   *     specification</a>
   */
  public EventId sendText(RoomId roomId, String text) {
    return sendMessageEvent(roomId, "m.room.message", MessageBody.text(text).toJson());
  }

  /**
   * Sends a message built from the given body, with a generated transaction identifier.
   *
   * @param roomId the room to send the message to
   * @param message the message body
   * @return the created event identifier
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#put_matrixclientv3roomsroomidsendeventtypetxnid">Matrix
   *     specification</a>
   */
  public EventId sendMessage(RoomId roomId, MessageBody message) {
    return sendMessageEvent(roomId, "m.room.message", message.toJson());
  }

  /**
   * Retrieves a page of room history starting from the given pagination token. The chunk order is
   * homeserver-defined; use the returned tokens to continue paginating.
   *
   * @param roomId the room whose history to retrieve
   * @param from the token to start from, as returned in a previous page
   * @param direction the direction to walk, towards older or newer events
   * @param limit the maximum number of events per page, or a non-positive value to let the
   *     homeserver decide
   * @return the requested page of history
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3roomsroomidmessages">Matrix
   *     specification</a>
   */
  public RoomMessagesPage getRoomMessages(
      RoomId roomId, String from, Direction direction, long limit) {
    return getRoomMessages(roomId, from, null, direction, limit, null);
  }

  /**
   * Retrieves a page of room history with an optional stop token and event filter. The chunk order
   * is homeserver-defined; use the returned tokens to continue paginating.
   *
   * @param roomId the room whose history to retrieve
   * @param from the token to start from, as returned in a previous page or by {@code /sync}
   * @param to the token to stop at, or {@code null} for none
   * @param direction the direction to walk, towards older or newer events
   * @param limit the maximum number of events per page, or a non-positive value to let the
   *     homeserver decide
   * @param filter the room event filter, or {@code null} for none
   * @return the requested page of history
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3roomsroomidmessages">Matrix
   *     specification</a>
   */
  public RoomMessagesPage getRoomMessages(
      RoomId roomId, String from, String to, Direction direction, long limit, JsonValue filter) {
    var query =
        new StringBuilder(ROOMS_PATH)
            .append(encode(roomId.value()))
            .append("/messages?from=")
            .append(encode(from))
            .append(DIR_QUERY_PARAM)
            .append(direction.value());
    if (to != null) {
      query.append("&to=").append(encode(to));
    }
    if (limit > 0) {
      query.append(LIMIT_QUERY_PARAM).append(limit);
    }
    if (filter != null) {
      query.append("&filter=").append(encode(filter.toJson()));
    }
    return RoomMessagesPage.from(authenticated("GET", query.toString(), null));
  }

  /**
   * Retrieves a single event of a room by its identifier.
   *
   * @param roomId the room containing the event
   * @param eventId the event to retrieve
   * @return the event
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3roomsroomideventeventid">Matrix
   *     specification</a>
   */
  public RoomEvent getRoomEvent(RoomId roomId, EventId eventId) {
    return RoomEvent.from(
        authenticated(
            "GET",
            ROOMS_PATH + encode(roomId.value()) + "/event/" + encode(eventId.value()),
            null));
  }

  /**
   * Retrieves the identifier of the event closest to the given timestamp, per the {@code
   * timestamp_to_event} endpoint. The homeserver may be rate-limiting this call.
   *
   * @param roomId the room to search
   * @param timestamp the timestamp to look up, in milliseconds since the Unix epoch
   * @param direction the direction to search towards from the timestamp
   * @return the closest event identifier and its server timestamp
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1roomsroomidtimestamp_to_event">Matrix
   *     specification</a>
   */
  public RoomEvent getEventForTimestamp(RoomId roomId, long timestamp, Direction direction) {
    JsonValue response =
        authenticated(
            "GET",
            CLIENT_V1_ROOMS_PATH
                + encode(roomId.value())
                + "/timestamp_to_event?ts="
                + timestamp
                + DIR_QUERY_PARAM
                + direction.value(),
            null);
    return RoomEvent.from(response);
  }

  /**
   * Retrieves the local aliases of a room declared in the room directory.
   *
   * @param roomId the room whose aliases to retrieve
   * @return the aliases of the room, possibly empty
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3roomsroomidaliases">Matrix
   *     specification</a>
   */
  public List<RoomAlias> getRoomAliases(RoomId roomId) {
    JsonValue response =
        authenticated("GET", ROOMS_PATH + encode(roomId.value()) + "/aliases", null);
    var aliases = new ArrayList<RoomAlias>();
    JsonValue aliasValue = response.asObject().get("aliases");
    if (aliasValue != null && aliasValue.isArray()) {
      var aliasArray = aliasValue.asArray();
      for (int i = 0; i < aliasArray.size(); i++) {
        aliases.add(RoomAlias.of(aliasArray.get(i).asString()));
      }
    }
    return aliases;
  }

  /**
   * Retrieves the child events related to a room event.
   *
   * @param roomId the room containing the event
   * @param eventId the parent event identifier
   * @return the first relation page
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1roomsroomidrelationseventid">Matrix
   *     specification</a>
   */
  public RelationsResponse getEventRelations(RoomId roomId, EventId eventId) {
    return getEventRelations(roomId, eventId, null, null, RelationsOptions.defaults());
  }

  /**
   * Retrieves relation children with optional relation and event-type filters.
   *
   * @param roomId the room containing the event
   * @param eventId the parent event identifier
   * @param relationType optional relation type filter
   * @param eventType optional child event type filter
   * @param options pagination and recursion options
   * @return the matching relation page
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1roomsroomidrelations">Matrix
   *     specification</a>
   */
  public RelationsResponse getEventRelations(
      RoomId roomId,
      EventId eventId,
      String relationType,
      String eventType,
      RelationsOptions options) {
    StringBuilder path =
        new StringBuilder(CLIENT_V1_ROOMS_PATH)
            .append(encode(roomId.value()))
            .append("/relations/")
            .append(encode(eventId.value()));
    if (relationType != null) {
      path.append('/').append(encode(relationType));
      if (eventType != null) {
        path.append('/').append(encode(eventType));
      }
    } else if (eventType != null) {
      throw new IllegalArgumentException("eventType requires a relationType");
    }
    if (options != null) {
      path = new StringBuilder(appendQuery(path.toString(), options.toQuery()));
    }
    return RelationsResponse.from(authenticated("GET", path.toString(), null));
  }

  /**
   * Retrieves the next page of related events using an opaque continuation token.
   *
   * @param roomId the room containing the event
   * @param eventId the parent event identifier
   * @param from pagination token from the previous result
   * @param limit maximum number of events, or a non-positive value to use the server default
   * @return the next relation page
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1roomsroomidrelations">Matrix
   *     specification</a>
   */
  public RelationsResponse getNextEventRelations(
      RoomId roomId, EventId eventId, String from, long limit) {
    var query = new JsonObject().put("from", from);
    if (limit > 0) {
      query.put("limit", limit);
    }
    String path =
        CLIENT_V1_ROOMS_PATH + encode(roomId.value()) + "/relations/" + encode(eventId.value());
    return RelationsResponse.from(authenticated("GET", appendQuery(path, query), null));
  }

  /**
   * Retrieves thread roots in a room, optionally limited to threads the user participated in.
   *
   * @param roomId the room whose threads to list
   * @param options pagination and participation filters
   * @return the thread-root page
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1roomsroomidthreads">Matrix
   *     specification</a>
   */
  public ThreadsResponse getRoomThreads(RoomId roomId, ThreadsOptions options) {
    String path = CLIENT_V1_ROOMS_PATH + encode(roomId.value()) + "/threads";
    return ThreadsResponse.from(authenticated("GET", appendQuery(path, options.toQuery()), null));
  }

  /**
   * Retrieves the first page of all thread roots in a room.
   *
   * @param roomId the room whose thread roots to retrieve
   * @return the first page of thread roots
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1roomsroomidthreads">Matrix
   *     specification</a>
   */
  public ThreadsResponse getRoomThreads(RoomId roomId) {
    return getRoomThreads(roomId, ThreadsOptions.defaults());
  }

  /**
   * Retrieves the latest page of room history, walking backwards from the room start. Use {@link
   * #getRoomMessages(RoomId, String, Direction, long)} with the returned {@code end} token to page
   * further.
   *
   * @param roomId the room whose history to retrieve
   * @param limit the maximum number of events per page, or a non-positive value to let the
   *     homeserver decide
   * @return the requested page of history
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3roomsroomidmessages">Matrix
   *     specification</a>
   */
  public RoomMessagesPage getLatestRoomMessages(RoomId roomId, long limit) {
    var query =
        new StringBuilder(ROOMS_PATH).append(encode(roomId.value())).append("/messages?dir=b");
    if (limit > 0) {
      query.append(LIMIT_QUERY_PARAM).append(limit);
    }
    return RoomMessagesPage.from(authenticated("GET", query.toString(), null));
  }

  /**
   * Sends a message event to a room with a generated transaction identifier.
   *
   * @param roomId the room to send the event to
   * @param eventType the event type
   * @param content the event content
   * @return the created event identifier
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#put_matrixclientv3roomsroomidsendeventtypetxnid">Matrix
   *     specification</a>
   */
  public EventId sendMessageEvent(RoomId roomId, String eventType, JsonValue content) {
    return sendMessageEventWithTransactionId(
        roomId, eventType, content, UUID.randomUUID().toString());
  }

  /**
   * Sends a message event using an explicit transaction ID.
   *
   * @param roomId the room to receive the event
   * @param eventType the event type
   * @param content the event content
   * @param transactionId the idempotent transaction identifier
   * @return the created event identifier
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#put_matrixclientv3roomsroomidsendeventtypetxnid">Matrix
   *     specification</a>
   */
  public EventId sendEvent(
      RoomId roomId, String eventType, JsonValue content, String transactionId) {
    return sendMessageEventWithTransactionId(roomId, eventType, content, transactionId);
  }

  private EventId sendMessageEventWithTransactionId(
      RoomId roomId, String eventType, JsonValue content, String transactionId) {
    String eventId =
        authenticated(
                "PUT",
                ROOMS_PATH
                    + encode(roomId.value())
                    + "/send/"
                    + encode(eventType)
                    + "/"
                    + encode(transactionId),
                content,
                true)
            .asObject()
            .get(EVENT_ID_FIELD)
            .asString();
    return EventId.of(eventId);
  }

  private static String redactionOperationKey(RoomId roomId, EventId eventId) {
    return "redact\n" + roomId.value() + "\n" + eventId.value();
  }

  /**
   * Sends a message event, reusing its persisted transaction ID when an operation is retried after
   * process restart. The transaction mapping is removed only after the server accepts the event.
   *
   * @param roomId the room to receive the message
   * @param eventType the event type
   * @param content the event content
   * @param operationKey stable key for this logical operation
   * @return the created event identifier
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#put_matrixclientv3roomsroomidsendeventtypetxnid">Matrix
   *     specification</a>
   */
  public EventId sendMessageEventWithKey(
      RoomId roomId, String eventType, JsonValue content, String operationKey) {
    String transactionId = transactionIdStore.getOrCreate(operationKey);
    EventId eventId = sendMessageEventWithTransactionId(roomId, eventType, content, transactionId);
    transactionIdStore.complete(operationKey);
    return eventId;
  }

  /**
   * Sends a state event to a room with an empty state key.
   *
   * @param roomId the room to send the event to
   * @param eventType the event type
   * @param content the event content
   * @return the created event identifier
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#put_matrixclientv3roomsroomidstateeventtypestatekey">Matrix
   *     specification</a>
   */
  public EventId sendStateEvent(RoomId roomId, String eventType, JsonValue content) {
    return sendStateEvent(roomId, eventType, "", content);
  }

  /**
   * Sends a state event to a room with an explicit state key.
   *
   * @param roomId the room to send the event to
   * @param eventType the event type
   * @param stateKey the state key
   * @param content the event content
   * @return the created event identifier
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#put_matrixclientv3roomsroomidstateeventtypestatekey">Matrix
   *     specification</a>
   */
  public EventId sendStateEvent(
      RoomId roomId, String eventType, String stateKey, JsonValue content) {
    String eventId =
        authenticated(
                "PUT",
                ROOMS_PATH
                    + encode(roomId.value())
                    + "/state/"
                    + encode(eventType)
                    + "/"
                    + encode(stateKey),
                content)
            .asObject()
            .get(EVENT_ID_FIELD)
            .asString();
    return EventId.of(eventId);
  }

  /**
   * Redacts an event in a room, optionally with a reason.
   *
   * @param roomId the room containing the event
   * @param eventId the event to redact
   * @param reason the optional reason for the redaction
   * @return the redaction event identifier
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#put_matrixclientv3roomsroomidredacteventidtxnid">Matrix
   *     specification</a>
   */
  public EventId redact(RoomId roomId, EventId eventId, String reason) {
    String operationKey = redactionOperationKey(roomId, eventId);
    String transactionId = transactionIdStore.getOrCreate(operationKey);
    var body = new JsonObject();
    if (reason != null) {
      body.put(REASON_FIELD, reason);
    }
    EventId redactionId =
        EventId.of(
            authenticated(
                    "PUT",
                    ROOMS_PATH
                        + encode(roomId.value())
                        + "/redact/"
                        + encode(eventId.value())
                        + "/"
                        + encode(transactionId),
                    body,
                    true)
                .asObject()
                .get(EVENT_ID_FIELD)
                .asString());
    transactionIdStore.complete(operationKey);
    return redactionId;
  }

  /**
   * Retrieves the profile of a user.
   *
   * @param userId the user whose profile to retrieve
   * @return the user profile
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3profileuserid">Matrix
   *     specification</a>
   */
  public UserProfile getProfile(UserId userId) {
    return UserProfile.from(get(PROFILE_PATH + encode(userId.value())));
  }

  /**
   * Retrieves a single profile field of a user, such as {@code displayname} or {@code avatar_url}.
   *
   * @param userId the user whose profile field to retrieve
   * @param keyName the profile field name
   * @return the field value, or empty when unset
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3profileuseridkeyname">Matrix
   *     specification</a>
   */
  public Optional<String> getProfileField(UserId userId, String keyName) {
    JsonValue response = get(PROFILE_PATH + encode(userId.value()) + "/" + encode(keyName));
    JsonValue value = response.asObject().get(keyName);
    return value != null && value.isString() ? Optional.of(value.asString()) : Optional.empty();
  }

  /**
   * Sets a single profile field of a user; a {@code null} value clears the field.
   *
   * @param userId the user to update
   * @param keyName the profile field name
   * @param value the value to set, or {@code null} to clear the field
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#put_matrixclientv3profileuseridkeyname">Matrix
   *     specification</a>
   */
  public void setProfileField(UserId userId, String keyName, String value) {
    if (value == null) {
      authenticated(
          HTTP_DELETE, PROFILE_PATH + encode(userId.value()) + "/" + encode(keyName), null);
      return;
    }
    authenticated(
        "PUT",
        PROFILE_PATH + encode(userId.value()) + "/" + encode(keyName),
        new JsonObject().put(keyName, value));
  }

  /**
   * Sets the display name of a user; a {@code null} display name clears it.
   *
   * @param userId the user to update
   * @param displayName the display name to set, or {@code null} to clear
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#put_matrixclientv3profileuseridkeyname">Matrix
   *     specification</a>
   */
  public void setDisplayName(UserId userId, String displayName) {
    setProfileField(userId, "displayname", displayName);
  }

  /**
   * Sets the avatar URL of a user; a {@code null} avatar URL clears it.
   *
   * @param userId the user to update
   * @param avatarUrl the avatar URL to set, or {@code null} to clear
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#put_matrixclientv3profileuseridkeyname">Matrix
   *     specification</a>
   */
  public void setAvatarUrl(UserId userId, String avatarUrl) {
    setProfileField(userId, "avatar_url", avatarUrl);
  }

  /**
   * Creates a room alias pointing to a room in the room directory.
   *
   * @param roomAlias the alias to create
   * @param roomId the room the alias points to
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#put_matrixclientv3directoryroomroomalias">Matrix
   *     specification</a>
   */
  public void createRoomAlias(RoomAlias roomAlias, RoomId roomId) {
    authenticated(
        "PUT",
        DIRECTORY_PATH + encode(roomAlias.value()),
        new JsonObject().put(ROOM_ID_FIELD, roomId.value()));
  }

  /**
   * Deletes a room alias from the room directory.
   *
   * @param roomAlias the alias to delete
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#delete_matrixclientv3directoryroomroomalias">Matrix
   *     specification</a>
   */
  public void deleteRoomAlias(RoomAlias roomAlias) {
    authenticated(HTTP_DELETE, DIRECTORY_PATH + encode(roomAlias.value()), null);
  }

  /**
   * Retrieves the public room directory.
   *
   * @return the public rooms
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3publicrooms">Matrix
   *     specification</a>
   */
  public PublicRoomsResponse getPublicRooms() {
    return PublicRoomsResponse.from(get("_matrix/client/v3/publicRooms"));
  }

  /**
   * Retrieves a page of the public room directory, filtered by a server-specific generic search
   * filter.
   *
   * @param limit the maximum number of rooms to return
   * @param filter the generic search term
   * @param since the pagination token of the previous page
   * @return the public rooms page
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3publicrooms">Matrix
   *     specification</a>
   */
  public PublicRoomsResponse getPublicRooms(long limit, String filter, String since) {
    var body = new JsonObject();
    if (limit > 0) {
      body.put("limit", limit);
    }
    if (filter != null) {
      body.put("filter", new JsonObject().put("generic_search_term", filter));
    }
    if (since != null) {
      body.put("since", since);
    }
    return PublicRoomsResponse.from(authenticated("POST", "_matrix/client/v3/publicRooms", body));
  }

  /**
   * Retrieves a room summary by room ID.
   *
   * @param roomId the room identifier to summarize
   * @param via servers to try if the local server cannot generate a summary
   * @return the room summary
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1room_summaryroomidoralias">Matrix
   *     specification</a>
   */
  public RoomSummary getRoomSummary(RoomId roomId, List<String> via) {
    return getRoomSummary(roomId.value(), via);
  }

  /**
   * Retrieves a room summary by room alias.
   *
   * @param roomAlias the room alias to summarize
   * @param via servers to try if the local server cannot generate a summary
   * @return the room summary
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1room_summaryroomidoralias">Matrix
   *     specification</a>
   */
  public RoomSummary getRoomSummary(RoomAlias roomAlias, List<String> via) {
    return getRoomSummary(roomAlias.value(), via);
  }

  private RoomSummary getRoomSummary(String roomIdOrAlias, List<String> via) {
    String path = "_matrix/client/v1/room_summary/" + encode(roomIdOrAlias);
    if (via != null && !via.isEmpty()) {
      var query = new StringBuilder(path).append('?');
      for (int index = 0; index < via.size(); index++) {
        if (index > 0) {
          query.append('&');
        }
        query.append("via=").append(encode(via.get(index)));
      }
      path = query.toString();
    }
    return RoomSummary.from(authenticated("GET", path, null));
  }

  /**
   * Retrieves a room summary by room ID without federation hints.
   *
   * @param roomId the room identifier to summarize
   * @return the room summary
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1room_summaryroomidoralias">Matrix
   *     specification</a>
   */
  public RoomSummary getRoomSummary(RoomId roomId) {
    return getRoomSummary(roomId.value(), List.of());
  }

  /**
   * Retrieves a room summary by room alias without federation hints.
   *
   * @param roomAlias the room alias to summarize
   * @return the room summary
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1room_summaryroomidoralias">Matrix
   *     specification</a>
   */
  public RoomSummary getRoomSummary(RoomAlias roomAlias) {
    return getRoomSummary(roomAlias.value(), List.of());
  }

  /**
   * Retrieves one page of a space hierarchy.
   *
   * @param roomId the space room ID
   * @param options pagination and hierarchy filters
   * @return the hierarchy page
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1roomsroomidhierarchy">Matrix
   *     specification</a>
   */
  public SpaceHierarchyResponse getSpaceHierarchy(RoomId roomId, SpaceHierarchyOptions options) {
    String path = CLIENT_V1_ROOMS_PATH + encode(roomId.value()) + "/hierarchy";
    JsonObject query = options == null ? new JsonObject() : options.toQuery();
    return SpaceHierarchyResponse.from(authenticated("GET", appendQuery(path, query), null));
  }

  /**
   * Retrieves the first page of a space hierarchy with default server options.
   *
   * @param roomId the space room ID
   * @return the hierarchy page
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1roomsroomidhierarchy">Matrix
   *     specification</a>
   */
  public SpaceHierarchyResponse getSpaceHierarchy(RoomId roomId) {
    return getSpaceHierarchy(roomId, null);
  }

  /**
   * Searches the user directory.
   *
   * @param request the search term and optional result limit
   * @return the directory search response
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3user_directorysearch">Matrix
   *     specification</a>
   */
  public UserDirectorySearchResponse searchUsers(UserDirectorySearchRequest request) {
    return UserDirectorySearchResponse.from(
        authenticated("POST", "_matrix/client/v3/user_directory/search", request.toJson()));
  }

  /**
   * Searches the user directory by term.
   *
   * @param searchTerm the term to search for
   * @return the directory search response
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3user_directorysearch">Matrix
   *     specification</a>
   */
  public UserDirectorySearchResponse searchUsers(String searchTerm) {
    return searchUsers(new UserDirectorySearchRequest(searchTerm, null));
  }

  /**
   * Searches room events on the homeserver.
   *
   * @param request search criteria and optional opaque continuation token
   * @return matching search results
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3search">Matrix
   *     specification</a>
   */
  public SearchResponse search(SearchRequest request) {
    String path = appendQuery("_matrix/client/v3/search", request.toQuery());
    return SearchResponse.from(authenticated("POST", path, request.toJson()));
  }

  /**
   * Retrieves the third-party protocols supported by the homeserver.
   *
   * @return protocol metadata keyed by protocol name
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3thirdpartyprotocols">Matrix
   *     specification</a>
   */
  public ThirdPartyProtocols getThirdPartyProtocols() {
    return ThirdPartyProtocols.from(authenticated("GET", THIRD_PARTY_PATH + "protocols", null));
  }

  /**
   * Retrieves metadata for one third-party protocol.
   *
   * @param protocol the protocol name
   * @return protocol metadata
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3thirdpartyprotocolprotocol">Matrix
   *     specification</a>
   */
  public ThirdPartyProtocol getThirdPartyProtocol(String protocol) {
    return ThirdPartyProtocol.from(
        authenticated("GET", THIRD_PARTY_PATH + "protocol/" + encode(protocol), null));
  }

  /**
   * Looks up Matrix room aliases that map to a third-party location.
   *
   * @param alias the Matrix room alias to look up
   * @return matching third-party locations
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3thirdpartylocation">Matrix
   *     specification</a>
   */
  public ThirdPartyLocations getThirdPartyLocations(RoomAlias alias) {
    JsonObject query = new JsonObject().put("alias", alias.value());
    return thirdPartyLocations(appendQuery(THIRD_PARTY_PATH + "location", query));
  }

  /**
   * Looks up Matrix room aliases matching third-party location fields.
   *
   * @param protocol the third-party protocol name
   * @param fields custom protocol fields, or {@code null} if none
   * @return matching third-party locations
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3thirdpartylocationprotocol">Matrix
   *     specification</a>
   */
  public ThirdPartyLocations getThirdPartyLocations(String protocol, Map<String, String> fields) {
    String path = THIRD_PARTY_PATH + "location/" + encode(protocol);
    return thirdPartyLocations(appendFields(path, fields));
  }

  private ThirdPartyLocations thirdPartyLocations(String path) {
    return ThirdPartyLocations.from(authenticated("GET", path, null));
  }

  /**
   * Looks up third-party users associated with a Matrix user ID.
   *
   * @param userId the Matrix user ID to look up
   * @return matching third-party users
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3thirdpartyuser">Matrix
   *     specification</a>
   */
  public ThirdPartyUsers getThirdPartyUsers(UserId userId) {
    JsonObject query = new JsonObject().put("userid", userId.value());
    return ThirdPartyUsers.from(
        authenticated("GET", appendQuery(THIRD_PARTY_PATH + "user", query), null));
  }

  /**
   * Looks up Matrix user IDs matching third-party user fields.
   *
   * @param protocol the third-party protocol name
   * @param fields custom protocol fields, or {@code null} if none
   * @return matching third-party users
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3thirdpartyuserprotocol">Matrix
   *     specification</a>
   */
  public ThirdPartyUsers getThirdPartyUsers(String protocol, Map<String, String> fields) {
    String path = THIRD_PARTY_PATH + "user/" + encode(protocol);
    return ThirdPartyUsers.from(authenticated("GET", appendFields(path, fields), null));
  }

  private static String appendFields(String path, Map<String, String> fields) {
    if (fields == null || fields.isEmpty()) {
      return path;
    }
    JsonObject query = new JsonObject();
    fields.forEach(query::put);
    return appendQuery(path, query);
  }

  /**
   * Retrieves rooms shared between the current user and another user.
   *
   * @param userId the user whose mutual rooms to retrieve
   * @param from optional pagination token returned as {@code next_batch} by the preceding response
   * @return the mutual rooms and an optional continuation token
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1mutual_rooms">Matrix
   *     specification</a>
   */
  public MutualRoomsResponse getMutualRooms(UserId userId, String from) {
    JsonObject query = new JsonObject().put(USER_ID_FIELD, userId.value());
    if (from != null) {
      query.put("from", from);
    }
    return MutualRoomsResponse.from(
        authenticated("GET", appendQuery(MUTUAL_ROOMS_PATH, query), null));
  }

  /**
   * Retrieves the first page of rooms shared with another user.
   *
   * @param userId the user whose mutual rooms to retrieve
   * @return the first mutual-rooms page
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1mutual_rooms">Matrix
   *     specification</a>
   */
  public MutualRoomsResponse getMutualRooms(UserId userId) {
    return getMutualRooms(userId, null);
  }

  /**
   * Retrieves the identity of the user the current access token belongs to.
   *
   * @return the session of the token owner
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3accountwhoami">Matrix
   *     specification</a>
   */
  public WhoamiResponse whoami() {
    return WhoamiResponse.from(authenticated("GET", "_matrix/client/v3/account/whoami", null));
  }

  /**
   * Retrieves the presence status of a user.
   *
   * @param userId the user whose presence to retrieve
   * @return the presence status of the user
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3presenceuseridstatus">Matrix
   *     specification</a>
   */
  public PresenceStatus getPresence(UserId userId) {
    return PresenceStatus.from(
        authenticated(
            "GET", "_matrix/client/v3/presence/" + encode(userId.value()) + "/status", null));
  }

  /**
   * Sets the presence status of the current user. Presence updates are rate-limited by the
   * homeserver.
   *
   * @param presence the presence to set
   * @param statusMessage the optional status message
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#put_matrixclientv3presenceuseridstatus">Matrix
   *     specification</a>
   */
  public void setPresence(Presence presence, String statusMessage) {
    var status = PresenceStatus.of(presence, statusMessage);
    authenticated(
        "PUT",
        "_matrix/client/v3/presence/" + encode(currentUserId()) + "/status",
        status.toUpdate());
  }

  /**
   * Sends a read receipt for an event in a room. Use {@link #sendReadMarkers(RoomId, ReadMarkers)}
   * to send {@code m.fully_read} markers.
   *
   * @param roomId the room containing the event
   * @param receiptType the receipt type: {@code m.read} or {@code m.read.private}
   * @param eventId the event the receipt acknowledges up to
   * @param threadId the thread root the receipt belongs to, {@code main} for the main timeline, or
   *     {@code null} for an unthreaded receipt
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3roomsroomidreceiptreceipttypeeventid">Matrix
   *     specification</a>
   */
  public void sendReceipt(RoomId roomId, String receiptType, EventId eventId, String threadId) {
    if (!"m.read".equals(receiptType) && !"m.read.private".equals(receiptType)) {
      throw new IllegalArgumentException("receiptType must be m.read or m.read.private");
    }
    var path =
        ROOMS_PATH
            + encode(roomId.value())
            + "/receipt/"
            + encode(receiptType)
            + '/'
            + encode(eventId.value());
    JsonValue body =
        threadId == null ? new JsonObject() : new JsonObject().put("thread_id", threadId);
    authenticated("POST", path, body);
  }

  /**
   * Sends read receipts and the fully-read marker of a room in a single call.
   *
   * @param roomId the room to mark
   * @param markers the receipts and marker to set
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3roomsroomidread_markers">Matrix
   *     specification</a>
   */
  public void sendReadMarkers(RoomId roomId, ReadMarkers markers) {
    authenticated("POST", ROOMS_PATH + encode(roomId.value()) + "/read_markers", markers.toJson());
  }

  /**
   * Sets whether the current user is typing in a room. Typing notifications are ephemeral and
   * rate-limited by the homeserver; clients typically re-send them every 20 to 30 seconds while
   * typing.
   *
   * @param roomId the room in which the user is typing
   * @param typing whether the user is typing
   * @param timeout the typing duration in milliseconds, or a non-positive value to let the
   *     homeserver decide
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#put_matrixclientv3roomsroomidtypinguserid">Matrix
   *     specification</a>
   */
  public void setTyping(RoomId roomId, boolean typing, long timeout) {
    var body = new JsonObject().put("typing", typing);
    if (timeout > 0) {
      body.put("timeout", timeout);
    }
    authenticated(
        "PUT", ROOMS_PATH + encode(roomId.value()) + "/typing/" + encode(currentUserId()), body);
  }

  /**
   * Retrieves a global account-data event of the current user.
   *
   * @param type the event type, namespaced for custom events
   * @return the event content, or empty when no data exists for the type
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a href="https://spec.matrix.org/latest/client-server-api/#account-data">Matrix
   *     specification</a>
   */
  public Optional<JsonValue> getAccountData(String type) {
    return accountData(userAccountDataPath(null, type));
  }

  /**
   * Writes a global account-data event of the given type for the current user. Server-managed types
   * (such as {@code m.fully_read} or {@code m.push_rules}) are rejected by homeservers.
   *
   * @param type the event type, namespaced for custom events
   * @param content the event content
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a href="https://spec.matrix.org/latest/client-server-api/#account-data">Matrix
   *     specification</a>
   */
  public void setAccountData(String type, JsonValue content) {
    authenticated("PUT", userAccountDataPath(null, type), content);
  }

  /**
   * Retrieves a room account-data event of the current user. Room account data does not inherit
   * from global account data.
   *
   * @param roomId the room the data is scoped to
   * @param type the event type, namespaced for custom events
   * @return the event content, or empty when no data exists for the type
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a href="https://spec.matrix.org/latest/client-server-api/#account-data">Matrix
   *     specification</a>
   */
  public Optional<JsonValue> getRoomAccountData(RoomId roomId, String type) {
    return accountData(userAccountDataPath(roomId, type));
  }

  /**
   * Writes a room account-data event of the given type for the current user.
   *
   * @param roomId the room the data is scoped to
   * @param type the event type, namespaced for custom events
   * @param content the event content
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a href="https://spec.matrix.org/latest/client-server-api/#account-data">Matrix
   *     specification</a>
   */
  public void setRoomAccountData(RoomId roomId, String type, JsonValue content) {
    authenticated("PUT", userAccountDataPath(roomId, type), content);
  }

  private Optional<JsonValue> accountData(String path) {
    try {
      return Optional.of(authenticated("GET", path, null));
    } catch (MatrixServerException e) {
      if ("M_NOT_FOUND".equals(e.getErrcode())) {
        return Optional.empty();
      }
      throw e;
    }
  }

  private String userAccountDataPath(RoomId roomId, String type) {
    var path = new StringBuilder(USER_PATH).append(encode(currentUserId()));
    if (roomId != null) {
      path.append(USER_ROOMS_SEGMENT).append(encode(roomId.value()));
    }
    return path.append(ACCOUNT_DATA_PATH).append(encode(type)).toString();
  }

  private String currentUserId() {
    return getSession()
        .orElseThrow(() -> new AuthenticationException(M_MISSING_TOKEN, NO_SESSION_MESSAGE))
        .userId();
  }

  /**
   * Retrieves the current user's tags for a room.
   *
   * @param roomId the room whose tags to retrieve
   * @return the room tags
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3useruseridroomsroomidtags">Matrix
   *     specification</a>
   */
  public RoomTags getRoomTags(RoomId roomId) {
    String path =
        USER_PATH + encode(currentUserId()) + USER_ROOMS_SEGMENT + encode(roomId.value()) + "/tags";
    return RoomTags.from(authenticated("GET", path, null));
  }

  /**
   * Adds or updates a tag for a room.
   *
   * @param roomId the room to tag
   * @param tagName the tag name
   * @param tag the tag metadata
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#put_matrixclientv3useruseridroomsroomidtagstag">Matrix
   *     specification</a>
   */
  public void setRoomTag(RoomId roomId, String tagName, RoomTag tag) {
    String path = roomTagPath(roomId, tagName);
    authenticated("PUT", path, tag.toJson());
  }

  /**
   * Removes a tag from a room.
   *
   * @param roomId the room to untag
   * @param tagName the tag name
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#delete_matrixclientv3useruseridroomsroomidtagstag">Matrix
   *     specification</a>
   */
  public void deleteRoomTag(RoomId roomId, String tagName) {
    authenticated(HTTP_DELETE, roomTagPath(roomId, tagName), null);
  }

  private String roomTagPath(RoomId roomId, String tagName) {
    return USER_PATH
        + encode(currentUserId())
        + USER_ROOMS_SEGMENT
        + encode(roomId.value())
        + "/tags/"
        + encode(tagName);
  }

  /**
   * Reports a room to the homeserver.
   *
   * @param roomId the room being reported
   * @param report the report reason
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3roomsroomidreport">Matrix
   *     specification</a>
   */
  public void reportRoom(RoomId roomId, ContentReport report) {
    requireReportReason(report);
    authenticated("POST", ROOMS_PATH + encode(roomId.value()) + "/report", report.toJson());
  }

  /**
   * Reports an event in a room to the homeserver.
   *
   * @param roomId the room containing the event
   * @param eventId the event being reported
   * @param report the optional report reason
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3roomsroomidreporteventid">Matrix
   *     specification</a>
   */
  public void reportEvent(RoomId roomId, EventId eventId, ContentReport report) {
    authenticated(
        "POST",
        ROOMS_PATH + encode(roomId.value()) + "/report/" + encode(eventId.value()),
        report.toJson());
  }

  /**
   * Reports a user to the homeserver.
   *
   * @param userId the user being reported
   * @param report the report reason
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3usersuseridreport">Matrix
   *     specification</a>
   */
  public void reportUser(UserId userId, ContentReport report) {
    requireReportReason(report);
    authenticated("POST", USERS_PATH + encode(userId.value()) + "/report", report.toJson());
  }

  private static void requireReportReason(ContentReport report) {
    if (report.reason() == null) {
      throw new IllegalArgumentException("report reason is required");
    }
  }

  /**
   * Retrieves a sync response using a typed filter and the saved opaque sync token, or performs an
   * initial sync when no token is stored.
   *
   * @param timeoutMs maximum long-poll duration in milliseconds
   * @param filter the typed filter, or null for no filter
   * @return the parsed response
   * @see <a href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3sync">Matrix
   *     specification</a>
   */
  public SyncResponse syncWithFilter(long timeoutMs, MatrixFilter filter) {
    String filterJson = filter == null ? null : filter.toJson().toJson();
    return syncWithFilterJson(timeoutMs, filterJson);
  }

  private SyncResponse syncWithFilterJson(long timeoutMs, String inlineFilterJson) {
    String since = syncTokenStore.current().orElse(null);
    var query = new JsonObject();
    if (since != null) {
      query.put("since", since);
    }
    if (timeoutMs > 0) {
      query.put("timeout", timeoutMs);
    }
    query.put("full_state", false);
    if (inlineFilterJson != null) {
      query.put("filter", inlineFilterJson);
    }
    SyncResponse response =
        SyncResponse.from(authenticated("GET", appendQuery(SYNC_URI, query), null));
    syncTokenStore.save(response.nextBatch());
    return response;
  }

  /**
   * Creates a server-side sync filter and returns its filter ID.
   *
   * @param filter the filter definition
   * @return the server-issued filter ID
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3useruseridfilter">Matrix
   *     specification</a>
   */
  public String createFilter(MatrixFilter filter) {
    JsonValue result =
        authenticated("POST", USER_PATH + encode(currentUserId()) + "/filter", filter.toJson());
    JsonValue filterId = result.asObject().get("filter_id");
    if (filterId == null || !filterId.isString()) {
      throw new DiscoveryException("Filter response must contain filter_id");
    }
    return filterId.asString();
  }

  /**
   * Retrieves a previously created sync filter by ID.
   *
   * @param filterId the server-issued filter ID
   * @return the parsed filter definition
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3useruseridfilterfilterid">Matrix
   *     specification</a>
   */
  public MatrixFilter getFilter(String filterId) {
    return MatrixFilter.from(
        authenticated(
            "GET", USER_PATH + encode(currentUserId()) + "/filter/" + encode(filterId), null));
  }

  /**
   * Performs a sync request and atomically stores the returned opaque {@code next_batch} token when
   * parsing succeeds. Sync is optional: callers that do not use this API need no sync-specific
   * configuration.
   *
   * @param options sync query options
   * @return the parsed sync response
   * @throws IllegalArgumentException if the server response is malformed
   * @see <a href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3sync">Matrix
   *     specification</a>
   */
  public SyncResponse sync(SyncOptions options) {
    JsonValue response = authenticated("GET", appendQuery(SYNC_URI, options.toQuery()), null);
    SyncResponse parsed = SyncResponse.from(response);
    syncTokenStore.save(parsed.nextBatch());
    return parsed;
  }

  /**
   * Performs an incremental sync from the saved token, if present; otherwise starts an initial
   * sync. The returned {@code next_batch} token is saved only after a complete successful response.
   *
   * @param timeoutMs maximum long-poll duration in milliseconds
   * @param filter saved filter ID or null for no filter
   * @return the parsed sync response
   * @see <a href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3sync">Matrix
   *     specification</a>
   */
  public SyncResponse sync(long timeoutMs, String filter) {
    SyncOptions options =
        syncTokenStore
            .current()
            .map(token -> SyncOptions.incremental(token, timeoutMs, filter))
            .orElseGet(() -> SyncOptions.initial(timeoutMs, filter));
    return sync(options);
  }

  /**
   * Returns the saved opaque sync token, if a successful sync has completed.
   *
   * @return the saved {@code next_batch} token, if available
   */
  public Optional<String> syncToken() {
    return syncTokenStore.current();
  }

  /** Clears the saved sync token so the next sync starts an initial sync. */
  public void clearSyncToken() {
    syncTokenStore.clear();
  }

  private static String appendQuery(String path, JsonObject query) {
    var builder = new StringBuilder(path);
    boolean first = true;
    for (Map.Entry<String, JsonValue> entry : query.entrySet()) {
      builder.append(first ? '?' : '&');
      first = false;
      builder.append(encode(entry.getKey())).append('=');
      JsonValue value = entry.getValue();
      builder.append(encode(value.isString() ? value.asString() : value.toJson()));
    }
    return builder.toString();
  }

  /**
   * Creates a reservation for a later media upload.
   *
   * @return the reserved MXC URI and optional expiry timestamp
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixmediav1create">Matrix
   *     specification</a>
   */
  public MediaUploadReservation createMediaUpload() {
    return MediaUploadReservation.from(authenticated("POST", "_matrix/media/v1/create", null));
  }

  /**
   * Uploads raw bytes to a previously reserved content URI.
   *
   * @param reservation the reserved URI
   * @param content the media bytes
   * @param contentType the optional MIME type
   * @param filename the optional filename presented to other users
   * @return the uploaded MXC URI
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @throws MediaSizeLimitException if the upload exceeds the configured client limit
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#put_matrixmediav3uploadservernamemediaid">Matrix
   *     specification</a>
   */
  public MxcUri uploadReservedMedia(
      MediaUploadReservation reservation, byte[] content, String contentType, String filename) {
    return uploadReservedMedia(
        reservation, new ByteArrayInputStream(content), content.length, contentType, filename);
  }

  /**
   * Uploads a stream to a previously reserved content URI without buffering the full content when
   * the media transport supports streaming uploads.
   *
   * @param reservation the reserved URI
   * @param content the media stream, closed after the request completes
   * @param contentLength the source length, or a negative value when unknown
   * @param contentType the optional MIME type
   * @param filename the optional filename presented to other users
   * @return the uploaded MXC URI
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @throws MediaSizeLimitException if the upload exceeds the configured client limit
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#put_matrixmediav3uploadservernamemediaid">Matrix
   *     specification</a>
   */
  public MxcUri uploadReservedMedia(
      MediaUploadReservation reservation,
      InputStream content,
      long contentLength,
      String contentType,
      String filename) {
    Objects.requireNonNull(reservation, "reservation");
    Objects.requireNonNull(content, "content");
    var path =
        new StringBuilder("_matrix/media/v3/upload/")
            .append(encode(reservation.contentUri().serverName()))
            .append('/')
            .append(encode(reservation.contentUri().mediaId()));
    if (filename != null) {
      path.append("?filename=").append(encode(filename));
    }
    OptionalLong length =
        contentLength >= 0 ? OptionalLong.of(contentLength) : OptionalLong.empty();
    var request =
        new StreamingBinaryRequest(
            "PUT",
            homeserverUrl + "/" + path,
            authHeaders(),
            content,
            length,
            contentType == null ? FILE_TYPE : contentType);
    BinaryResponse response = sendBinary(request, path.toString());
    try (response) {
      throwIfBinaryError(response);
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
    return reservation.contentUri();
  }

  /**
   * Uploads a file to a previously reserved content URI without loading the full file into memory.
   *
   * @param reservation the reserved URI
   * @param file the file to upload
   * @param contentType the optional MIME type
   * @param filename the optional filename presented to other users
   * @return the uploaded MXC URI
   * @throws IOException if the file cannot be opened or its size cannot be read
   * @throws MediaSizeLimitException if the file exceeds the configured client limit
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#put_matrixmediav3uploadservernamemediaid">Matrix
   *     specification</a>
   */
  public MxcUri uploadReservedMedia(
      MediaUploadReservation reservation, Path file, String contentType, String filename)
      throws IOException {
    try (InputStream input = Files.newInputStream(file)) {
      return uploadReservedMedia(reservation, input, Files.size(file), contentType, filename);
    }
  }

  /**
   * Retrieves OpenGraph metadata for a URL.
   *
   * @param url the URL to preview
   * @param timestamp the preferred Unix-epoch timestamp in milliseconds, or {@code null}
   * @return typed preview metadata with raw fields retained
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1mediapreview_url">Matrix
   *     specification</a>
   */
  public UrlPreview getUrlPreview(String url, Long timestamp) {
    JsonObject query = new JsonObject().put("url", url);
    if (timestamp != null) {
      query.put("ts", timestamp);
    }
    return UrlPreview.from(
        authenticated("GET", appendQuery("_matrix/client/v1/media/preview_url", query), null));
  }

  /**
   * Retrieves OpenGraph metadata for a URL without requesting a specific preview timestamp.
   *
   * @param url the URL to preview
   * @return typed preview metadata with raw fields retained
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1mediapreview_url">Matrix
   *     specification</a>
   */
  public UrlPreview getUrlPreview(String url) {
    return getUrlPreview(url, null);
  }

  /**
   * Uploads raw bytes to the content repository and returns their Matrix content URI.
   *
   * @param content the media bytes
   * @param contentType the optional MIME type of the media; defaults to {@code
   *     application/octet-stream}
   * @param filename the optional filename presented to other users
   * @return the {@code mxc://} URI of the uploaded media
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixmediav3upload">Matrix
   *     specification</a>
   */
  public MxcUri uploadMedia(byte[] content, String contentType, String filename) {
    return uploadMedia(new ByteArrayInputStream(content), content.length, contentType, filename);
  }

  /**
   * Uploads media from a stream without requiring callers to buffer the complete file.
   *
   * @param content the media stream, closed after the request completes
   * @param contentLength the source length, or a negative value when unknown
   * @param contentType the optional MIME type
   * @param filename the optional filename presented to other users
   * @return the uploaded media URI
   * @throws MediaSizeLimitException if the upload exceeds the configured client limit
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixmediav3upload">Matrix
   *     specification</a>
   */
  public MxcUri uploadMedia(
      InputStream content, long contentLength, String contentType, String filename) {
    var path = new StringBuilder("_matrix/media/v3/upload");
    if (filename != null) {
      path.append("?filename=").append(encode(filename));
    }
    var length = OptionalLong.of(contentLength).stream().filter(c -> c >= 0).findFirst();
    var request =
        new StreamingBinaryRequest(
            "POST",
            homeserverUrl + "/" + path,
            authHeaders(),
            content,
            length,
            contentType == null ? FILE_TYPE : contentType);
    return parseUploadResponse(sendBinary(request, path.toString()));
  }

  /**
   * Uploads a file from disk without loading the entire file into memory.
   *
   * @param file the file to upload
   * @param contentType the optional MIME type
   * @param filename the optional filename presented to other users
   * @return the uploaded media URI
   * @throws IOException if the file cannot be opened or its size cannot be read
   * @throws MediaSizeLimitException if the file exceeds the configured client limit
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixmediav3upload">Matrix
   *     specification</a>
   */
  public MxcUri uploadMedia(Path file, String contentType, String filename) throws IOException {
    try (InputStream input = Files.newInputStream(file)) {
      return uploadMedia(input, Files.size(file), contentType, filename);
    }
  }

  private MxcUri parseUploadResponse(BinaryResponse response) {
    String responseBody = asString(response);
    throwIfError(response.statusCode(), responseBody, response.headers());
    JsonObject body = JsonParser.parse(responseBody).asObject();
    JsonValue uri = body.get("content_uri");
    if (uri == null || !uri.isString()) {
      throw new MatrixServerException(
          response.statusCode(), "M_UNKNOWN", "Upload response must contain content_uri");
    }
    return MxcUri.parse(uri.asString());
  }

  /**
   * Downloads media from the content repository using the authenticated v1.11 endpoint. The
   * response body streams through {@link
   * io.github.fherbreteau.matrix.transport.BinaryResponse#bodyStream()}; callers must close it.
   *
   * @param uri the {@code mxc://} URI of the media
   * @param maxBytes the maximum accepted media size in bytes; a larger response raises {@link
   *     io.github.fherbreteau.matrix.error.MatrixServerException} with {@code M_TOO_LARGE}
   * @return the downloaded media response
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1mediadownloadservernamemediaid">Matrix
   *     specification</a>
   */
  public MediaDownload downloadMedia(MxcUri uri, long maxBytes) {
    return downloadMedia(uri, null, maxBytes);
  }

  /**
   * Downloads media from the content repository with an explicit filename presented in the {@code
   * Content-Disposition} header.
   *
   * @param uri the {@code mxc://} URI of the media
   * @param fileName the filename to request in the response
   * @param maxBytes the maximum accepted media size in bytes; a larger response raises {@link
   *     io.github.fherbreteau.matrix.error.MatrixServerException} with {@code M_TOO_LARGE}
   * @return the downloaded media response
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1mediadownloadservernamemediaidfilename">Matrix
   *     specification</a>
   */
  public MediaDownload downloadMedia(MxcUri uri, String fileName, long maxBytes) {
    var path =
        new StringBuilder("_matrix/client/v1/media/download/")
            .append(encode(uri.serverName()))
            .append('/')
            .append(encode(uri.mediaId()));
    if (fileName != null) {
      path.append('/').append(encode(fileName));
    }
    var request =
        new BinaryRequest("GET", homeserverUrl + "/" + path, authHeaders(), null, FILE_TYPE);
    var response = sendBinary(request, path.toString(), maxBytes);
    throwIfBinaryError(response);
    return new MediaDownload(
        response.header(CONTENT_TYPE_HEADER),
        response.header(CONTENT_DISPOSITION_HEADER),
        response.bodyStream());
  }

  /**
   * Retrieves a thumbnail of media from the content repository using the authenticated v1.11
   * endpoint. Thumbnails are small by definition, so the response is bounded by the same {@code
   * maxBytes} rule as downloads.
   *
   * @param uri the {@code mxc://} URI of the media
   * @param width the desired minimum width in pixels
   * @param height the desired minimum height in pixels
   * @param method the resize method, or {@code null} to let the homeserver decide
   * @param animated whether an animated thumbnail is preferred, or {@code null} to let the
   *     homeserver decide
   * @param maxBytes the maximum accepted thumbnail size in bytes
   * @return the thumbnail response
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1mediathumbnailservernamemediaid">Matrix
   *     specification</a>
   */
  public MediaDownload getThumbnail(
      MxcUri uri, int width, int height, ThumbnailMethod method, Boolean animated, long maxBytes) {
    var query =
        new StringBuilder("_matrix/client/v1/media/thumbnail/")
            .append(encode(uri.serverName()))
            .append('/')
            .append(encode(uri.mediaId()))
            .append("?width=")
            .append(width)
            .append("&height=")
            .append(height);
    if (method != null) {
      query.append("&method=").append(method.value());
    }
    if (animated != null) {
      query.append("&animated=").append(animated);
    }
    var request =
        new BinaryRequest("GET", homeserverUrl + "/" + query, authHeaders(), null, FILE_TYPE);
    var response = sendBinary(request, query.toString(), maxBytes);
    throwIfBinaryError(response);
    return new MediaDownload(
        response.header(CONTENT_TYPE_HEADER),
        response.header(CONTENT_DISPOSITION_HEADER),
        response.bodyStream());
  }

  /**
   * Retrieves the upload size limits configured on the homeserver.
   *
   * @return the maximum upload size in bytes, or empty when the homeserver does not advertise one
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1mediaconfig">Matrix
   *     specification</a>
   */
  public Optional<Long> getMediaConfig() {
    JsonValue response = authenticated("GET", "_matrix/client/v1/media/config", null);
    JsonValue size = response.asObject().get("m.upload.size");
    if (size != null && size.isNumber()) {
      return Optional.of(size.asLong());
    }
    return Optional.empty();
  }

  private Map<String, String> authHeaders() {
    Session session =
        sessionStore
            .current()
            .orElseThrow(() -> new AuthenticationException(M_MISSING_TOKEN, NO_SESSION_MESSAGE));
    return Map.of(Request.AUTHORIZATION_HEADER, BEARER_PREFIX + session.accessToken());
  }

  private BinaryResponse sendBinary(StreamingBinaryRequest request, String path) {
    return sendBinary(request, path, 0);
  }

  private BinaryResponse sendBinary(
      StreamingBinaryRequest request, String path, long maxResponseBytes) {
    var context =
        new AttemptContext(
            UUID.randomUUID(), request.method(), sanitizeEndpoint(path), 1, System.nanoTime());
    try {
      BinaryResponse response = mediaTransport.send(request, maxMediaUploadBytes, maxResponseBytes);
      RequestAttempt.Outcome outcome =
          response.statusCode() >= 200 && response.statusCode() < 300
              ? RequestAttempt.Outcome.SUCCEEDED
              : RequestAttempt.Outcome.FAILED;
      observe(context, response.statusCode(), null, outcome);
      return response;
    } catch (TransportInterruptedException exception) {
      observe(context, null, null, RequestAttempt.Outcome.INTERRUPTED);
      throw exception;
    } catch (MediaSizeLimitException exception) {
      observe(context, null, null, RequestAttempt.Outcome.FAILED);
      throw new MatrixServerException(413, "M_TOO_LARGE", exception.getMessage());
    } catch (RuntimeException exception) {
      observe(context, null, null, RequestAttempt.Outcome.FAILED);
      throw exception;
    }
  }

  private BinaryResponse sendBinary(BinaryRequest request, String path, long maxResponseBytes) {
    var context =
        new AttemptContext(
            UUID.randomUUID(), request.method(), sanitizeEndpoint(path), 1, System.nanoTime());
    try {
      BinaryResponse response = mediaTransport.send(request, maxResponseBytes);
      RequestAttempt.Outcome outcome =
          response.statusCode() >= 200 && response.statusCode() < 300
              ? RequestAttempt.Outcome.SUCCEEDED
              : RequestAttempt.Outcome.FAILED;
      observe(context, response.statusCode(), null, outcome);
      return response;
    } catch (TransportInterruptedException exception) {
      observe(context, null, null, RequestAttempt.Outcome.INTERRUPTED);
      throw exception;
    } catch (MediaSizeLimitException exception) {
      observe(context, null, null, RequestAttempt.Outcome.FAILED);
      throw new MatrixServerException(413, "M_TOO_LARGE", exception.getMessage());
    } catch (RuntimeException exception) {
      observe(context, null, null, RequestAttempt.Outcome.FAILED);
      throw exception;
    }
  }

  private void throwIfBinaryError(BinaryResponse response) {
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throwIfError(response.statusCode(), asString(response), response.headers());
    }
  }

  private void throwIfError(int statusCode, String body, Map<String, String> headers) {
    if (statusCode >= 200 && statusCode < 300) {
      return;
    }
    throw MatrixServerException.fromResponse(statusCode, parseOrNull(body), headers);
  }

  private static String asString(BinaryResponse response) {
    try (var stream = response.bodyStream()) {
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private Optional<String> getRoomStateField(RoomId roomId, String type, String field) {
    try {
      JsonValue content =
          authenticated("GET", ROOMS_PATH + encode(roomId.value()) + "/state/" + type, null);
      JsonValue value = content.asObject().get(field);
      return value != null && value.isString() ? Optional.of(value.asString()) : Optional.empty();
    } catch (MatrixServerException e) {
      if ("M_NOT_FOUND".equals(e.getErrcode())) {
        return Optional.empty();
      }
      throw e;
    }
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }

  /**
   * Performs a GET request against the homeserver and returns the parsed JSON body.
   *
   * @param path the endpoint path relative to the homeserver URL
   * @return the parsed JSON response
   * @see <a href="https://spec.matrix.org/latest/client-server-api/#api-standards">Matrix
   *     specification</a>
   */
  public JsonValue get(String path) {
    return request("GET", path, null, Map.of(), false);
  }

  /**
   * Performs a POST request with a JSON body and returns the parsed JSON response.
   *
   * @param path the endpoint path relative to the homeserver URL
   * @param body the JSON request body, or {@code null} for none
   * @return the parsed JSON response
   * @see <a href="https://spec.matrix.org/latest/client-server-api/#api-standards">Matrix
   *     specification</a>
   */
  public JsonValue post(String path, JsonValue body) {
    return request("POST", path, body, Map.of(), false);
  }

  /**
   * Performs an HTTP request against the homeserver and returns the parsed JSON response.
   *
   * @param method the HTTP method
   * @param path the endpoint path relative to the homeserver URL
   * @param body the JSON request body, or {@code null} for none
   * @return the parsed JSON response
   * @see <a href="https://spec.matrix.org/latest/client-server-api/#api-standards">Matrix
   *     specification</a>
   */
  public JsonValue request(String method, String path, JsonValue body) {
    return request(method, path, body, Map.of(), false);
  }

  /**
   * Performs an HTTP request with per-call opt-in to replay a non-idempotent operation.
   *
   * @param method the HTTP method
   * @param path the endpoint path relative to the homeserver URL
   * @param body the JSON request body, or {@code null} for none
   * @param idempotent whether repeating this operation is safe
   * @return the parsed JSON response
   * @see <a href="https://spec.matrix.org/latest/client-server-api/#rate-limiting">Matrix
   *     specification</a>
   */
  public JsonValue request(String method, String path, JsonValue body, boolean idempotent) {
    return request(method, path, body, Map.of(), idempotent);
  }

  private JsonValue request(
      String method,
      String path,
      JsonValue body,
      Map<String, String> headers,
      boolean explicitlyIdempotent) {
    Request request =
        new Request(
            method, homeserverUrl + "/" + path, headers, body == null ? null : body.toJson());
    UUID correlationId = UUID.randomUUID();
    boolean safeToRetry =
        (retryPolicy.retries(method) || explicitlyIdempotent) && !isSyncPath(path);
    for (int attempt = 1; ; attempt++) {
      AttemptContext context =
          new AttemptContext(correlationId, method, path, attempt, System.nanoTime());
      HttpTransport.Response response = send(request, context, safeToRetry);
      if (response != null) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
          MatrixServerException failure =
              MatrixServerException.fromResponse(
                  response.statusCode(), parseOrNull(response.body()), response.headers());
          retryOrThrow(failure, context, response.statusCode(), safeToRetry);
          response = null;
        }
        if (response != null) {
          return parseResponse(response, context);
        }
      }
    }
  }

  private JsonValue parseResponse(HttpTransport.Response response, AttemptContext context) {
    try {
      JsonValue result =
          response.body() == null || response.body().isBlank()
              ? new JsonObject()
              : JsonParser.parse(response.body());
      observe(context, response.statusCode(), null, RequestAttempt.Outcome.SUCCEEDED);
      return result;
    } catch (RuntimeException exception) {
      observe(context, response.statusCode(), null, RequestAttempt.Outcome.FAILED);
      throw exception;
    }
  }

  private HttpTransport.Response send(
      Request request, AttemptContext context, boolean safeToRetry) {
    try {
      return transport.send(request);
    } catch (TransportInterruptedException exception) {
      observe(context, null, null, RequestAttempt.Outcome.INTERRUPTED);
      throw exception;
    } catch (TransportTimeoutException | UncheckedTransportException exception) {
      retryOrThrow(exception, context, null, safeToRetry);
      throw exception;
    }
  }

  private void retryOrThrow(
      RuntimeException failure, AttemptContext context, Integer statusCode, boolean safeToRetry) {
    Duration delay = retryDelay(failure, context.attempt(), safeToRetry);
    if (delay == null) {
      observe(context, statusCode, null, RequestAttempt.Outcome.FAILED);
      throw failure;
    }
    observe(context, statusCode, delay, RequestAttempt.Outcome.RETRYING);
    sleepBeforeRetry(delay);
  }

  private Duration retryDelay(RuntimeException failure, int attempt, boolean safeToRetry) {
    if (!safeToRetry || attempt > retryPolicy.maxRetries() || !isRetryableFailure(failure)) {
      return null;
    }
    Long retryAfterMs =
        failure instanceof RateLimitedException rateLimited ? rateLimited.getRetryAfterMs() : null;
    return Duration.ofMillis(retryPolicy.delayMs(attempt, retryAfterMs));
  }

  private static boolean isRetryableFailure(RuntimeException failure) {
    return (failure instanceof MatrixServerException serverException
            && serverException.isRetryable())
        || failure instanceof TransportTimeoutException
        || failure instanceof UncheckedTransportException;
  }

  private static boolean isSyncPath(String path) {
    return path.equals(SYNC_URI) || path.startsWith("_matrix/client/v3/sync?");
  }

  private void sleepBeforeRetry(Duration delay) {
    if (Thread.currentThread().isInterrupted()) {
      throw new TransportInterruptedException("Retry interrupted", new InterruptedException());
    }
    try {
      retrySleeper.sleep(delay);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new TransportInterruptedException("Retry interrupted", exception);
    }
    if (Thread.currentThread().isInterrupted()) {
      throw new TransportInterruptedException("Retry interrupted", new InterruptedException());
    }
  }

  private void observe(
      AttemptContext context,
      Integer statusCode,
      Duration retryDelay,
      RequestAttempt.Outcome outcome) {
    requestObserver.onAttempt(
        new RequestAttempt(
            context.correlationId(),
            context.method(),
            sanitizeEndpoint(context.path()),
            context.attempt(),
            statusCode,
            Duration.ofNanos(Math.max(0, System.nanoTime() - context.started())),
            retryDelay,
            outcome));
  }

  private record AttemptContext(
      UUID correlationId, String method, String path, int attempt, long started) {}

  private static String sanitizeEndpoint(String path) {
    int query = path.indexOf('?');
    String endpoint = query < 0 ? path : path.substring(0, query);
    String[] segments = endpoint.split("/");
    var sanitized = new StringBuilder();
    int mediaIdentifiersToHide = 0;
    for (String segment : segments) {
      if (segment.isEmpty()) {
        continue;
      }
      sanitized.append('/');
      if (mediaIdentifiersToHide > 0) {
        sanitized.append('*');
        mediaIdentifiersToHide--;
      } else if (isSensitiveSegment(segment)) {
        sanitized.append('*');
      } else {
        sanitized.append(segment);
      }
      if ("download".equals(segment) || "thumbnail".equals(segment) || "upload".equals(segment)) {
        mediaIdentifiersToHide = 2;
      }
    }
    return sanitized.isEmpty() ? "/" : sanitized.toString();
  }

  private static boolean isSensitiveSegment(String segment) {
    return segment.startsWith("%40")
        || segment.startsWith("%21")
        || segment.startsWith("%24")
        || segment.toLowerCase(Locale.ROOT).contains("token")
        || segment.toLowerCase(Locale.ROOT).contains("secret");
  }

  private void ensureDeviceDeletionSupportedBySession() {
    if (sessionStore.current().filter(Session::usesOauth).isPresent()) {
      throw new UnsupportedOperationException(
          "Device deletion for OAuth sessions must use the homeserver account-management URL; "
              + "check getAuthMetadata() for account_management_uri and supported actions");
    }
  }

  private void ensureLegacyAccountApi() {
    if (sessionStore.current().filter(Session::usesOauth).isPresent()) {
      throw new UnsupportedOperationException(accountManagementMessage());
    }
  }

  private String accountManagementMessage() {
    return "Account management for OAuth sessions must use the homeserver account-management URL; "
        + "check getAuthMetadata() for account_management_uri and supported actions";
  }

  private JsonValue authenticatedUiAuth(String method, String path, JsonValue body) {
    Session session =
        sessionStore
            .current()
            .orElseThrow(() -> new AuthenticationException(M_MISSING_TOKEN, NO_SESSION_MESSAGE));
    try {
      return request(
          method,
          path,
          body,
          Map.of(Request.AUTHORIZATION_HEADER, BEARER_PREFIX + session.accessToken()),
          false);
    } catch (RateLimitedException exception) {
      throw exception;
    } catch (MatrixServerException exception) {
      if (isTokenError(exception)) {
        throw new AuthenticationException(exception.getErrcode(), exception.getMessage());
      }
      throw exception;
    }
  }

  private JsonValue authenticated(String method, String path, JsonValue body) {
    Session session =
        sessionStore
            .current()
            .orElseThrow(() -> new AuthenticationException(M_MISSING_TOKEN, NO_SESSION_MESSAGE));
    try {
      return request(
          method,
          path,
          body,
          Map.of(Request.AUTHORIZATION_HEADER, BEARER_PREFIX + session.accessToken()),
          false);
    } catch (MatrixServerException e) {
      if (isTokenError(e)) {
        throw new AuthenticationException(e.getErrcode(), e.getMessage());
      }
      throw e;
    }
  }

  private JsonValue authenticated(
      String method, String path, JsonValue body, boolean explicitlyIdempotent) {
    Session session =
        sessionStore
            .current()
            .orElseThrow(() -> new AuthenticationException(M_MISSING_TOKEN, NO_SESSION_MESSAGE));
    try {
      return request(
          method,
          path,
          body,
          Map.of(Request.AUTHORIZATION_HEADER, BEARER_PREFIX + session.accessToken()),
          explicitlyIdempotent);
    } catch (MatrixServerException e) {
      if (isTokenError(e)) {
        throw new AuthenticationException(e.getErrcode(), e.getMessage());
      }
      throw e;
    }
  }

  private static boolean isTokenError(MatrixServerException e) {
    String errcode = e.getErrcode();
    return "M_UNKNOWN_TOKEN".equals(errcode)
        || M_MISSING_TOKEN.equals(errcode)
        || "M_INVALID_TOKEN".equals(errcode);
  }

  private static JsonValue fetch(HttpTransport transport, String base, String path) {
    Request request = new Request("GET", base + "/" + path, Map.of(), null);
    HttpTransport.Response response = transport.send(request);
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw MatrixServerException.fromResponse(
          response.statusCode(), parseOrNull(response.body()), response.headers());
    }
    if (response.body() == null || response.body().isBlank()) {
      return new JsonObject();
    }
    return JsonParser.parse(response.body());
  }

  private static JsonValue parseOrNull(String body) {
    if (body == null || body.isBlank()) {
      return null;
    }
    try {
      return JsonParser.parse(body);
    } catch (IllegalArgumentException _) {
      return null;
    }
  }

  /** Builder for {@link MatrixClient}. */
  public static final class Builder {

    private final String homeserverUrl;
    private HttpTransport transport = HttpTransport.create();
    private SessionStore sessionStore = SessionStore.create();
    private MediaTransport mediaTransport = MediaTransport.create();
    private long maxMediaUploadBytes;
    private SyncTokenStore syncTokenStore = SyncTokenStore.inMemory();
    private TransactionIdStore transactionIdStore = TransactionIdStore.inMemory();
    private RetryPolicy retryPolicy = RetryPolicy.defaults();
    private RequestObserver requestObserver = RequestObserver.noop();
    private RetrySleeper retrySleeper = RetrySleeper.threadSleeper();
    private boolean discover;
    private boolean validateVersions;

    private Builder(String homeserverUrl) {
      if (homeserverUrl == null || homeserverUrl.isBlank()) {
        throw new IllegalArgumentException("homeserverUrl is required");
      }
      this.homeserverUrl =
          homeserverUrl.endsWith("/")
              ? homeserverUrl.substring(0, homeserverUrl.length() - 1)
              : homeserverUrl;
    }

    /**
     * Overrides the session store used to persist the authenticated session; defaults to an
     * in-memory store.
     *
     * @param sessionStore the store persisting the authenticated session
     * @return this builder for chaining
     */
    public Builder sessionStore(SessionStore sessionStore) {
      this.sessionStore = sessionStore;
      return this;
    }

    /**
     * Resolves the homeserver URL through {@code /.well-known/matrix/client} at build time; on any
     * discovery failure the explicit base URL is used as fallback.
     *
     * @return this builder for chaining
     */
    public Builder discover() {
      this.discover = true;
      return this;
    }

    /**
     * Fetches and validates {@code /_matrix/client/versions} at build time so malformed capability
     * responses fail fast.
     *
     * @return this builder for chaining
     */
    public Builder validateVersions() {
      this.validateVersions = true;
      return this;
    }

    /**
     * Supplies the optional sync-token store. Sync remains opt-in; this store is consulted only by
     * the sync methods.
     *
     * @param syncTokenStore the store for opaque next_batch tokens
     * @return this builder for chaining
     */
    public Builder syncTokenStore(SyncTokenStore syncTokenStore) {
      this.syncTokenStore = syncTokenStore;
      return this;
    }

    /**
     * Supplies the store that maps logical operation keys to reusable Matrix transaction IDs.
     *
     * @param transactionIdStore the store for idempotent transaction IDs
     * @return this builder for chaining
     */
    public Builder transactionIdStore(TransactionIdStore transactionIdStore) {
      this.transactionIdStore = transactionIdStore;
      return this;
    }

    /**
     * Sets automatic retry limits for idempotent requests; defaults to two retries.
     *
     * @param retryPolicy the retry policy
     * @return this builder for chaining
     */
    public Builder retryPolicy(RetryPolicy retryPolicy) {
      this.retryPolicy = Objects.requireNonNull(retryPolicy, "retryPolicy");
      return this;
    }

    /**
     * Sets an observer for redacted request attempt metadata.
     *
     * @param requestObserver the observer
     * @return this builder for chaining
     */
    public Builder requestObserver(RequestObserver requestObserver) {
      this.requestObserver = Objects.requireNonNull(requestObserver, "requestObserver");
      return this;
    }

    Builder retrySleeper(RetrySleeper retrySleeper) {
      this.retrySleeper = Objects.requireNonNull(retrySleeper, "retrySleeper");
      return this;
    }

    /**
     * Overrides the transport used for media transfers; defaults to a transport backed by {@code
     * java.net.http.HttpClient}.
     *
     * @param mediaTransport the transport used for media transfers
     * @return this builder for chaining
     * @see <a href="https://spec.matrix.org/latest/client-server-api/#content-repository">Matrix
     *     specification</a>
     */
    public Builder mediaTransport(MediaTransport mediaTransport) {
      this.mediaTransport = mediaTransport;
      return this;
    }

    /**
     * Sets the maximum size accepted for media uploads; zero disables the client-side limit.
     *
     * @param maxMediaUploadBytes maximum upload size in bytes, or zero for no limit
     * @return this builder for chaining
     */
    public Builder maxMediaUploadBytes(long maxMediaUploadBytes) {
      if (maxMediaUploadBytes < 0) {
        throw new IllegalArgumentException("maxMediaUploadBytes must not be negative");
      }
      this.maxMediaUploadBytes = maxMediaUploadBytes;
      return this;
    }

    /**
     * Overrides the transport used to reach the homeserver.
     *
     * @param transport the transport used to reach the homeserver
     * @return this builder for chaining
     * @see <a href="https://spec.matrix.org/latest/client-server-api/#api-standards">Matrix
     *     specification</a>
     */
    public Builder transport(HttpTransport transport) {
      this.transport = transport;
      return this;
    }

    /**
     * Builds the client, applying discovery and capability validation if requested.
     *
     * @return the configured client
     */
    public MatrixClient build() {
      return new MatrixClient(this);
    }
  }
}
