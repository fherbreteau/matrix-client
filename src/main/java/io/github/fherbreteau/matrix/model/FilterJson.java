package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonArray;
import io.github.fherbreteau.matrix.json.JsonString;
import java.util.List;

interface FilterJson {

  static JsonArray strings(List<String> values) {
    var array = new JsonArray();
    for (String value : values) {
      array.add(JsonString.of(value));
    }
    return array;
  }
}
