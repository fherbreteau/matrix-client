package io.github.fherbreteau.matrix.error;

import io.github.fherbreteau.matrix.json.JsonParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MatrixExceptionTest {

    @Test
    void carriesErrcodeAndMessage() {
        MatrixException exception = new MatrixException("M_LIMIT_EXCEEDED", "Too many requests");
        assertThat(exception.getErrcode()).isEqualTo("M_LIMIT_EXCEEDED");
        assertThat(exception.getMessage()).isEqualTo("Too many requests");
    }

    @Test
    void carriesCause() {
        var cause = new RuntimeException("root");
        MatrixException exception = new MatrixException("M_UNKNOWN", "failed", cause);
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void parsesMatrixErrorBody() {
        var exception = MatrixServerException.fromResponse(429,
                JsonParser.parse("{\"errcode\":\"M_LIMIT_EXCEEDED\",\"error\":\"Rate limited\"}"));
        assertThat(exception.getStatusCode()).isEqualTo(429);
        assertThat(exception.getErrcode()).isEqualTo("M_LIMIT_EXCEEDED");
        assertThat(exception.getMessage()).isEqualTo("Rate limited");
    }

    @Test
    void fallsBackOnNonJsonBody() {
        var exception = MatrixServerException.fromResponse(502, null);
        assertThat(exception.getStatusCode()).isEqualTo(502);
        assertThat(exception.getErrcode()).isEqualTo("M_UNRECOGNIZED");
        assertThat(exception.getMessage()).isEqualTo("HTTP 502");
    }

    @Test
    void fallsBackOnNonObjectBody() {
        var exception = MatrixServerException.fromResponse(500, JsonParser.parse("[]"));
        assertThat(exception.getErrcode()).isEqualTo("M_UNRECOGNIZED");
        assertThat(exception.getMessage()).isEqualTo("HTTP 500");
    }

    @Test
    void fallsBackOnPartialBody() {
        var exception = MatrixServerException.fromResponse(400, JsonParser.parse("{\"error\":\"bad\"}"));
        assertThat(exception.getErrcode()).isEqualTo("M_UNRECOGNIZED");
        assertThat(exception.getMessage()).isEqualTo("bad");
    }

    @Test
    void baseExceptionWithoutErrcode() {
        MatrixException exception = new MatrixServerException(400, null, "oops");
        assertThat(exception.getErrcode()).isNull();
        assertThat(exception.getMessage()).isEqualTo("oops");
    }
}
