package io.github.fherbreteau.matrix.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.fherbreteau.matrix.json.JsonParser;
import org.junit.jupiter.api.Test;

class RoomTagsTest {

  @Test
  void parsesAndSerializesRoomTags() {
    RoomTags tags =
        RoomTags.from(
            JsonParser.parse(
                "{\"tags\":{\"m.favourite\":{\"order\":0.25},"
                    + "\"org.example/custom\":{\"extension\":true}}}"));

    assertThat(tags.tags()).containsOnlyKeys("m.favourite", "org.example/custom");
    assertThat(tags.tags().get("m.favourite").order()).isEqualTo(0.25);
    assertThat(tags.raw().toJson())
        .isEqualTo(
            "{\"tags\":{\"m.favourite\":{\"order\":0.25},"
                + "\"org.example/custom\":{\"extension\":true}}}");
    assertThat(tags.tags().get("org.example/custom").properties())
        .containsEntry("extension", JsonParser.parse("true"));
    assertThat(tags.toJson().toJson())
        .isEqualTo(
            "{\"tags\":{\"m.favourite\":{\"order\":0.25},"
                + "\"org.example/custom\":{\"extension\":true}}}");
  }

  @Test
  void permitsEmptyTagAndOrderingBounds() {
    assertThat(RoomTag.empty().toJson().toJson()).isEqualTo("{}");
    assertThat(RoomTag.withOrder(0).order()).isZero();
    assertThat(RoomTag.withOrder(1).order()).isEqualTo(1.0);
    assertThat(RoomTags.from(JsonParser.parse("{\"tags\":{}}")))
        .extracting(RoomTags::tags)
        .isEqualTo(java.util.Map.of());
  }

  @Test
  void rejectsInvalidTagResponseAndOrder() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> RoomTags.from(null))
        .withMessage("room tags response must be a JSON object");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> RoomTags.from(JsonParser.parse("{}")))
        .withMessage("room tags response must contain a tags object");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> RoomTags.from(JsonParser.parse("{\"tags\":{\"m.x\":[]} }")))
        .withMessage("room tag must be a JSON object");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> RoomTag.from(JsonParser.parse("{\"order\":\"0.5\"}")))
        .withMessage("tag order must be a number between 0 and 1");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> RoomTag.withOrder(1.1))
        .withMessage("tag order must be a number between 0 and 1");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> RoomTag.withOrder(-0.1))
        .withMessage("tag order must be a number between 0 and 1");
  }
}
