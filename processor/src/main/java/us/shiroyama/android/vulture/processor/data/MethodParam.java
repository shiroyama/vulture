package us.shiroyama.android.vulture.processor.data;

import com.squareup.javapoet.TypeName;

/**
 * @author Fumihiko Shiroyama
 */

public class MethodParam {
    public final String name;

    public final TypeName type;

    public MethodParam(String name, TypeName type) {
        this.name = name;
        this.type = type;
    }

    public static MethodParam of(String name, TypeName type) {
        return new MethodParam(name, type);
    }
}
