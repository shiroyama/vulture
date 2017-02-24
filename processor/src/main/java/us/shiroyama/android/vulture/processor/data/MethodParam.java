package us.shiroyama.android.vulture.processor.data;

import com.squareup.javapoet.TypeName;

import javax.lang.model.element.VariableElement;

/**
 * @author Fumihiko Shiroyama
 */

public class MethodParam {
    public final String name;

    public final TypeName type;

    public final VariableElement element;

    private MethodParam(String name, TypeName type, VariableElement element) {
        this.name = name;
        this.type = type;
        this.element = element;
    }


    public static MethodParam of(String name, TypeName type, VariableElement element) {
        return new MethodParam(name, type, element);
    }
}
