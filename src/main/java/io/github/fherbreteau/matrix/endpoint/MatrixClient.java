package io.github.fherbreteau.matrix.endpoint;

import io.github.fherbreteau.matrix.error.AuthenticationException;
import io.github.fherbreteau.matrix.error.MatrixServerException;
import io.github.fherbreteau.matrix.error.RateLimitedException;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonParser;
import io.github.fherbreteau.matrix.json.JsonValue;
import io.github.fherbreteau.matrix.model.Credentials;
import io.github.fherbreteau.matrix.model.EventId;
import io.github.fherbreteau.matrix.model.JoinedMembers;
import io.github.fherbreteau.matrix.model.MatrixVersions;
import io.github.fherbreteau.matrix.model.PublicRoomsResponse;
import io.github.fherbreteau.matrix.model.RoomAlias;
import io.github.fherbreteau.matrix.model.RoomAliasResolution;
import io.github.fherbreteau.matrix.model.RoomCreation;
import io.github.fherbreteau.matrix.model.RoomEvent;
import io.github.fherbreteau.matrix.model.RoomId;
import io.github.fherbreteau.matrix.model.Session;
import io.github.fherbreteau.matrix.model.SessionStore;
import io.github.fherbreteau.matrix.model.UserId;
import io.github.fherbreteau.matrix.model.UserProfile;
import io.github.fherbreteau.matrix.transport.HttpTransport;
import io.github.fherbreteau.matrix.transport.HttpTransport.Request;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
  private static final String USER_ID_FIELD = "user_id";
  private static final String DEVICE_ID_FIELD = "device_id";
  private static final String REASON_FIELD = "reason";
  private static final String EVENT_ID_FIELD = "event_id";
  private static final String CHUNK_FIELD = "chunk";
  private static final String ROOMS_PATH = "_matrix/client/v3/rooms/";
  private static final String DIRECTORY_PATH = "_matrix/client/v3/directory/room/";
  private static final String PROFILE_PATH = "_matrix/client/v3/profile/";
  private static final String ROOM_ID_FIELD = "room_id";

  private final HttpTransport transport;
  private final String homeserverUrl;
  private final MatrixVersions versions;
  private final DiscoveredHomeserver discovery;
  private final SessionStore sessionStore;

  private MatrixClient(Builder builder) {
    this.transport = builder.transport;
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
   */
  public static Builder builder(String homeserverUrl) {
    return new Builder(homeserverUrl);
  }

  /**
   * Returns the transport used to reach the homeserver.
   *
   * @return the transport used to reach the homeserver
   */
  public HttpTransport getTransport() {
    return transport;
  }

  public String getHomeserverUrl() {
    return homeserverUrl;
  }

  /**
   * Returns the result of the discovery performed at build time, or {@code null} when discovery was
   * not requested.
   *
   * @return the discovery result, or {@code null} when discovery was not requested
   */
  public DiscoveredHomeserver getDiscovery() {
    return discovery;
  }

  /**
   * Returns the validated capabilities of the homeserver when version validation was requested at
   * build time, or {@code null} otherwise.
   *
   * @return the validated capabilities, or {@code null} when not validated
   */
  public MatrixVersions getCapabilities() {
    return versions;
  }

  /**
   * Retrieves the homeserver's supported Matrix spec versions.
   *
   * @return the parsed {@code /versions} response
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
   */
  public Session login(
      Credentials credentials, String deviceDisplayName, boolean requestRefreshToken) {
    var body = (JsonObject) credentials.toJson();
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
   */
  public Session refresh() {
    Session session =
        sessionStore
            .current()
            .orElseThrow(
                () -> new AuthenticationException(M_MISSING_TOKEN, "No authenticated session"));
    if (!session.isRefreshable()) {
      throw new AuthenticationException(M_MISSING_TOKEN, "Session is not refreshable");
    }
    try {
      Session refreshed =
          Session.from(
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
   */
  public RoomId createRoom(RoomCreation creation) {
    return RoomId.of(
        authenticated("POST", "_matrix/client/v3/createRoom", creation.toJson().toJson())
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
   */
  public RoomId joinRoom(RoomId roomId) {
    return RoomId.of(
        authenticated("POST", "_matrix/client/v3/join/" + encode(roomId.value()), "{}")
            .asObject()
            .get(ROOM_ID_FIELD)
            .asString());
  }

  /**
   * Joins a room by one of its aliases and returns the joined room.
   *
   * @param roomAlias the room alias to join
   * @return the joined room identifier
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   */
  public RoomId joinRoom(RoomAlias roomAlias) {
    return RoomId.of(
        authenticated("POST", "_matrix/client/v3/join/" + encode(roomAlias.value()), "{}")
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
   */
  public void leaveRoom(RoomId roomId) {
    authenticated("POST", ROOMS_PATH + encode(roomId.value()) + "/leave", "{}");
  }

  /**
   * Invites a user to a room.
   *
   * @param roomId the room to invite the user to
   * @param userId the user to invite
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   */
  public void invite(RoomId roomId, UserId userId) {
    authenticated(
        "POST",
        ROOMS_PATH + encode(roomId.value()) + "/invite",
        new JsonObject().put(USER_ID_FIELD, userId.value()).toJson());
  }

  /**
   * Retrieves the full state of a room. Unknown event types are preserved.
   *
   * @param roomId the room whose state to retrieve
   * @return the full room state, preserving unknown event types
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
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
   */
  public Optional<RoomAlias> getCanonicalAlias(RoomId roomId) {
    return getRoomStateField(roomId, "m.room.canonical_alias", "alias").map(RoomAlias::of);
  }

  /**
   * Resolves a room alias to its room identifier and candidate servers.
   *
   * @param roomAlias the alias to resolve
   * @return the room identifier and candidate servers
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
   */
  public void kick(RoomId roomId, UserId userId, String reason) {
    var body = new JsonObject().put(USER_ID_FIELD, userId.value());
    if (reason != null) {
      body.put(REASON_FIELD, reason);
    }
    authenticated("POST", ROOMS_PATH + encode(roomId.value()) + "/kick", body.toJson());
  }

  /**
   * Bans a user from a room, preventing them from joining it.
   *
   * @param roomId the room to ban the user from
   * @param userId the user to ban
   * @param reason the optional reason for the ban
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   */
  public void ban(RoomId roomId, UserId userId, String reason) {
    var body = new JsonObject().put(USER_ID_FIELD, userId.value());
    if (reason != null) {
      body.put(REASON_FIELD, reason);
    }
    authenticated("POST", ROOMS_PATH + encode(roomId.value()) + "/ban", body.toJson());
  }

  /**
   * Removes a previous ban on a user so they can join the room again.
   *
   * @param roomId the room to unban the user from
   * @param userId the user to unban
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   */
  public void unban(RoomId roomId, UserId userId) {
    authenticated(
        "POST",
        ROOMS_PATH + encode(roomId.value()) + "/unban",
        new JsonObject().put(USER_ID_FIELD, userId.value()).toJson());
  }

  /**
   * Forgets a room the user has left; the room is removed from the room list.
   *
   * @param roomId the room to forget
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   */
  public void forget(RoomId roomId) {
    authenticated("POST", ROOMS_PATH + encode(roomId.value()) + "/forget", "{}");
  }

  /**
   * Retrieves the membership events of a room.
   *
   * @param roomId the room whose members to retrieve
   * @return the membership events of the room
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
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
   * Sends a message event to a room with a generated transaction identifier.
   *
   * @param roomId the room to send the event to
   * @param eventType the event type
   * @param content the event content
   * @return the created event identifier
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
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
                content.toJson())
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
                content.toJson())
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
                body.toJson())
            .asObject()
            .get(EVENT_ID_FIELD)
            .asString());
  }

  /**
   * Retrieves the profile of a user.
   *
   * @param userId the user whose profile to retrieve
   * @return the user profile
   */
  public UserProfile getProfile(UserId userId) {
    return UserProfile.from(get(PROFILE_PATH + encode(userId.value())));
  }

  /**
   * Sets the display name of a user.
   *
   * @param userId the user to update
   * @param displayName the display name to set
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   */
  public void setDisplayName(UserId userId, String displayName) {
    authenticated(
        "PUT",
        PROFILE_PATH + encode(userId.value()) + "/displayname",
        new JsonObject().put("displayname", displayName).toJson());
  }

  /**
   * Sets the avatar URL of a user.
   *
   * @param userId the user to update
   * @param avatarUrl the avatar URL to set
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   */
  public void setAvatarUrl(UserId userId, String avatarUrl) {
    authenticated(
        "PUT",
        PROFILE_PATH + encode(userId.value()) + "/avatar_url",
        new JsonObject().put("avatar_url", avatarUrl).toJson());
  }

  /**
   * Creates a room alias pointing to a room in the room directory.
   *
   * @param roomAlias the alias to create
   * @param roomId the room the alias points to
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   */
  public void createRoomAlias(RoomAlias roomAlias, RoomId roomId) {
    authenticated(
        "PUT",
        DIRECTORY_PATH + encode(roomAlias.value()),
        new JsonObject().put(ROOM_ID_FIELD, roomId.value()).toJson());
  }

  /**
   * Deletes a room alias from the room directory.
   *
   * @param roomAlias the alias to delete
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   */
  public void deleteRoomAlias(RoomAlias roomAlias) {
    authenticated("DELETE", DIRECTORY_PATH + encode(roomAlias.value()), null);
  }

  /**
   * Retrieves the public room directory.
   *
   * @return the public rooms
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
    return PublicRoomsResponse.from(post("_matrix/client/v3/publicRooms", body));
  }

  /**
   * Retrieves the identity of the user the current access token belongs to.
   *
   * @return the session of the token owner
   * @throws io.github.fherbreteau.matrix.error.AuthenticationException if there is no session or
   *     the token is no longer valid
   */
  public Session whoami() {
    JsonValue body = authenticated("GET", "_matrix/client/v3/account/whoami", null);
    JsonObject obj = body.asObject();
    return new Session(
        obj.get(USER_ID_FIELD).asString(),
        null,
        null,
        null,
        obj.get(DEVICE_ID_FIELD) != null && obj.get(DEVICE_ID_FIELD).isString()
            ? obj.get(DEVICE_ID_FIELD).asString()
            : null,
        null,
        body);
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
   */
  public JsonValue post(String path, JsonValue body) {
    return request("POST", path, body == null ? null : body.toJson());
  }

  /**
   * Performs an HTTP request against the homeserver and returns the parsed JSON response.
   *
   * @param method the HTTP method
   * @param path the endpoint path relative to the homeserver URL
   * @param body the JSON request body, or {@code null} for none
   * @return the parsed JSON response
   */
  public JsonValue request(String method, String path, String body) {
    return request(method, path, body, Map.of());
  }

  private JsonValue request(String method, String path, String body, Map<String, String> headers) {
    Request request = new Request(method, homeserverUrl + "/" + path, headers, body);
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

  private JsonValue authenticated(String method, String path, String body) {
    Session session =
        sessionStore
            .current()
            .orElseThrow(
                () -> new AuthenticationException(M_MISSING_TOKEN, "No authenticated session"));
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
