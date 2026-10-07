package io.github.fherbreteau.matrix.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.fherbreteau.matrix.json.JsonParser;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EncryptionKeysModelsTest {

  @Test
  void keyRequestsUseTypedImmutableValues() {
    var user = UserId.of("@a:hs");
    var deviceId = DeviceId.of("D");
    var device =
        new DeviceInformation(
            List.of("m.megolm.v1.aes-sha2"),
            deviceId,
            Map.of("ed25519:D", "public"),
            Map.of(),
            user);
    assertThat(new KeysUploadRequest(null, null, null).oneTimeKeys()).isEmpty();
    assertThat(new KeysUploadRequest(null, null, null).fallbackKeys()).isEmpty();
    var signed =
        new KeysUploadRequest.SignedKey("public", Map.of(user, Map.of("ed25519:D", "sig")), false);
    var request = new KeysUploadRequest(device, Map.of("signed_curve25519:k", signed), null);

    assertThat(device.toUnsignedJson().asObject().has("signatures")).isFalse();
    assertThat(device.withSignatures(Map.of(user, Map.of("ed25519:D", "sig"))).signatures())
        .containsKey(user);
    var signedUnsigned = signed.toUnsignedJson().asObject();
    assertThat(signedUnsigned.has("signatures")).isFalse();
    assertThat(signed.withSignatures(Map.of(user, Map.of("ed25519:D", "sig"))).signatures())
        .containsKey(user);
    assertThat(request.deviceKeys().deviceId()).isEqualTo(deviceId);
    assertThatThrownBy(() -> new DeviceInformation(null, deviceId, Map.of(), Map.of(), user))
        .isInstanceOf(IllegalArgumentException.class);
    var oneTimeKeys = request.oneTimeKeys();
    assertThat(oneTimeKeys.get("signed_curve25519:k")).isEqualTo(signed);
    var plainKey = new KeysUploadRequest.PlainKey("k");
    assertThatThrownBy(() -> oneTimeKeys.put("other", plainKey))
        .isInstanceOf(UnsupportedOperationException.class);
    var sign = signed.signatures().get(user);
    assertThatThrownBy(() -> sign.put("other", "changed"))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThat(new KeysUploadRequest.SignedKey("public", null, false).signatures()).isEmpty();
    assertThat(new KeysUploadRequest(null, null, null).oneTimeKeys()).isEmpty();

    var query = new KeysQueryRequest(Map.of(user, List.of("D1", "D2")), 1000L);
    assertThat(query.deviceKeys()).containsKey(user);
    assertThat(query.timeout()).isEqualTo(1000L);
    assertThatThrownBy(() -> new KeysQueryRequest(null, null))
        .isInstanceOf(NullPointerException.class);
    var claim = new KeysClaimRequest(Map.of(user, Map.of("D1", "signed_curve25519")), null);
    assertThatThrownBy(() -> new KeysClaimRequest(null, null))
        .isInstanceOf(NullPointerException.class);
    assertThat(claim.oneTimeKeys()).containsKey(user);
    var key =
        new CrossSigningKey(List.of("master"), user, Map.of("ed25519:master", "pub"), Map.of());
    assertThatThrownBy(() -> new CrossSigningKey(null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class);
    var signing =
        new DeviceSigningUploadRequest(
            key,
            null,
            null,
            new DeviceSigningUploadRequest.UiAuth(
                "m.login.password", "S", Map.of("username", "@a:hs", "password", "pw")));
    assertThat(signing.auth().session()).isEqualTo("S");
    var signatures =
        new KeySignaturesUploadRequest(
            Map.of(
                user,
                Map.of(
                    "ed25519:D",
                    new DeviceInformation(
                        List.of("usage"), deviceId, Map.of("ed25519:D", "pub"), Map.of(), user))));
    assertThat(signatures.signatures()).containsKey(user);
  }

  @Test
  void responseModelsExposeTypedKeyStructures() {
    var user = UserId.of("@a:hs");
    var deviceId = DeviceId.of("D");
    var device =
        new DeviceInformation(
            List.of("m.megolm.v1.aes-sha2"),
            deviceId,
            Map.of("ed25519:D", "public"),
            Map.of(),
            user);
    var query =
        new KeysQueryResponse(
            Map.of(user, Map.of("D", device)),
            Map.of("remote", new KeysQueryResponse.Failure("M_TIMEOUT", "timeout")),
            Map.of(
                user,
                new CrossSigningKey(List.of("master"), user, Map.of("ed25519:m", "pub"), Map.of())),
            null,
            null);
    assertThat(query.deviceKeys().get(user).get("D").deviceId()).isEqualTo(deviceId);
    assertThat(query.deviceKeys().get(user)).isUnmodifiable();
    assertThat(query.failures().get("remote").errcode()).isEqualTo("M_TIMEOUT");
    assertThat(query.masterKeys().get(user).usage()).containsExactly("master");
    assertThat(new KeysQueryResponse(null, null, null, null, null).deviceKeys()).isEmpty();
    var invalidDevices = new LinkedHashMap<UserId, Map<String, DeviceInformation>>();
    invalidDevices.put(user, null);
    assertThat(new KeysQueryResponse(invalidDevices, null, null, null, null).deviceKeys())
        .containsEntry(user, null);
    assertThatThrownBy(() -> new DeviceInformation(null, null, null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class);
    var oneTimeKey =
        new KeysClaimResponse.SignedKey("public", Map.of(user, Map.of("ed25519:m", "pub")));
    var claim =
        new KeysClaimResponse(
            Map.of(user, Map.of("D", Map.of("signed_curve25519:k", oneTimeKey))),
            Map.of("remote", new KeysClaimResponse.Failure("M_TIMEOUT", "timeout")));
    assertThat(claim.oneTimeKeys().get(user).get("D").get("signed_curve25519:k"))
        .isEqualTo(oneTimeKey);
    var upload = new KeysUploadResponse(Map.of("signed_curve25519", 7L));
    assertThat(upload.oneTimeKeyCounts()).containsEntry("signed_curve25519", 7L);
    var changes = new KeyChangesResponse(List.of(user), List.of());
    assertThat(changes.changed()).containsExactly(user);
    var signatureResponse =
        new KeySignaturesUploadResponse(
            Map.of(
                user,
                Map.of(
                    "ed25519:D",
                    new KeySignaturesUploadResponse.Failure("M_INVALID_PARAM", "bad signature"))));
    assertThat(new KeySignaturesUploadResponse(null).failures()).isEmpty();
    assertThat(new KeySignaturesUploadRequest(null).signatures()).isEmpty();
    assertThatThrownBy(() -> new KeysClaimResponse(null, null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(new KeysClaimResponse.PlainKey("key"))
        .extracting(KeysClaimResponse.PlainKey::key)
        .isEqualTo("key");
    assertThat(signatureResponse.failures().get(user).get("ed25519:D").errcode())
        .isEqualTo("M_INVALID_PARAM");
  }

  @Test
  void e2eeResponseParsingValidatesRequiredFieldsAndOptionalUnsignedData() {
    var user = UserId.of("@a:hs");
    var queryDevice =
        JsonParser.parse(
            "{\"algorithms\":[\"m.megolm.v1.aes-sha2\"],\"device_id\":\"D\","
                + "\"keys\":{\"ed25519:D\":\"pub\"},\"signatures\":{},"
                + "\"unsigned\":{},\"user_id\":\"@a:hs\"}");
    assertThat(DeviceInformation.from(queryDevice).deviceId()).isEqualTo(DeviceId.of("D"));
    var query =
        KeysQueryResponse.from(
            JsonParser.parse(
                "{\"device_keys\":{\"@a:hs\":{\"D\":{\"algorithms\":[\"m.megolm.v1.aes-sha2\"],"
                    + "\"device_id\":\"D\",\"keys\":{\"ed25519:D\":\"pub\"},"
                    + "\"signatures\":{},\"user_id\":\"@a:hs\"}}},\"failures\":{}}"));
    assertThat(query.deviceKeys().get(user).get("D").unsigned()).isNull();
    var crossSigningJson =
        JsonParser.parse(
            "{\"keys\":{\"ed25519:master\":\"pub\"},\"usage\":[\"master\"],"
                + "\"user_id\":\"@a:hs\"}");
    var parsedCrossSigning = CrossSigningKey.from(crossSigningJson);
    assertThat(parsedCrossSigning.usage()).containsExactly("master");
    assertThat(parsedCrossSigning.toUnsignedJson().asObject().has("signatures")).isFalse();
    assertThat(
            parsedCrossSigning
                .withSignatures(Map.of(user, Map.of("ed25519:master", "signature")))
                .signatures())
        .containsKey(user);
    var missingError = JsonParser.parse("{\"failures\":{\"remote\":{\"errcode\":\"M_TIMEOUT\"}}}");
    assertThatThrownBy(() -> KeysQueryResponse.from(missingError))
        .isInstanceOf(IllegalArgumentException.class);
    var missingErrorCode =
        JsonParser.parse(
            "{\"one_time_keys\":{},\"failures\":{\"remote\":{\"error\":\"timeout\"}}}");
    assertThatThrownBy(() -> KeysClaimResponse.from(missingErrorCode))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(KeyChangesResponse.from(JsonParser.parse("{}")).changed()).isEmpty();
    assertThat(KeyChangesResponse.from(JsonParser.parse("{}")).left()).isEmpty();
  }

  @Test
  void deviceKeyQueryRespectsRequiredAndOptionalFields() {
    var deviceWithoutUnsigned =
        JsonParser.parse(
            "{\"algorithms\":[\"m.megolm.v1.aes-sha2\"],\"device_id\":\"D\","
                + "\"keys\":{\"ed25519:D\":\"pub\"},\"signatures\":{},"
                + "\"user_id\":\"@a:hs\"}");
    assertThat(DeviceInformation.from(deviceWithoutUnsigned).unsigned()).isNull();

    var missingAlgo =
        JsonParser.parse(
            "{\"device_id\":\"D\",\"keys\":{},\"signatures\":{}," + "\"user_id\":\"@a:hs\"}");
    assertThatThrownBy(() -> DeviceInformation.from(missingAlgo))
        .isInstanceOf(IllegalArgumentException.class);
    var missingKeys =
        JsonParser.parse(
            "{\"algorithms\":[],\"device_id\":\"D\",\"signatures\":{}," + "\"user_id\":\"@a:hs\"}");
    assertThatThrownBy(() -> DeviceInformation.from(missingKeys))
        .isInstanceOf(IllegalArgumentException.class);
    var missingSignatures =
        JsonParser.parse(
            "{\"algorithms\":[],\"device_id\":\"D\",\"keys\":{}," + "\"user_id\":\"@a:hs\"}");
    assertThatThrownBy(() -> DeviceInformation.from(missingSignatures))
        .isInstanceOf(IllegalArgumentException.class);
    var missingDeviceId =
        JsonParser.parse(
            "{\"algorithms\":[\"m.megolm.v1.aes-sha2\"],\"keys\":{},"
                + "\"signatures\":{},\"user_id\":\"@a:hs\"}");
    assertThatThrownBy(() -> DeviceInformation.from(missingDeviceId))
        .isInstanceOf(IllegalArgumentException.class);
    var malformedUnsigned =
        JsonParser.parse(
            "{\"algorithms\":[],\"device_id\":\"D\",\"keys\":{},\"signatures\":{},"
                + "\"unsigned\":[],\"user_id\":\"@a:hs\"}");
    assertThatThrownBy(() -> DeviceInformation.from(malformedUnsigned))
        .isInstanceOf(IllegalArgumentException.class);
    var malformedAlgorithms =
        JsonParser.parse(
            "{\"algorithms\":[1],\"device_id\":\"D\",\"keys\":{},\"signatures\":{},"
                + "\"user_id\":\"@a:hs\"}");
    assertThatThrownBy(() -> DeviceInformation.from(malformedAlgorithms))
        .isInstanceOf(IllegalArgumentException.class);
    var malformedDeviceKey =
        JsonParser.parse(
            "{\"algorithms\":[],\"device_id\":\"D\",\"keys\":{\"ed25519:D\":1},"
                + "\"signatures\":{},\"user_id\":\"@a:hs\"}");
    assertThatThrownBy(() -> DeviceInformation.from(malformedDeviceKey))
        .isInstanceOf(IllegalArgumentException.class);
    var malformedDeviceSignatures =
        JsonParser.parse(
            "{\"algorithms\":[],\"device_id\":\"D\",\"keys\":{},\"signatures\":[],"
                + "\"user_id\":\"@a:hs\"}");
    assertThatThrownBy(() -> DeviceInformation.from(malformedDeviceSignatures))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void crossSigningAndClaimedKeyParsingRequireSpecFields() {
    var missingUsage = JsonParser.parse("{\"keys\":{\"ed25519:k\":\"pub\"},\"user_id\":\"@a:hs\"}");
    assertThatThrownBy(() -> CrossSigningKey.from(missingUsage))
        .isInstanceOf(IllegalArgumentException.class);
    var malformedUsage =
        JsonParser.parse(
            "{\"usage\":[1],\"keys\":{\"ed25519:k\":\"pub\"}," + "\"user_id\":\"@a:hs\"}");
    assertThatThrownBy(() -> CrossSigningKey.from(malformedUsage))
        .isInstanceOf(IllegalArgumentException.class);
    var missingKey = JsonParser.parse("{\"usage\":[\"master\"],\"user_id\":\"@a:hs\"}");
    assertThatThrownBy(() -> CrossSigningKey.from(missingKey))
        .isInstanceOf(IllegalArgumentException.class);
    var malformedKey =
        JsonParser.parse(
            "{\"usage\":[\"master\"],\"keys\":{\"ed25519:k\":1}," + "\"user_id\":\"@a:hs\"}");
    assertThatThrownBy(() -> CrossSigningKey.from(malformedKey))
        .isInstanceOf(IllegalArgumentException.class);
    var missingSignature =
        JsonParser.parse(
            "{\"one_time_keys\":{\"@a:hs\":{\"D\":{\"signed_curve25519:k\":"
                + "{\"key\":\"public\"}}}}}");
    assertThatThrownBy(() -> KeysClaimResponse.from(missingSignature))
        .isInstanceOf(IllegalArgumentException.class);
    var malformedSignature =
        JsonParser.parse(
            "{\"one_time_keys\":{\"@a:hs\":{\"D\":{\"signed_curve25519:k\":"
                + "{\"key\":\"public\",\"signatures\":[]}}}}}");
    assertThatThrownBy(() -> KeysClaimResponse.from(malformedSignature))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void queryModelParsersValidateOptionalAndRequiredFieldHelpers() {
    var count = JsonParser.parse("{\"count\":3}").asObject();
    assertThat(ModelJson.number(count, "count")).isEqualTo(3L);
    assertThat(ModelJson.number(count, "missing")).isNull();
    assertThat(ModelJson.string(count, "missing")).isNull();
    assertThat(ModelJson.bool(JsonParser.parse("{\"enabled\":true}").asObject(), "enabled"))
        .isTrue();
  }

  @Test
  void backupJsonRoundTripsTypedExtensionValuesAndSignedTargets() {
    var user = UserId.of("@a:hs");
    var extension =
        Map.<String, Object>of(
            "usage",
            List.of("first", "second"),
            "enabled",
            true,
            "precision",
            new BigDecimal("12.340"),
            "nested",
            Map.of("value", "kept"));
    var cipher = new EncryptionRequest.EncryptedSessionData("encrypted", "key", "tag", extension);
    var cipherJson = cipher.toJson().toJson();
    var parsedCipher =
        EncryptionRequest.EncryptedSessionData.from(JsonParser.parse(cipherJson).asObject());
    assertThat(parsedCipher.extraFields()).containsEntry("usage", List.of("first", "second"));
    assertThat(parsedCipher.extraFields()).containsEntry("enabled", true);
    assertThat(parsedCipher.extraFields())
        .extractingByKey("precision")
        .isEqualTo(new BigDecimal("12.34"));
    assertThat(parsedCipher.extraFields()).containsEntry("nested", Map.of("value", "kept"));
    assertThat(parsedCipher.toJson().toJson()).isEqualTo(cipherJson);

    var authData = new RoomKeyBackupVersionRequest.BackupAuthData("public", null, extension);
    var parsedAuthData =
        RoomKeyBackupVersionRequest.BackupAuthData.from(
            JsonParser.parse(authData.toJson().toJson()).asObject());
    assertThat(parsedAuthData.extraFields()).containsEntry("usage", List.of("first", "second"));
    assertThat(parsedAuthData.extraFields()).containsEntry("enabled", true);
    assertThat(parsedAuthData.extraFields())
        .extractingByKey("precision")
        .isEqualTo(new BigDecimal("12.34"));

    var crossSigningKey =
        new CrossSigningKey(List.of("master"), user, Map.of("ed25519:master", "public"), Map.of());
    var request =
        new KeySignaturesUploadRequest(Map.of(user, Map.of("ed25519:master", crossSigningKey)));
    assertThat(request.toJson().toJson()).contains("\"usage\":[\"master\"]");
  }

  @Test
  void backupModelsFullyTypeSessionMetadataAndEncryptedData() {
    var cipher = new EncryptionRequest.EncryptedSessionData("encrypted", "key", "tag", Map.of());
    assertThat(cipher.ciphertext()).isEqualTo("encrypted");
    var key = new EncryptionRequest.KeyBackupData(12, 2, true, cipher);
    var room = RoomId.of("!r:hs");
    var request =
        new EncryptionRequest(Map.of(room, new EncryptionRequest.RoomKeyBackup(Map.of("s", key))));
    assertThat(request.rooms().get(room).sessions().get("s").firstMessageIndex()).isEqualTo(12);
    assertThat(request.rooms().get(room).sessions().get("s").sessionData().ciphertext())
        .isEqualTo("encrypted");
    assertThat(key.verified()).isTrue();
    assertThatThrownBy(() -> new EncryptionRequest.EncryptedSessionData(null, "e", "m"))
        .isInstanceOf(NullPointerException.class);

    var authData = new RoomKeyBackupVersionRequest.BackupAuthData("key", null, Map.of());
    var versionRequest = new RoomKeyBackupVersionRequest("m.megolm_backup.v1", authData, "v1");
    assertThat(versionRequest.version()).isEqualTo("v1");
    var info = new RoomKeyBackupInfo("m.megolm_backup.v1", authData, 1, "etag", "v1");
    assertThat(info.authData().publicKey()).isEqualTo("key");
    assertThat(new RoomKeyBackupVersionRequest.BackupAuthData("key", null).signatures()).isEmpty();
    assertThat(new RoomKeyBackupVersion("v1").version()).isEqualTo("v1");
    assertThat(new RoomKeyBackupWriteResponse(1, "etag").count()).isEqualTo(1);

    var response =
        new RoomKeyBackupKeysResponse(
            Map.of(room, new RoomKeyBackupKeysResponse.RoomSessions(Map.of("s", key))));
    assertThat(new RoomKeyBackupKeysResponse(null).rooms()).isEmpty();
    assertThat(new RoomKeyBackupKeysResponse.RoomSessions(null).sessions()).isEmpty();
    assertThat(response.rooms().get(room).sessions().get("s").forwardedCount()).isEqualTo(2);
    assertThat(new EncryptionRequest(null).rooms()).isEmpty();
    assertThatThrownBy(() -> new EncryptionRequest.KeyBackupData(1, 0, true, null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new EncryptionRequest.EncryptedSessionData(null, "ephemeral", "mac"))
        .isInstanceOf(NullPointerException.class);
  }
}
