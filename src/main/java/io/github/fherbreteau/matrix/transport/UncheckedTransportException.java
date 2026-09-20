package io.github.fherbreteau.matrix.transport;

/**
 * Thrown when the HTTP layer fails to complete a request (connection error,
 * DNS failure, ...). This is generally a retryable failure.
 */
public class UncheckedTransportException extends TransportException {

    public UncheckedTransportException(String message, Throwable cause) {
        super(message, cause);
    }
}
