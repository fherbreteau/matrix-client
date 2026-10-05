package io.github.fherbreteau.matrix.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.fherbreteau.matrix.json.JsonParser;
import org.junit.jupiter.api.Test;

class ThirdPartyLookupModelsTest {

  @Test
  void parsesProtocolMetadataAndRetainsExtensions() {
    var parsed = JsonParser.parse(protocolMapJson());
    assertThat(parsed.asObject().get("irc").isObject()).isTrue();
    var protocols = ThirdPartyProtocols.from(parsed);
    ThirdPartyProtocol protocol = protocols.protocols().get("irc");

    assertThat(protocol.fieldTypes()).containsOnlyKeys("channel");
    assertThat(protocol.fieldTypes().get("channel").placeholder()).isEqualTo("#matrix");
    assertThat(protocol.fieldTypes().get("channel").regexp()).isEqualTo("#[^ ]+");
    assertThat(protocol.instances())
        .singleElement()
        .satisfies(
            instance -> {
              assertThat(instance.description()).isEqualTo("IRC network");
              assertThat(instance.networkId()).isEqualTo("freenode");
              assertThat(instance.instanceId()).isEqualTo("irc-freenode");
              assertThat(instance.icon()).isNull();
              assertThat(instance.raw().asObject().get("future_instance").asBoolean()).isTrue();
            });
    assertThat(protocol.locationFields()).containsExactly("channel");
    assertThat(protocol.userFields()).containsExactly("nickname");
    assertThat(protocol.raw().asObject().get("future_protocol").asBoolean()).isTrue();
    assertThat(protocols.raw().isObject()).isTrue();
  }

  @Test
  void parsesThirdPartyLocationAndUserWithUnknownFields() {
    var locations =
        ThirdPartyLocations.from(
            JsonParser.parse(
                "[{\"alias\":\"#matrix:example.org\",\"fields\":{\"channel\":\"#matrix\"},"
                    + "\"protocol\":\"irc\",\"future\":true}]"));
    assertThat(locations.locations())
        .singleElement()
        .satisfies(
            location -> {
              assertThat(location.alias()).isEqualTo(RoomAlias.of("#matrix:example.org"));
              assertThat(location.fields().asObject().get("channel").asString())
                  .isEqualTo("#matrix");
              assertThat(location.raw().asObject().get("future").asBoolean()).isTrue();
            });

    var users =
        ThirdPartyUsers.from(
            JsonParser.parse(
                "[{\"fields\":{\"nickname\":\"bob\"},\"protocol\":\"irc\","
                    + "\"userid\":\"@bob:example.org\",\"future\":1}]"));
    assertThat(users.users())
        .singleElement()
        .satisfies(
            user -> {
              assertThat(user.userId()).isEqualTo(UserId.of("@bob:example.org"));
              assertThat(user.protocol()).isEqualTo("irc");
              assertThat(user.raw().asObject().get("future").asLong()).isEqualTo(1);
            });
  }

  @Test
  void allowsEmptyProtocolMapsAndLookupArrays() {
    assertThat(ThirdPartyProtocols.from(JsonParser.parse("{}")).protocols()).isEmpty();
    assertThat(ThirdPartyLocations.from(JsonParser.parse("[]")).locations()).isEmpty();
    assertThat(ThirdPartyUsers.from(JsonParser.parse("[]")).users()).isEmpty();
  }

  @Test
  void rejectsMalformedProtocolLocationAndUserResponses() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ThirdPartyProtocols.from(JsonParser.parse("[]")))
        .withMessage("third-party protocols must be a JSON object");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ThirdPartyProtocol.from(JsonParser.parse("{}")))
        .withMessage("third-party protocol must contain field_types object");
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                ThirdPartyProtocol.from(
                    JsonParser.parse(
                        "{\"field_types\":{},\"icon\":\"mxc://hs/id\","
                            + "\"instances\":[],\"location_fields\":[],\"user_fields\":[1]}")))
        .withMessage("third-party protocol user_fields must contain strings");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ThirdPartyLocations.from(JsonParser.parse("{}")))
        .withMessage("third-party locations must be a JSON array");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ThirdPartyLocations.from(JsonParser.parse("[{}]")))
        .withMessage("third-party location must contain fields object");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ThirdPartyUsers.from(JsonParser.parse("[{}]")))
        .withMessage("third-party user must contain fields object");
  }

  private static String protocolMapJson() {
    return "{\"irc\":{\"field_types\":{\"channel\":{\"placeholder\":\"#matrix\","
        + "\"regexp\":\"#[^ ]+\"}},\"icon\":\"mxc://example.org/icon\","
        + "\"instances\":[{\"desc\":\"IRC network\",\"fields\":{},"
        + "\"instance_id\":\"irc-freenode\",\"network_id\":\"freenode\","
        + "\"future_instance\":true}],\"location_fields\":[\"channel\"],"
        + "\"user_fields\":[\"nickname\"],\"future_protocol\":true}}";
  }
}
