package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.error.DiscoveryException;
import io.github.fherbreteau.matrix.json.JsonParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void rejectsNonObjectBody() {
        assertThatThrownBy(() -> MatrixVersions.from(JsonParser.parse("[]")))
                .isInstanceOf(DiscoveryException.class)
                .hasMessageContaining("JSON object");
    }

    @Test
    void rejectsMissingVersions() {
        assertThatThrownBy(() -> MatrixVersions.from(JsonParser.parse("{}")))
                .isInstanceOf(DiscoveryException.class)
                .hasMessageContaining("versions array");
    }

    @Test
    void rejectsNonArrayVersions() {
        assertThatThrownBy(() -> MatrixVersions.from(JsonParser.parse("{\"versions\":\"v1.11\"}")))
                .isInstanceOf(DiscoveryException.class)
                .hasMessageContaining("versions array");
    }

    @Test
    void rejectsNonStringVersionEntries() {
        assertThatThrownBy(() -> MatrixVersions.from(JsonParser.parse("{\"versions\":[\"v1.11\",42]}")))
                .isInstanceOf(DiscoveryException.class)
                .hasMessageContaining("only strings");
    }

    @Test
    void rejectsNullVersionEntries() {
        assertThatThrownBy(() -> MatrixVersions.from(JsonParser.parse("{\"versions\":[null]}")))
                .isInstanceOf(DiscoveryException.class)
                .hasMessageContaining("only strings");
    }

    @Test
    void supportsEmptyVersions() {
        var versions = MatrixVersions.from(JsonParser.parse("{\"versions\":[]}"));
        assertThat(versions.getVersions()).isEmpty();
        assertThat(versions.supports("v1.11")).isFalse();
    }
}
