package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonArray;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.ArrayList;
import java.util.List;

/**
 * User-Interactive Authentication challenge returned by a homeserver.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#user-interactive-authentication-api">Matrix
 *     specification</a>
 */
public record UserInteractiveAuthChallenge(
    List<Flow> flows, List<String> completed, JsonValue params, String session, JsonValue raw) {

  /**
   * Creates an immutable challenge.
   *
   * @param flows supported authentication flows
   * @param completed completed authentication stages
   * @param params flow-specific parameters
   * @param session the UI-auth session identifier
   * @param raw the complete challenge response
   */
  public UserInteractiveAuthChallenge {
    flows = List.copyOf(flows);
    completed = List.copyOf(completed);
  }

  /**
   * Parses a UI-auth challenge from the additional fields of a Matrix error.
   *
   * @param value the challenge fields
   * @return the parsed challenge
   */
  public static UserInteractiveAuthChallenge from(JsonValue value) {
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException("UI-auth challenge must be a JSON object");
    }
    JsonObject object = value.asObject();
    List<Flow> flows = new ArrayList<>();
    JsonValue flowsValue = object.get("flows");
    if (flowsValue != null && flowsValue.isArray()) {
      JsonArray array = flowsValue.asArray();
      for (int index = 0; index < array.size(); index++) {
        flows.add(Flow.from(array.get(index)));
      }
    }
    List<String> completed = new ArrayList<>();
    JsonValue completedValue = object.get("completed");
    if (completedValue != null && completedValue.isArray()) {
      JsonArray array = completedValue.asArray();
      for (int index = 0; index < array.size(); index++) {
        JsonValue stage = array.get(index);
        if (stage.isString()) {
          completed.add(stage.asString());
        }
      }
    }
    JsonValue sessionValue = object.get("session");
    return new UserInteractiveAuthChallenge(
        flows,
        completed,
        object.get("params"),
        sessionValue != null && sessionValue.isString() ? sessionValue.asString() : null,
        value);
  }

  /** A supported UI-auth flow. */
  public record Flow(List<String> stages, JsonValue raw) {

    /**
     * Creates an immutable flow.
     *
     * @param stages authentication stages accepted by this flow
     * @param raw the complete flow response
     */
    public Flow {
      stages = List.copyOf(stages);
    }

    private static Flow from(JsonValue value) {
      if (value == null || !value.isObject()) {
        throw new IllegalArgumentException("UI-auth flow must be a JSON object");
      }
      JsonValue stagesValue = value.asObject().get("stages");
      if (stagesValue == null || !stagesValue.isArray()) {
        throw new IllegalArgumentException("UI-auth flow must contain stages");
      }
      List<String> stages = new ArrayList<>();
      JsonArray array = stagesValue.asArray();
      for (int index = 0; index < array.size(); index++) {
        JsonValue stage = array.get(index);
        if (stage.isString()) {
          stages.add(stage.asString());
        }
      }
      return new Flow(stages, value);
    }
  }
}
