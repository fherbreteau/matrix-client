package io.github.fherbreteau.matrix.error;

import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Thrown when the homeserver returns a non-2xx HTTP response.
 */
public class MatrixServerException extends MatrixException {

    private final int statusCode;

    public MatrixServerException(int statusCode, String errcode, String message) {
        super(errcode, message);
        this.statusCode = statusCode;
    }

    public static MatrixServerException fromResponse(int statusCode, JsonValue body) {
        String errcode = null;
        String message = null;
        if (body != null && body.isObject()) {
            var obj = body.asObject();
            var err = obj.get("errcode");
            var msg = obj.get("error");
            if (err != null && err.isString()) {
                errcode = err.asString();
            }
            if (msg != null && msg.isString()) {
                message = msg.asString();
            }
        }
        if (errcode == null) {
            errcode = "M_UNRECOGNIZED";
        }
        if (message == null) {
            message = "HTTP " + statusCode;
        }
        return new MatrixServerException(statusCode, errcode, message);
    }

    public int getStatusCode() {
        return statusCode;
    }
}
