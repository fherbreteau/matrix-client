package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.Map;

/**
 * An object that is or can be signed.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3keyssignaturesupload">Matrix
 *     specification</a>
 */
public sealed interface SignedObject extends Serializable
    permits CrossSigningKey, DeviceInformation, KeysUploadRequest.SignedKey {

  /**
   * Extract the signatures of the object.
   *
   * @return Signatures of the object
   */
  Map<UserId, Map<String, String>> signatures();

  /**
   * Generate the unsigned version of the object.
   *
   * @return the JsonValue
   */
  JsonValue toUnsignedJson();

  /**
   * Return a new instance of Signed Object with the attached signatures.
   *
   * @param signatures the signatures to attach to the signed object
   * @return Return a new instance of Signed Object
   */
  SignedObject withSignatures(Map<UserId, Map<String, String>> signatures);
}
