package io.github.fherbreteau.matrix.transport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class JdkMediaTransportHttpServerTest {

  @Test
  void legacyTransportBoundsResponseWhileReading() throws Exception {
    var transport =
        (MediaTransport)
            request ->
                new MediaTransport.BinaryResponse(
                    200, Map.of(), new ByteArrayInputStream(new byte[] {1, 2, 3}), -1, null);
    var response =
        transport.send(
            new MediaTransport.BinaryRequest("GET", "https://media/x", Map.of(), null, "x/y"), 2);
    InputStream body = response.bodyStream();
    assertThat(body.readNBytes(2)).containsExactly(1, 2);
    assertThatThrownBy(body::read).isInstanceOf(MediaSizeLimitException.class);
    response.close();
  }

  @Test
  void legacyTransportBoundsStreamUpload() {
    var sent = new AtomicReference<MediaTransport.BinaryRequest>();
    MediaTransport transport =
        request -> {
          sent.set(request);
          return new MediaTransport.BinaryResponse(200, Map.of(), new byte[0], null);
        };
    var request =
        new MediaTransport.StreamingBinaryRequest(
            "POST",
            "https://media/upload",
            Map.of(),
            new ByteArrayInputStream(new byte[] {1, 2, 3}),
            OptionalLong.empty(),
            "application/octet-stream");
    assertThatThrownBy(() -> transport.send(request, 2, 0))
        .isInstanceOf(MediaSizeLimitException.class);
    assertThat(sent).hasValue(null);
  }

  @Test
  void legacyTransportStreamsWithinConfiguredUploadLimit() {
    var sent = new AtomicReference<MediaTransport.BinaryRequest>();
    MediaTransport transport =
        request -> {
          sent.set(request);
          return new MediaTransport.BinaryResponse(200, Map.of(), new byte[0], null);
        };
    var request =
        new MediaTransport.StreamingBinaryRequest(
            "POST",
            "https://media/upload",
            Map.of(),
            new ByteArrayInputStream(new byte[] {1, 2}),
            OptionalLong.of(2),
            "application/octet-stream");
    assertThat(transport.send(request, 2, 0).statusCode()).isEqualTo(200);
    assertThat(sent.get().body()).containsExactly(1, 2);
  }
}
