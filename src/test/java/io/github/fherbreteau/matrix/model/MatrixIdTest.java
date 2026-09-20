package io.github.fherbreteau.matrix.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class MatrixIdTest {

  @Test
  void validIdentifiersAreAccepted() {
    assertThat(RoomId.of("!localpart:matrix.org").value()).isEqualTo("!localpart:matrix.org");
    assertThat(RoomAlias.of("#alias:matrix.org").value()).isEqualTo("#alias:matrix.org");
    assertThat(UserId.of("@alice:matrix.org").value()).isEqualTo("@alice:matrix.org");
    assertThat(EventId.of("$opaque-id").value()).isEqualTo("$opaque-id");
    assertThat(DeviceId.of("DEV123").value()).isEqualTo("DEV123");
    assertThat(DeviceId.of("a.b_c/d=e+f").value()).isEqualTo("a.b_c/d=e+f");
  }

  @Test
  void identifiersExposeTheirValueThroughToString() {
    assertThat(RoomId.of("!a:b")).hasToString("!a:b");
    assertThat(RoomAlias.of("#a:b")).hasToString("#a:b");
    assertThat(UserId.of("@a:b")).hasToString("@a:b");
    assertThat(EventId.of("$e1")).hasToString("$e1");
    assertThat(DeviceId.of("DEV123")).hasToString("DEV123");
  }

  @Test
  void rejectsMissingValues() {
    assertThatIllegalArgumentException().isThrownBy(() -> RoomId.of(null));
    assertThatIllegalArgumentException().isThrownBy(() -> RoomId.of(" "));
    assertThatIllegalArgumentException().isThrownBy(() -> RoomAlias.of(null));
    assertThatIllegalArgumentException().isThrownBy(() -> UserId.of(null));
    assertThatIllegalArgumentException().isThrownBy(() -> EventId.of(null));
    assertThatIllegalArgumentException().isThrownBy(() -> DeviceId.of(null));
    assertThatIllegalArgumentException().isThrownBy(() -> DeviceId.of(" "));
  }

  @Test
  void rejectsWrongPrefixes() {
    assertThatIllegalArgumentException().isThrownBy(() -> RoomId.of("#a:b"));
    assertThatIllegalArgumentException().isThrownBy(() -> RoomAlias.of("!a:b"));
    assertThatIllegalArgumentException().isThrownBy(() -> UserId.of("!a:b"));
    assertThatIllegalArgumentException().isThrownBy(() -> EventId.of("!a:b"));
  }

  @Test
  void rejectsMissingSeparator() {
    assertThatIllegalArgumentException().isThrownBy(() -> RoomId.of("!noseparator"));
    assertThatIllegalArgumentException().isThrownBy(() -> RoomId.of("!:server"));
    assertThatIllegalArgumentException().isThrownBy(() -> RoomId.of("!localpart:"));
    assertThatIllegalArgumentException().isThrownBy(() -> RoomAlias.of("#noseparator"));
    assertThatIllegalArgumentException().isThrownBy(() -> UserId.of("@:server"));
    assertThatIllegalArgumentException().isThrownBy(() -> UserId.of("@localpart:"));
  }

  @Test
  void rejectsShortLocalparts() {
    assertThatIllegalArgumentException().isThrownBy(() -> EventId.of("$"));
    assertThatIllegalArgumentException().isThrownBy(() -> EventId.of("!a:b"));
  }

  @Test
  void rejectsIllegalDeviceIdentifiers() {
    assertThatIllegalArgumentException().isThrownBy(() -> DeviceId.of("with:colon"));
    assertThatIllegalArgumentException().isThrownBy(() -> DeviceId.of("with space"));
    assertThatIllegalArgumentException().isThrownBy(() -> DeviceId.of("with\ttab"));
  }
}
