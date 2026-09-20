package io.github.fherbreteau.matrix.transport;

/**
 * Thrown when the thread waiting for an HTTP response is interrupted. The
 * interrupt flag of the current thread is restored before this exception is
 * thrown.
 */
public final class TransportInterruptedException extends TransportException {

    public TransportInterruptedException(String message, Throwable cause) {
        super(message, cause);
    }
}
