package io.github.fherbreteau.matrix.transport;

/**
 * Base exception for transport-level failures: connection errors, timeouts,
 * interruptions and malformed responses. Callers can distinguish these
 * transport errors from {@link io.github.fherbreteau.matrix.error.MatrixException}
 * hierarchy errors returned by the homeserver.
 */
public class TransportException extends RuntimeException {

    public TransportException(String message) {
        super(message);
    }

    public TransportException(String message, Throwable cause) {
        super(message, cause);
    }
}
