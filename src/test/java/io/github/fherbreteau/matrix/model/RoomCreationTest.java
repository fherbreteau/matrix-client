package io.github.fherbreteau.matrix.model;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.fherbreteau.matrix.json.JsonParser;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoomCreationTest {

  @Test
  void emptyParametersSerializeToAnEmptyBody() {
    assertThat(RoomCreation.builder().build().toJson().toJson()).isEqualTo("{}");
  }

  @Test
  void allParametersAreSerializedWithTheirSpecNames() {
    var creation =
        RoomCreation.builder()
            .visibility("public")
            .roomAliasName("general")
            .name("The Room")
            .topic("About everything")
            .invites(List.of(UserId.of("@bob:matrix.org"), UserId.of("@carol:matrix.org")))
            .invites3pid(List.of(Invite3pid.of("id.example.org", "tok", "email", "a@b.c")))
            .roomVersion("11")
            .preset("public_chat")
            .direct(true)
            .initialState(
                List.of(
                    StateEvent.of(
                        "m.room.history_visibility",
                        JsonParser.parse("{\"history_visibility\":\"world_readable\"}"))))
            .creationContent(CreationContent.builder().federate(false).build())
            .powerLevelContentOverride(JsonParser.parse("{\"ban\":50}"))
            .build();
    String body = creation.toJson().toJson();
    assertThat(body)
        .contains("\"visibility\":\"public\"")
        .contains("\"room_alias_name\":\"general\"")
        .contains("\"name\":\"The Room\"")
        .contains("\"topic\":\"About everything\"")
        .contains("\"room_version\":\"11\"")
        .contains("\"preset\":\"public_chat\"")
        .contains("\"is_direct\":true")
        .contains("\"initial_state\":")
        .contains("\"creation_content\":{\"m.federate\":false}")
        .contains("\"power_level_content_override\":{\"ban\":50}")
        .contains("\"user_id\":\"@bob:matrix.org\"")
        .contains("\"user_id\":\"@carol:matrix.org\"")
        .contains(
            "\"invite_3pid\":[{\"id_server\":\"id.example.org\",\"id_access_token\":\"tok\",\"medium\":\"email\",\"address\":\"a@b.c\"}]");
  }

  @Test
  void unsetParametersAreOmitted() {
    var creation = RoomCreation.builder().name("Only a name").build();
    assertThat(creation.toJson().toJson()).isEqualTo("{\"name\":\"Only a name\"}");
  }

  @Test
  void directDefaultsToFalse() {
    var creation = RoomCreation.builder().build();
    assertThat(creation.toJson().asObject().has("is_direct")).isFalse();
  }
}
