package io.github.fherbreteau.matrix.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.fherbreteau.matrix.json.JsonParser;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UrlPreviewTest {
  @Test
  void parsesKnownAndOpenGraphPropertiesAndPreservesRawUnknownFields() {
    JsonValue json =
        JsonParser.parse(
            "{\"og:image\":\"mxc://example.org/image\",\"matrix:image:size\":9007199254740993,"
                + "\"og:title\":\"Example\",\"og:image:width\":48,"
                + "\"unrelated\":\"ignored\"}");

    UrlPreview preview = UrlPreview.from(json);

    assertThat(preview.imageUri()).isEqualTo(MxcUri.of("example.org", "image"));
    assertThat(preview.imageSize()).isEqualTo(9007199254740993L);
    assertThat(preview.properties())
        .containsOnlyKeys("og:image", "matrix:image:size", "og:title", "og:image:width");
    assertThat(preview.properties().get("og:title").asString()).isEqualTo("Example");
    assertThat(preview.properties().get("og:image:width").asLong()).isEqualTo(48);
    assertThat(preview.raw()).isSameAs(json);
    assertThat(preview.raw().asObject().get("unrelated").asString()).isEqualTo("ignored");
  }

  @Test
  void allowsMissingOptionalFieldsAndReturnsEmptyProperties() {
    UrlPreview preview = UrlPreview.from(JsonParser.parse("{}"));

    assertThat(preview.imageUri()).isNull();
    assertThat(preview.imageSize()).isNull();
    assertThat(preview.properties()).isEmpty();
  }

  @Test
  void rejectsNonObjectAndInvalidKnownFieldTypes() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> UrlPreview.from(null))
        .withMessage("URL preview must be a JSON object");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> UrlPreview.from(JsonParser.parse("[]")))
        .withMessage("URL preview must be a JSON object");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> UrlPreview.from(JsonParser.parse("{\"og:image\":42}")))
        .withMessage("og:image must be a string");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> UrlPreview.from(JsonParser.parse("{\"matrix:image:size\":\"42\"}")))
        .withMessage("matrix:image:size must be an integer");
  }

  @Test
  void rejectsFractionalAndOutOfRangeImageSizes() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> UrlPreview.from(JsonParser.parse("{\"matrix:image:size\":1.25}")))
        .withMessage("matrix:image:size must be a long integer");
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> UrlPreview.from(JsonParser.parse("{\"matrix:image:size\":9223372036854775808}")))
        .withMessage("matrix:image:size must be a long integer");
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> UrlPreview.from(JsonParser.parse("{\"matrix:image:size\":-9223372036854775809}")))
        .withMessage("matrix:image:size must be a long integer");
  }

  @Test
  void propertiesAreCopiedAndImmutable() {
    Map<String, JsonValue> properties = new LinkedHashMap<>();
    properties.put("og:title", JsonParser.parse("\"Original\""));
    UrlPreview preview = new UrlPreview(null, null, properties, null);
    properties.put("og:later", JsonParser.parse("true"));

    assertThat(preview.properties()).containsOnlyKeys("og:title");
    JsonValue replacement = JsonParser.parse("false");
    assertThatThrownBy(() -> preview.properties().put("og:changed", replacement))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
