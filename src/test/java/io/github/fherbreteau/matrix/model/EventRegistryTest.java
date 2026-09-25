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
                    + "\"format\":\"org.matrix.custom.html\",\"formatted_body\":\"<b>hello</b>\","
                    + "\"m.mentions\":{\"user_ids\":[\"@b:b\"]},"
                    + "\"m.relates_to\":{\"rel_type\":\"m.thread\",\"event_id\":\"$root\"},"
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
    var message = (MessageEventContent) typed.content();
    assertThat(message.format()).isEqualTo("org.matrix.custom.html");
    assertThat(message.formattedBody()).isEqualTo("<b>hello</b>");
    assertThat(message.mentions().userIds().asArray().get(0).asString()).isEqualTo("@b:b");
    assertThat(message.relatesTo().relationType()).isEqualTo("m.thread");
    assertThat(message.raw().asObject().get("custom").asBoolean()).isTrue();
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
  void messageInfoParsesImageFileAudioVideoAndLocationMetadata() {
    var registry = new EventRegistry();
    var image =
        (MessageEventContent)
            registry.parse(
                event(
                    "m.room.message",
                    "{\"msgtype\":\"m.image\",\"body\":\"image\",\"url\":\"mxc://h/i\","
                        + "\"filename\":\"image.png\",\"info\":{\"h\":100,\"w\":200,"
                        + "\"size\":300,\"mimetype\":\"image/png\",\"is_animated\":true,"
                        + "\"thumbnail_url\":\"mxc://h/t\",\"thumbnail_info\":{"
                        + "\"h\":20,\"w\":40,\"size\":50,\"mimetype\":\"image/jpeg\"}}}"));
    assertThat(image.info().height()).isEqualTo(100L);
    assertThat(image.info().width()).isEqualTo(200L);
    assertThat(image.info().size()).isEqualTo(300L);
    assertThat(image.info().mimeType()).isEqualTo("image/png");
    assertThat(image.info().animated()).isTrue();
    assertThat(image.info().thumbnailUrl()).isEqualTo("mxc://h/t");
    assertThat(image.info().thumbnailInfo().mimeType()).isEqualTo("image/jpeg");

    var file =
        (MessageEventContent)
            registry.parse(
                event(
                    "m.room.message",
                    "{\"msgtype\":\"m.file\",\"body\":\"file\",\"file\":{\"url\":\"mxc://h/f\"},"
                        + "\"info\":{\"mimetype\":\"application/pdf\",\"size\":10}}"));
    assertThat(file.file().isObject()).isTrue();
    assertThat(file.info().mimeType()).isEqualTo("application/pdf");

    var audio =
        (MessageEventContent)
            registry.parse(
                event(
                    "m.room.message",
                    "{\"msgtype\":\"m.audio\",\"body\":\"audio\","
                        + "\"info\":{\"duration\":900,\"mimetype\":\"audio/ogg\",\"size\":99}}"));
    assertThat(audio.info().duration()).isEqualTo(900L);

    var video =
        (MessageEventContent)
            registry.parse(
                event(
                    "m.room.message",
                    "{\"msgtype\":\"m.video\",\"body\":\"video\","
                        + "\"info\":{\"duration\":1000,\"h\":720,\"w\":1280,"
                        + "\"mimetype\":\"video/mp4\",\"size\":500}}"));
    assertThat(video.info())
        .extracting(MessageInfo::height, MessageInfo::width)
        .containsExactly(720L, 1280L);

    var location =
        (MessageEventContent)
            registry.parse(
                event(
                    "m.room.message",
                    "{\"msgtype\":\"m.location\",\"body\":\"place\","
                        + "\"geo_uri\":\"geo:1,2\",\"info\":{\"thumbnail_url\":\"mxc://h/map\"}}"));
    assertThat(location.geoUri()).isEqualTo("geo:1,2");
    assertThat(location.info().thumbnailUrl()).isEqualTo("mxc://h/map");
  }

  @Test
  void malformedKnownFieldsFallBackToUnknownRawContent() {
    var registry = new EventRegistry();
    assertThat(registry.parse(event("m.room.message", "{\"msgtype\":7,\"body\":\"x\"}")))
        .isInstanceOf(UnknownEventContent.class);
    assertThat(registry.parse(event("m.room.member", "{\"membership\":false}")))
        .isInstanceOf(UnknownEventContent.class);
    assertThat(registry.parse(event("m.room.name", "{\"name\":9}")))
        .isInstanceOf(UnknownEventContent.class);
    assertThat(registry.parse(event("m.room.topic", "{\"topic\":\"t\",\"m.topic\":false}")))
        .isInstanceOf(UnknownEventContent.class);
    assertThat(registry.parse(event("m.room.power_levels", "{\"ban\":1.5}")))
        .isInstanceOf(UnknownEventContent.class);
    assertThat(registry.parse(event("m.room.power_levels", "{\"users\":{\"@a:b\":true}}")))
        .isInstanceOf(UnknownEventContent.class);
    assertThat(registry.parse(event("m.room.canonical_alias", "{\"alias\":2}")))
        .isInstanceOf(UnknownEventContent.class);
    assertThat(registry.parse(event("m.room.canonical_alias", "{\"alt_aliases\":[7]}")))
        .isInstanceOf(UnknownEventContent.class);
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
                        + "\"avatar_url\":\"mxc://b/a\",\"is_direct\":true,"
                        + "\"join_authorised_via_users_server\":\"@mod:b\",\"reason\":\"hello\","
                        + "\"third_party_invite\":{\"display_name\":\"Guest\","
                        + "\"signed\":{\"mxid\":\"@guest:b\",\"token\":\"invite-token\","
                        + "\"signatures\":{\"example.org\":{\"ed25519:key\":\"sig\"}}}}}"));
    assertThat(member)
        .extracting(
            MembershipEventContent::membership,
            MembershipEventContent::displayName,
            MembershipEventContent::avatarUrl,
            MembershipEventContent::direct,
            MembershipEventContent::joinAuthorisedViaUsersServer,
            MembershipEventContent::reason)
        .containsExactly("join", "Alice", "mxc://b/a", true, "@mod:b", "hello");
    assertThat(member.thirdPartyInvite().displayName()).isEqualTo("Guest");
    assertThat(member.thirdPartyInvite().matrixUserId()).isEqualTo("@guest:b");
    assertThat(member.thirdPartyInvite().token()).isEqualTo("invite-token");
    assertThat(member.thirdPartyInvite().signatures().asObject().has("example.org")).isTrue();

    var name = (RoomNameEventContent) registry.parse(event("m.room.name", "{\"name\":\"Room\"}"));
    assertThat(name.name()).isEqualTo("Room");
    var topic =
        (RoomTopicEventContent)
            registry.parse(
                event(
                    "m.room.topic",
                    "{\"topic\":\"Topic\",\"m.topic\":{\"m.text\":[{\"body\":\"Topic\",\"mimetype\":\"text/plain\"}]}}"));
    assertThat(topic.topic()).isEqualTo("Topic");
    assertThat(topic.topicTranslations().text().asArray().get(0).asObject().get("body").asString())
        .isEqualTo("Topic");
    var levels =
        (PowerLevelsEventContent)
            registry.parse(
                event(
                    "m.room.power_levels",
                    "{\"ban\":55,\"events\":{\"m.room.name\":60},"
                        + "\"events_default\":7,\"invite\":8,\"kick\":9,"
                        + "\"notifications\":{\"room\":50},\"redact\":10,"
                        + "\"state_default\":11,\"users\":{\"@a:b\":50},"
                        + "\"users_default\":12}"));
    assertThat(levels)
        .extracting(
            PowerLevelsEventContent::ban,
            PowerLevelsEventContent::eventsDefault,
            PowerLevelsEventContent::invite,
            PowerLevelsEventContent::kick,
            PowerLevelsEventContent::redact,
            PowerLevelsEventContent::stateDefault,
            PowerLevelsEventContent::usersDefault)
        .containsExactly(55L, 7L, 8L, 9L, 10L, 11L, 12L);
    assertThat(levels.events().asObject().get("m.room.name").asLong()).isEqualTo(60);
    assertThat(levels.notifications().asObject().get("room").asLong()).isEqualTo(50);
    assertThat(levels.users().asObject().get("@a:b").asLong()).isEqualTo(50);
    assertThat(
            ((CanonicalAliasEventContent)
                    registry.parse(
                        event(
                            "m.room.canonical_alias",
                            "{\"alias\":null,\"alt_aliases\":[\"#alt:b\"]}")))
                .alias())
        .isNull();
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
