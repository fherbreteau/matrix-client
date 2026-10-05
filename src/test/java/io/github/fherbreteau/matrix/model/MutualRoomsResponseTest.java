package io.github.fherbreteau.matrix.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.github.fherbreteau.matrix.json.JsonParser;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MutualRoomsResponseTest {

  @Test
  void parsesRoomIdsPaginationAndUnknownFields() {
    JsonValue json =
        JsonParser.parse(
            "{\"count\":2,\"joined\":[\"!one:example.org\",\"!two:example.org\"],"
                + "\"next_batch\":\"opaque+/=\",\"future\":true}");
    MutualRoomsResponse response = MutualRoomsResponse.from(json);

    assertThat(response.count()).isEqualTo(2);
    assertThat(response.joined())
        .containsExactly(RoomId.of("!one:example.org"), RoomId.of("!two:example.org"));
    assertThat(response.nextBatch()).isEqualTo("opaque+/=");
    assertThat(response.hasNextBatch()).isTrue();
    assertThat(response.raw()).isSameAs(json);
    assertThat(response.raw().asObject().get("future").asBoolean()).isTrue();
  }

  @Test
  void supportsMissingTokenAndEmptyJoinedList() {
    MutualRoomsResponse response =
        MutualRoomsResponse.from(JsonParser.parse("{\"count\":0,\"joined\":[]}"));

    assertThat(response.joined()).isEmpty();
    assertThat(response.nextBatch()).isNull();
    assertThat(response.hasNextBatch()).isFalse();
  }

  @Test
  void copiesAndProtectsJoinedRoomList() {
    List<RoomId> roomIds = new ArrayList<>(List.of(RoomId.of("!room:example.org")));
    MutualRoomsResponse response = new MutualRoomsResponse(1, roomIds, null, null);
    roomIds.clear();

    assertThat(response.joined()).containsExactly(RoomId.of("!room:example.org"));
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(response.joined()::clear);
  }

  @Test
  void rejectsMalformedRequiredAndOptionalFields() {
    var malformedObject = JsonParser.parse("{}");
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> MutualRoomsResponse.from(JsonParser.parse("[]")))
        .withMessage("mutual rooms response must be a JSON object");
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> MutualRoomsResponse.from(malformedObject))
        .withMessage("mutual rooms response is missing required fields");
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> MutualRoomsResponse.from(JsonParser.parse("{\"count\":-1,\"joined\":[]}")))
        .withMessage("mutual rooms response is missing required fields");
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> MutualRoomsResponse.from(JsonParser.parse("{\"count\":0,\"joined\":[1]}")))
        .withMessage("mutual rooms joined must contain room IDs");
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () ->
                MutualRoomsResponse.from(
                    JsonParser.parse("{\"count\":0,\"joined\":[],\"next_batch\":1}")))
        .withMessage("mutual rooms next_batch must be a string");
  }
}
