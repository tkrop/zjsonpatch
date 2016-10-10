package org.zalando.zjsonpatch;

/**
 * JSON Patch exception.
 */
public class JsonPatchException extends RuntimeException {
    /**
     * Create JSON Patch exception with given exception failure message.
     * 
     * @param message exception failure message.
     */
    public JsonPatchException(String message) {
        super(message);
    }

    /**
     * Create JSON Patch exception with given exception failure message and exception failure cause.
     * 
     * @param message exception failure message.
     * @param cause exception failure cause.
     */
    public JsonPatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
