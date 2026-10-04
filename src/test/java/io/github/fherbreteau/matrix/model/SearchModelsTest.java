package io.github.fherbreteau.matrix.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.fherbreteau.matrix.json.JsonParser;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SearchModelsTest {
  @Test
  void usesDefaultEventContext() {
    assertThat(SearchEventContext.defaults())
        .extracting(
            SearchEventContext::beforeLimit,
            SearchEventContext::afterLimit,
            SearchEventContext::includeProfile)
        .containsExactly(5, 5, false);
  }

  @Test
  void requestSerializesCriteriaAndOptionalFields() {
    var criteria =
        new RoomEventsSearchCriteria(
            "hello",
            List.of("content.body"),
            "recent",
            true,
            RoomEventFilter.builder().rooms(List.of(RoomId.of("!room:example.org"))).build(),
            new SearchGrouping(List.of("room_id", "sender")),
            new SearchEventContext(2, 3, true));
    var request = new SearchRequest(criteria, "cursor /+==");
    var json = request.toJson().asObject();
    var categories = json.get("search_categories").asObject();
    var roomEvents = categories.get("room_events").asObject();
    assertThat(roomEvents.get("search_term").asString()).isEqualTo("hello");
    assertThat(roomEvents.get("keys").asArray().get(0).asString()).isEqualTo("content.body");
    assertThat(roomEvents.get("event_context").asObject().get("before_limit").asLong())
        .isEqualTo(2);
    assertThat(request.toQuery().get("next_batch").asString()).isEqualTo("cursor /+==");
    assertThat(
            new SearchRequest(
                    new RoomEventsSearchCriteria("x", null, null, null, null, null, null), null)
                .toJson()
                .toJson())
        .isEqualTo("{\"search_categories\":{\"room_events\":{\"search_term\":\"x\"}}}");
  }

  @Test
  void parsesSearchResultsContextGroupsAndOpaqueTokens() {
    var response =
        SearchResponse.from(
            JsonParser.parse(
                """
                {"search_categories":{"room_events":{"count":1,"next_batch":"opaque-1+/==","highlights":["hello"],"groups":{"room_id":{"!room:example.org":{"next_batch":"inner token","order":0,"results":["0"]}}},"results":[{"rank":0.5,"result":{"event_id":"$event:example.org","sender":"@alice:example.org","type":"m.room.message","origin_server_ts":1,"room_id":"!room:example.org","content":{"body":"hello"},"unknown_event":true},"context":{"start":"start-token","end":"end-token","events_before":[],"events_after":[],"profile_info":{"@alice:example.org":{"displayname":"Alice"}},"extra":"preserved"}}],"state":{"!room:example.org":[]},"future":true}}}
                """));
    var category = response.roomEvents();
    assertThat(category.count()).isEqualTo(1);
    assertThat(category.nextBatch()).isEqualTo("opaque-1+/==");
    assertThat(category.results())
        .singleElement()
        .satisfies(
            result -> {
              assertThat(result.result().eventId()).isEqualTo("$event:example.org");
              assertThat(result.result().raw().asObject().get("unknown_event").asBoolean())
                  .isTrue();
              assertThat(result.context().start()).isEqualTo("start-token");
              assertThat(result.context().raw().asObject().get("extra").asString())
                  .isEqualTo("preserved");
            });
    assertThat(category.groups().get("room_id").get("!room:example.org").nextBatch())
        .isEqualTo("inner token");
    assertThat(category.raw().asObject().get("future").asBoolean()).isTrue();
    assertThat(response.raw().asObject()).isNotNull();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> SearchResponse.from(JsonParser.parse("{}")));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> SearchResult.from(JsonParser.parse("{}")));
  }

  @Test
  void validatesIntegerAndOptionalSearchFields() {
    var roomSummary =
        JsonParser.parse(
            "{\"room_id\":\"!r:s\",\"guest_can_join\":false,\"num_joined_members\":1.5,"
                + "\"world_readable\":true}");
    assertThatIllegalArgumentException().isThrownBy(() -> RoomSummary.from(roomSummary));
    var searchResults = JsonParser.parse("{\"count\":1.5,\"results\":[]}");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> RoomEventsSearchResults.from(searchResults));
    var searchResult =
        JsonParser.parse(
            "{\"result\":{\"event_id\":\"$e\",\"sender\":\"@a:s\",\"type\":\"m.room.message\","
                + "\"origin_server_ts\":1,\"room_id\":\"!r:s\",\"content\":{}},"
                + "\"rank\":\"wrong\"}");
    assertThatIllegalArgumentException().isThrownBy(() -> SearchResult.from(searchResult));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new RoomEventsSearchCriteria(null, null, null, null, null, null, null));
    assertThatIllegalArgumentException().isThrownBy(() -> new SearchEventContext(-1, null, null));
  }

  @Test
  void searchCollectionsAreImmutable() {
    List<String> keys = new ArrayList<>(List.of("content.body"));
    var criteria = new RoomEventsSearchCriteria("x", keys, null, null, null, null, null);
    keys.clear();
    assertThat(criteria.keys()).containsExactly("content.body");
    List<String> criteriaKeys = criteria.keys();
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(criteriaKeys::clear);
    List<String> groupKeys = new ArrayList<>(List.of("sender"));
    var grouping = new SearchGrouping(groupKeys);
    groupKeys.clear();
    assertThat(grouping.groupBy()).containsExactly("sender");
    List<String> groupingKeys = grouping.groupBy();
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(groupingKeys::clear);
  }
}
