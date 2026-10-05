package io.github.fherbreteau.matrix.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.fherbreteau.matrix.json.JsonNumber;
import io.github.fherbreteau.matrix.json.JsonParser;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RoomTagTest {

  @Test
  void createsEmptyAndOrderedTags() {
    assertThat(RoomTag.empty().toJson().toJson()).isEqualTo("{}");
    assertThat(RoomTag.withOrder(0).order()).isZero();
    assertThat(RoomTag.withOrder(1).order()).isEqualTo(1.0);
  }

  @Test
  void parsesAndCopiesAllTagProperties() {
    RoomTag tag = RoomTag.from(JsonParser.parse("{\"order\":0.5,\"extension\":true}"));
    assertThat(tag.order()).isEqualTo(0.5);
    assertThat(tag.toJson().toJson()).isEqualTo("{\"order\":0.5,\"extension\":true}");

    Map<String, JsonValue> properties = new LinkedHashMap<>();
    properties.put("order", JsonNumber.of(0.25));
    RoomTag copied = new RoomTag(properties);
    properties.put("later", JsonParser.parse("true"));
    assertThat(copied.properties()).containsOnlyKeys("order");
    JsonValue value = JsonParser.parse("true");
    assertThatThrownBy(() -> copied.properties().put("later", value))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void rejectsNonObjectAndInvalidOrderValues() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> RoomTag.from(null))
        .withMessage("room tag must be a JSON object");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> RoomTag.from(JsonParser.parse("[]")))
        .withMessage("room tag must be a JSON object");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> RoomTag.from(JsonParser.parse("{\"order\":\"bad\"}")))
        .withMessage("tag order must be a number between 0 and 1");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> RoomTag.withOrder(1.1))
        .withMessage("tag order must be a number between 0 and 1");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> RoomTag.withOrder(-0.1))
        .withMessage("tag order must be a number between 0 and 1");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new RoomTag(Map.of("order", JsonParser.parse("null"))))
        .withMessage("tag order must be a number between 0 and 1");
  }
}
