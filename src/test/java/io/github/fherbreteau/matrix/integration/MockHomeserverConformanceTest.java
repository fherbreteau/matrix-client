package io.github.fherbreteau.matrix.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.github.fherbreteau.matrix.endpoint.MatrixClient;
import io.github.fherbreteau.matrix.error.RateLimitedException;
import io.github.fherbreteau.matrix.model.EventId;
import io.github.fherbreteau.matrix.model.MessageBody;
import io.github.fherbreteau.matrix.model.MxcUri;
import io.github.fherbreteau.matrix.model.PasswordCredentials;
import io.github.fherbreteau.matrix.model.RoomCreation;
import io.github.fherbreteau.matrix.model.RoomId;
import io.github.fherbreteau.matrix.model.SyncOptions;
import io.github.fherbreteau.matrix.model.events.EventRegistry;
import io.github.fherbreteau.matrix.model.events.MessageEventContent;
import io.github.fherbreteau.matrix.retry.RetryPolicy;
import io.github.fherbreteau.matrix.transport.HttpTransport;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MockHomeserverConformanceTest {

  private static final String LOGIN_RESPONSE =
      "{\"user_id\":\"@alice:example.org\",\"access_token\":\"fixture-secret\","
          + "\"device_id\":\"DEVICE\",\"org.example.login\":true}";

  @Test
  void loginCreateRoomSendAndReadUnknownStateThroughRealHttp() {
    try (var server = new MockMatrixHomeserver()) {
      server.enqueue(200, LOGIN_RESPONSE);
      server.enqueue(200, "{\"room_id\":\"!room:example.org\",\"org.example.room\":1}");
      server.enqueue(200, "{\"event_id\":\"$event:example.org\"}");
      server.enqueue(
          200, "[{\"type\":\"org.example.custom\",\"state_key\":\"\",\"content\":{\"x\":1}}]");
      MatrixClient client =
          MatrixClient.builder(server.baseUrl()).retryPolicy(RetryPolicy.disabled()).build();

      client.login(new PasswordCredentials("@alice:example.org", "not-logged"));
      RoomId roomId = client.createRoom(RoomCreation.builder().name("fixture room").build());
      EventId eventId =
          client.sendMessageEvent(
              roomId, "m.room.message", MessageBody.text("hello fixture").toJson());
      var events = client.getRoomState(roomId);

      server.assertWithDiagnostics(
          () -> {
            assertThat(roomId.value()).isEqualTo("!room:example.org");
            assertThat(eventId.value()).isEqualTo("$event:example.org");
            assertThat(events).hasSize(1);
            assertThat(events.getFirst().type()).isEqualTo("org.example.custom");
            assertThat(
                    events.getFirst().raw().asObject().get("content").asObject().get("x").asLong())
                .isEqualTo(1);
            assertThat(server.requests()).hasSize(4);
            assertThat(server.requests().get(1).authorization()).isEqualTo("Bearer fixture-secret");
            assertThat(server.requests().get(1).body()).contains("fixture room");
            assertThat(server.requests().getFirst().toString()).doesNotContain("fixture-secret");
            assertThat(server.requests().get(2).method()).isEqualTo("PUT");
            assertThat(server.requests().get(2).body()).contains("hello fixture");
          });
    }
  }

  @Test
  void unknownMessageTypeRetainsRawFieldsAcrossHttpBoundary() {
    try (var server = new MockMatrixHomeserver()) {
      server.enqueue(200, LOGIN_RESPONSE);
      server.enqueue(
          200,
          "{\"event_id\":\"$e\",\"type\":\"m.room.message\",\"sender\":\"@bob:example.org\","
              + "\"content\":{\"msgtype\":\"org.example.custom\",\"body\":\"payload\","
              + "\"org.example.ext\":true}}");
      MatrixClient client = MatrixClient.builder(server.baseUrl()).build();
      client.login(new PasswordCredentials("@alice:example.org", "unused"));

      var event = client.getRoomEvent(RoomId.of("!room:example.org"), EventId.of("$e"));
      var typed = event.withTypedContent(new EventRegistry());

      server.assertWithDiagnostics(
          () -> {
            assertThat(typed.content()).isInstanceOf(MessageEventContent.Unknown.class);
            assertThat(
                    typed
                        .envelope()
                        .raw()
                        .asObject()
                        .get("content")
                        .asObject()
                        .get("org.example.ext")
                        .asBoolean())
                .isTrue();
          });
    }
  }

  @Test
  void errorsPreserveUnknownFieldsAndRetryAfterOverHttp() {
    try (var server = new MockMatrixHomeserver()) {
      server.enqueue(200, LOGIN_RESPONSE);
      server.enqueue(
          new HttpTransport.Response(
              429,
              Map.of("retry-after", "9"),
              "{\"errcode\":\"M_LIMIT_EXCEEDED\",\"error\":\"slow"
                  + " down\",\"org.example.retry\":true}",
              9_000L));
      MatrixClient client =
          MatrixClient.builder(server.baseUrl()).retryPolicy(RetryPolicy.disabled()).build();
      client.login(new PasswordCredentials("@alice:example.org", "unused"));

      RateLimitedException exception =
          assertThatExceptionOfType(RateLimitedException.class).isThrownBy(client::whoami).actual();

      server.assertWithDiagnostics(
          () -> {
            assertThat(exception.getRetryAfterMs()).isEqualTo(9_000L);
            assertThat(exception.getFields()).containsKey("org.example.retry");
            assertThat(server.lastRequest().redactedDescription()).doesNotContain("fixture-secret");
          });
    }
  }

  @Test
  void syncPersistsOpaqueNextBatchFromActualHomeserverResponse() {
    try (var server = new MockMatrixHomeserver()) {
      server.enqueue(200, LOGIN_RESPONSE);
      server.enqueue(200, "{\"next_batch\":\"opaque-token-1\",\"rooms\":{},\"org.example\":7}");
      MatrixClient client = MatrixClient.builder(server.baseUrl()).build();
      client.login(new PasswordCredentials("@alice:example.org", "unused"));

      var response = client.sync(SyncOptions.defaults());

      server.assertWithDiagnostics(
          () -> {
            assertThat(response.nextBatch()).isEqualTo("opaque-token-1");
            assertThat(client.syncToken()).contains("opaque-token-1");
            assertThat(server.lastRequest().path()).isEqualTo("/_matrix/client/v3/sync");
            assertThat(server.lastRequest().query()).isNull();
          });
    }
  }

  @Test
  void assertionDiagnosticsIncludeExchangeMetadataWithoutSecrets() {
    try (var server = new MockMatrixHomeserver()) {
      server.enqueue(200, LOGIN_RESPONSE);
      server.enqueue(new HttpTransport.Response(200, "{\"value\":\"response-secret\"}"));
      MatrixClient client = MatrixClient.builder(server.baseUrl()).build();
      client.login(new PasswordCredentials("@alice:example.org", "request-password"));
      client.get("_matrix/client/v3/example?access_token=query-secret");

      assertThatExceptionOfType(AssertionError.class)
          .isThrownBy(
              () -> server.assertWithDiagnostics(() -> assertThat("actual").isEqualTo("expected")))
          .withMessageContaining("POST /_matrix/client/v3/login")
          .withMessageContaining("status=200")
          .withMessageNotContaining("request-password")
          .withMessageNotContaining("response-secret")
          .withMessageNotContaining("query-secret")
          .withMessageNotContaining("fixture-secret");
    }
  }

  @Test
  void mediaUploadAndDownloadUseRealHttpAndPreservePayload() {
    try (var server = new MockMatrixHomeserver()) {
      server.enqueue(200, LOGIN_RESPONSE);
      server.enqueue(200, "{\"content_uri\":\"mxc://example.org/media-1\"}");
      server.enqueue(200, "media bytes");
      MatrixClient client = MatrixClient.builder(server.baseUrl()).build();
      client.login(new PasswordCredentials("@alice:example.org", "unused"));

      MxcUri uri =
          client.uploadMedia("upload bytes".getBytes(StandardCharsets.UTF_8), "text/plain", null);
      try (var download = client.downloadMedia(uri, 100)) {
        assertThat(new String(download.body().readAllBytes(), StandardCharsets.UTF_8))
            .isEqualTo("media bytes");
      } catch (Exception exception) {
        throw new AssertionError(exception);
      }
      server.assertWithDiagnostics(
          () -> {
            assertThat(server.requests().get(1).method()).isEqualTo("POST");
            assertThat(server.requests().get(1).body()).isEqualTo("upload bytes");
            assertThat(server.requests().get(2).path())
                .isEqualTo("/_matrix/client/v1/media/download/example.org/media-1");
          });
    }
  }
}
