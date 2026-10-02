package io.github.fherbreteau.matrix.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.fherbreteau.matrix.endpoint.MatrixClient;
import io.github.fherbreteau.matrix.error.AuthenticationException;
import io.github.fherbreteau.matrix.json.JsonParser;
import io.github.fherbreteau.matrix.model.AccountRequest;
import io.github.fherbreteau.matrix.model.Direction;
import io.github.fherbreteau.matrix.model.EventFilter;
import io.github.fherbreteau.matrix.model.EventId;
import io.github.fherbreteau.matrix.model.MatrixFilter;
import io.github.fherbreteau.matrix.model.MessageBody;
import io.github.fherbreteau.matrix.model.MxcUri;
import io.github.fherbreteau.matrix.model.PasswordCredentials;
import io.github.fherbreteau.matrix.model.PresenceStatus;
import io.github.fherbreteau.matrix.model.PublicRoomsResponse;
import io.github.fherbreteau.matrix.model.ReadMarkers;
import io.github.fherbreteau.matrix.model.RegistrationRequest;
import io.github.fherbreteau.matrix.model.RoomAlias;
import io.github.fherbreteau.matrix.model.RoomCreation;
import io.github.fherbreteau.matrix.model.RoomEvent;
import io.github.fherbreteau.matrix.model.RoomId;
import io.github.fherbreteau.matrix.model.RoomMessagesPage;
import io.github.fherbreteau.matrix.model.SyncOptions;
import io.github.fherbreteau.matrix.model.UserId;
import io.github.fherbreteau.matrix.retry.RetryPolicy;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

@Execution(ExecutionMode.SAME_THREAD)
class SynapseContainerIT {

  private static final String SERVER_NAME = "localhost";
  private static final String PASSWORD = UUID.randomUUID().toString();
  private static final String MEMBER_PASSWORD = UUID.randomUUID().toString();
  private static final String SHARED_SECRET = UUID.randomUUID().toString();
  private static final String IMAGE = "matrixdotorg/synapse:v1.162.0";
  private static Path dataDirectory;
  private static GenericContainer<?> synapse;
  private static MatrixClient client;
  private static RoomId moderationRoom;

  @BeforeAll
  static void startSynapseAndLogin() throws IOException, InterruptedException {
    dataDirectory = Files.createTempDirectory(Path.of("target"), "synapse-it-");
    generateConfig();
    configureRegistrationSecret();
    synapse = startServer();
    createDisposableUser();
    client = MatrixClient.builder(homeserverUrl()).retryPolicy(RetryPolicy.disabled()).build();
    assertThat(client.getSupportedVersions().getVersions()).isNotEmpty();
    var session = client.login(new PasswordCredentials("@integration:localhost", PASSWORD));
    assertThat(session.userId()).isEqualTo("@integration:localhost");
    assertThat(client.whoami().userId()).isEqualTo(session.userId());
    moderationRoom = createPrivateRoom();
  }

  @AfterAll
  static void stopSynapseAndCleanUp() throws IOException {
    if (client != null) {
      if (moderationRoom != null) {
        try {
          client.leaveRoom(moderationRoom);
          client.forget(moderationRoom);
        } catch (RuntimeException _) {
          Thread.onSpinWait();
        }
      }
      client.logout();
    }
    if (synapse != null) {
      synapse.stop();
    }
    deleteDataDirectory();
  }

  @Test
  void exercisesRoomStateMessagingHistoryAndReceipts() {
    RoomId roomId = createPrivateRoom();
    try {
      assertThat(client.getRoomName(roomId)).isPresent();
      assertThat(client.getJoinedMembers(roomId).displayNames())
          .containsKey("@integration:localhost");
      EventId topicEvent =
          client.sendStateEvent(
              roomId, "m.room.topic", JsonParser.parse("{\"topic\":\"updated topic\"}"));
      assertThat(topicEvent.value()).startsWith("$");
      assertThat(client.getRoomState(roomId)).extracting(RoomEvent::type).contains("m.room.topic");
      EventId firstMessage = client.sendText(roomId, "first integration message");
      EventId secondMessage =
          client.sendMessage(roomId, MessageBody.notice("second integration message"));
      RoomEvent retrieved = client.getRoomEvent(roomId, firstMessage);
      assertThat(retrieved.eventId()).isEqualTo(firstMessage.value());
      assertThat(
              client
                  .getEventForTimestamp(roomId, retrieved.originServerTs(), Direction.BACKWARD)
                  .eventId())
          .isNotBlank();
      RoomMessagesPage latest = client.getLatestRoomMessages(roomId, 10);
      assertThat(latest.chunk())
          .extracting(RoomEvent::eventId)
          .contains(firstMessage.value(), secondMessage.value());
      assertThat(latest.start()).isNotBlank();
      assertThat(client.getRoomMessages(roomId, latest.start(), Direction.BACKWARD, 10).chunk())
          .isNotEmpty();
      client.sendReceipt(roomId, "m.read", secondMessage, "main");
      client.sendReadMarkers(
          roomId,
          ReadMarkers.builder()
              .fullyRead(secondMessage.value())
              .read(secondMessage.value())
              .build());
      assertThat(client.redact(roomId, secondMessage, "integration cleanup").value())
          .startsWith("$");
    } finally {
      leaveAndForget(roomId);
    }
  }

