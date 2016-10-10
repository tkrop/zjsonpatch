package org.zalando.zjsonpatch;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON path helper.
 */
abstract class PathHelper {
    /**
     * Convert patch application specific path array to path string.
     * 
     * @param path patch application specific path array.
     * 
     * @return path string.
     */
    public static String toString(List<String> path) {
        StringBuilder builder = new StringBuilder();
        int size = path.size();
        for (int index = 0; index < size; index++) {
            if (index != 0) {
                builder.append('/');
            }
            builder.append(encodeSubPath(path.get(index)));
        }
        return builder.toString();
    }

    /**
     * Convert given path string into patch specific path array.
     * 
     * @param path path string.
     * 
     * @return patch specific path array.
     */
    public static List<String> getPath(String path) {
        List<String> paths = new ArrayList<String>();
        int index = 0, last = 0, len = path.length();
        while (index < len) {
            if (path.charAt(index) == '/') {
                paths.add(decodeSubPath(path.substring(last, index)));
                last = ++index;
            } else {
                index++;
            }
        }
        paths.add(decodeSubPath(path.substring(last, index)));
        return paths;
    }

    /**
     * Extend given patch application specific path array with given path segment key.
     * 
     * @param path patch application specific path array.
     * @param key path segment key.
     * 
     * @return extended patch specific path array.
     */
    @SuppressWarnings("unchecked")
    public static List<Object> getPathExt(List<Object> path, Object key) {
        List<Object> ext = (List<Object>) ((ArrayList<Object>) path).clone();
        ext.add(key);
        return ext;
    }

    /**
     * Convert given patch creation specific path array into path string.
     * 
     * @param path patch creation specific path array.
     * 
     * @return path string.
     */
    public static String getPathRep(List<?> path) {
        StringBuilder builder = new StringBuilder();
        for (Object elem : path) {
            builder.append('/').append(encodeSubPath(elem.toString()));
        }
        return builder.toString();
    }

    /**
     * Decode given sub-path segment.
     * 
     * @param path sub-path segment.
     * 
     * @return decoded sub-path segment.
     */
    private static String decodeSubPath(String path) {
        // see http://tools.ietf.org/html/rfc6901#section-4
        return path.replaceAll("~1", "/").replaceAll("~0", "~");
    }

    /**
     * Encode given sub-path segment.
     * 
     * @param path sub-path segment.
     * 
     * @return encoded sub-path segment.
     */
    private static String encodeSubPath(String path) {
        // see http://tools.ietf.org/html/rfc6901#section-4
        return path.replaceAll("~", "~0").replaceAll("/", "~1");
    }
}
