package io.github.fherbreteau.matrix.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class CreationContentTest {

  @Test
  void emptyCreationContentSerializesToAnEmptyObject() {
    assertThat(CreationContent.builder().build().toJson().toJson()).isEqualTo("{}");
  }

  @Test
  void allKeysAreSerializedWithTheirSpecNames() {
    String content =
        CreationContent.builder()
            .creator(UserId.of("@alice:matrix.org"))
            .additionalCreators(
                List.of(UserId.of("@bob:matrix.org"), UserId.of("@carol:matrix.org")))
            .federate(false)
            .type("m.space")
            .build()
            .toJson()
            .toJson();
    assertThat(content)
        .contains("\"creator\":\"@alice:matrix.org\"")
        .contains("\"additional_creators\":[\"@bob:matrix.org\",\"@carol:matrix.org\"]")
        .contains("\"m.federate\":false")
        .contains("\"type\":\"m.space\"");
  }

  @Test
  void federateDefaultsToTrueAndIsOmitted() {
    var creation = CreationContent.builder().build();
    assertThat(creation.toJson().asObject().has("m.federate")).isFalse();
  }

  @Test
  void nullCreatorIsOmitted() {
    var creation = CreationContent.builder().type("m.space").build();
    assertThat(creation.toJson().asObject().has("creator")).isFalse();
  }

  @Test
  void roomVersionOptionIsIgnoredByTheCreationContent() {
    assertThat(CreationContent.builder().build().toJson().asObject().names()).isEmpty();
  }
}
