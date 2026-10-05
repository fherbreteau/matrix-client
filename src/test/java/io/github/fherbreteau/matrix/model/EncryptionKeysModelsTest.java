package io.github.fherbreteau.matrix.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.fherbreteau.matrix.json.JsonParser;
import org.junit.jupiter.api.Test;

class EncryptionKeysModelsTest {

  @Test
  void keyChangesPreservesUnknownFieldsAndAllowsOptionalLists() {
    var response =
        KeyChangesResponse.from(
            JsonParser.parse("{\"changed\":[\"@a:hs\"],\"left\":[],\"future\":7}"));
    assertThat(response.changed()).containsExactly(UserId.of("@a:hs"));
    assertThat(response.left()).isEmpty();
    assertThat(response.raw().asObject().get("future").asLong()).isEqualTo(7);

    assertThat(KeyChangesResponse.from(JsonParser.parse("{}")))
        .satisfies(
            empty -> {
              assertThat(empty.changed()).isEmpty();
              assertThat(empty.left()).isEmpty();
            });
  }

  @Test
  void keyChangesRejectsMalformedUserList() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> KeyChangesResponse.from(JsonParser.parse("{\"changed\":[1]}")));
  }

  @Test
  void uploadRequestPreservesNestedCryptographicJson() {
    var request =
        KeysUploadRequest.of(
            JsonParser.parse(
                "{\"device_keys\":{\"keys\":{\"ed25519:D\":\"abc\"}},"
                    + "\"one_time_keys\":{\"signed_curve25519:k\":{\"key\":\"x\","
                    + "\"signatures\":{\"@a:hs\":{}}}},\"extension\":true}"));
    assertThat(request.toJson().toJson())
        .isEqualTo(
            "{\"device_keys\":{\"keys\":{\"ed25519:D\":\"abc\"}},"
                + "\"one_time_keys\":{\"signed_curve25519:k\":{\"key\":\"x\","
                + "\"signatures\":{\"@a:hs\":{}}}},\"extension\":true}");
  }

  @Test
  void queryAndClaimResponsesPreserveKeyMapsAndOptionalFailures() {
    var query =
        KeysQueryResponse.from(
            JsonParser.parse(
                "{\"device_keys\":{\"@a:hs\":{\"D\":{\"keys\":{\"ed25519:D\":\"x\"}}}},"
                    + "\"master_keys\":{\"@a:hs\":{\"keys\":{}}},\"future\":true}"));
    assertThat(query.deviceKeys().get("@a:hs").asObject().get("D").asObject().has("keys")).isTrue();
    assertThat(query.masterKeys().names()).contains("@a:hs");
    assertThat(query.failures()).isNull();
    assertThat(query.toJson().get("future").asBoolean()).isTrue();
    assertThat(query.selfSigningKeys()).isNull();
    assertThat(query.userSigningKeys()).isNull();
    var response =
        EncryptionResponse.from(
            JsonParser.parse(
                "{\"device_keys\":{\"@a:hs\":{}},\"failures\":{},\"master_keys\":{},"
                    + "\"self_signing_keys\":{},\"user_signing_keys\":{},"
                    + "\"one_time_key_counts\":{},\"one_time_keys\":{},\"changed\":[],\"left\":[]}"));
    assertThat(response.toJson().size()).isEqualTo(9);
    assertThat(response.deviceKeys().size()).isEqualTo(1);
    assertThat(response.failures().size()).isZero();
    assertThat(response.masterKeys().size()).isZero();
    assertThat(response.selfSigningKeys().size()).isZero();
    assertThat(response.userSigningKeys().size()).isZero();
    assertThat(response.oneTimeKeyCounts().size()).isZero();
    assertThat(response.oneTimeKeys().size()).isZero();
    assertThat(response.oneTimeKeyCounts().size()).isZero();
    assertThat(response.changed().asArray().size()).isZero();
    assertThat(response.left().asArray().size()).isZero();
    assertThat(
            EncryptionResponse.from(
                    JsonParser.parse(
                        "{\"device_keys\":{\"@a:hs\":{}},"
                            + "\"failures\":{},\"master_keys\":{},\"self_signing_keys\":{},"
                            + "\"user_signing_keys\":{},\"one_time_key_counts\":{},"
                            + "\"one_time_keys\":{},\"changed\":[],\"left\":[]}"))
                .deviceKeys()
                .size())
        .isEqualTo(1);

    var claim =
        KeysClaimResponse.from(
            JsonParser.parse(
                "{\"one_time_keys\":{\"@a:hs\":{\"D\":{\"signed_curve25519:k\":{"
                    + "\"key\":\"secret-public\",\"signatures\":{}}}}},\"failures\":{}}"));
    assertThat(claim.oneTimeKeys().get("@a:hs").asObject().names()).contains("D");
    assertThat(claim.failures().size()).isZero();
  }

  @Test
  void signingAndSignatureRequestsRetainExtensionsAndUiAuth() {
    var signing =
        DeviceSigningUploadRequest.of(
            JsonParser.parse(
                "{\"master_key\":{\"keys\":{\"ed25519:k\":\"pub\"}},"
                    + "\"auth\":{\"type\":\"m.login.dummy\"},\"future\":[]}"));
    assertThat(signing.toJson().get("auth").asObject().get("type").asString())
        .isEqualTo("m.login.dummy");
    assertThat(signing.toJson().get("future").isArray()).isTrue();

    var signatures =
        KeySignaturesUploadRequest.of(
            JsonParser.parse("{\"@a:hs\":{\"ed25519:k\":{\"signatures\":{}}}}"));
    assertThat(signatures.toJson().get("@a:hs").asObject().names()).contains("ed25519:k");
  }

  @Test
  void responseWrappersValidateRequiredFieldsAndRetainUnknownFields() {
    var counts =
        KeysUploadResponse.from(
            JsonParser.parse("{\"one_time_key_counts\":{\"signed_curve25519\":23},\"new\":1}"));
    assertThat(counts.oneTimeKeyCounts().get("signed_curve25519").asLong()).isEqualTo(23);
    assertThat(counts.raw().asObject().get("new").asLong()).isEqualTo(1);

    assertThat(KeySignaturesUploadResponse.from(JsonParser.parse("{\"failures\":{}}")))
        .extracting(KeySignaturesUploadResponse::failures)
        .isNotNull();
    assertThat(DeviceSigningUploadResult.from(JsonParser.parse("{}")).toJson().size()).isZero();
    assertThat(EncryptionResponse.from(JsonParser.parse("{}")))
        .extracting(EncryptionResponse::changed)
        .isNull();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> EncryptionResponse.from(JsonParser.parse("[]")));

    assertThatIllegalArgumentException()
        .isThrownBy(() -> KeysUploadResponse.from(JsonParser.parse("{}")));
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> KeysUploadResponse.from(JsonParser.parse("{\"one_time_key_counts\":[]}")));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> KeysQueryResponse.from(JsonParser.parse("{}")));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> KeysClaimResponse.from(JsonParser.parse("{}")));
  }

  @Test
  void backupVersionAndWriteModelsPreserveFields() {
    var info =
        RoomKeyBackupInfo.from(
            JsonParser.parse(
                "{\"algorithm\":\"m.megolm_backup.v1\",\"auth_data\":{\"k\":\"v\"},"
                    + "\"count\":2,\"etag\":\"E\",\"version\":\"V\",\"future\":true}"));
    assertThat(info.algorithm()).isEqualTo("m.megolm_backup.v1");
    assertThat(info.authData().get("k").asString()).isEqualTo("v");
    assertThat(info.count()).isEqualTo(2);
    assertThat(info.etag()).isEqualTo("E");
    assertThat(info.version()).isEqualTo("V");
    assertThat(info.raw().get("future").asBoolean()).isTrue();

    var created = RoomKeyBackupVersion.from(JsonParser.parse("{\"version\":\"opaque\"}"));
    assertThat(created.version()).isEqualTo("opaque");

    var write =
        RoomKeyBackupWriteResponse.from(
            JsonParser.parse("{\"count\":3,\"etag\":\"new-etag\",\"extension\":1}"));
    assertThat(write.count()).isEqualTo(3);
    assertThat(write.etag()).isEqualTo("new-etag");
    assertThat(write.raw().get("extension").asLong()).isEqualTo(1);

    var versionRequest =
        RoomKeyBackupVersionRequest.of(
            JsonParser.parse("{\"algorithm\":\"a\",\"auth_data\":{},\"version\":\"V\"}"));
    assertThat(versionRequest.toJson().get("algorithm").asString()).isEqualTo("a");
    assertThat(versionRequest.version()).isEqualTo("V");
    assertThat(versionRequest.authData()).isNotNull();
    assertThat(versionRequest.toJson().has("auth_data")).isTrue();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> RoomKeyBackupInfo.from(JsonParser.parse("{}")));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> RoomKeyBackupWriteResponse.from(JsonParser.parse("{}")));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> RoomKeyBackupVersionRequest.of(JsonParser.parse("{}")));
  }

  @Test
  void encryptionRequestAndBackupResponsePreserveRawMaps() {
    var request =
        EncryptionRequest.of(
            JsonParser.parse(
                "{\"rooms\":{\"!r:hs\":{\"sessions\":{\"s\":{\"session_data\":{"
                    + "\"ciphertext\":\"opaque\"}}}}},\"unknown\":7}"));
    assertThat(request.toJson().get("rooms").asObject().get("!r:hs").asObject().has("sessions"))
        .isTrue();
    assertThat(request.toJson().get("unknown").asLong()).isEqualTo(7);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> EncryptionRequest.of(JsonParser.parse("[]")));

    var response =
        RoomKeyBackupKeysResponse.from(
            JsonParser.parse("{\"rooms\":{},\"extension\":\"retained\"}"));
    assertThat(response.toJson().get("rooms").asObject().size()).isZero();
    assertThat(response.toJson().get("extension").asString()).isEqualTo("retained");
  }
}
