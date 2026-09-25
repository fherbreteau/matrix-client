package io.github.fherbreteau.matrix.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.fherbreteau.matrix.json.JsonParser;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class EventRegistryTest {

  @Test
  void commonMessageContentIsTypedAndRetainsUnknownContentFieldsAndEnvelope() {
    var event =
        RoomEvent.from(
            JsonParser.parse(
                "{\"event_id\":\"$e\",\"sender\":\"@a:b\",\"origin_server_ts\":10,"
                    + "\"room_id\":\"!r:b\",\"type\":\"m.room.message\","
                    + "\"content\":{\"msgtype\":\"m.text\",\"body\":\"hello\","
                    + "\"custom\":true},\"unsigned\":{\"age\":1},\"future\":42}"));

    RegisteredRoomEvent typed = event.withTypedContent(new EventRegistry());

    assertThat(event.raw().asObject().get("future").asLong()).isEqualTo(42);
    assertThat(typed.envelope()).isSameAs(event);
    assertThat(typed.content())
        .isInstanceOf(MessageEventContent.class)
        .asInstanceOf(
            org.assertj.core.api.InstanceOfAssertFactories.type(MessageEventContent.class))
        .extracting(MessageEventContent::msgtype, MessageEventContent::body)
        .containsExactly("m.text", "hello");
    assertThat(typed.content().raw().asObject().get("custom").asBoolean()).isTrue();
  }

  @Test
  void parsesCompleteEventEnvelopeAndRejectsNonObject() {
    var registry = new EventRegistry();
    var parsed =
        registry.parseEvent(
            JsonParser.parse("{\"type\":\"m.room.name\",\"content\":{\"name\":\"R\"}}"));
    assertThat(parsed.envelope().type()).isEqualTo("m.room.name");
    assertThat(parsed.content()).isInstanceOf(RoomNameEventContent.class);
    assertThat(parsed.envelope().raw().isObject()).isTrue();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> registry.parseEvent(JsonParser.parse("[]")));
  }

  @Test
  void commonStateEventContentTypesAreRegistered() {
    var registry = new EventRegistry();
    assertThat(registry.parse(event("m.room.member", "{\"membership\":\"join\",\"x\":1}")))
        .isInstanceOf(MembershipEventContent.class);
    assertThat(registry.parse(event("m.room.name", "{\"name\":\"Room\"}")))
        .isInstanceOf(RoomNameEventContent.class);
    assertThat(registry.parse(event("m.room.topic", "{\"topic\":\"Topic\"}")))
        .isInstanceOf(RoomTopicEventContent.class);
    assertThat(registry.parse(event("m.room.power_levels", "{\"users\":{},\"events\":{}}")))
        .isInstanceOf(PowerLevelsEventContent.class);
    assertThat(
            registry.parse(
                event("m.room.canonical_alias", "{\"alias\":\"#room:b\",\"alt_aliases\":[]}")))
        .isInstanceOf(CanonicalAliasEventContent.class);
  }

  @Test
  void typedStateContentValuesAreAccessible() {
    var registry = new EventRegistry();
    var member =
        (MembershipEventContent)
            registry.parse(
                event(
                    "m.room.member",
                    "{\"membership\":\"join\",\"displayname\":\"Alice\","
                        + "\"avatar_url\":\"mxc://b/a\",\"reason\":\"hello\"}"));
    assertThat(member)
        .extracting(
            MembershipEventContent::membership,
            MembershipEventContent::displayName,
            MembershipEventContent::avatarUrl,
            MembershipEventContent::reason)
        .containsExactly("join", "Alice", "mxc://b/a", "hello");

    var name = (RoomNameEventContent) registry.parse(event("m.room.name", "{\"name\":\"Room\"}"));
    assertThat(name.name()).isEqualTo("Room");
    var topic =
        (RoomTopicEventContent) registry.parse(event("m.room.topic", "{\"topic\":\"Topic\"}"));
    assertThat(topic.topic()).isEqualTo("Topic");
    var levels =
        (PowerLevelsEventContent)
            registry.parse(event("m.room.power_levels", "{\"users\":{\"@a:b\":50}}"));
    assertThat(levels.users().asObject().get("@a:b").asLong()).isEqualTo(50);
    var alias =
        (CanonicalAliasEventContent)
            registry.parse(
                event("m.room.canonical_alias", "{\"alias\":\"#room:b\",\"alt_aliases\":[]}"));
    assertThat(alias.alias()).isEqualTo("#room:b");
    assertThat(alias.altAliases().asArray().size()).isZero();
  }

  @Test
  void unknownAndMalformedEventsFallBackToRawJson() {
    var registry = new EventRegistry();
    var unknown = event("org.example.future", "{\"x\":1}");
    var unknownContent = registry.parse(unknown);
    assertThat(unknownContent)
        .isInstanceOf(UnknownEventContent.class)
        .extracting(content -> content.raw().asObject().get("x").asLong())
        .isEqualTo(1L);

    registry.register(
        "org.example.broken",
        content -> {
          throw new IllegalArgumentException();
        });
    var malformed = event("org.example.broken", "{\"kept\":true}");
    assertThat(registry.parse(malformed))
        .isInstanceOf(UnknownEventContent.class)
        .extracting(content -> content.raw().asObject().get("kept").asBoolean())
        .isEqualTo(true);

    registry.register("m.room.message", content -> null);
    var nullResult = registry.parse(event("m.room.message", "{\"body\":\"raw\"}"));
    assertThat(nullResult).isInstanceOf(UnknownEventContent.class);
  }

  @Test
  void customParsersCanRegisterReplaceAndUnregisterDeterministically() {
    var registry = new EventRegistry();
    registry.register("org.example.number", content -> content.asObject().get("n").asLong());
    var first = registry.parse(event("org.example.number", "{\"n\":5}"));
    assertThat(first)
        .isInstanceOf(RegisteredEventContent.class)
        .asInstanceOf(
            org.assertj.core.api.InstanceOfAssertFactories.type(RegisteredEventContent.class))
        .extracting(RegisteredEventContent::value)
        .isEqualTo(5L);

    registry.register("org.example.number", content -> "replacement");
    assertThat(((RegisteredEventContent) registry.parse(event("org.example.number", "{}"))).value())
        .isEqualTo("replacement");
    registry.unregister("org.example.number");
    assertThat(registry.parse(event("org.example.number", "{\"n\":5}")))
        .isInstanceOf(UnknownEventContent.class);
  }

  @Test
  void customParserMayReturnEventContentDirectly() {
    var registry = new EventRegistry();
    registry.register("org.example.topic", RoomTopicEventContent::from);
    var parsed = registry.parse(event("org.example.topic", "{\"topic\":\"custom\"}"));
    assertThat(parsed).isInstanceOf(RoomTopicEventContent.class);
    assertThat(((RoomTopicEventContent) parsed).topic()).isEqualTo("custom");
  }

  @Test
  void invalidParserRegistrationsAreRejected() {
    var registry = new EventRegistry();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> registry.register(" ", content -> content));
    assertThatIllegalArgumentException().isThrownBy(() -> registry.register("x", null));
  }

  @Test
  void registrySupportsConcurrentCustomRegistrationAndParsing() throws Exception {
    var registry = new EventRegistry();
    var start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(8)) {
      var tasks = new ArrayList<java.util.concurrent.Future<?>>();
      for (int i = 0; i < 16; i++) {
        int index = i;
        tasks.add(
            executor.submit(
                () -> {
                  start.await();
                  String type = "org.example." + index;
                  registry.register(type, content -> content);
                  assertThat(registry.parse(event(type, "{\"index\":" + index + "}")))
                      .isInstanceOf(RegisteredEventContent.class);
                  return null;
                }));
      }
      start.countDown();
      for (var task : tasks) {
        task.get(5, TimeUnit.SECONDS);
      }
    }
  }

  @Test
  void parserPreservesMissingAndMalformedContentWithoutFailing() {
    var registry = new EventRegistry();
    var noContent =
        registry.parse(RoomEvent.from(JsonParser.parse("{\"type\":\"m.room.message\"}")));
    assertThat(noContent).isInstanceOf(UnknownEventContent.class);
    var wrongContent = registry.parse(event("m.room.name", "[]"));
    assertThat(wrongContent).isInstanceOf(UnknownEventContent.class);
    assertThat(wrongContent.raw().isArray()).isTrue();
  }

  private static RoomEvent event(String type, String contentJson) {
    JsonValue content = JsonParser.parse(contentJson);
    return RoomEvent.from(
        JsonParser.parse("{\"type\":\"" + type + "\",\"content\":" + content.toJson() + "}"));
  }
}
