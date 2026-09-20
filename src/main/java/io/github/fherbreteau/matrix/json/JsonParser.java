package io.github.fherbreteau.matrix.json;

import java.math.BigDecimal;

/**
 * A minimal, dependency-free JSON parser producing {@link JsonValue} trees. Parsing follows RFC
 * 8259: strict number grammar, escape sequences (including surrogate pairs), and clear {@link
 * JsonParseException}s carrying the position of the offending character.
 */
public final class JsonParser {

  private final String input;
  private int pos;

  private JsonParser(String input) {
    if (input == null) {
      throw new JsonParseException("input must not be null", -1);
    }
    this.input = input;
  }

  /** Parses a complete JSON document (a single JSON value, optionally surrounded by whitespace). */
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
      case '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> parseNumber();
      default -> throw error("Unexpected character '" + input.charAt(pos) + "'");
    };
  }

  private JsonObject parseObject() {
    var obj = new JsonObject();
    pos++;
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
      final String key = parseString();
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
    pos++;
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
    pos++;
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
          case 'u' -> appendUnicodeEscape(sb);
          default -> throw error("Invalid escape character: " + e);
        }
      } else {
        sb.append(c);
      }
    }
  }

  private void appendUnicodeEscape(StringBuilder sb) {
    char first = parseHexEscape();
    if (Character.isHighSurrogate(first)) {
      if (pos + 1 < input.length() && input.charAt(pos) == '\\' && input.charAt(pos + 1) == 'u') {
        pos += 2;
        char second = parseHexEscape();
        if (!Character.isLowSurrogate(second)) {
          throw error("Unpaired high surrogate in unicode escape");
        }
        sb.append(first).append(second);
      } else {
        throw error("Unpaired high surrogate in unicode escape");
      }
    } else if (Character.isLowSurrogate(first)) {
      throw error("Unpaired low surrogate in unicode escape");
    } else {
      sb.append(first);
    }
  }

  private char parseHexEscape() {
    if (pos + 4 > input.length()) {
      throw error("Invalid unicode escape");
    }
    String hex = input.substring(pos, pos + 4);
    pos += 4;
    try {
      return (char) Integer.parseUnsignedInt(hex, 16);
    } catch (NumberFormatException _) {
      throw error("Invalid unicode escape: " + hex);
    }
  }

  private JsonValue parseNumber() {
    final int start = pos;
    if (peek() == '-') {
      pos++;
    }
    parseIntegerPart();
    parseFractionPart();
    parseExponentPart();
    try {
      return JsonNumber.of(new BigDecimal(input.substring(start, pos)));
    } catch (NumberFormatException _) {
      throw error("Invalid number");
    }
  }

  private void parseIntegerPart() {
    if (pos >= input.length() || !isDigit(input.charAt(pos))) {
      throw error("Invalid number: missing digits");
    }
    if (input.charAt(pos) == '0') {
      pos++;
    } else {
      while (pos < input.length() && isDigit(input.charAt(pos))) {
        pos++;
      }
    }
  }

  private boolean parseFractionPart() {
    if (pos < input.length() && input.charAt(pos) == '.') {
      pos++;
      if (pos >= input.length() || !isDigit(input.charAt(pos))) {
        throw error("Invalid number: missing digits after decimal point");
      }
      while (pos < input.length() && isDigit(input.charAt(pos))) {
        pos++;
      }
      return true;
    }
    return false;
  }

  private void parseExponentPart() {
    if (pos < input.length() && (input.charAt(pos) == 'e' || input.charAt(pos) == 'E')) {
      pos++;
      if (pos < input.length() && (input.charAt(pos) == '+' || input.charAt(pos) == '-')) {
        pos++;
      }
      if (pos >= input.length() || !isDigit(input.charAt(pos))) {
        throw error("Invalid number: missing exponent digits");
      }
      while (pos < input.length() && isDigit(input.charAt(pos))) {
        pos++;
      }
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
    while (pos < input.length()
        && (input.charAt(pos) == ' '
            || input.charAt(pos) == '\t'
            || input.charAt(pos) == '\n'
            || input.charAt(pos) == '\r')) {
      pos++;
    }
  }

  private static boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private JsonParseException error(String message) {
    return new JsonParseException(message, pos);
  }
}
