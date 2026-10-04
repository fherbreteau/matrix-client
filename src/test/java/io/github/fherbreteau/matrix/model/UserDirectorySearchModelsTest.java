package io.github.fherbreteau.matrix.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.fherbreteau.matrix.json.JsonParser;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserDirectorySearchModelsTest {
  @Test
  void requestJsonRoundTripsAndOmitsOptionalLimit() {
    var request = new UserDirectorySearchRequest("Ada", 12);
    assertThat(request.toJson().toJson()).isEqualTo("{\"search_term\":\"Ada\",\"limit\":12}");
    assertThat(new UserDirectorySearchRequest("Ada", null).toJson().toJson())
        .isEqualTo("{\"search_term\":\"Ada\"}");
  }

  @Test
  void parsesUsersAndRetainsUnknownFields() {
    var response =
        UserDirectorySearchResponse.from(
            JsonParser.parse(
                "{\"limited\":false,\"results\":[{\"user_id\":\"@ada:example.org\","
                    + "\"display_name\":\"Ada\",\"avatar_url\":\"mxc://example.org/avatar\","
                    + "\"new_field\":1}],\"extension\":true}"));
    assertThat(response.limited()).isFalse();
    assertThat(response.results())
        .singleElement()
        .satisfies(
            user -> {
              assertThat(user.userId()).isEqualTo(UserId.of("@ada:example.org"));
              assertThat(user.displayName()).isEqualTo("Ada");
              assertThat(user.raw().asObject().get("new_field").asLong()).isEqualTo(1);
            });
    assertThat(response.raw().asObject().get("extension").asBoolean()).isTrue();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> UserDirectorySearchResponse.from(JsonParser.parse("{}")));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new UserDirectorySearchRequest(null, null));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new UserDirectorySearchRequest("alice", 0));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> DirectoryUser.from(JsonParser.parse("{\"user_id\":\"bad\"}")));
  }

  @Test
  void resultsAreImmutable() {
    List<DirectoryUser> users = new ArrayList<>();
    UserDirectorySearchResponse response = new UserDirectorySearchResponse(false, users, null);
    users.add(null);
    assertThat(response.results()).isEmpty();
    List<DirectoryUser> results = response.results();
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> results.add(null));
  }
}
