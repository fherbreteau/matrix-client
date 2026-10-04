package io.github.fherbreteau.matrix.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.fherbreteau.matrix.json.JsonParser;
import io.github.fherbreteau.matrix.json.JsonValue;
import org.junit.jupiter.api.Test;

class MediaUploadReservationTest {
  @Test
  void parsesKnownFieldsAndPreservesRawUnknownFields() {
    JsonValue json =
        JsonParser.parse(
            "{\"content_uri\":\"mxc://example.org/media_id-1\","
                + "\"unused_expires_at\":9007199254740993,\"future_field\":{\"enabled\":true}}");

    MediaUploadReservation reservation = MediaUploadReservation.from(json);

    assertThat(reservation.contentUri()).isEqualTo(MxcUri.of("example.org", "media_id-1"));
    assertThat(reservation.unusedExpiresAt()).isEqualTo(9007199254740993L);
    assertThat(reservation.raw()).isSameAs(json);
    assertThat(
            reservation.raw().asObject().get("future_field").asObject().get("enabled").asBoolean())
        .isTrue();
  }

  @Test
  void parsesOptionalExpiryAndAcceptsExactLongBoundaries() {
    assertThat(MediaUploadReservation.from(JsonParser.parse("{\"content_uri\":\"mxc://hs/id\"}")))
        .extracting(MediaUploadReservation::unusedExpiresAt)
        .isNull();
    assertThat(
            MediaUploadReservation.from(
                JsonParser.parse(
                    "{\"content_uri\":\"mxc://hs/id\",\"unused_expires_at\":"
                        + "9223372036854775807}")))
        .extracting(MediaUploadReservation::unusedExpiresAt)
        .isEqualTo(Long.MAX_VALUE);
    assertThat(
            MediaUploadReservation.from(
                JsonParser.parse(
                    "{\"content_uri\":\"mxc://hs/id\",\"unused_expires_at\":"
                        + "-9223372036854775808}")))
        .extracting(MediaUploadReservation::unusedExpiresAt)
        .isEqualTo(Long.MIN_VALUE);
  }

  @Test
  void rejectsNonObjectMissingFieldsAndInvalidKnownFieldTypes() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> MediaUploadReservation.from(null))
        .withMessage("media upload reservation must be a JSON object");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> MediaUploadReservation.from(JsonParser.parse("[]")))
        .withMessage("media upload reservation must be a JSON object");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> MediaUploadReservation.from(JsonParser.parse("{}")))
        .withMessage("media upload reservation must contain content_uri");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> MediaUploadReservation.from(JsonParser.parse("{\"content_uri\":null}")))
        .withMessage("media upload reservation must contain content_uri");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> MediaUploadReservation.from(JsonParser.parse("{\"content_uri\":42}")))
        .withMessage("media upload reservation must contain content_uri");
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                MediaUploadReservation.from(
                    JsonParser.parse(
                        "{\"content_uri\":\"mxc://hs/id\",\"unused_expires_at\":\"5\"}")))
        .withMessage("unused_expires_at must be an integer");
  }

  @Test
  void rejectsFractionalAndOutOfRangeExpiryValues() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                MediaUploadReservation.from(
                    JsonParser.parse(
                        "{\"content_uri\":\"mxc://hs/id\",\"unused_expires_at\":1.5}")))
        .withMessage("unused_expires_at must be a long integer");
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                MediaUploadReservation.from(
                    JsonParser.parse(
                        "{\"content_uri\":\"mxc://hs/id\",\"unused_expires_at\":"
                            + "9223372036854775808}")))
        .withMessage("unused_expires_at must be a long integer");
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                MediaUploadReservation.from(
                    JsonParser.parse(
                        "{\"content_uri\":\"mxc://hs/id\",\"unused_expires_at\":"
                            + "-9223372036854775809}")))
        .withMessage("unused_expires_at must be a long integer");
  }
}
