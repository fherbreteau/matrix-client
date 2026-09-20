package io.github.fherbreteau.matrix.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.fherbreteau.matrix.error.DiscoveryException;
import io.github.fherbreteau.matrix.json.JsonParser;
import io.github.fherbreteau.matrix.json.JsonValue;
import org.junit.jupiter.api.Test;

class MatrixVersionsTest {

    @Test
    void parsesVersionsAndUnknownFields() {
        var versions = MatrixVersions.from(JsonParser.parse("""
            {"versions":["v1.5","v1.11"],"unstable_features":{"new_feature":true},"other":42}
            """));
        assertThat(versions.getVersions()).containsExactly("v1.5", "v1.11");
        assertThat(versions.supports("v1.11")).isTrue();
        assertThat(versions.supports("v9.99")).isFalse();
        assertThat(versions.getFields())
                .containsKey("unstable_features")
                .containsKey("other")
                .doesNotContainKey("versions");
    }

    @Test
    void rejectsNullBody() {
        assertThatThrownBy(() -> MatrixVersions.from(null))
                .isInstanceOf(DiscoveryException.class)
                .hasMessageContaining("JSON object");
    }

    @Test
    void rejectsInvalidBodies() {
        record Case(String body, String message) {
        }
        var cases = new Case[] {
            new Case("[]", "JSON object"),
            new Case("{}", "versions array"),
            new Case("{\"versions\":\"v1.11\"}", "versions array"),
            new Case("{\"versions\":[\"v1.11\",42]}", "only strings"),
            new Case("{\"versions\":[null]}", "only strings")
        };
        for (Case c : cases) {
            JsonValue body = JsonParser.parse(c.body());
            assertThatThrownBy(() -> MatrixVersions.from(body))
                    .as("body: %s", c.body())
                    .isInstanceOf(DiscoveryException.class)
                    .hasMessageContaining(c.message());
        }
    }

    @Test
    void supportsEmptyVersions() {
        var versions = MatrixVersions.from(JsonParser.parse("{\"versions\":[]}"));
        assertThat(versions.getVersions()).isEmpty();
        assertThat(versions.supports("v1.11")).isFalse();
    }
}
