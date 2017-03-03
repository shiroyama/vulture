package us.shiroyama.android.vulture.processor.data;

import android.annotation.TargetApi;

import com.squareup.javapoet.TypeName;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;

import us.shiroyama.android.vulture.annotations.Serializable;
import us.shiroyama.android.vulture.processor.utils.Pair;
import us.shiroyama.android.vulture.processor.utils.TypeUtils;
import us.shiroyama.android.vulture.serializers.DefaultSerializer;

/**
 * @author Fumihiko Shiroyama
 */

@TargetApi(24)
public class MethodParam {
    public final String name;

    public final TypeName type;

    public final VariableElement element;

    public TypeName serializerType;

    public TypeName serializeFrom;

    public TypeName serializeTo;

    private MethodParam(String name, TypeName type, VariableElement element) {
        this.name = name;
        this.type = type;
        this.element = element;

        Serializable serializable = element.getAnnotation(Serializable.class);
        if (serializable != null) {
            try {
                serializerType = TypeName.get(serializable.serializer());

                Pair<TypeName, TypeName> serializeDeserializeTypes = TypeUtils.getSerializeTypes(serializable.serializer());
                serializeFrom = serializeDeserializeTypes.first;
                serializeTo = serializeDeserializeTypes.second;
            } catch (MirroredTypeException e) {
                DeclaredType declaredType = (DeclaredType) e.getTypeMirror();
                serializerType = TypeName.get(declaredType);

                TypeElement typeElement = (TypeElement) declaredType.asElement();
                Pair<TypeName, TypeName> serializeDeserializeTypes = TypeUtils.getSerializeTypes(typeElement);
                serializeFrom = serializeDeserializeTypes.first;
                serializeTo = serializeDeserializeTypes.second;
            }
        }
    }

    public boolean hasSerializer() {
        return serializerType != null && !TypeName.get(DefaultSerializer.class).equals(serializerType);
    }

    public static MethodParam of(String name, TypeName type, VariableElement element) {
        return new MethodParam(name, type, element);
    }
}
