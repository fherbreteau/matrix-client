package io.github.fherbreteau.matrix.transport;

import org.junit.jupiter.api.Test;

import java.net.ProxySelector;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.BOOLEAN;

class TransportConfigTest {

    @Test
    void builderCarriesAllSettings() {
        var proxy = ProxySelector.getDefault();
        var config = HttpTransportConfig.builder()
                .connectTimeout(Duration.ofSeconds(3))
                .requestTimeout(Duration.ofSeconds(10))
                .followRedirects(false)
                .proxy(proxy)
                .accessToken("token")
                .build();
        assertThat(config).extracting(HttpTransportConfig::connectTimeout).isEqualTo(Duration.ofSeconds(3));
        assertThat(config).extracting(HttpTransportConfig::requestTimeout).isEqualTo(Duration.ofSeconds(10));
        assertThat(config).extracting(HttpTransportConfig::followRedirects, BOOLEAN).isFalse();
        assertThat(config).extracting(HttpTransportConfig::proxy).isSameAs(proxy);
        assertThat(config).extracting(HttpTransportConfig::accessToken).isEqualTo("token");
    }

    @Test
    void defaults() {
        var config = HttpTransportConfig.builder().build();
        assertThat(config).extracting(HttpTransportConfig::connectTimeout).isNull();
        assertThat(config).extracting(HttpTransportConfig::requestTimeout).isNull();
        assertThat(config).extracting(HttpTransportConfig::followRedirects, BOOLEAN).isTrue();
        assertThat(config).extracting(HttpTransportConfig::proxy).isNull();
        assertThat(config).extracting(HttpTransportConfig::accessToken).isNull();
    }

    @Test
    void buildsTransportFromConfig() {
        var transport = new JdkHttpTransport(HttpTransportConfig.builder()
                .connectTimeout(Duration.ofSeconds(2))
                .proxy(ProxySelector.getDefault())
                .build());
        assertThat(transport).isNotNull();
    }

    @Test
    void transportExceptionMessages() {
        var single = new TransportException("boom");
        assertThat(single).hasMessage("boom");
        var cause = new RuntimeException("root");
        var withCause = new TransportException("failed", cause);
        assertThat(withCause).hasMessage("failed")
            .hasCause(cause);
        assertThatThrownBy(() -> {
            throw single;
        }).isInstanceOf(RuntimeException.class);
    }
}
