package io.github.fherbreteau.matrix.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import io.github.fherbreteau.matrix.endpoint.MatrixClient;
import io.github.fherbreteau.matrix.error.AuthenticationException;
import io.github.fherbreteau.matrix.json.JsonNull;
import io.github.fherbreteau.matrix.json.JsonNumber;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonParser;
import io.github.fherbreteau.matrix.json.JsonString;
import io.github.fherbreteau.matrix.json.JsonValue;
import io.github.fherbreteau.matrix.model.AccountRequest;
import io.github.fherbreteau.matrix.model.ContentReport;
import io.github.fherbreteau.matrix.model.CrossSigningKey;
import io.github.fherbreteau.matrix.model.Device;
import io.github.fherbreteau.matrix.model.DeviceId;
import io.github.fherbreteau.matrix.model.DeviceInformation;
import io.github.fherbreteau.matrix.model.DeviceSigningUploadRequest;
import io.github.fherbreteau.matrix.model.DeviceUpdateRequest;
import io.github.fherbreteau.matrix.model.Direction;
import io.github.fherbreteau.matrix.model.EventFilter;
import io.github.fherbreteau.matrix.model.EventId;
import io.github.fherbreteau.matrix.model.KeySignaturesUploadRequest;
import io.github.fherbreteau.matrix.model.KeysClaimRequest;
import io.github.fherbreteau.matrix.model.KeysClaimResponse;
import io.github.fherbreteau.matrix.model.KeysQueryRequest;
import io.github.fherbreteau.matrix.model.KeysQueryResponse;
import io.github.fherbreteau.matrix.model.KeysUploadRequest;
import io.github.fherbreteau.matrix.model.KeysUploadRequest.KeyValue;
import io.github.fherbreteau.matrix.model.KeysUploadResponse;
import io.github.fherbreteau.matrix.model.MatrixFilter;
import io.github.fherbreteau.matrix.model.MediaDownload;
import io.github.fherbreteau.matrix.model.MediaUploadReservation;
import io.github.fherbreteau.matrix.model.MessageBody;
import io.github.fherbreteau.matrix.model.MxcUri;
import io.github.fherbreteau.matrix.model.PasswordCredentials;
import io.github.fherbreteau.matrix.model.PresenceStatus;
import io.github.fherbreteau.matrix.model.PublicRoomsResponse;
import io.github.fherbreteau.matrix.model.ReadMarkers;
import io.github.fherbreteau.matrix.model.RegistrationRequest;
import io.github.fherbreteau.matrix.model.RelationsOptions;
import io.github.fherbreteau.matrix.model.RelationsResponse;
import io.github.fherbreteau.matrix.model.RoomAlias;
import io.github.fherbreteau.matrix.model.RoomCreation;
import io.github.fherbreteau.matrix.model.RoomEvent;
import io.github.fherbreteau.matrix.model.RoomId;
import io.github.fherbreteau.matrix.model.RoomMessagesPage;
import io.github.fherbreteau.matrix.model.RoomTag;
import io.github.fherbreteau.matrix.model.RoomTags;
import io.github.fherbreteau.matrix.model.SignedObject;
import io.github.fherbreteau.matrix.model.SyncOptions;
import io.github.fherbreteau.matrix.model.ThirdPartyLocations;
import io.github.fherbreteau.matrix.model.ThirdPartyProtocols;
import io.github.fherbreteau.matrix.model.ThirdPartyUsers;
import io.github.fherbreteau.matrix.model.ThreadsResponse;
import io.github.fherbreteau.matrix.model.UserId;
import io.github.fherbreteau.matrix.retry.RetryPolicy;
import io.github.fherbreteau.vodozemac.account.Account;
import io.github.fherbreteau.vodozemac.olm.InboundCreationResult;
import io.github.fherbreteau.vodozemac.olm.OlmSession;
import io.github.fherbreteau.vodozemac.types.Curve25519PublicKey;
import io.github.fherbreteau.vodozemac.types.Ed25519KeyPair;
import io.github.fherbreteau.vodozemac.types.Ed25519PublicKey;
import io.github.fherbreteau.vodozemac.types.Ed25519Signature;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
  private static final String ED25519_KEYID = "ed25519:%s";
  private static final String CURVE25519_KEYID = "curve25519:%s";
  private static final String SIGNED_CURVE25519_KEYID = "signed_curve25519:%s";
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
  void exercisesRelationAndThreadQueries() {
    RoomId roomId = createPrivateRoom();
    try {
      EventId root = client.sendText(roomId, "thread root");
      EventId reply =
          client.sendEvent(
              roomId,
              "m.room.message",
              JsonParser.parse(
                  "{\"msgtype\":\"m.text\",\"body\":\"thread reply\","
                      + "\"m.relates_to\":{\"rel_type\":\"m.thread\","
                      + "\"event_id\":\""
                      + root.value()
                      + "\",\"m.in_reply_to\":{\"event_id\":\""
                      + root.value()
                      + "\"}}}"),
              UUID.randomUUID().toString());
      RelationsResponse relations =
          client.getEventRelations(
              roomId, root, "m.thread", "m.room.message", RelationsOptions.defaults());
      assertThat(relations.chunk()).extracting(RoomEvent::eventId).contains(reply.value());
      ThreadsResponse threads = client.getRoomThreads(roomId);
      assertThat(threads.chunk()).extracting(RoomEvent::eventId).contains(root.value());
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
  void exercisesRoomTagsAndContentReporting() {
    RoomId roomId = createPrivateRoom();
    try {
      RoomTags initialTags = client.getRoomTags(roomId);
      assertThat(initialTags.tags()).doesNotContainKey("org.example.integration");
      client.setRoomTag(roomId, "org.example.integration", RoomTag.withOrder(0.5));
      assertThat(client.getRoomTags(roomId).tags().get("org.example.integration").order())
          .isEqualTo(0.5);
      client.deleteRoomTag(roomId, "org.example.integration");
      assertThat(client.getRoomTags(roomId).tags()).doesNotContainKey("org.example.integration");

      client.reportRoom(roomId, ContentReport.withReason("integration report"));
      EventId eventId = client.sendText(roomId, "reported integration event");
      client.reportEvent(roomId, eventId, ContentReport.withReason("integration report"));
      client.reportUser(
          UserId.of("@integration-member:localhost"), ContentReport.withReason("spam"));
    } finally {
      leaveAndForget(roomId);
    }
  }

  @Test
  void exercisesThirdPartyLookups() {
    ThirdPartyProtocols protocols = client.getThirdPartyProtocols();
    assertThat(protocols.raw().isObject()).isTrue();
    protocols.protocols().keySet().stream()
        .findFirst()
        .ifPresent(protocol -> assertThat(client.getThirdPartyProtocol(protocol)).isNotNull());
    assertThatThrownBy(() -> client.getThirdPartyProtocol("missing-protocol"))
        .isInstanceOf(RuntimeException.class);
    ThirdPartyLocations locations =
        client.getThirdPartyLocations(RoomAlias.of("#missing:localhost"));
    assertThat(locations.locations()).isEmpty();
    ThirdPartyUsers users = client.getThirdPartyUsers(UserId.of("@integration-member:localhost"));
    assertThat(users.users()).isEmpty();
    assertThat(client.getThirdPartyLocations("missing-protocol", Map.of()).locations()).isEmpty();
    assertThat(client.getThirdPartyUsers("missing-protocol", Map.of()).users()).isEmpty();
  }

  @Test
  void exercisesMutualRoomsPagination() {
    RoomId roomId = createPrivateRoom();
    MatrixClient memberClient = loginAsOtherUser();
    try {
      memberClient.joinRoom(roomId);
      var sharedRooms = client.getMutualRooms(UserId.of("@integration-member:localhost"));
      assertThat(sharedRooms.count()).isGreaterThanOrEqualTo(1);
      assertThat(sharedRooms.joined()).contains(roomId);
      assertThat(sharedRooms.raw().isObject()).isTrue();
    } finally {
      memberClient.leaveRoom(roomId);
      memberClient.logout();
      leaveAndForget(roomId);
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
    assertThat(downloadedBytes(uri)).containsExactly(payload);

    MediaUploadReservation reservation = client.createMediaUpload();
    byte[] reservedPayload = "synapse reserved media".getBytes(StandardCharsets.UTF_8);
    MxcUri reservedUri =
        client.uploadReservedMedia(reservation, reservedPayload, "text/plain", "reserved.txt");
    assertThat(reservedUri).isEqualTo(reservation.contentUri());
    assertThat(downloadedBytes(reservedUri)).containsExactly(reservedPayload);
  }

  private static String canonicalJson(JsonValue value) {
    if (value.isArray()) {
      var elements = new ArrayList<String>();
      for (int index = 0; index < value.asArray().size(); index++) {
        elements.add(canonicalJson(value.asArray().get(index)));
      }
      return "[" + String.join(",", elements) + "]";
    }
    if (value.isObject()) {
      var entries = new TreeMap<String, JsonValue>();
      value.asObject().entrySet().forEach(entry -> entries.put(entry.getKey(), entry.getValue()));
      var members = new ArrayList<String>();
      entries.forEach(
          (key, member) -> members.add(JsonString.of(key).toJson() + ":" + canonicalJson(member)));
      return "{" + String.join(",", members) + "}";
    }
    return value.toJson();
  }

  private byte[] downloadedBytes(MxcUri uri) throws IOException {
    try (var download = client.downloadMedia(uri, 1_024)) {
      return download.body().readAllBytes();
    }
  }

  @Test
  void exercisesMediaUploadAndThumbnailDownload() throws IOException {
    try (var stream = getClass().getResourceAsStream("/sample.png")) {
      MxcUri uri = client.uploadMedia(stream, 21_485, "image/png", "sample.png");

      try (MediaDownload thumbnail = client.getThumbnail(uri, 200, 150, null, false, 15_000)) {
        assertThat(thumbnail.contentType()).isEqualTo("image/png");
        assertThat(thumbnail.contentDisposition()).isEqualTo("inline");
        byte[] content = thumbnail.body().readAllBytes();
        assertThat(content).hasSizeLessThan(15_000);
      }
    }
  }

  @Test
  void claimsVerifiesAndUsesAnUploadedOneTimeKeyAndFallbackKey() {
    MatrixClient memberClient = loginAsOtherUser();
    UserId member = UserId.of(memberClient.getSession().orElseThrow().userId());
    DeviceId deviceId = DeviceId.of(memberClient.getSession().orElseThrow().deviceId());
    Map<String, String> pickleData = new LinkedHashMap<>();
    try {
      client.queryKeys(new KeysQueryRequest(Map.of(member, List.of()), null));
      DeviceInformation deviceKeys = buildDeviceKey(deviceId, pickleData, member);
      Map<String, KeyValue> oneTimeKeys = buildOneTimeKeys(deviceId, pickleData, member, 1);
      Map<String, KeyValue> fallbackKeys = buildFallbackKey(deviceId, pickleData, member);
      KeysUploadResponse upload =
          memberClient.uploadKeys(new KeysUploadRequest(deviceKeys, oneTimeKeys, fallbackKeys));
      assertThat(upload.oneTimeKeyCounts()).containsKey("signed_curve25519");
      markKeysAsPublished(deviceId, pickleData);

      KeysQueryResponse query =
          client.queryKeys(new KeysQueryRequest(Map.of(member, List.of(deviceId.value())), null));
      DeviceInformation queriedDevice = query.deviceKeys().get(member).get(deviceId.value());
      String deviceSignatureKeyId = deviceKeys.signatures().get(member).keySet().iterator().next();
      String uploadedEd25519Key =
          queriedDevice.keys().entrySet().stream()
              .filter(entry -> entry.getKey().startsWith("ed25519:"))
              .map(Map.Entry::getValue)
              .findFirst()
              .orElseThrow();
      Ed25519PublicKey deviceSigningKey = Ed25519PublicKey.fromBase64(uploadedEd25519Key);
      Ed25519Signature deviceSignature =
          Ed25519Signature.fromBase64(
              deviceKeys.signatures().get(member).get(deviceSignatureKeyId));
      assertThat(
              deviceSigningKey.verify(canonicalJson(deviceKeys.toUnsignedJson()), deviceSignature))
          .isTrue();

      var claimed =
          client.claimKeys(
              new KeysClaimRequest(
                  Map.of(member, Map.of(deviceId.value(), "signed_curve25519")), null));
      var claimedDeviceKeys = claimed.oneTimeKeys().get(member).get(deviceId.value());
      var claimedEntry = claimedDeviceKeys.entrySet().iterator().next();
      var claimedKey =
          switch (claimedEntry.getValue()) {
            case KeysClaimResponse.SignedKey key -> key;
            case KeysClaimResponse.PlainKey ignored -> throw new AssertionError();
          };
      assertThat(claimedEntry.getKey()).startsWith("signed_curve25519:");
      assertThat(claimedKey.key()).isNotBlank();
      JsonObject claimedKeyUnsigned = new JsonObject().put("key", claimedKey.key());
      Ed25519Signature claimedKeySignature =
          Ed25519Signature.fromBase64(
              claimedKey.signatures().get(member).get(deviceSignatureKeyId));
      assertThat(deviceSigningKey.verify(canonicalJson(claimedKeyUnsigned), claimedKeySignature))
          .isTrue();

      String identityKeyId =
          queriedDevice.keys().keySet().stream()
              .filter(keyId -> keyId.startsWith("curve25519:"))
              .findFirst()
              .orElseThrow();
      try (Account recipient = Account.unpickle(pickleData.get(deviceId.value()));
          Account sender = new Account()) {
        var outbound =
            sender.createOutboundSession(
                Curve25519PublicKey.fromBase64(queriedDevice.keys().get(identityKeyId)),
                Curve25519PublicKey.fromBase64(claimedKey.key()));
        try (OlmSession outboundSession = outbound) {
          var preKeyMessage =
              outboundSession.encrypt("e2ee end-to-end payload".getBytes(StandardCharsets.UTF_8));
          try (InboundCreationResult inboundResult =
                  recipient.createInboundSession(sender.curve25519Key(), preKeyMessage);
              OlmSession inboundSession = inboundResult.session()) {
            assertThat(new String(inboundResult.plaintext(), StandardCharsets.UTF_8))
                .isEqualTo("e2ee end-to-end payload");
            assertThat(
                    new String(
                        inboundSession.decrypt(
                            outboundSession.encrypt(
                                "second encrypted payload".getBytes(StandardCharsets.UTF_8))),
                        StandardCharsets.UTF_8))
                .isEqualTo("second encrypted payload");
          }
        }
      }

      try (Account fallbackRecipient = Account.unpickle(pickleData.get(deviceId.value()))) {
        fallbackRecipient.forgetFallbackKey();
        fallbackRecipient.generateFallbackKey();
        pickleData.put(deviceId.value(), fallbackRecipient.pickle());
      }
      memberClient.uploadKeys(
          new KeysUploadRequest(null, Map.of(), buildFallbackKey(deviceId, pickleData, member)));
      markKeysAsPublished(deviceId, pickleData);
      var fallbackClaim =
          client.claimKeys(
              new KeysClaimRequest(
                  Map.of(member, Map.of(deviceId.value(), "signed_curve25519")), null));
      var fallbackDeviceKeys = fallbackClaim.oneTimeKeys().get(member).get(deviceId.value());
      assertThat(fallbackDeviceKeys).isNotEmpty();
      assertThat(fallbackDeviceKeys.values())
          .allSatisfy(
              value ->
                  assertThat(value)
                      .isInstanceOf(KeysClaimResponse.SignedKey.class)
                      .extracting("key")
                      .asString()
                      .isNotBlank());

      String from = client.sync(SyncOptions.defaults()).nextBatch();
      Map<String, KeyValue> replacementKeys = buildOneTimeKeys(deviceId, pickleData, member, 1);
      memberClient.uploadKeys(new KeysUploadRequest(null, replacementKeys, null));
      markKeysAsPublished(deviceId, pickleData);
      var sync = client.sync(SyncOptions.incremental(from, 0, null));
      assertThat(sync.nextBatch()).isNotBlank();
      assertThat(client.getKeyChanges(from, sync.nextBatch()).changed()).isNotNull();

      var afterUpload = client.sync(SyncOptions.incremental(sync.nextBatch(), 0, null));
      assertThat(afterUpload.nextBatch()).isNotBlank();
      assertThat(client.getKeyChanges(sync.nextBatch(), afterUpload.nextBatch()).changed())
          .isNotNull();
    } finally {
      memberClient.logout();
    }
  }

  @Test
  void exercicesCrossSigningAndSigningUploadAndQuery() throws IOException {
    UserId currentUser = UserId.of(client.getSession().get().userId());
    DeviceId deviceId = DeviceId.of(client.getSession().get().deviceId());
    var pickleData = new LinkedHashMap<String, String>();

    // User Cross-Signing Keys

    var masterKey = buildCrossSigningKey("master", pickleData, currentUser);
    var selfSigningKey = buildCrossSigningKey("self_signing", pickleData, currentUser);
    var userSigningKey = buildCrossSigningKey("user_signing", pickleData, currentUser);

    var signedSelfSigningKey =
        signWithCrossSigningKey(selfSigningKey, "master", pickleData, currentUser);
    var signedUserSigningKey =
        signWithCrossSigningKey(userSigningKey, "master", pickleData, currentUser);
    var masterKeyId = masterKey.keys().keySet().iterator().next();
    var masterSignatureKeyId =
        signedSelfSigningKey.signatures().get(currentUser).keySet().iterator().next();
    var masterSignature =
        Ed25519Signature.fromBase64(
            signedSelfSigningKey.signatures().get(currentUser).get(masterSignatureKeyId));
    assertThat(
            Ed25519PublicKey.fromBase64(masterKey.keys().get(masterKeyId))
                .verify(canonicalJson(signedSelfSigningKey.toUnsignedJson()), masterSignature))
        .isTrue();
    // Upload the users cross-signing keys;
    client.uploadDeviceSigningKeys(
        new DeviceSigningUploadRequest(masterKey, signedSelfSigningKey, signedUserSigningKey));

    // Device keys
    var deviceKeys = buildDeviceKey(deviceId, pickleData, currentUser);
    var oneTimeKeys = buildOneTimeKeys(deviceId, pickleData, currentUser);
    var fallbackKeys = buildFallbackKey(deviceId, pickleData, currentUser);

    // Upload user device keys and one-time keys and a fallback key
    client.uploadKeys(new KeysUploadRequest(deviceKeys, oneTimeKeys, fallbackKeys));

    markKeysAsPublished(deviceId, pickleData);

    var signedMasterKey = signWithAccount(masterKey, deviceId, pickleData, currentUser);
    var signedMasterSignatureKeyId =
        signedMasterKey.signatures().get(currentUser).keySet().iterator().next();
    var signedMasterSignature =
        Ed25519Signature.fromBase64(
            signedMasterKey.signatures().get(currentUser).get(signedMasterSignatureKeyId));

    Map<UserId, Map<String, SignedObject>> signatures = new LinkedHashMap<>();
    signatures
        .computeIfAbsent(currentUser, u -> new LinkedHashMap<>())
        .put(masterKeyId, signedMasterKey);
    var result = client.uploadKeySignatures(new KeySignaturesUploadRequest(signatures));
    assertThat(result.failures()).containsKey(currentUser);
    assertThat(result.failures().get(currentUser).get(masterKeyId).errcode())
        .isEqualTo("M_INVALID_SIGNATURE");

    var signedDeviceKeys =
        signWithCrossSigningKey(deviceKeys, "self_signing", pickleData, currentUser);
    var deviceKeyId = deviceId.value();
    signatures = new LinkedHashMap<>();
    signatures
        .computeIfAbsent(currentUser, u -> new LinkedHashMap<>())
        .put(deviceKeyId, signedDeviceKeys);
    var result2 = client.uploadKeySignatures(new KeySignaturesUploadRequest(signatures));
    assertThat(result2).isNotNull();

    KeysQueryResponse response =
        client.queryKeys(
            new KeysQueryRequest(Map.of(currentUser, List.of(deviceId.value())), 10_000L));
    assertThat(response.deviceKeys())
        .extractingByKey(currentUser, map(String.class, DeviceInformation.class))
        .extractingByKey(deviceId.value(), type(DeviceInformation.class))
        .extracting(DeviceInformation::deviceId, DeviceInformation::userId)
        .containsExactly(deviceId, currentUser);
    assertThat(response.deviceKeys())
        .extractingByKey(currentUser, map(String.class, DeviceInformation.class))
        .extractingByKey(deviceId.value(), type(DeviceInformation.class))
        .extracting(DeviceInformation::signatures, map(UserId.class, Map.class))
        .extractingByKey(currentUser, map(String.class, String.class))
        .hasSizeGreaterThanOrEqualTo(1);
    assertThat(response.masterKeys())
        .extractingByKey(currentUser, type(CrossSigningKey.class))
        .extracting(CrossSigningKey::signatures, map(UserId.class, Map.class))
        .isNotNull();
    assertThat(response.selfSigningKeys())
        .extractingByKey(currentUser, type(CrossSigningKey.class))
        .extracting(CrossSigningKey::signatures, map(UserId.class, Map.class))
        .extractingByKey(currentUser, map(String.class, String.class))
        .hasSize(1);
    assertThat(response.userSigningKeys())
        .extractingByKey(currentUser, type(CrossSigningKey.class))
        .extracting(CrossSigningKey::signatures, map(UserId.class, Map.class))
        .extractingByKey(currentUser, map(String.class, String.class))
        .hasSize(1);
  }

  private CrossSigningKey buildCrossSigningKey(
      String type, Map<String, String> pickleData, UserId user) {
    try (Ed25519KeyPair keyPair = new Ed25519KeyPair()) {
      String publicKey = keyPair.publicKey().toBase64();
      String keyId = buildFingerprintKeyId(publicKey);
      pickleData.put(type, keyPair.pickle());
      pickleData.put(type + "_public", publicKey);
      return new CrossSigningKey(List.of(type), user, Map.of(keyId, publicKey), null);
    }
  }

  private String buildIdentityKeyId(String key) {
    return String.format(CURVE25519_KEYID, key);
  }

  private String buildFingerprintKeyId(String key) {
    return String.format(ED25519_KEYID, key);
  }

  @SuppressWarnings("unchecked")
  private <T extends SignedObject> T signWithCrossSigningKey(
      T source, String keyType, Map<String, String> pickleData, UserId user) {
    String keyData = pickleData.get(keyType);
    if (keyData == null) {
      throw new IllegalStateException(keyType + " is missing in pickle datas");
    }
    try (Ed25519KeyPair keyPair = Ed25519KeyPair.unpickle(keyData)) {
      String json = canonicalJson(source.toUnsignedJson());
      Ed25519Signature signature = keyPair.sign(json);
      Map<UserId, Map<String, String>> signatures = new LinkedHashMap<>();
      source.signatures().forEach((u, values) -> signatures.put(u, new LinkedHashMap<>(values)));

      String keyId = buildFingerprintKeyId(pickleData.get(keyType + "_public"));
      signatures
          .computeIfAbsent(user, u -> new LinkedHashMap<String, String>())
          .put(keyId, signature.toBase64());

      return (T) source.withSignatures(signatures);
    }
  }

  private <T extends SignedObject> T signWithAccount(
      T source, DeviceId device, Map<String, String> pickleData, UserId user) {
    String accountData = pickleData.get(device.value());
    if (accountData == null) {
      throw new IllegalStateException(device + " is missing in pickle datas");
    }
    try (Account account = Account.unpickle(accountData)) {
      return signWithAccount(source, device, account, user);
    }
  }

  @SuppressWarnings("unchecked")
  private <T extends SignedObject> T signWithAccount(
      T source, DeviceId device, Account account, UserId user) {
    String json = canonicalJson(source.toUnsignedJson());
    Ed25519Signature signature = account.sign(json);

    Map<UserId, Map<String, String>> signatures = new LinkedHashMap<>();
    source.signatures().forEach((u, values) -> signatures.put(u, new LinkedHashMap<>(values)));

    String keyId = buildFingerprintKeyId(device.value());
    signatures
        .computeIfAbsent(user, u -> new LinkedHashMap<String, String>())
        .put(keyId, signature.toBase64());
    return (T) source.withSignatures(signatures);
  }

  private DeviceInformation buildDeviceKey(
      DeviceId device, Map<String, String> pickleData, UserId user) {
    try (Account account = new Account()) {
      String identityKey = account.curve25519Key().toBase64();
      String fingerprintKey = account.ed25519Key().toBase64();
      var identityKeyId = buildIdentityKeyId(identityKey);
      var fingerprintKeyId = buildFingerprintKeyId(fingerprintKey);

      var keys = Map.of(identityKeyId, identityKey, fingerprintKeyId, fingerprintKey);
      pickleData.put(identityKeyId, identityKey);
      pickleData.put(fingerprintKeyId, fingerprintKey);
      pickleData.put(device.value(), account.pickle());
      var deviceInfo =
          new DeviceInformation(
              List.of("m.olm.v1.curve25519-aes-sha2", "m.megolm.v1.aes-sha2"),
              device,
              keys,
              Map.of(),
              null,
              user);

      return signWithAccount(deviceInfo, device, account, user);
    }
  }

  private Map<String, KeyValue> buildOneTimeKeys(
      DeviceId device, Map<String, String> pickleData, UserId user) {
    return buildOneTimeKeys(device, pickleData, user, 25);
  }

  private Map<String, KeyValue> buildOneTimeKeys(
      DeviceId device, Map<String, String> pickleData, UserId user, long count) {
    String accountData = pickleData.get(device.value());
    if (accountData == null) {
      throw new IllegalStateException(device + " is missing in pickle datas");
    }
    try (Account account = Account.unpickle(accountData)) {
      account.generateOneTimeKeys(count);
      Map<String, Curve25519PublicKey> oneTimeKeys = account.unpublishedOneTimeKeys();

      Map<String, KeyValue> result = new LinkedHashMap<>();
      oneTimeKeys.forEach(
          (index, oneTime) -> {
            var keyId = String.format(SIGNED_CURVE25519_KEYID, index);
            var unsigned = new KeysUploadRequest.SignedKey(oneTime.toBase64(), null, null);
            var signed = signWithAccount(unsigned, device, account, user);
            result.put(keyId, signed);
          });
      pickleData.put(device.value(), account.pickle());
      return result;
    }
  }

  private Map<String, KeyValue> buildFallbackKey(
      DeviceId device, Map<String, String> pickleData, UserId user) {
    String accountData = pickleData.get(device.value());
    if (accountData == null) {
      throw new IllegalStateException(device + " is missing in pickle datas");
    }
    try (Account account = Account.unpickle(accountData)) {
      account.generateFallbackKey();
      Map<String, Curve25519PublicKey> fallbackKeys = account.unpublishedFallbackKey();

      Map<String, KeyValue> result = new LinkedHashMap<>();
      fallbackKeys.forEach(
          (index, fallback) -> {
            var keyId = String.format(SIGNED_CURVE25519_KEYID, index);
            var unsigned = new KeysUploadRequest.SignedKey(fallback.toBase64(), null, true);
            var signed = signWithAccount(unsigned, device, account, user);
            result.put(keyId, signed);
          });
      pickleData.put(device.value(), account.pickle());
      return result;
    }
  }

  private void markKeysAsPublished(DeviceId device, Map<String, String> pickleData) {
    String accountData = pickleData.get(device.value());
    if (accountData == null) {
      throw new IllegalStateException(device + " is missing in pickle datas");
    }
    try (Account account = Account.unpickle(accountData)) {
      account.markKeysAsPublished();
      pickleData.put(device.value(), account.pickle());
    }
  }

  @Test
  void previewsAndDownloadsImageFromExternalUrl() throws IOException {
    var preview = client.getUrlPreview("https://dummyfiles.dev/image/800x600");

    assertThat(preview.imageUri()).isNotNull();
    assertThat(preview.imageSize()).isBetween(21000L, 22000L);
    assertThat(preview.properties())
        .extractingByKey("og:description", type(JsonValue.class))
        .isEqualTo(JsonNull.INSTANCE);
    assertThat(preview.properties())
        .extractingByKey("og:image", type(JsonValue.class))
        .isInstanceOf(JsonString.class)
        .asInstanceOf(type(JsonString.class))
        .extracting(JsonString::asString)
        .isEqualTo(preview.imageUri().toString());
    assertThat(preview.properties())
        .extractingByKey("og:image:type", type(JsonValue.class))
        .isInstanceOf(JsonString.class)
        .asInstanceOf(type(JsonString.class))
        .extracting(JsonString::asString)
        .isEqualTo("image/png");
    assertThat(preview.properties())
        .extractingByKey("matrix:image:size", type(JsonValue.class))
        .isInstanceOf(JsonNumber.class)
        .asInstanceOf(type(JsonNumber.class))
        .extracting(JsonNumber::asLong)
        .isEqualTo(preview.imageSize());
    assertThat(preview.properties())
        .extractingByKey("og:image:width", type(JsonValue.class))
        .isInstanceOf(JsonNumber.class)
        .asInstanceOf(type(JsonNumber.class))
        .extracting(JsonNumber::asLong)
        .isEqualTo(800L);
    assertThat(preview.properties())
        .extractingByKey("og:image:height", type(JsonValue.class))
        .isInstanceOf(JsonNumber.class)
        .asInstanceOf(type(JsonNumber.class))
        .extracting(JsonNumber::asLong)
        .isEqualTo(600L);
    try (MediaDownload image = client.downloadMedia(preview.imageUri(), 1_000_000)) {
      assertThat(image.contentType()).startsWith("image/");
      assertThat(image.body().readAllBytes()).isNotEmpty();
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
  void listsInspectsAndUpdatesTheCurrentDevice() {
    var session = client.getSession().orElseThrow();
    DeviceId deviceId = DeviceId.of(session.deviceId());
    assertThat(client.getDevices().devices()).extracting(Device::deviceId).contains(deviceId);
    assertThat(client.getDevice(deviceId).deviceId()).isEqualTo(deviceId);
    String displayName = "Synapse integration " + UUID.randomUUID();
    client.updateDevice(deviceId, DeviceUpdateRequest.displayName(displayName));
    assertThat(client.getDevice(deviceId).displayName()).isEqualTo(displayName);
  }

  @Test
  void changesPasswordWithInteractiveAuthenticationAndRestoresIt() {
    String changedPassword = UUID.randomUUID().toString();
    try {
      var request =
          "{\"type\":\"m.login.password\",\"identifier\":{\"type\":\"m.id.user\","
              + "\"user\":\"@integration:localhost\"},\"password\":\"%s\"}".formatted(PASSWORD);
      client.changePassword(
          AccountRequest.builder()
              .newPassword(changedPassword)
              .logoutDevices(false)
              .put("auth", JsonParser.parse(request))
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
        var request =
            "{\"type\":\"m.login.password\",\"identifier\":{\"type\":\"m.id.user\","
                + "\"user\":\"@integration:localhost\"},\"password\":\"%s\"}"
                    .formatted(changedPassword);
        client.changePassword(
            AccountRequest.builder()
                .newPassword(PASSWORD)
                .logoutDevices(false)
                .put("auth", JsonParser.parse(request))
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
    var matrixConfig =
        """
        enable_registration: true
        enable_registration_without_verification: true
        registration_shared_secret: "%s"
        public_baseurl: "http://localhost:8008/"
        default_room_version: "10"
        url_preview_enabled: true
        url_preview_ip_range_blacklist:
        - 127.0.0.0/8
        - 10.0.0.0/8
        - 172.16.0.0/12
        - 192.168.0.0/16
        - 100.64.0.0/10
        - 192.0.0.0/24
        - 169.254.0.0/16
        - 192.88.99.0/24
        - 198.18.0.0/15
        - 192.0.2.0/24
        - 198.51.100.0/24
        - 203.0.113.0/24
        - 224.0.0.0/4
        - ::1/128
        - fe80::/10
        - fc00::/7
        - 2001:db8::/32
        - ff00::/8
        - fec0::/10
        allow_public_rooms_without_auth: true
        presence:
          enabled: true
        rc_message:
          per_second: 100
          burst_count: 100
        rc_registration:
          per_second: 100
          burst_count: 100
        rc_login:
          address:
            per_second: 100
            burst_count: 100
          account:
            per_second: 100
            burst_count: 100
          failed_attempts:
            per_second: 100
            burst_count: 100
        """
            .formatted(SHARED_SECRET);
    Files.writeString(
        dataDirectory.resolve("homeserver.yaml"),
        "\n" + matrixConfig,
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
