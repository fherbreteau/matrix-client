package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Raw content fallback for built-in events with malformed data and unregistered event types.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#room-event-format">Matrix
 *     specification</a>
 */
public record UnknownEventContent(String eventType, JsonValue raw) implements EventContent {}
