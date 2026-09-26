package io.github.fherbreteau.matrix.model;

/**
 * A Matrix room-event envelope paired with typed or raw content.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#room-event-format">Matrix
 *     specification</a>
 */
public record RegisteredRoomEvent(RoomEvent envelope, EventContent content) {}