  @Test
  void exercisesInviteKickBanUnbanAndJoinedRoomMethods() {
    MatrixClient secondClient = loginAsOtherUser();
    UserId secondUser = UserId.of("@integration-member:localhost");
    RoomId roomId = moderationRoom;
    try {
      assertThat(client.getJoinedRooms()).contains(roomId);
      secondClient.joinRoom(roomId);
      assertThat(client.getMembers(roomId))
          .extracting(RoomEvent::stateKey)
          .contains("@integration-member:localhost");
      client.kick(roomId, secondUser, "integration kick");
      assertThat(client.getMembers(roomId))
          .extracting(RoomEvent::stateKey)
          .contains("@integration:localhost");
      client.ban(roomId, secondUser, "integration ban");
      client.unban(roomId, secondUser);
      client.invite(roomId, secondUser);
      secondClient.joinRoom(roomId);
      assertThat(client.getJoinedMembers(roomId).displayNames())
          .containsKey("@integration-member:localhost");
      secondClient.leaveRoom(roomId);
    } finally {
      try {
        secondClient.leaveRoom(roomId);
      } finally {
        secondClient.logout();
      }
    }
  }

  @Test
  void exercisesPresenceAndAuthenticatedPublicDirectoryMethods() {
    UserId userId = UserId.of("@integration:localhost");
    PresenceStatus initial = client.getPresence(userId);
    assertThat(initial.presence()).isNotNull();
    PublicRoomsResponse publicRooms = client.getPublicRooms();
    assertThat(publicRooms.chunk()).isNotNull();
    assertThat(client.getPublicRooms(10, null, null).chunk()).hasSizeLessThanOrEqualTo(10);
    assertThat(client.getPresence(userId).presence()).isEqualTo(initial.presence());
  }

  @Test
  void exercisesProfileAccountDataDirectoryAndTyping() {
    RoomId roomId = createPrivateRoom();
    UserId userId = UserId.of("@integration:localhost");
    RoomAlias alias = RoomAlias.of("#it-" + UUID.randomUUID() + ":localhost");
    try {
      String displayName = "Integration User " + UUID.randomUUID();
      client.setDisplayName(userId, displayName);
      assertThat(client.getProfile(userId).displayName()).isEqualTo(displayName);
      client.setDisplayName(userId, null);
      String accountDataType =
          "org.example.integration." + UUID.randomUUID().toString().replace('-', '_');
      client.setAccountData(accountDataType, JsonParser.parse("{\"enabled\":true}"));
      assertThat(client.getAccountData(accountDataType))
          .hasValueSatisfying(
              value -> assertThat(value.asObject().get("enabled").asBoolean()).isTrue());
      client.createRoomAlias(alias, roomId);
      assertThat(client.resolveRoomAlias(alias).roomId()).isEqualTo(roomId);
      assertThat(client.getRoomAliases(roomId)).contains(alias);
      client.deleteRoomAlias(alias);
      client.setTyping(roomId, true, 1_000);
      client.setTyping(roomId, false, 0);
    } finally {
      client.leaveRoom(roomId);
      client.forget(roomId);
    }
  }

  @Test
  void exercisesServerSideFiltersAndOpaqueSyncTokens() {
    String accountDataType =
        "org.example.integration." + UUID.randomUUID().toString().replace('-', '_');
    MatrixFilter filter =
        MatrixFilter.builder()
            .accountData(EventFilter.builder().types(List.of(accountDataType)).build())
            .build();
    String filterId = client.createFilter(filter);
    assertThat(filterId).isNotBlank();
    assertThat(client.getFilter(filterId).toJson().isObject()).isTrue();
    var sync = client.sync(SyncOptions.initial(0, filterId));
    assertThat(sync.nextBatch()).isNotBlank();
    assertThat(client.syncToken()).contains(sync.nextBatch());
    client.clearSyncToken();
    assertThat(client.syncToken()).isEmpty();
  }

