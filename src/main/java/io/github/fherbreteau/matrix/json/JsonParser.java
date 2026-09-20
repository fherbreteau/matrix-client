package io.github.fherbreteau.matrix.json;

/**
 * A minimal, dependency-free JSON parser producing immutable {@link JsonValue} trees.
 */
public final class JsonParser {

    private final String input;
    private int pos;

    private JsonParser(String input) {
        this.input = input;
    }

    public static JsonValue parse(String input) {
        var parser = new JsonParser(input);
        parser.skipWhitespace();
        JsonValue value = parser.parseValue();
        parser.skipWhitespace();
        if (parser.pos != input.length()) {
            throw parser.error("Unexpected trailing content");
        }
        return value;
    }

    private JsonValue parseValue() {
        if (pos >= input.length()) {
            throw error("Unexpected end of input");
        }
        return switch (input.charAt(pos)) {
            case '{' -> parseObject();
            case '[' -> parseArray();
            case '"' -> JsonString.of(parseString());
            case 't' -> parseLiteral("true", JsonBoolean.TRUE);
            case 'f' -> parseLiteral("false", JsonBoolean.FALSE);
            case 'n' -> parseLiteral("null", JsonNull.INSTANCE);
            default -> parseNumber();
        };
    }

    private JsonObject parseObject() {
        var obj = new JsonObject();
        pos++; // {
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            return obj;
        }
        while (true) {
            skipWhitespace();
            if (peek() != '"') {
                throw error("Expected object key");
            }
            String key = parseString();
            skipWhitespace();
            if (peek() != ':') {
                throw error("Expected ':'");
            }
            pos++;
            skipWhitespace();
            obj.put(key, parseValue());
            skipWhitespace();
            char c = peek();
            if (c == ',') {
                pos++;
            } else if (c == '}') {
                pos++;
                return obj;
            } else {
                throw error("Expected ',' or '}'");
            }
        }
    }

    private JsonArray parseArray() {
        var arr = new JsonArray();
        pos++; // [
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            return arr;
        }
        while (true) {
            skipWhitespace();
            arr.add(parseValue());
            skipWhitespace();
            char c = peek();
            if (c == ',') {
                pos++;
            } else if (c == ']') {
                pos++;
                return arr;
            } else {
                throw error("Expected ',' or ']'");
            }
        }
    }

    private String parseString() {
        pos++; // opening quote
        var sb = new StringBuilder();
        while (true) {
            if (pos >= input.length()) {
                throw error("Unterminated string");
            }
            char c = input.charAt(pos++);
            if (c == '"') {
                return sb.toString();
            }
            if (c == '\\') {
                if (pos >= input.length()) {
                    throw error("Unterminated escape sequence");
                }
                char e = input.charAt(pos++);
                switch (e) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> sb.append(parseUnicodeEscape());
                    default -> throw error("Invalid escape character: " + e);
                }
            } else {
                sb.append(c);
            }
        }
    }

    private char parseUnicodeEscape() {
        if (pos + 4 > input.length()) {
            throw error("Invalid unicode escape");
        }
        String hex = input.substring(pos, pos + 4);
        pos += 4;
        try {
            return (char) Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            throw error("Invalid unicode escape: " + hex);
        }
    }

    private JsonValue parseNumber() {
        int start = pos;
        if (peek() == '-') {
            pos++;
        }
        while (pos < input.length() && "0123456789.eE+-".indexOf(input.charAt(pos)) >= 0) {
            pos++;
        }
        try {
            return JsonNumber.of(Double.parseDouble(input.substring(start, pos)));
        } catch (NumberFormatException e) {
            throw error("Invalid number");
        }
    }

    private JsonValue parseLiteral(String literal, JsonValue value) {
        if (input.startsWith(literal, pos)) {
            pos += literal.length();
            return value;
        }
        throw error("Invalid literal");
    }

    private char peek() {
        if (pos >= input.length()) {
            throw error("Unexpected end of input");
        }
        return input.charAt(pos);
    }

    private void skipWhitespace() {
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
            pos++;
        }
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException(message + " at position " + pos);
    }
}
