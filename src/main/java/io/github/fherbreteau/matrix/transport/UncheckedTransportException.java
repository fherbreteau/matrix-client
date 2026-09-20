package io.github.fherbreteau.matrix.transport;

/**
 * Thrown when the HTTP layer fails (connection error, DNS failure, ...).
 */
public class UncheckedTransportException extends RuntimeException {

    public UncheckedTransportException(String message, Throwable cause) {
        super(message, cause);
    }
}