  @Test
  void exercisesMediaConfigUploadAndDownload() throws IOException {
    assertThat(client.getMediaConfig()).isPresent();
    byte[] payload = "synapse integration media".getBytes(StandardCharsets.UTF_8);
    MxcUri uri = client.uploadMedia(payload, "text/plain", "integration.txt");
    try (var download = client.downloadMedia(uri, 1_024)) {
      assertThat(download.body().readAllBytes()).containsExactly(payload);
    }
  }

  @Test
  void checksRegistrationAvailabilityAndRegistersUser() {
    String localpart = "it-" + UUID.randomUUID().toString().replace('-', '_');
    var availability = client.isUsernameAvailable(localpart);
    assertThat(availability.available()).isTrue();
    assertThat(client.isRegistrationTokenValid("invalid-" + localpart).valid()).isFalse();
    var registration =
        RegistrationRequest.builder()
            .username(localpart)
            .password(UUID.randomUUID().toString())
            .inhibitLogin(true)
            .build();
    assertThatThrownBy(() -> client.register(registration))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("HTTP 401");
  }

  @Test
  void changesPasswordWithInteractiveAuthenticationAndRestoresIt() {
    String changedPassword = UUID.randomUUID().toString();
    try {
      client.changePassword(
          AccountRequest.builder()
              .newPassword(changedPassword)
              .logoutDevices(false)
              .put(
                  "auth",
                  JsonParser.parse(
                      "{\"type\":\"m.login.password\",\"identifier\":{\"type\":\"m.id.user\",\"user\":\"@integration:localhost\"},\"password\":\""
                          + PASSWORD
                          + "\"}"))
              .build());
      MatrixClient changedPasswordClient =
          MatrixClient.builder(homeserverUrl()).retryPolicy(RetryPolicy.disabled()).build();
      var changedSession =
          changedPasswordClient.login(
              new PasswordCredentials("@integration:localhost", changedPassword));
      assertThat(changedSession.userId()).isEqualTo("@integration:localhost");
      changedPasswordClient.logout();
    } finally {
      var currentSession = client.getSession();
      if (currentSession.isPresent()) {
        client.changePassword(
            AccountRequest.builder()
                .newPassword(PASSWORD)
                .logoutDevices(false)
                .put(
                    "auth",
                    JsonParser.parse(
                        "{\"type\":\"m.login.password\",\"identifier\":{\"type\":\"m.id.user\",\"user\":\"@integration:localhost\"},\"password\":\""
                            + changedPassword
                            + "\"}"))
                .build());
      }
    }
  }

  @Test
  void rejectsInvalidCredentialsWithoutLeakingThePassword() {
    MatrixClient unauthenticatedClient =
        MatrixClient.builder(homeserverUrl()).retryPolicy(RetryPolicy.disabled()).build();
    PasswordCredentials invalidCredentials =
        new PasswordCredentials("@integration:localhost", "invalid-integration-password");
    assertThatThrownBy(() -> login(unauthenticatedClient, invalidCredentials))
        .isInstanceOf(AuthenticationException.class)
        .hasMessageNotContaining("invalid-integration-password");
  }

  private static void generateConfig() throws IOException {
    String uid = Files.getAttribute(dataDirectory, "unix:uid").toString();
    String gid = Files.getAttribute(dataDirectory, "unix:gid").toString();
    try (@SuppressWarnings("resource")
        var generator =
            new GenericContainer<>(DockerImageName.parse(IMAGE))
                .withFileSystemBind(
                    dataDirectory.toAbsolutePath().toString(), "/data", BindMode.READ_WRITE)
                .withEnv("SYNAPSE_SERVER_NAME", SERVER_NAME)
                .withEnv("SYNAPSE_REPORT_STATS", "no")
                .withEnv("UID", uid)
                .withEnv("GID", gid)
                .withCommand(
                    "generate",
                    "--config-path",
                    "/data/homeserver.yaml",
                    "--server-name",
                    SERVER_NAME,
                    "--report-stats",
                    "no")
                .withStartupCheckStrategy(
                    new OneShotStartupCheckStrategy().withTimeout(Duration.ofMinutes(2)))) {
      generator.start();
      if (generator.getCurrentContainerInfo().getState().getExitCodeLong() != 0) {
        throw new IllegalStateException("Synapse configuration generation failed");
      }
    }
  }

