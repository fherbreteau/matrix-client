package io.github.fherbreteau.matrix.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class ImmutableUtils {
  private ImmutableUtils() {}

  @SuppressWarnings("unchecked")
  private static <T> T immutableValue(T value) {
    if (value instanceof Map<?, ?> map) {
      return (T) immutableMap(map);
    }
    if (value instanceof List<?> list) {
      return (T) immutableList(list);
    }
    if (value instanceof String
        || value instanceof Boolean
        || value instanceof Number
        || value == null) {
      return value;
    }
    if (value.getClass().isRecord()) {
      return value;
    }
    throw new IllegalArgumentException("unsupported typed extension value");
  }

  static <K, V> Map<K, V> immutableMap(Map<K, V> source) {
    if (source == null) {
      return Map.of();
    }
    var result = new LinkedHashMap<K, V>();
    source.forEach((key, value) -> result.put(key, immutableValue(value)));
    return Collections.unmodifiableMap(result);
  }

  static <V> List<V> immutableList(List<V> source) {
    if (source == null) {
      return List.of();
    }
    return source.stream().map(ImmutableUtils::immutableValue).toList();
  }
}
