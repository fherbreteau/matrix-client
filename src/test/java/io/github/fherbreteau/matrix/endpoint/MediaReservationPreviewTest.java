package io.github.fherbreteau.matrix.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import io.github.fherbreteau.matrix.error.AuthenticationException;
import io.github.fherbreteau.matrix.error.MatrixServerException;
import io.github.fherbreteau.matrix.model.MediaUploadReservation;
import io.github.fherbreteau.matrix.model.MxcUri;
import io.github.fherbreteau.matrix.model.PasswordCredentials;
import io.github.fherbreteau.matrix.model.UrlPreview;
import io.github.fherbreteau.matrix.transport.BinaryResponse;
import io.github.fherbreteau.matrix.transport.HttpTransport.Request;
import io.github.fherbreteau.matrix.transport.HttpTransport.Response;
import io.github.fherbreteau.matrix.transport.MediaSizeLimitException;
import io.github.fherbreteau.matrix.transport.MediaTransport;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MediaReservationPreviewTest {

  private static final String LOGIN_OK =
      "{\"user_id\":\"@alice:matrix.org\",\"access_token\":\"secret-token\",\"device_id\":\"DEV\"}";

  @Test
  void createsReservationWithAuthAndOptionalExpiry() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    requests,
                    new Response(200, LOGIN_OK),
                    new Response(
                        200,
                        "{\"content_uri\":\"mxc://media.example/id_1\","
                            + "\"unused_expires_at\":42,\"future\":true}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "password"));

    MediaUploadReservation reservation = client.createMediaUpload();

    assertThat(reservation.contentUri()).isEqualTo(MxcUri.of("media.example", "id_1"));
    assertThat(reservation.unusedExpiresAt()).isEqualTo(42L);
    assertThat(reservation.raw().asObject().get("future").asBoolean()).isTrue();
    Request request = requests.getLast();
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.url()).endsWith("/_matrix/media/v1/create");
    assertThat(request.body()).isNull();
    assertThat(request.headers()).containsEntry("Authorization", "Bearer secret-token");
  }

  @Test
  void reservedUploadStreamsBytesToMxcUriWithEncodedFilename() {
    var requests = new ArrayList<MediaTransport.StreamingBinaryRequest>();
    MediaUploadReservation reservation =
        new MediaUploadReservation(MxcUri.of("media.example", "reserved_id"), 42L, null);
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(stub -> new Response(200, LOGIN_OK))
            .mediaTransport(
                new MediaTransport() {
                  @Override
                  public BinaryResponse send(BinaryRequest request) {
                    throw new AssertionError("streaming request expected");
                  }

                  @Override
                  public BinaryResponse send(
                      StreamingBinaryRequest request, long maxUploadBytes, long maxResponseBytes) {
                    requests.add(request);
                    return new BinaryResponse(200, Map.of(), new byte[0], null);
                  }
                })
            .maxMediaUploadBytes(64)
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "password"));
    byte[] content = "reserved bytes".getBytes(StandardCharsets.UTF_8);

    MxcUri uploaded = client.uploadReservedMedia(reservation, content, "text/plain", "a b.txt");

    assertThat(uploaded).isEqualTo(reservation.contentUri());
    assertThat(requests).hasSize(1);
    MediaTransport.StreamingBinaryRequest request = requests.getFirst();
    assertThat(request.method()).isEqualTo("PUT");
    assertThat(request.url())
        .isEqualTo(
            "https://matrix.example.org/_matrix/media/v3/upload/media.example/reserved_id?filename=a%20b.txt");
    assertThat(request.headers()).containsEntry("Authorization", "Bearer secret-token");
    assertThat(request.contentType()).isEqualTo("text/plain");
    assertThat(request.contentLength()).hasValue(content.length);
    try {
      assertThat(request.body().readAllBytes()).isEqualTo(content);
    } catch (IOException exception) {
      throw new AssertionError(exception);
    }
  }

  @Test
  void reservedUploadDefaultsContentTypeAndOmitsAbsentFilename() {
    var requests = new ArrayList<MediaTransport.StreamingBinaryRequest>();
    MediaUploadReservation reservation =
        new MediaUploadReservation(MxcUri.of("media.example", "reserved_id"), null, null);
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(stub -> new Response(200, LOGIN_OK))
            .mediaTransport(
                new MediaTransport() {
                  @Override
                  public BinaryResponse send(BinaryRequest request) {
                    throw new AssertionError("streaming request expected");
                  }

                  @Override
                  public BinaryResponse send(
                      StreamingBinaryRequest request, long maxUploadBytes, long maxResponseBytes) {
                    requests.add(request);
                    return new BinaryResponse(200, Map.of(), new byte[0], null);
                  }
                })
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "password"));

    client.uploadReservedMedia(reservation, new byte[] {1, 2}, "application/octet-stream", null);

    assertThat(requests)
        .singleElement()
        .satisfies(
            request -> {
              assertThat(request.url())
                  .isEqualTo(
                      "https://matrix.example.org/_matrix/media/v3/upload/media.example/reserved_id");
              assertThat(request.contentType()).isEqualTo("application/octet-stream");
            });
  }

  @Test
  void reservedUploadFromPathStreamsFileAndUsesItsKnownLength() throws IOException {
    var requests = new ArrayList<MediaTransport.StreamingBinaryRequest>();
    MediaUploadReservation reservation =
        new MediaUploadReservation(MxcUri.of("media.example", "reserved_id"), null, null);
    Path file = Files.createTempFile("reserved-media", ".bin");
    try (var fileHandle = Files.newInputStream(file)) {
      assertThat(fileHandle.read()).isEqualTo(-1);
      byte[] content = "file payload".getBytes(StandardCharsets.UTF_8);
      Files.write(file, content);
      MatrixClient client =
          MatrixClient.builder("https://matrix.example.org")
              .transport(stub -> new Response(200, LOGIN_OK))
              .mediaTransport(
                  new MediaTransport() {
                    @Override
                    public BinaryResponse send(MediaTransport.BinaryRequest request) {
                      throw new AssertionError("streaming request expected");
                    }

                    @Override
                    public BinaryResponse send(
                        StreamingBinaryRequest request,
                        long maxUploadBytes,
                        long maxResponseBytes) {
                      requests.add(request);
                      return new BinaryResponse(200, Map.of(), new byte[0], null);
                    }
                  })
              .maxMediaUploadBytes(64)
              .build();

      client.login(new PasswordCredentials("@alice:matrix.org", "password"));

      assertThat(client.uploadReservedMedia(reservation, file, "text/plain", "file.txt"))
          .isEqualTo(reservation.contentUri());
      assertThat(requests)
          .singleElement()
          .satisfies(
              request -> {
                assertThat(request.contentLength()).hasValue(content.length);
                assertThat(request.contentType()).isEqualTo("text/plain");
              });
    } finally {
      Files.deleteIfExists(file);
    }
  }

  @Test
  void reservedUploadOmitsFilenameAndEnforcesUploadLimit() {
    var requests = new ArrayList<MediaTransport.StreamingBinaryRequest>();
    MediaUploadReservation reservation =
        new MediaUploadReservation(MxcUri.of("media.example", "reserved_id"), null, null);
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(stub -> new Response(200, LOGIN_OK))
            .mediaTransport(
                new MediaTransport() {
                  @Override
                  public BinaryResponse send(BinaryRequest request) {
                    throw new AssertionError("streaming request expected");
                  }

                  @Override
                  public BinaryResponse send(
                      StreamingBinaryRequest request, long maxUploadBytes, long maxResponseBytes) {
                    requests.add(request);
                    throw new MediaSizeLimitException(maxUploadBytes);
                  }
                })
            .maxMediaUploadBytes(2)
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "password"));

    ByteArrayInputStream oversizedContent = new ByteArrayInputStream(new byte[3]);
    assertThatThrownBy(
            () -> client.uploadReservedMedia(reservation, oversizedContent, 3, null, null))
        .isInstanceOf(MatrixServerException.class)
        .hasMessageContaining("maximum size of 2 bytes");
    assertThat(requests).hasSize(1);
    assertThat(requests.getFirst().url())
        .isEqualTo("https://matrix.example.org/_matrix/media/v3/upload/media.example/reserved_id");
  }

  @Test
  void reservedUploadMapsMatrixErrorsAndIsRejectedWithoutSession() {
    MediaUploadReservation reservation =
        new MediaUploadReservation(MxcUri.of("media.example", "reserved_id"), null, null);
    MatrixClient unauthenticated =
        MatrixClient.builder("https://matrix.example.org")
            .mediaTransport(ignored -> new BinaryResponse(200, Map.of(), new byte[0], null))
            .build();
    assertThatThrownBy(
            () -> unauthenticated.uploadReservedMedia(reservation, new byte[0], null, null))
        .isInstanceOf(AuthenticationException.class);

    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(stub -> new Response(200, LOGIN_OK))
            .mediaTransport(
                ignored ->
                    new BinaryResponse(
                        404,
                        Map.of("content-type", "application/json"),
                        "{\"errcode\":\"M_NOT_FOUND\",\"error\":\"expired\"}"
                            .getBytes(StandardCharsets.UTF_8),
                        null))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "password"));
    assertThatExceptionOfType(MatrixServerException.class)
        .isThrownBy(() -> client.uploadReservedMedia(reservation, new byte[0], null, null))
        .asInstanceOf(type(MatrixServerException.class))
        .extracting(MatrixServerException::getErrcode)
        .isEqualTo("M_NOT_FOUND");

    MatrixClient forbidden =
        MatrixClient.builder("https://matrix.example.org")
            .transport(stub -> new Response(200, LOGIN_OK))
            .mediaTransport(
                ignored ->
                    new BinaryResponse(
                        409,
                        Map.of("content-type", "application/json"),
                        "{\"errcode\":\"M_CANNOT_OVERWRITE_MEDIA\",\"error\":\"exists\"}"
                            .getBytes(StandardCharsets.UTF_8),
                        null))
            .build();
    forbidden.login(new PasswordCredentials("@alice:matrix.org", "password"));
    assertThatExceptionOfType(MatrixServerException.class)
        .isThrownBy(() -> forbidden.uploadReservedMedia(reservation, new byte[0], null, null))
        .asInstanceOf(type(MatrixServerException.class))
        .extracting(MatrixServerException::getErrcode)
        .isEqualTo("M_CANNOT_OVERWRITE_MEDIA");
  }

  @Test
  void urlPreviewUsesEncodedQueryAndParsesTypedAndUnknownMetadata() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    requests,
                    new Response(200, LOGIN_OK),
                    new Response(
                        200,
                        "{\"og:title\":\"Example\",\"og:image\":\"mxc://media.example/image\","
                            + "\"matrix:image:size\":12,\"other\":true}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "password"));

    UrlPreview preview = client.getUrlPreview("https://example.org/a b?x=1", 123L);

    assertThat(preview.imageUri()).isEqualTo(MxcUri.of("media.example", "image"));
    assertThat(preview.imageSize()).isEqualTo(12L);
    assertThat(preview.properties().get("og:title").asString()).isEqualTo("Example");
    assertThat(preview.raw().asObject().get("other").asBoolean()).isTrue();
    assertThat(requests.getLast().method()).isEqualTo("GET");
    assertThat(requests.getLast().url())
        .endsWith(
            "/_matrix/client/v1/media/preview_url?url=https%3A%2F%2Fexample.org%2Fa%20b%3Fx%3D1&ts=123");
    assertThat(requests.getLast().headers()).containsEntry("Authorization", "Bearer secret-token");
  }

  @Test
  void urlPreviewWithoutImageIsEmptyAndMapsServerErrors() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                queued(
                    new Response(200, LOGIN_OK),
                    new Response(200, "{}"),
                    new Response(403, "{\"errcode\":\"M_FORBIDDEN\",\"error\":\"denied\"}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "password"));

    String url = "https://example.org";
    assertThat(client.getUrlPreview(url).properties()).isEmpty();
    assertThatExceptionOfType(MatrixServerException.class)
        .isThrownBy(() -> client.getUrlPreview(url))
        .asInstanceOf(type(MatrixServerException.class))
        .extracting(MatrixServerException::getErrcode)
        .isEqualTo("M_FORBIDDEN");
  }

  @Test
  void mediaReservationAndPreviewEndpointsRequireAuthentication() {
    MatrixClient client = MatrixClient.builder("https://matrix.example.org").build();
    assertThatThrownBy(client::createMediaUpload).isInstanceOf(AuthenticationException.class);
    assertThatThrownBy(() -> client.getUrlPreview("https://example.org"))
        .isInstanceOf(AuthenticationException.class);
  }

  private static HttpTransportStub recording(List<Request> requests, Response... responses) {
    var stub = new HttpTransportStub();
    for (Response response : responses) {
      stub.enqueue(response);
    }
    stub.recordInto(requests);
    return stub;
  }

  private static HttpTransportStub queued(Response... responses) {
    var stub = new HttpTransportStub();
    for (Response response : responses) {
      stub.enqueue(response);
    }
    return stub;
  }
}
