package io.github.fherbreteau.matrix.transport;

import org.junit.jupiter.api.Test;

import java.net.ProxySelector;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        assertThat(config.connectTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(config.requestTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(config.followRedirects()).isFalse();
        assertThat(config.proxy()).isSameAs(proxy);
        assertThat(config.accessToken()).isEqualTo("token");
    }

    @Test
    void defaults() {
        var config = HttpTransportConfig.builder().build();
        assertThat(config.connectTimeout()).isNull();
        assertThat(config.requestTimeout()).isNull();
        assertThat(config.followRedirects()).isTrue();
        assertThat(config.proxy()).isNull();
        assertThat(config.accessToken()).isNull();
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
        assertThat(single.getMessage()).isEqualTo("boom");
        var cause = new RuntimeException("root");
        var withCause = new TransportException("failed", cause);
        assertThat(withCause.getMessage()).isEqualTo("failed");
        assertThat(withCause.getCause()).isSameAs(cause);
        assertThatThrownBy(() -> {
            throw single;
        }).isInstanceOf(RuntimeException.class);
    }
}