  private static void configureRegistrationSecret() throws IOException {
    Files.writeString(
        dataDirectory.resolve("homeserver.yaml"),
        "\n"
            + "enable_registration: true\n"
            + "enable_registration_without_verification: true\n"
            + "registration_shared_secret: \""
            + SHARED_SECRET
            + "\"\npublic_baseurl: \"http://localhost:8008/\"\n"
            + "default_room_version: \"10\"\n"
            + "allow_public_rooms_without_auth: true\n"
            + "presence:\n  enabled: true\n"
            + "rc_message:\n  per_second: 100\n  burst_count: 100\n"
            + "rc_registration:\n  per_second: 100\n  burst_count: 100\n"
            + "rc_login:\n  address:\n    per_second: 100\n    burst_count: 100\n"
            + "  account:\n    per_second: 100\n    burst_count: 100\n"
            + "  failed_attempts:\n    per_second: 100\n    burst_count: 100\n",
        StandardCharsets.UTF_8,
        StandardOpenOption.APPEND);
  }

  @SuppressWarnings("resource")
  private static GenericContainer<?> startServer() throws IOException {
    String uid = Files.getAttribute(dataDirectory, "unix:uid").toString();
    String gid = Files.getAttribute(dataDirectory, "unix:gid").toString();
    var container =
        new GenericContainer<>(DockerImageName.parse(IMAGE))
            .withFileSystemBind(
                dataDirectory.toAbsolutePath().toString(), "/data", BindMode.READ_WRITE)
            .withEnv("UID", uid)
            .withEnv("GID", gid)
            .withCommand("run", "--config-path", "/data/homeserver.yaml")
            .withExposedPorts(8008)
            .waitingFor(Wait.forHttp("/_matrix/client/versions").forStatusCode(200))
            .withStartupTimeout(Duration.ofMinutes(3));
    container.start();
    return container;
  }

  private static void createDisposableUser() throws IOException, InterruptedException {
    registerUser("integration", PASSWORD, true);
    registerUser("integration-member", MEMBER_PASSWORD, false);
  }

  private static void registerUser(String localpart, String password, boolean admin)
      throws IOException, InterruptedException {
    var command =
        new ArrayList<>(
            List.of(
                "register_new_matrix_user",
                "-c",
                "/data/homeserver.yaml",
                "-u",
                localpart,
                "-p",
                password));
    command.add(admin ? "-a" : "--no-admin");
    command.add("http://localhost:8008");
    var result = synapse.execInContainer(command.toArray(String[]::new));
    if (result.getExitCode() != 0) {
      String details =
          (result.getStdout() + " " + result.getStderr())
              .replace(PASSWORD, "***")
              .replace(MEMBER_PASSWORD, "***")
              .replace(SHARED_SECRET, "***")
              .strip();
      throw new IllegalStateException(
          "Synapse integration user creation failed (exit "
              + result.getExitCode()
              + "): "
              + details);
    }
  }

  private static void login(MatrixClient client, PasswordCredentials credentials) {
    client.login(credentials);
  }

  private static MatrixClient loginAsOtherUser() {
    MatrixClient secondClient =
        MatrixClient.builder(homeserverUrl()).retryPolicy(RetryPolicy.disabled()).build();
    secondClient.login(new PasswordCredentials("@integration-member:localhost", MEMBER_PASSWORD));
    return secondClient;
  }

  private static RoomId createPrivateRoom() {
    return client.createRoom(
        RoomCreation.builder()
            .name("integration-" + UUID.randomUUID())
            .invites(List.of(UserId.of("@integration-member:localhost")))
            .powerLevelContentOverride(
                JsonParser.parse("{\"users\":{\"@integration:localhost\":100}}"))
            .build());
  }

  private static void leaveAndForget(RoomId roomId) {
    client.leaveRoom(roomId);
    client.forget(roomId);
  }

  private static String homeserverUrl() {
    return "http://" + synapse.getHost() + ":" + synapse.getMappedPort(8008);
  }

  private static void deleteDataDirectory() throws IOException {
    if (dataDirectory == null) {
      return;
    }
    try (var paths = Files.walk(dataDirectory)) {
      for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
        Files.deleteIfExists(path);
      }
    }
  }
}
