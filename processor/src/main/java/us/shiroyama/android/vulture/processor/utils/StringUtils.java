package us.shiroyama.android.vulture.processor.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Fumihiko Shiroyama
 */

public class StringUtils {
    public static String camelToSnake(String from) {
        if (from == null) {
            throw new IllegalArgumentException("from is null");
        }

        List<String> chunks = new ArrayList<>();
        int lastUpperCase = 0;
        for (int i = 0; i < from.toCharArray().length; i++) {
            char c = from.charAt(i);
            if (Character.isUpperCase(c)) {
                chunks.add(from.substring(lastUpperCase, i).toLowerCase());
                lastUpperCase = i;
            }
            if (i == from.toCharArray().length -1) {
                chunks.add(from.substring(lastUpperCase).toLowerCase());
            }
        }

        return String.join("_", chunks);
    }
}
