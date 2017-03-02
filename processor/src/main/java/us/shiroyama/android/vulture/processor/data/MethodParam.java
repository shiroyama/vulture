package us.shiroyama.android.vulture.processor.data;

import com.squareup.javapoet.TypeName;

import javax.lang.model.element.VariableElement;

import us.shiroyama.android.vulture.annotations.Serializable;

/**
 * @author Fumihiko Shiroyama
 */

public class MethodParam {
    public final String name;

    public final TypeName type;

    public final VariableElement element;

    public Serializable serializable;

    private MethodParam(String name, TypeName type, VariableElement element) {
        this.name = name;
        this.type = type;
        this.element = element;

        Serializable serializable = element.getAnnotation(Serializable.class);
        if (serializable != null) {
            this.serializable = serializable;
        }
    }

    public static MethodParam of(String name, TypeName type, VariableElement element) {
        return new MethodParam(name, type, element);
    }
}
