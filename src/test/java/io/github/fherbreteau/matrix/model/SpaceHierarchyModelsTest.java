package io.github.fherbreteau.matrix.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.fherbreteau.matrix.json.JsonParser;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpaceHierarchyModelsTest {
  @Test
  void parsesRoomChildrenAndOpaquePagination() {
    JsonValue json =
        JsonParser.parse(
            "{\"rooms\":[{\"room_id\":\"!space:example.org\",\"guest_can_join\":true,\"num_joined_members\":2,\"world_readable\":false,\"children_state\":[{\"content\":{\"via\":[\"example.org\"]},\"origin_server_ts\":12,\"sender\":\"@alice:example.org\",\"state_key\":\"!child:example.org\",\"type\":\"m.space.child\",\"future\":true}]}],\"next_batch\":\"opaque+/%==\",\"other\":true}");
    SpaceHierarchyResponse response = SpaceHierarchyResponse.from(json);
    assertThat(response.rooms())
        .singleElement()
        .satisfies(
            room -> {
              assertThat(room.roomId()).isEqualTo(RoomId.of("!space:example.org"));
              assertThat(room.childrenState())
                  .singleElement()
                  .satisfies(
                      child -> {
                        assertThat(child.originServerTs()).isEqualTo(12);
                        assertThat(child.sender()).isEqualTo(UserId.of("@alice:example.org"));
                        assertThat(child.raw().asObject().get("future").asBoolean()).isTrue();
                      });
            });
    assertThat(response.nextBatch()).isEqualTo("opaque+/%==");
    assertThat(response.raw().asObject().get("other").asBoolean()).isTrue();
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                SpaceHierarchyResponse.from(
                    JsonParser.parse(
                        "{\"rooms\":[{\"room_id\":\"!space:example.org\",\"guest_can_join\":true,\"num_joined_members\":2,\"world_readable\":false}]}")));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> SpaceChildStateEvent.from(JsonParser.parse("{}")));
  }

  @Test
  void optionsOmitNullsAndPreserveOpaqueToken() {
    var query = new SpaceHierarchyOptions("+/opaque==", 4, 3, true).toQuery();
    assertThat(query.toJson())
        .isEqualTo("{\"from\":\"+/opaque==\",\"limit\":4,\"max_depth\":3,\"suggested_only\":true}");
    assertThat(new SpaceHierarchyOptions(null, null, null, null).toQuery().toJson())
        .isEqualTo("{}");
  }

  @Test
  void optionsRejectInvalidLimits() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new SpaceHierarchyOptions(null, 0, null, null));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new SpaceHierarchyOptions(null, -1, null, null));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new SpaceHierarchyOptions(null, null, -1, null));
    assertThat(new SpaceHierarchyOptions(null, 1, 0, null).toQuery().size()).isEqualTo(2);
  }

  @Test
  void hierarchyCollectionsAreImmutable() {
    List<SpaceHierarchyRoom> rooms = new ArrayList<>();
    SpaceHierarchyResponse response = new SpaceHierarchyResponse(rooms, null, null);
    rooms.add(null);
    assertThat(response.rooms()).isEmpty();
    List<SpaceHierarchyRoom> responseRooms = response.rooms();
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> responseRooms.add(null));
  }
}
