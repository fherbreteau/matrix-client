package io.github.fherbreteau.matrix.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import io.github.fherbreteau.matrix.error.AuthenticationException;
import io.github.fherbreteau.matrix.error.DiscoveryException;
import io.github.fherbreteau.matrix.error.MatrixServerException;
import io.github.fherbreteau.matrix.model.MxcUri;
import io.github.fherbreteau.matrix.model.PasswordCredentials;
import io.github.fherbreteau.matrix.model.ThumbnailMethod;
import io.github.fherbreteau.matrix.transport.HttpTransport.Response;
import io.github.fherbreteau.matrix.transport.MediaTransport.BinaryRequest;
import io.github.fherbreteau.matrix.transport.MediaTransport.BinaryResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class MediaTest {

  private static final String LOGIN_OK =
      "{\"user_id\":\"@alice:matrix.org\",\"access_token\":\"secret-token\",\"device_id\":\"DEV\"}";

  @Test
  void mxcUrisParseAndValidate() {
    assertThat(MxcUri.parse("mxc://matrix.org/abc-123_X").toString())
        .isEqualTo("mxc://matrix.org/abc-123_X");
    assertThat(MxcUri.of("matrix.org", "abc").toString()).isEqualTo("mxc://matrix.org/abc");
  }

  @Test
  void mxcUrisRejectMalformedInput() {
    assertThatThrownBy(() -> MxcUri.parse(null)).isInstanceOf(DiscoveryException.class);
    assertThatThrownBy(() -> MxcUri.parse("https://x/y")).isInstanceOf(DiscoveryException.class);
    assertThatThrownBy(() -> MxcUri.parse("mxc://")).isInstanceOf(DiscoveryException.class);
    assertThatThrownBy(() -> MxcUri.parse("mxc://server")).isInstanceOf(DiscoveryException.class);
    assertThatThrownBy(() -> MxcUri.parse("mxc://server/")).isInstanceOf(DiscoveryException.class);
    assertThatThrownBy(() -> MxcUri.parse("mxc://server/a/b"))
        .isInstanceOf(DiscoveryException.class);
    assertThatThrownBy(() -> MxcUri.parse("mxc://server/../../etc/passwd"))
        .isInstanceOf(DiscoveryException.class);
    assertThatIllegalArgumentException().isThrownBy(() -> new MxcUri(null, "x"));
    assertThatIllegalArgumentException().isThrownBy(() -> new MxcUri("server", " "));
  }

  @Test
  void uploadSendsRawBytesWithContentTypeAndReturnsMxcUri() {
    var requests = new ArrayList<BinaryRequest>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(stub -> new Response(200, LOGIN_OK))
            .mediaTransport(
                binary -> {
                  requests.add(binary);
                  return new BinaryResponse(
                      200,
                      java.util.Map.of(),
                      "{\"content_uri\":\"mxc://matrix.org/abc123\"}"
                          .getBytes(StandardCharsets.UTF_8),
                      null);
                })
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    MxcUri uri =
        client.uploadMedia("hello".getBytes(StandardCharsets.UTF_8), "text/plain", "hello.txt");
    assertThat(uri.toString()).isEqualTo("mxc://matrix.org/abc123");
    BinaryRequest request = requests.getFirst();
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.url())
        .isEqualTo("https://matrix.example.org/_matrix/media/v3/upload?filename=hello.txt");
    assertThat(request.contentType()).isEqualTo("text/plain");
    assertThat(request.body()).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void uploadWithoutFilenameOmitsTheQuery() {
    var requests = new ArrayList<BinaryRequest>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(stub -> new Response(200, LOGIN_OK))
            .mediaTransport(
                binary -> {
                  requests.add(binary);
                  return new BinaryResponse(
                      200,
                      java.util.Map.of(),
                      "{\"content_uri\":\"mxc://m/abc\"}".getBytes(StandardCharsets.UTF_8),
                      null);
                })
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    client.uploadMedia(new byte[0], "application/octet-stream", null);
    assertThat(requests.getFirst().url()).endsWith("/_matrix/media/v3/upload");
    assertThat(requests.getFirst().body()).isEmpty();
  }

  @Test
  void uploadErrorsMapToMatrixServerException() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(stub -> new Response(200, LOGIN_OK))
            .mediaTransport(
                binary ->
                    new BinaryResponse(
                        413,
                        java.util.Map.of("content-type", "application/json"),
                        "{\"errcode\":\"M_TOO_LARGE\",\"error\":\"too big\"}"
                            .getBytes(StandardCharsets.UTF_8),
                        null))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    assertThatExceptionOfType(MatrixServerException.class)
        .isThrownBy(() -> client.uploadMedia(new byte[10], "text/plain", null))
        .asInstanceOf(type(MatrixServerException.class))
        .extracting(MatrixServerException::getErrcode)
        .isEqualTo("M_TOO_LARGE");
  }

  @Test
  void uploadWithoutAuthRaisesAuthenticationException() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(stub -> new Response(200, "{}"))
            .build();
    assertThatThrownBy(() -> client.uploadMedia(new byte[1], "text/plain", null))
        .isInstanceOf(AuthenticationException.class);
  }

  @Test
  void downloadStreamsTheBodyWithHeaders() {
    var requests = new ArrayList<BinaryRequest>();
    byte[] payload = "media-bytes".getBytes(StandardCharsets.UTF_8);
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(stub -> new Response(200, LOGIN_OK))
            .mediaTransport(
                binary -> {
                  requests.add(binary);
                  return new BinaryResponse(
                      200,
                      java.util.Map.of(
                          "Content-Type", "image/png",
                          "Content-Disposition", "inline; filename=\"picture.png\""),
                      payload,
                      null);
                })
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    try (var download = client.downloadMedia(MxcUri.parse("mxc://matrix.org/pic123"), 1024)) {
      assertThat(download.contentType()).isEqualTo("image/png");
      assertThat(download.contentDisposition()).contains("picture.png");
      assertThat(download.body().readAllBytes()).isEqualTo(payload);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
    BinaryRequest request = requests.getFirst();
    assertThat(request.url())
        .isEqualTo("https://matrix.example.org/_matrix/client/v1/media/download/matrix.org/pic123");
    assertThat(request.headers())
        .containsEntry(
            io.github.fherbreteau.matrix.transport.HttpTransport.Request.AUTHORIZATION_HEADER,
            "Bearer secret-token");
  }

  @Test
  void downloadWithFilenameIncludesItInThePath() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(stub -> new Response(200, LOGIN_OK))
            .mediaTransport(
                binary -> new BinaryResponse(200, java.util.Map.of(), new byte[0], null))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    try (var unused = client.downloadMedia(MxcUri.parse("mxc://m/abc"), "report.pdf", 1024)) {
      // closed immediately; the assertion below is what matters
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  @Test
  void downloadOverTheConfiguredLimitRaisesMTooLarge() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(stub -> new Response(200, LOGIN_OK))
            .mediaTransport(
                binary ->
                    new BinaryResponse(
                        200,
                        java.util.Map.of("content-type", "application/octet-stream"),
                        new byte[100],
                        null))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    assertThatExceptionOfType(MatrixServerException.class)
        .isThrownBy(() -> client.downloadMedia(MxcUri.parse("mxc://m/abc"), 10))
        .asInstanceOf(type(MatrixServerException.class))
        .extracting(MatrixServerException::getErrcode)
        .isEqualTo("M_TOO_LARGE");
  }

  @Test
  void thumbnailSendsWidthHeightMethodAndAnimated() {
    var requests = new ArrayList<BinaryRequest>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(stub -> new Response(200, LOGIN_OK))
            .mediaTransport(
                binary -> {
                  requests.add(binary);
                  return new BinaryResponse(
                      200, java.util.Map.of("content-type", "image/png"), new byte[10], null);
                })
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    try (var thumbnail =
        client.getThumbnail(
            MxcUri.parse("mxc://matrix.org/pic123"),
            640,
            480,
            ThumbnailMethod.SCALE,
            false,
            1024)) {
      assertThat(thumbnail.contentType()).isEqualTo("image/png");
    } catch (Exception e) {
      throw new AssertionError(e);
    }
    assertThat(requests.getFirst().url())
        .isEqualTo(
            "https://matrix.example.org/_matrix/client/v1/media/thumbnail/matrix.org/pic123"
                + "?width=640&height=480&method=scale&animated=false");
  }

  @Test
  void mediaConfigParsesTheUploadLimit() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                queued(
                    new Response(200, LOGIN_OK),
                    new Response(200, "{\"m.upload.size\":52428800}"),
                    new Response(200, "{}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    assertThat(client.getMediaConfig()).contains(52428800L);
    assertThat(client.getMediaConfig()).isEmpty();
  }

  @Test
  void mediaConfigWithoutUploadLimitYieldsEmpty() {
    var requests = new ArrayList<BinaryRequest>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(stub -> new Response(200, LOGIN_OK))
            .mediaTransport(
                binary -> {
                  requests.add(binary);
                  return new BinaryResponse(200, java.util.Map.of(), new byte[0], 5000L);
                })
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    assertThat(client.getMediaConfig()).isEmpty();
  }

  @Test
  void mediaWithoutSessionRaisesAuthenticationException() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(stub -> new Response(200, "{}"))
            .build();
    assertThatThrownBy(() -> client.downloadMedia(MxcUri.parse("mxc://m/abc"), 100))
        .isInstanceOf(AuthenticationException.class);
    assertThatThrownBy(() -> client.getMediaConfig()).isInstanceOf(AuthenticationException.class);
  }

  private static HttpTransportStub queued(Response... responses) {
    var stub = new HttpTransportStub();
    for (Response response : responses) {
      stub.enqueue(response);
    }
    return stub;
  }
}
