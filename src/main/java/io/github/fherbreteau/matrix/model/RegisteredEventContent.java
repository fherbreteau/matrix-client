package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Content returned by a custom event parser that does not implement {@link EventContent}.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#room-event-format">Matrix
 *     specification</a>
 */
public record RegisteredEventContent(String eventType, Object value, JsonValue raw)
    implements EventContent {}
