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
  void typedEventFactoriesReturnNullForAbsentOrNonObjectContent() {
    assertThat(MessageEventContent.from(null)).isNull();
    assertThat(MessageEventContent.from(JsonParser.parse("[]"))).isNull();
    assertThat(MembershipEventContent.from(null)).isNull();
    assertThat(MembershipEventContent.from(JsonParser.parse("[]"))).isNull();
    assertThat(RoomNameEventContent.from(null)).isNull();
    assertThat(RoomNameEventContent.from(JsonParser.parse("[]"))).isNull();
    assertThat(RoomTopicEventContent.from(null)).isNull();
    assertThat(RoomTopicEventContent.from(JsonParser.parse("[]"))).isNull();
    assertThat(PowerLevelsEventContent.from(null)).isNull();
    assertThat(PowerLevelsEventContent.from(JsonParser.parse("[]"))).isNull();
    assertThat(CanonicalAliasEventContent.from(null)).isNull();
    assertThat(CanonicalAliasEventContent.from(JsonParser.parse("[]"))).isNull();
    assertThat(TopicTranslations.from(null)).isNull();
    assertThat(TopicTranslations.from(JsonParser.parse("[]"))).isNull();
    assertThat(ThumbnailInfo.from(null)).isNull();
    assertThat(ThumbnailInfo.from(JsonParser.parse("[]"))).isNull();
    assertThat(MessageInfo.from(null)).isNull();
    assertThat(MessageInfo.from(JsonParser.parse("[]"))).isNull();
    assertThat(MessageMentions.from(null)).isNull();
    assertThat(MessageMentions.from(JsonParser.parse("[]"))).isNull();
    assertThat(EventRelation.from(null)).isNull();
    assertThat(EventRelation.from(JsonParser.parse("[]"))).isNull();
    assertThat(ThirdPartyInvite.from(null)).isNull();
    assertThat(ThirdPartyInvite.from(JsonParser.parse("[]"))).isNull();
  }

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
    assertThat(message.mentions().userIds().getFirst()).isEqualTo("@b:b");
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
                    "{\"msgtype\":\"m.file\",\"body\":\"file\",\"url\":\"mxc://h/f\","
                        + "\"info\":{\"mimetype\":\"application/pdf\",\"size\":10}}"));
    assertThat(file.url()).isEqualTo("mxc://h/f");
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
    assertThat(registry.parse(event("m.room.power_levels", "{\"events\":{\"m.room.name\":1.5}}")))
        .isInstanceOf(UnknownEventContent.class);
    assertThat(registry.parse(event("m.room.power_levels", "{\"notifications\":{\"room\":2.5}}")))
        .isInstanceOf(UnknownEventContent.class);
    assertThat(registry.parse(event("m.room.power_levels", "{\"ban\":9007199254740992}")))
        .isInstanceOf(UnknownEventContent.class);
    assertThat(
            registry.parse(
                event(
                    "m.room.message",
                    "{\"msgtype\":\"m.image\",\"body\":\"i\",\"info\":{\"w\":1.5}}")))
        .isInstanceOf(UnknownEventContent.class);
    assertThat(
            registry.parse(
                event(
                    "m.room.message",
                    "{\"msgtype\":\"m.image\",\"body\":\"i\",\"info\":{\"h\":1.5}}")))
        .isInstanceOf(UnknownEventContent.class);
    assertThat(
            registry.parse(
                event(
                    "m.room.message",
                    "{\"msgtype\":\"m.audio\",\"body\":\"a\",\"info\":{\"duration\":1.5}}")))
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
    assertThat(topic.topicTranslations().text().getFirst().body()).isEqualTo("Topic");
    var levels =
        (PowerLevelsEventContent)
            registry.parse(
                event(
                    "m.room.power_levels",
                    "{\"ban\":55,\"events\":{\"m.room.name\":60},"
                        + "\"events_default\":7,\"invite\":8,\"kick\":9,"
                        + "\"notifications\":{\"room\":50,\"custom_notification\":70},\"redact\":10,"
                        + "\"state_default\":11,\"users\":{\"@a:b\":50},\"users_default\":12}"));
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
    assertThat(levels.events()).containsEntry("m.room.name", 60L);
    assertThat(levels.notifications())
        .containsEntry("room", 50L)
        .containsEntry("custom_notification", 70L);
    assertThat(levels.users()).containsEntry("@a:b", 50L);
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
  void optionalSchemaFieldsMayBeAbsentOrNullWhenAllowed() {
    var registry = new EventRegistry();
    var topicWithoutTranslations =
        (RoomTopicEventContent) registry.parse(event("m.room.topic", "{\"topic\":\"plain\"}"));
    assertThat(topicWithoutTranslations.topicTranslations()).isNull();

    var aliasWithoutPrimaryAlias =
        (CanonicalAliasEventContent)
            registry.parse(event("m.room.canonical_alias", "{\"alias\":null}"));
    assertThat(aliasWithoutPrimaryAlias.alias()).isNull();

    var nullableDisplayName =
        (MembershipEventContent)
            registry.parse(
                event("m.room.member", "{\"membership\":\"join\",\"displayname\":null}"));
    assertThat(nullableDisplayName.displayName()).isNull();
  }

  @Test
  void schemaFieldValidatorsCoverExpectedAndMalformedJsonTypes() {
    var object =
        JsonParser.parse("{\"value\":\"ok\",\"flag\":true,\"count\":2,\"nested\":{},\"list\":[]}")
            .asObject();
    assertThat(EventFields.hasWrongType(object, "value", EventFields.STRING_TYPE)).isFalse();
    assertThat(EventFields.hasWrongType(object, "flag", EventFields.BOOLEAN_TYPE)).isFalse();
    assertThat(EventFields.hasWrongType(object, "count", EventFields.INTEGER_TYPE)).isFalse();
    assertThat(EventFields.hasWrongType(object, "nested", EventFields.OBJECT_TYPE)).isFalse();
    assertThat(EventFields.hasWrongType(object, "list", EventFields.ARRAY_TYPE)).isFalse();
    assertThat(EventFields.hasWrongType(object, "missing", EventFields.STRING_TYPE)).isFalse();
    assertThat(EventFields.hasWrongType(object, "value", EventFields.OBJECT_TYPE)).isTrue();
    assertThat(EventFields.hasWrongType(object, "nested", EventFields.STRING_TYPE)).isTrue();
    assertThat(EventFields.hasWrongType(JsonParser.parse("1.5"), EventFields.INTEGER_TYPE))
        .isTrue();
    assertThat(EventFields.hasWrongType(JsonParser.parse("null"), EventFields.STRING_TYPE))
        .isTrue();
    assertThat(EventFields.hasWrongType(JsonParser.parse("true"), "unknown-type")).isTrue();
    assertThat(
            EventFields.hasWrongType(
                JsonParser.parse("{\"value\":1.5}").asObject(), "value", EventFields.INTEGER_TYPE))
        .isTrue();
    assertThat(
            EventFields.hasWrongType(
                JsonParser.parse("{\"value\":null}").asObject(), "value", EventFields.STRING_TYPE))
        .isFalse();

    assertThat(
            EventFields.hasWrongMessageFields(
                JsonParser.parse("{\"msgtype\":\"m.text\",\"body\":\"x\"}")))
        .isFalse();
    assertThat(EventFields.hasWrongMessageFields(JsonParser.parse("{\"msgtype\":\"m.text\"}")))
        .isTrue();
    assertThat(
            EventFields.hasWrongMessageFields(
                JsonParser.parse(
                    "{\"msgtype\":\"m.text\",\"body\":\"x\",\"m.mentions\":{\"room\":\"yes\"}}")))
        .isTrue();
    assertThat(
            EventFields.hasWrongMessageFields(
                JsonParser.parse(
                    "{\"msgtype\":\"m.text\",\"body\":\"x\",\"m.mentions\":{\"user_ids\":[2]}}")))
        .isTrue();

    assertThat(EventFields.hasWrongTextRepresentations(JsonParser.parse("{}"))).isFalse();
    assertThat(EventFields.hasWrongTextRepresentations(JsonParser.parse("[]"))).isTrue();
    assertThat(EventFields.hasWrongTextRepresentations(JsonParser.parse("{\"m.text\":false}")))
        .isTrue();
    assertThat(
            EventFields.hasWrongTextRepresentations(
                JsonParser.parse("{\"m.text\":[{\"body\":\"x\"}]}")))
        .isFalse();
    assertThat(EventFields.hasWrongTextRepresentations(JsonParser.parse("{\"m.text\":[{}]}")))
        .isTrue();

    assertThat(EventFields.hasWrongStringArray(JsonParser.parse("{\"a\":null}").asObject(), "a"))
        .isFalse();
    assertThat(EventFields.hasWrongStringArray(JsonParser.parse("{\"a\":[]}").asObject(), "a"))
        .isFalse();
    assertThat(EventFields.hasWrongStringArray(JsonParser.parse("{\"a\":false}").asObject(), "a"))
        .isTrue();
    assertThat(EventFields.hasWrongStringArray(JsonParser.parse("{\"a\":[1]}").asObject(), "a"))
        .isTrue();

    assertThat(EventFields.hasWrongIntegerMap(JsonParser.parse("{\"a\":null}").asObject(), "a"))
        .isFalse();
    assertThat(
            EventFields.hasWrongIntegerMap(JsonParser.parse("{\"a\":{\"x\":1}}").asObject(), "a"))
        .isFalse();
    assertThat(EventFields.hasWrongIntegerMap(JsonParser.parse("{\"a\":false}").asObject(), "a"))
        .isTrue();
    assertThat(
            EventFields.hasWrongIntegerMap(
                JsonParser.parse("{\"a\":{\"x\":true}}").asObject(), "a"))
        .isTrue();
    assertThat(EventFields.hasWrongIntegerObject(null)).isFalse();
    assertThat(EventFields.hasWrongIntegerObject(JsonParser.parse("null"))).isFalse();
    assertThat(EventFields.hasWrongIntegerObject(JsonParser.parse("{\"room\":50}"))).isFalse();
    assertThat(EventFields.integerMap(null)).isEmpty();
    assertThat(EventFields.integerMap(JsonParser.parse("null"))).isEmpty();
    assertThat(EventFields.integerMap(JsonParser.parse("{\"x\":4}")).get("x")).isEqualTo(4L);
    assertThat(EventFields.integerMap(JsonParser.parse("{\"x\":false}"))).isNull();
    assertThat(
            EventFields.hasWrongIntegerMap(
                JsonParser.parse("{\"values\":null}").asObject(), "values"))
        .isFalse();
    assertThat(EventFields.hasWrongIntegerMap(JsonParser.parse("{}").asObject(), "missing"))
        .isFalse();
    assertThat(EventFields.hasWrongIntegerObject(JsonParser.parse("[]"))).isTrue();
    assertThat(EventFields.hasWrongIntegerObject(JsonParser.parse("{\"nullable\":null}"))).isTrue();
    assertThat(EventFields.hasWrongIntegerObject(JsonParser.parse("{\"custom\":false}"))).isTrue();
    assertThat(
            EventFields.hasWrongIntegerMap(
                JsonParser.parse("{\"a\":{\"x\":true}}").asObject(), "a"))
        .isTrue();
    assertThat(EventFields.longField(JsonParser.parse("{\"x\":5}").asObject(), "x")).isEqualTo(5L);
    assertThat(EventFields.longField(JsonParser.parse("{\"x\":1.5}").asObject(), "x")).isNull();
    assertThat(EventFields.longValue(JsonParser.parse("{\"x\":1e100}").asObject(), "x")).isNull();
  }

  @Test
  void encryptedFileAndJwkSchemasValidateKnownFieldTypes() {
    var registry = new EventRegistry();
    String encryptedFile =
        "{\"hashes\":{\"sha256\":\"hash\"},\"iv\":\"iv\","
            + "\"key\":{\"alg\":\"A256CTR\",\"ext\":true,\"k\":\"key\","
            + "\"key_ops\":[\"encrypt\",\"decrypt\"],\"kty\":\"oct\"},"
            + "\"url\":\"mxc://h/file\",\"v\":\"v2\"}";
    assertThat(
            registry.parse(
                event(
                    "m.room.message",
                    "{\"msgtype\":\"m.image\",\"body\":\"image\",\"file\":" + encryptedFile + "}")))
        .isInstanceOf(MessageEventContent.class);
    assertThat(
            registry.parse(
                event(
                    "m.room.message",
                    "{\"msgtype\":\"m.image\",\"body\":\"image\",\"file\":{\"key\":{\"alg\":4}}}")))
        .isInstanceOf(UnknownEventContent.class);
    assertThat(
            registry.parse(
                event(
                    "m.room.message",
                    "{\"msgtype\":\"m.image\",\"body\":\"image\","
                        + "\"info\":{\"thumbnail_file\":{\"key\":{\"kty\":false}}}}")))
        .isInstanceOf(UnknownEventContent.class);
  }

  @Test
  void infoOptionalIntegerAndThumbnailEncryptionBranchesAreCovered() {
    var registry = new EventRegistry();
    var image =
        (MessageEventContent)
            registry.parse(
                event(
                    "m.room.message",
                    "{\"msgtype\":\"m.image\",\"body\":\"i\",\"info\":{\"is_animated\":true}}"));
    assertThat(image.info().animated()).isTrue();
    var video =
        (MessageEventContent)
            registry.parse(
                event(
                    "m.room.message",
                    "{\"msgtype\":\"m.video\",\"body\":\"v\",\"info\":{\"height\":720}}"));
    assertThat(video.info()).isNotNull();
  }

  @Test
  void nestedMessageInfoAndEncryptedFileValidationCoversMalformedStructures() {
    var registry = new EventRegistry();
    assertThat(
            registry.parse(
                event("m.room.message", "{\"msgtype\":\"m.image\",\"body\":\"i\",\"file\":false}")))
        .isInstanceOf(UnknownEventContent.class);
    assertThat(
            registry.parse(
                event("m.room.message", "{\"msgtype\":\"m.image\",\"body\":\"i\",\"info\":false}")))
        .isInstanceOf(UnknownEventContent.class);
    assertThat(
            registry.parse(
                event(
                    "m.room.message",
                    "{\"msgtype\":\"m.image\",\"body\":\"i\",\"info\":{\"thumbnail_file\":false}}")))
        .isInstanceOf(UnknownEventContent.class);
    assertThat(
            registry.parse(
                event(
                    "m.room.message",
                    "{\"msgtype\":\"m.image\",\"body\":\"i\",\"info\":{\"thumbnail_file\":{\"key\":false}}}")))
        .isInstanceOf(UnknownEventContent.class);
    assertThat(
            registry.parse(
                event(
                    "m.room.message",
                    "{\"msgtype\":\"m.image\",\"body\":\"i\",\"file\":{\"key\":false}}")))
        .isInstanceOf(UnknownEventContent.class);
    assertThat(
            registry.parse(
                event(
                    "m.room.message",
                    "{\"msgtype\":\"m.image\",\"body\":\"i\",\"file\":{\"hashes\":{},\"iv\":\"iv\",\"url\":\"mxc://b/id\",\"v\":\"v2\",\"key\":{\"alg\":\"A256CTR\",\"ext\":true,\"k\":\"key\",\"key_ops\":[\"encrypt\",\"decrypt\"],\"kty\":\"oct\"}}}")))
        .isInstanceOf(MessageEventContent.class);
    assertThat(
            registry.parse(
                event(
                    "m.room.message",
                    "{\"msgtype\":\"m.image\",\"body\":\"i\",\"file\":{\"hashes\":{},\"iv\":\"iv\",\"url\":\"mxc://b/id\",\"v\":\"v2\",\"key\":{\"alg\":3}}}")))
        .isInstanceOf(UnknownEventContent.class);
  }

  @Test
  void roomTopicAndAliasMissingRequiredContentFallBackToRaw() {
    var registry = new EventRegistry();
    assertThat(registry.parse(event("m.room.topic", "{}"))).isInstanceOf(UnknownEventContent.class);
    assertThat(registry.parse(event("m.room.canonical_alias", "{}")))
        .isInstanceOf(CanonicalAliasEventContent.class);
  }

  @Test
  void integerFieldHelperRejectsNonNumberField() {
    assertThat(
            EventFields.hasWrongIntegerField(
                JsonParser.parse("{\"value\":\"number\"}").asObject(), "value"))
        .isTrue();
  }

  @Test
  void messageInfoAndThumbnailDimensionsRejectInvalidIntegerSchemaValues() {
    var registry = new EventRegistry();
    assertThat(
            registry.parse(
                event(
                    "m.room.message",
                    "{\"msgtype\":\"m.image\",\"body\":\"i\",\"info\":{\"w\":1.5}}")))
        .isInstanceOf(UnknownEventContent.class);
    assertThat(
            registry.parse(
                event(
                    "m.room.message",
                    "{\"msgtype\":\"m.image\",\"body\":\"i\",\"info\":{\"h\":1.5}}")))
        .isInstanceOf(UnknownEventContent.class);
    assertThat(
            registry.parse(
                event(
                    "m.room.message",
                    "{\"msgtype\":\"m.video\",\"body\":\"v\",\"info\":{\"duration\":1.5}}")))
        .isInstanceOf(UnknownEventContent.class);
    assertThat(
            registry.parse(
                event(
                    "m.room.message",
                    "{\"msgtype\":\"m.image\",\"body\":\"i\",\"info\":{\"thumbnail_info\":{\"size\":1.5}}}")))
        .isInstanceOf(UnknownEventContent.class);
  }

  @Test
  void eventFieldHelpersCoverNullableAndIncorrectScalarFieldTypes() {
    var nullFields = JsonParser.parse("{\"value\":null,\"flag\":false}").asObject();
    assertThat(EventFields.hasWrongType(nullFields, "value", EventFields.STRING_TYPE)).isFalse();
    assertThat(EventFields.hasWrongType(nullFields, "flag", EventFields.BOOLEAN_TYPE)).isFalse();
    assertThat(EventFields.hasWrongType(JsonParser.parse("null"), EventFields.OBJECT_TYPE))
        .isTrue();
    assertThat(EventFields.hasWrongType(JsonParser.parse("[]"), EventFields.STRING_TYPE)).isTrue();
    assertThat(EventFields.hasWrongType(JsonParser.parse("{}"), EventFields.BOOLEAN_TYPE)).isTrue();
    assertThat(EventFields.hasWrongType(JsonParser.parse("1.25"), EventFields.INTEGER_TYPE))
        .isTrue();
    assertThat(EventFields.hasWrongType(JsonParser.parse("false"), EventFields.ARRAY_TYPE))
        .isTrue();
    assertThat(EventFields.string(JsonParser.parse("{}"), "absent")).isNull();
    assertThat(EventFields.booleanValue(JsonParser.parse("{}"), "absent")).isNull();
    assertThat(EventFields.field(null, "x")).isNull();
    assertThat(EventFields.field(JsonParser.parse("[]"), "x")).isNull();
    assertThat(EventFields.string(JsonParser.parse("{\"x\":5}"), "x")).isNull();
    assertThat(EventFields.booleanValue(JsonParser.parse("{\"x\":0}"), "x")).isNull();
    assertThat(EventFields.longField(JsonParser.parse("{\"x\":\"one\"}"), "x")).isNull();
    assertThat(EventFields.longField(JsonParser.parse("{\"x\":1.25}"), "x")).isNull();
    assertThat(EventFields.longValue(JsonParser.parse("{\"x\":\"one\"}").asObject(), "x")).isNull();
    assertThat(EventFields.hasWrongType(JsonParser.parse("true"), "not-a-type")).isTrue();
    assertThat(
            EventFields.hasWrongType(
                JsonParser.parse("{\"value\":1.5}").asObject(), "value", EventFields.INTEGER_TYPE))
        .isTrue();
  }

  @Test
  void requiredTypedFieldsRejectMissingValuesAndMalformedNumericRepresentations() {
    var registry = new EventRegistry();
    assertThat(registry.parse(event("m.room.name", "{}"))).isInstanceOf(UnknownEventContent.class);
    assertThat(registry.parse(event("m.room.topic", "{\"topic\":2}")))
        .isInstanceOf(UnknownEventContent.class);
    assertThat(registry.parse(event("m.room.member", "{\"membership\":null}")))
        .isInstanceOf(UnknownEventContent.class);
    assertThat(
            registry.parse(
                event("m.room.member", "{\"membership\":\"join\",\"is_direct\":\"yes\"}")))
        .isInstanceOf(UnknownEventContent.class);
    assertThat(
            registry.parse(event("m.room.member", "{\"membership\":\"join\",\"displayname\":17}")))
        .isInstanceOf(UnknownEventContent.class);
    assertThat(registry.parse(event("m.room.canonical_alias", "{\"alt_aliases\":7}")))
        .isInstanceOf(UnknownEventContent.class);

    var malformedLongContent =
        MessageEventContent.from(JsonParser.parse("{\"msgtype\":\"m.text\",\"body\":\"x\"}"));
    assertThat(EventFields.hasWrongType(JsonParser.parse("{}"), EventFields.INTEGER_TYPE)).isTrue();
    assertThat(EventFields.longField(JsonParser.parse("{\"value\":1.5}"), "value")).isNull();
    assertThat(EventFields.longValue(JsonParser.parse("{\"value\":1e100}").asObject(), "value"))
        .isNull();
    assertThat(malformedLongContent).isNotNull();
  }

  @Test
  void nullableOptionalFieldsAndMissingOptionalMediaInfoAreAccepted() {
    var registry = new EventRegistry();
    var message =
        registry.parse(
            event("m.room.message", "{\"msgtype\":\"m.text\",\"body\":\"hi\",\"format\":null}"));
    assertThat(message).isInstanceOf(MessageEventContent.class);
    var member =
        registry.parse(
            event("m.room.member", "{\"membership\":\"join\",\"third_party_invite\":null}"));
    assertThat(member).isInstanceOf(MembershipEventContent.class);
    assertThat(((MembershipEventContent) member).thirdPartyInvite()).isNull();
    var image =
        registry.parse(
            event("m.room.message", "{\"msgtype\":\"m.image\",\"body\":\"i\",\"info\":{}}"));
    assertThat(image).isInstanceOf(MessageEventContent.class);
    assertThat(((MessageEventContent) image).info()).isNotNull();
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
