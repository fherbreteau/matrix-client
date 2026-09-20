package io.github.fherbreteau.matrix.json;

/**
 * Thrown when a JSON input cannot be parsed. The message carries the
 * position in the input so malformed JSON can be distinguished from valid
 * JSON values.
 */
public class JsonParseException extends IllegalArgumentException {

    private final int position;

    public JsonParseException(String message, int position) {
        super(message + " at position " + position);
        this.position = position;
    }

    public int getPosition() {
        return position;
    }
}
