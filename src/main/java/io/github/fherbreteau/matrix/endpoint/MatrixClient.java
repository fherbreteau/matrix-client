package io.github.fherbreteau.matrix.endpoint;

import io.github.fherbreteau.matrix.error.AuthenticationException;
import io.github.fherbreteau.matrix.error.MatrixServerException;
import io.github.fherbreteau.matrix.error.RateLimitedException;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonParser;
import io.github.fherbreteau.matrix.json.JsonValue;
import io.github.fherbreteau.matrix.model.Credentials;
import io.github.fherbreteau.matrix.model.Direction;
import io.github.fherbreteau.matrix.model.EventId;
import io.github.fherbreteau.matrix.model.JoinedMembers;
import io.github.fherbreteau.matrix.model.MatrixVersions;
import io.github.fherbreteau.matrix.model.MediaDownload;
import io.github.fherbreteau.matrix.model.MessageBody;
import io.github.fherbreteau.matrix.model.MxcUri;
import io.github.fherbreteau.matrix.model.Presence;
import io.github.fherbreteau.matrix.model.PresenceStatus;
import io.github.fherbreteau.matrix.model.PublicRoomsResponse;
import io.github.fherbreteau.matrix.model.ReadMarkers;
import io.github.fherbreteau.matrix.model.RoomAlias;
import io.github.fherbreteau.matrix.model.RoomAliasResolution;
import io.github.fherbreteau.matrix.model.RoomCreation;
import io.github.fherbreteau.matrix.model.RoomEvent;
import io.github.fherbreteau.matrix.model.RoomId;
import io.github.fherbreteau.matrix.model.RoomMessagesPage;
import io.github.fherbreteau.matrix.model.Session;
import io.github.fherbreteau.matrix.model.SessionStore;
import io.github.fherbreteau.matrix.model.ThumbnailMethod;
import io.github.fherbreteau.matrix.model.UserId;
import io.github.fherbreteau.matrix.model.UserProfile;
import io.github.fherbreteau.matrix.model.WhoamiResponse;
import io.github.fherbreteau.matrix.transport.HttpTransport;
import io.github.fherbreteau.matrix.transport.HttpTransport.Request;
import io.github.fherbreteau.matrix.transport.MediaTransport;
import io.github.fherbreteau.matrix.transport.MediaTransport.BinaryRequest;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

  private static final String M_MISSING_TOKEN = "M_MISSING_TOKEN";
  private static final String NO_SESSION_MESSAGE = "No authenticated session";
  private static final String USER_PATH = "_matrix/client/v3/user/";
  private static final String ACCOUNT_DATA_PATH = "/account_data/"; // NOSONAR
  private static final String USER_ID_FIELD = "user_id";
  private static final String REASON_FIELD = "reason";
  private static final String EVENT_ID_FIELD = "event_id";
  private static final String CHUNK_FIELD = "chunk";
  private static final String ROOMS_PATH = "_matrix/client/v3/rooms/";
  private static final String DIR_QUERY_PARAM = "&dir=";
  private static final String LIMIT_QUERY_PARAM = "&limit=";
  private static final String DIRECTORY_PATH = "_matrix/client/v3/directory/room/";
  private static final String PROFILE_PATH = "_matrix/client/v3/profile/";
  private static final String ROOM_ID_FIELD = "room_id";

  private final HttpTransport transport;
  private final String homeserverUrl;
  private final MatrixVersions versions;
  private final DiscoveredHomeserver discovery;
  private final SessionStore sessionStore;
  private final MediaTransport mediaTransport;

  private MatrixClient(Builder builder) {
    this.transport = builder.transport;
    this.mediaTransport = builder.mediaTransport;
    this.sessionStore = builder.sessionStore;
    if (builder.discover) {
      this.discovery = HomeserverDiscovery.discover(builder.transport, builder.homeserverUrl);
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
      Session session = Session.from(post("_matrix/client/v3/login", body));
      sessionStore.save(session);
      return session;
    } catch (RateLimitedException e) {
      throw e;
    } catch (MatrixServerException e) {
      throw new AuthenticationException(e.getErrcode(), e.getMessage());
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
            "_matrix/client/v1/rooms/"
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
    return sendEvent(roomId, eventType, content, UUID.randomUUID().toString());
  }

  /**
   * Sends a message event to a room with an explicit transaction identifier for idempotent retries.
   *
   * @param roomId the room to send the event to
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
    String eventId =
        authenticated(
                "PUT",
                ROOMS_PATH
                    + encode(roomId.value())
                    + "/send/"
                    + encode(eventType)
                    + "/"
                    + encode(transactionId),
                content)
            .asObject()
            .get(EVENT_ID_FIELD)
            .asString();
    return EventId.of(eventId);
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
    var body = new JsonObject();
    if (reason != null) {
      body.put(REASON_FIELD, reason);
    }
    return EventId.of(
        authenticated(
                "PUT",
                ROOMS_PATH
                    + encode(roomId.value())
                    + "/redact/"
                    + encode(eventId.value())
                    + "/"
                    + UUID.randomUUID(),
                body)
            .asObject()
            .get(EVENT_ID_FIELD)
            .asString());
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
      authenticated("DELETE", PROFILE_PATH + encode(userId.value()) + "/" + encode(keyName), null);
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
    authenticated("DELETE", DIRECTORY_PATH + encode(roomAlias.value()), null);
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
   * Sends a read receipt for an event in a room.
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
    var path =
        ROOMS_PATH
            + encode(roomId.value())
            + "/receipt/"
            + encode(receiptType)
            + '/'
            + encode(eventId.value());
    JsonValue body = threadId == null ? null : new JsonObject().put("thread_id", threadId);
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
      path.append("/rooms/").append(encode(roomId.value()));
    }
    return path.append(ACCOUNT_DATA_PATH).append(encode(type)).toString();
  }

  private String currentUserId() {
    return getSession()
        .orElseThrow(() -> new AuthenticationException(M_MISSING_TOKEN, NO_SESSION_MESSAGE))
        .userId();
  }

  /**
   * Uploads raw bytes to the content repository and returns their Matrix content URI.
   *
   * @param content the media bytes
   * @param contentType the MIME type of the media, sent as the {@code Content-Type} header
   * @param filename the optional filename presented to other users
   * @return the {@code mxc://} URI of the uploaded media
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   */
  public MxcUri uploadMedia(byte[] content, String contentType, String filename) {
    var path = new StringBuilder("_matrix/media/v3/upload");
    if (filename != null) {
      path.append("?filename=").append(encode(filename));
    }
    var headers = new HashMap<String, String>();
    headers.put("Content-Type", contentType);
    var response =
        mediaTransport.send(
            new BinaryRequest(
                "POST", homeserverUrl + "/" + path, authHeaders(), content, contentType));
    throwIfError(
        response.statusCode(),
        response.header("content-type"),
        asString(response),
        response.retryAfterMs());
    JsonObject body = JsonParser.parse(asString(response)).asObject();
    JsonValue uri = body.get("content_uri");
    if (uri == null || !uri.isString()) {
      throw new MatrixServerException(
          response.statusCode(), "M_UNKNOWN", "Upload response must contain content_uri");
    }
    return MxcUri.parse(uri.asString());
  }

  /**
   * Downloads media from the content repository using the authenticated v1.11 endpoint. The
   * response body streams through {@link MediaTransport.BinaryResponse#bodyStream()}; callers must
   * close it.
   *
   * @param uri the {@code mxc://} URI of the media
   * @param maxBytes the maximum accepted media size in bytes; a larger response raises {@link
   *     io.github.fherbreteau.matrix.error.MatrixServerException} with {@code M_TOO_LARGE}
   * @return the downloaded media response
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
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
    var response =
        mediaTransport.send(
            new BinaryRequest(
                "GET",
                homeserverUrl + "/" + path,
                authHeaders(),
                null,
                "application/octet-stream"));
    throwIfError(
        response.statusCode(),
        response.header("content-type"),
        asString(response),
        response.retryAfterMs());
    if (maxBytes > 0 && response.contentLength() > maxBytes) {
      throw new MatrixServerException(
          response.statusCode(), "M_TOO_LARGE", "Media exceeds the configured maximum size");
    }
    return new MediaDownload(
        response.header("content-type"),
        response.header("content-disposition"),
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
    var response =
        mediaTransport.send(
            new BinaryRequest(
                "GET",
                homeserverUrl + "/" + query,
                authHeaders(),
                null,
                "application/octet-stream"));
    throwIfError(
        response.statusCode(),
        response.header("content-type"),
        asString(response),
        response.retryAfterMs());
    if (maxBytes > 0 && response.contentLength() > maxBytes) {
      throw new MatrixServerException(
          response.statusCode(), "M_TOO_LARGE", "Thumbnail exceeds the configured maximum size");
    }
    return new MediaDownload(
        response.header("content-type"),
        response.header("content-disposition"),
        response.bodyStream());
  }

  /**
   * Retrieves the upload size limits configured on the homeserver.
   *
   * @return the maximum upload size in bytes, or empty when the homeserver does not advertise one
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
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
    return Map.of(Request.AUTHORIZATION_HEADER, "Bearer " + session.accessToken());
  }

  private void throwIfError(int statusCode, String contentType, String body, Long retryAfterMs) {
    if (statusCode >= 200 && statusCode < 300) {
      return;
    }
    JsonValue parsed = parseOrNull(body);
    MatrixServerException exception = MatrixServerException.fromResponse(statusCode, parsed, null);
    throw exception;
  }

  private static String asString(
      io.github.fherbreteau.matrix.transport.MediaTransport.BinaryResponse response) {
    try (var stream = response.bodyStream()) {
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (java.io.IOException e) {
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
    return request("GET", path, null);
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
    return request("POST", path, body);
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
    return request(method, path, body, Map.of());
  }

  private JsonValue request(
      String method, String path, JsonValue body, Map<String, String> headers) {
    String requestBody = body == null ? null : body.toJson();
    Request request = new Request(method, homeserverUrl + "/" + path, headers, requestBody);
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
          Map.of(Request.AUTHORIZATION_HEADER, "Bearer " + session.accessToken()));
    } catch (MatrixServerException e) {
      if (isTokenError(e)) {
        throw new AuthenticationException(e.getErrcode(), e.getMessage());
      }
      throw e;
    }
  }

  private static boolean isTokenError(MatrixServerException e) {
    String errcode = e.getErrcode();
    return e.getStatusCode() == 401
        || "M_UNKNOWN_TOKEN".equals(errcode)
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
     * Overrides the transport used to reach the homeserver.
     *
     * @param transport the transport used to reach the homeserver
     * @return this builder for chaining
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
