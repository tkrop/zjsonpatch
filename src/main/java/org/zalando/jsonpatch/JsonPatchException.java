package org.zalando.jsonpatch;

/**
 * User: holograph
 * Date: 03/08/16
 */
public class JsonPatchException extends RuntimeException {
    public JsonPatchException(String message) {
        super(message);
    }

    public JsonPatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
